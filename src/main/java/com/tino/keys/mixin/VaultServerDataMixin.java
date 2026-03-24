package com.tino.keys.mixin;

import com.tino.keys.VaultSignatureLedger;
import net.minecraft.world.level.block.entity.vault.VaultServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import java.util.Set;
import java.util.UUID;

@Mixin(VaultServerData.class)
public class VaultServerDataMixin implements VaultSignatureLedger {

    @Unique
    private final Map<UUID, Set<List<UUID>>> uuidkeys$playerSignatures = new HashMap<>();

    @Override
    public boolean uuidkeys$hasRedeemedSignature(UUID playerUUID, List<UUID> signature) {
        Set<List<UUID>> sigs = this.uuidkeys$playerSignatures.get(playerUUID);
        return sigs != null && sigs.contains(signature);
    }

    @Override
    public void uuidkeys$addRedeemedSignature(UUID playerUUID, List<UUID> signature) {
        this.uuidkeys$playerSignatures.computeIfAbsent(playerUUID, k -> new HashSet<>())
                .add(new ArrayList<>(signature));
        // Mark dirty so the Vault saves to disk on next tick
        ((VaultServerDataAccessor) this).uuidkeys$setDirty(true);
    }

    @Override
    public Map<UUID, Set<List<UUID>>> uuidkeys$getPlayerSignatures() {
        return this.uuidkeys$playerSignatures;
    }

    @Inject(method = "set", at = @At("TAIL"))
    private void onSet(VaultServerData from, CallbackInfo ci) {
        if (from instanceof VaultSignatureLedger fromLedger) {
            this.uuidkeys$playerSignatures.clear();
            fromLedger.uuidkeys$getPlayerSignatures().forEach((playerId, sigs) -> {
                Set<List<UUID>> newSigs = new HashSet<>();
                for (List<UUID> sig : sigs) {
                    newSigs.add(new ArrayList<>(sig));
                }
                this.uuidkeys$playerSignatures.put(playerId, newSigs);
            });
        }
    }
}
