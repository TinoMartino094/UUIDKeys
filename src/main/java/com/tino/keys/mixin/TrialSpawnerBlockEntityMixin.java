package com.tino.keys.mixin;

import com.tino.keys.TrialSpawnerTracker;

import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mixin(TrialSpawnerBlockEntity.class)
public class TrialSpawnerBlockEntityMixin implements TrialSpawnerTracker {
    @Unique
    private final Set<UUID> uuidkeys$participants = new HashSet<>();

    @Override
    public void uuidkeys$addParticipant(UUID playerUUID) {
        this.uuidkeys$participants.add(playerUUID);
        ((TrialSpawnerBlockEntity) (Object) this).setChanged();
    }

    @Override
    public Set<UUID> uuidkeys$getParticipants() {
        return this.uuidkeys$participants;
    }

    @Override
    public void uuidkeys$clearParticipants() {
        this.uuidkeys$participants.clear();
        ((TrialSpawnerBlockEntity) (Object) this).setChanged();
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void onSaveAdditional(net.minecraft.world.level.storage.ValueOutput output, CallbackInfo ci) {
        if (!this.uuidkeys$participants.isEmpty()) {
            net.minecraft.world.level.storage.ValueOutput.ValueOutputList listOut = output
                    .childrenList("UUIDKeys_Participants");
            for (UUID uuid : this.uuidkeys$participants) {
                net.minecraft.world.level.storage.ValueOutput child = listOut.addChild();
                child.putIntArray("V", net.minecraft.core.UUIDUtil.uuidToIntArray(uuid));
            }
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void onLoadAdditional(net.minecraft.world.level.storage.ValueInput input, CallbackInfo ci) {
        this.uuidkeys$participants.clear();
        net.minecraft.world.level.storage.ValueInput.ValueInputList listIn = input
                .childrenListOrEmpty("UUIDKeys_Participants");
        for (net.minecraft.world.level.storage.ValueInput child : listIn) {
            child.getIntArray("V").ifPresent(
                    arr -> this.uuidkeys$participants.add(net.minecraft.core.UUIDUtil.uuidFromIntArray(arr)));
        }
    }
}
