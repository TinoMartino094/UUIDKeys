package com.tino.keys.mixin;

import com.tino.keys.TrialSpawnedEntity;
import net.minecraft.core.BlockPos;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements TrialSpawnedEntity {

    @Unique
    private BlockPos uuidkeys$trialSpawnerPos = null;

    @Override
    public BlockPos uuidkeys$getTrialSpawnerPos() {
        return this.uuidkeys$trialSpawnerPos;
    }

    @Override
    public void uuidkeys$setTrialSpawnerPos(BlockPos pos) {
        this.uuidkeys$trialSpawnerPos = pos;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void saveTrialSpawnerPos(net.minecraft.world.level.storage.ValueOutput output, CallbackInfo ci) {
        if (this.uuidkeys$trialSpawnerPos != null) {
            output.putInt("UUIDKeys_TrialSpawnerPosX", this.uuidkeys$trialSpawnerPos.getX());
            output.putInt("UUIDKeys_TrialSpawnerPosY", this.uuidkeys$trialSpawnerPos.getY());
            output.putInt("UUIDKeys_TrialSpawnerPosZ", this.uuidkeys$trialSpawnerPos.getZ());
            output.putBoolean("UUIDKeys_HasTrialSpawnerPos", true);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void loadTrialSpawnerPos(net.minecraft.world.level.storage.ValueInput input, CallbackInfo ci) {
        if (input.getBooleanOr("UUIDKeys_HasTrialSpawnerPos", false)) {
            this.uuidkeys$trialSpawnerPos = new BlockPos(
                    input.getIntOr("UUIDKeys_TrialSpawnerPosX", 0),
                    input.getIntOr("UUIDKeys_TrialSpawnerPosY", 0),
                    input.getIntOr("UUIDKeys_TrialSpawnerPosZ", 0));
        }
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void onDeathTrackParticipation(DamageSource damageSource, CallbackInfo ci) {
        if (this.uuidkeys$trialSpawnerPos != null && damageSource.getEntity() instanceof Player player) {
            LivingEntity self = (LivingEntity) (Object) this;
            if (self.level() instanceof ServerLevel serverLevel) {
                BlockEntity be = serverLevel.getBlockEntity(this.uuidkeys$trialSpawnerPos);
                if (be instanceof TrialSpawnerBlockEntity spawnerEntity) {
                    ((com.tino.keys.TrialSpawnerTracker) spawnerEntity).uuidkeys$addParticipant(player.getUUID());
                }
            }
        }
    }
}
