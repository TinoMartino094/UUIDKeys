package com.tino.keys.mixin;

import com.tino.keys.VaultSignatureLedger;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Mixin(VaultBlockEntity.class)
public class VaultBlockEntityMixin implements VaultSignatureLedger {

    @org.spongepowered.asm.mixin.Shadow
    private net.minecraft.world.level.block.entity.vault.VaultServerData serverData;

    @Override
    public boolean uuidkeys$hasRedeemedSignature(UUID playerUUID, List<UUID> signature) {
        if (this.serverData instanceof VaultSignatureLedger ledger) {
            return ledger.uuidkeys$hasRedeemedSignature(playerUUID, signature);
        }
        return false;
    }

    @Override
    public void uuidkeys$addRedeemedSignature(UUID playerUUID, List<UUID> signature) {
        if (this.serverData instanceof VaultSignatureLedger ledger) {
            ledger.uuidkeys$addRedeemedSignature(playerUUID, signature);
        }
    }

    @Override
    public Map<UUID, Set<List<UUID>>> uuidkeys$getPlayerSignatures() {
        if (this.serverData instanceof VaultSignatureLedger ledger) {
            return ledger.uuidkeys$getPlayerSignatures();
        }
        return new HashMap<>();
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void onSaveAdditional(ValueOutput output, CallbackInfo ci) {
        if (this.serverData instanceof VaultSignatureLedger ledger) {
            Map<UUID, Set<List<UUID>>> signatures = ledger.uuidkeys$getPlayerSignatures();
            if (!signatures.isEmpty()) {
                net.minecraft.world.level.storage.ValueOutput.ValueOutputList playersList = output
                        .childrenList("UUIDKeys_SignatureLedger");
                for (Map.Entry<UUID, Set<List<UUID>>> entry : signatures.entrySet()) {
                    ValueOutput playerEntryOut = playersList.addChild();
                    playerEntryOut.putIntArray("Player", net.minecraft.core.UUIDUtil.uuidToIntArray(entry.getKey()));

                    net.minecraft.world.level.storage.ValueOutput.ValueOutputList signaturesList = playerEntryOut
                            .childrenList("Signatures");
                    for (List<UUID> sig : entry.getValue()) {
                        ValueOutput sigOut = signaturesList.addChild();
                        net.minecraft.world.level.storage.ValueOutput.ValueOutputList uuidList = sigOut
                                .childrenList("UUIDs");
                        for (UUID u : sig) {
                            ValueOutput uOut = uuidList.addChild();
                            uOut.putIntArray("V", net.minecraft.core.UUIDUtil.uuidToIntArray(u));
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void onLoadAdditional(ValueInput input, CallbackInfo ci) {
        if (this.serverData instanceof VaultSignatureLedger ledger) {

            // Only clear and read if the tag actually exists.
            // This prevents client-sync packets (which lack this tag) from wiping the
            // server-side memory.
            net.minecraft.world.level.storage.ValueInput.ValueInputList playersList = input
                    .childrenListOrEmpty("UUIDKeys_SignatureLedger");

            if (!playersList.isEmpty()) {
                Map<UUID, Set<List<UUID>>> signatures = ledger.uuidkeys$getPlayerSignatures();
                signatures.clear();

                for (ValueInput playerEntryIn : playersList) {
                    Optional<int[]> playerUUIDArray = playerEntryIn.getIntArray("Player");
                    if (playerUUIDArray.isPresent()) {
                        UUID playerUUID = net.minecraft.core.UUIDUtil.uuidFromIntArray(playerUUIDArray.get());
                        Set<List<UUID>> sigs = new HashSet<>();
                        net.minecraft.world.level.storage.ValueInput.ValueInputList signaturesList = playerEntryIn
                                .childrenListOrEmpty("Signatures");
                        for (ValueInput sigIn : signaturesList) {
                            List<UUID> sig = new ArrayList<>();
                            net.minecraft.world.level.storage.ValueInput.ValueInputList uuidList = sigIn
                                    .childrenListOrEmpty("UUIDs");
                            for (ValueInput uIn : uuidList) {
                                int[] arr = uIn.getIntArray("V").orElse(new int[4]);
                                sig.add(net.minecraft.core.UUIDUtil.uuidFromIntArray(arr));
                            }
                            sigs.add(sig);
                        }
                        signatures.put(playerUUID, sigs);
                    }
                }
            }
        }
    }
}
