package com.tino.keys.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mixin(TrialSpawner.class)
public class TrialSpawnerMixin {

    @org.spongepowered.asm.mixin.injection.ModifyArg(method = "spawnMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tryAddFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)Z"))
    private net.minecraft.world.entity.Entity onSpawnMobAddEntity(net.minecraft.world.entity.Entity entity,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) BlockPos spawnerPos) {
        if (entity instanceof com.tino.keys.TrialSpawnedEntity tracked) {
            tracked.uuidkeys$setTrialSpawnerPos(spawnerPos);
        }
        return entity;
    }

    @Inject(method = "ejectReward", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/dispenser/DefaultDispenseItemBehavior;spawnItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;ILnet/minecraft/core/Direction;Lnet/minecraft/core/Position;)V"))
    private void onEjectReward(ServerLevel level, BlockPos pos, ResourceKey<LootTable> ejectingLootTable,
            CallbackInfo ci, @com.llamalad7.mixinextras.sugar.Local ItemStack item) {
        if (item.is(Items.TRIAL_KEY) || item.is(Items.OMINOUS_TRIAL_KEY)) {
            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof com.tino.keys.TrialSpawnerTracker tracker) {
                Set<UUID> participants = tracker.uuidkeys$getParticipants();
                if (participants != null && !participants.isEmpty()) {
                    List<UUID> sortedUUIDs = new ArrayList<>(participants);
                    Collections.sort(sortedUUIDs);

                    net.minecraft.world.item.component.CustomData
                            .update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, item, tag -> {
                                net.minecraft.nbt.ListTag listTag = new net.minecraft.nbt.ListTag();
                                for (UUID u : sortedUUIDs) {
                                    listTag.add(new net.minecraft.nbt.IntArrayTag(
                                            net.minecraft.core.UUIDUtil.uuidToIntArray(u)));
                                }
                                tag.put("UUIDKeys_CombinatorialSignature", listTag);
                                // Initialize original max signatures based on current participants
                                tag.putInt("UUIDKeys_OriginalMaxSignatures", sortedUUIDs.size());
                            });

                    List<Component> loreLines = new ArrayList<>();
                    loreLines.add(Component.literal("Obtained by: ")
                            .withStyle(net.minecraft.ChatFormatting.GOLD));
                    for (UUID uuid : sortedUUIDs) {
                        net.minecraft.world.entity.player.Player player = level.getPlayerByUUID(uuid);
                        String name;
                        if (player != null) {
                            name = player.getScoreboardName();
                        } else {
                            name = "Player " + uuid.toString().substring(0, 8);
                        }
                        loreLines.add(Component.literal("- " + name).withStyle(net.minecraft.ChatFormatting.GRAY));
                    }
                    // Since it's newly minted, current == max, so we don't display "Max signatures" yet.
                    item.set(net.minecraft.core.component.DataComponents.LORE, new ItemLore(loreLines));
                }
            }
        }
    }

    @Inject(method = "tickServer", at = @At("TAIL"))
    private void onTickServer(ServerLevel serverLevel, BlockPos spawnerPos, boolean isOminous, CallbackInfo ci) {
        TrialSpawner spawner = (TrialSpawner) (Object) this;
        if (spawner.getState() == TrialSpawnerState.WAITING_FOR_PLAYERS) {
            net.minecraft.world.level.block.entity.BlockEntity be = serverLevel.getBlockEntity(spawnerPos);
            if (be instanceof com.tino.keys.TrialSpawnerTracker tracker) {
                tracker.uuidkeys$clearParticipants();
            }
        }
    }
}
