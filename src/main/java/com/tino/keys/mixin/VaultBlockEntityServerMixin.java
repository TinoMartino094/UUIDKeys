package com.tino.keys.mixin;

import com.tino.keys.VaultSignatureLedger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
import net.minecraft.world.level.block.entity.vault.VaultServerData;
import net.minecraft.world.level.block.entity.vault.VaultState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.UUID;

@Mixin(VaultBlockEntity.Server.class)
public class VaultBlockEntityServerMixin {

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
        return null;
    }

    // Allow any key (combinatorial or vanilla) to still open an INACTIVE vault as
    // long as the player has not yet redeemed that key type.
    //
    // Context: VaultSharedDataMixin removes combinatorial redeemers from
    // connectedPlayers, which causes the vault to go INACTIVE once no other
    // eligible players remain. Without this bypass, the INACTIVE gate would then
    // also block their remaining vanilla key slot.
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "tryInsertKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/vault/VaultBlockEntity$Server;canEjectReward(Lnet/minecraft/world/level/block/entity/vault/VaultConfig;Lnet/minecraft/world/level/block/entity/vault/VaultState;)Z"))
    private static boolean onCanEjectReward(VaultConfig config, VaultState vaultState,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) net.minecraft.world.level.block.entity.vault.VaultServerData serverData,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) net.minecraft.world.entity.player.Player player,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) ItemStack stackToInsert) {
        if (original.call(config, vaultState)) {
            return true;
        }
        if (vaultState == VaultState.INACTIVE && !config.keyItem().isEmpty()) {
            List<UUID> signature = uuidkeys$getSignature(stackToInsert);
            if (signature != null) {
                // Combinatorial key: allow if this specific signature hasn't been redeemed yet
                if (serverData instanceof VaultSignatureLedger ledger
                        && !ledger.uuidkeys$hasRedeemedSignature(player.getUUID(), signature)) {
                    return true;
                }
            } else {
                // Vanilla key: allow if the player hasn't used their vanilla slot yet
                if (!((VaultServerDataInvoker) serverData).invokeHasRewardedPlayer(player)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Allow combinatorial keys (same item type + has our signature) to pass the
    // strict component equality check that vanilla enforces on the key item.
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "tryInsertKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/vault/VaultBlockEntity$Server;isValidToInsert(Lnet/minecraft/world/level/block/entity/vault/VaultConfig;Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean onIsValidToInsert(VaultConfig config, ItemStack stackToInsert,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original) {
        if (original.call(config, stackToInsert)) {
            return true;
        }
        if (stackToInsert.is(config.keyItem().getItem()) && stackToInsert.getCount() >= config.keyItem().getCount()) {
            if (uuidkeys$getSignature(stackToInsert) != null) {
                return true;
            }
        }
        return false;
    }

    // Replace the standard "has this player ever opened it" check with a
    // per-signature check for combinatorial keys, so each unique combination
    // can be redeemed independently.
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "tryInsertKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/vault/VaultServerData;hasRewardedPlayer(Lnet/minecraft/world/entity/player/Player;)Z"))
    private static boolean onCheckRewardedPlayer(VaultServerData instance, Player player,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) ServerLevel serverLevel,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) BlockPos pos,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) ItemStack stackToInsert) {
        List<UUID> signature = uuidkeys$getSignature(stackToInsert);
        if (signature != null) {
            if (instance instanceof VaultSignatureLedger ledger) {
                return ledger.uuidkeys$hasRedeemedSignature(player.getUUID(), signature);
            }
        }
        return original.call(instance, player);
    }

    // When a combinatorial key is used:
    // • Record the signature in our custom ledger.
    // • Skip the vanilla addToRewardedPlayers call so the player's vanilla
    // slot stays open, letting them still use a normal Trial Key later.
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "tryInsertKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/vault/VaultServerData;addToRewardedPlayers(Lnet/minecraft/world/entity/player/Player;)V"))
    private static void onAddToRewardedPlayers(VaultServerData instance, Player player,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) ItemStack stackToInsert) {
        List<UUID> signature = uuidkeys$getSignature(stackToInsert);
        if (signature != null) {
            // Combinatorial key: record in our custom ledger only.
            if (instance instanceof VaultSignatureLedger ledger) {
                ledger.uuidkeys$addRedeemedSignature(player.getUUID(), signature);
            }
            // Skip original — keeps vanilla rewarded_players slot free for normal keys.
        } else {
            // Normal key: use vanilla tracking as usual.
            original.call(instance, player);
        }
    }
}
