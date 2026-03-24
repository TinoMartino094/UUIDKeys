package com.tino.keys.mixin;

import com.tino.keys.VaultSignatureLedger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
import net.minecraft.world.level.block.entity.vault.VaultServerData;
import net.minecraft.world.level.block.entity.vault.VaultSharedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Mixin(VaultSharedData.class)
public class VaultSharedDataMixin {

    @org.spongepowered.asm.mixin.Unique
    private static List<UUID> uuidkeys$getSignature(ItemStack stack) {
        net.minecraft.world.item.component.CustomData customData = stack.getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY);
        net.minecraft.nbt.CompoundTag tag = customData.copyTag();
        if (tag.contains("UUIDKeys_CombinatorialSignature")) {
            List<UUID> signature = new java.util.ArrayList<>();
            net.minecraft.nbt.ListTag listTag = tag.getListOrEmpty("UUIDKeys_CombinatorialSignature");
            for (int i = 0; i < listTag.size(); i++) {
                int[] arr = listTag.getIntArray(i).orElse(new int[4]);
                signature.add(net.minecraft.core.UUIDUtil.uuidFromIntArray(arr));
            }
            return signature;
        }
        return null; // Vanilla key (or not a key at all)
    }

    /**
     * Replaces the vanilla connectedPlayers update to accurately reflect who can
     * STILL
     * open this vault. A player is eligible (keeps the vault "ACTIVE") if:
     * 1. They haven't used ANY key on this vault yet.
     * OR
     * 2. They are physically holding a usable key right now (vanilla or
     * combinatorial).
     */
    @Inject(method = "updateConnectedPlayersWithinRange", at = @At("HEAD"), cancellable = true)
    private void onUpdateConnectedPlayers(ServerLevel serverLevel, BlockPos pos,
            VaultServerData serverData, VaultConfig config, double limit, CallbackInfo ci) {

        if (!(serverData instanceof VaultSignatureLedger ledger)) {
            return;
        }

        VaultSharedDataAccessor self = (VaultSharedDataAccessor) this;

        // Find all player UUIDs in range
        List<UUID> playersInRange = config.playerDetector().detect(serverLevel, config.entitySelector(), pos, limit,
                false);

        // Filter down to players who actually can open it right now
        Set<UUID> eligiblePlayers = playersInRange.stream()
                .map(serverLevel::getPlayerByUUID)
                .filter(player -> player != null)
                .filter(player -> {
                    boolean usedVanilla = ((VaultServerDataInvoker) serverData).invokeHasRewardedPlayer(player);
                    boolean usedCombinatorial = !ledger.uuidkeys$getPlayerSignatures()
                            .getOrDefault(player.getUUID(), Set.of()).isEmpty();

                    // If they haven't used ANY key, they are eligible
                    if (!usedVanilla && !usedCombinatorial) {
                        return true;
                    }

                    // They used *some* key. Are they holding a new valid key?
                    ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
                    ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);

                    for (ItemStack stack : new ItemStack[] { mainHand, offHand }) {
                        if (stack.is(config.keyItem().getItem()) && stack.getCount() >= config.keyItem().getCount()) {
                            List<UUID> signature = uuidkeys$getSignature(stack);
                            if (signature != null) {
                                // Holding a Combinatorial key: check if THIS specific signature is unused
                                if (!ledger.uuidkeys$hasRedeemedSignature(player.getUUID(), signature)) {
                                    return true;
                                }
                            } else {
                                // Holding a Vanilla key: check if vanilla slot is unused
                                if (!usedVanilla) {
                                    return true;
                                }
                            }
                        }
                    }

                    return false;
                })
                .map(Player::getUUID)
                .collect(Collectors.toSet());

        // Update if changed
        if (!self.uuidkeys$getConnectedPlayers().equals(eligiblePlayers)) {
            self.uuidkeys$setConnectedPlayers(eligiblePlayers);
            self.uuidkeys$setDirty(true);
        }

        // We completely handled the logic, no need for the vanilla default checks to
        // run
        ci.cancel();
    }
}
