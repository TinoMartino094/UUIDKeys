package com.tino.keys.mixin;

import com.tino.keys.VaultSignatureLedger;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
import net.minecraft.world.level.block.entity.vault.VaultServerData;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

@Mixin(VaultBlock.class)
public class VaultBlockMixin {

    @org.spongepowered.asm.mixin.Unique
    private static List<UUID> uuidkeys$getSignature(ItemStack stack) {
        net.minecraft.world.item.component.CustomData customData = stack.getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY);
        net.minecraft.nbt.CompoundTag tag = customData.copyTag();
        if (tag.contains("UUIDKeys_CombinatorialSignature")) {
            List<UUID> signature = new java.util.ArrayList<>();
            tag.getList("UUIDKeys_CombinatorialSignature").ifPresent(listTag -> {
                for (int i = 0; i < listTag.size(); i++) {
                    listTag.getIntArray(i)
                            .ifPresent(arr -> signature.add(net.minecraft.core.UUIDUtil.uuidFromIntArray(arr)));
                }
            });
            return signature;
        }
        return null;
    }

    // VaultBlock#useItemOn explicitly blocks interaction if the state is not
    // ACTIVE.
    // If it's INACTIVE but the player holds a key they can still use, we want to
    // bypass the strict ACTIVE check and process the interaction normally.
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onUseItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir) {

        if (itemStack.isEmpty()) {
            return;
        }

        // Only intervene if vanilla is going to block it due to state
        if (state.getValue(VaultBlock.STATE) != VaultState.ACTIVE) {

            // Interaction logic only runs server-side
            if (!level.isClientSide() && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                BlockEntity blockEntity = serverLevel.getBlockEntity(pos);
                if (blockEntity instanceof VaultBlockEntity vault) {
                    VaultConfig config = vault.getConfig();
                    VaultServerData serverData = vault.getServerData();

                    // Check if it's the right item type
                    if (itemStack.is(config.keyItem().getItem())
                            && itemStack.getCount() >= config.keyItem().getCount()) {

                        boolean canUseKey = false;
                        List<UUID> signature = uuidkeys$getSignature(itemStack);

                        if (signature != null) {
                            // Combinatorial key: check if unused
                            if (serverData instanceof VaultSignatureLedger ledger &&
                                    !ledger.uuidkeys$hasRedeemedSignature(player.getUUID(), signature)) {
                                canUseKey = true;
                            }
                        } else {
                            // Vanilla key: check if unused
                            if (!((VaultServerDataInvoker) serverData).invokeHasRewardedPlayer(player)) {
                                canUseKey = true;
                            }
                        }

                        // If usable, we manually trigger tryInsertKey and return SUCCESS so vanilla
                        // doesn't return TRY_WITH_EMPTY_HAND.
                        if (canUseKey) {
                            VaultBlockEntity.Server.tryInsertKey(serverLevel, pos, state, config, serverData,
                                    vault.getSharedData(), player, itemStack);
                            cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
                        }
                    }
                }
            } else if (level.isClientSide()) {
                // Return success on client so hand swings and interaction isn't blocked
                // client-side
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
        }
    }
}
