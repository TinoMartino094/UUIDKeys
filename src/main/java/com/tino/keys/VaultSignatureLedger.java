package com.tino.keys;

import java.util.List;
import java.util.UUID;

public interface VaultSignatureLedger {
    boolean uuidkeys$hasRedeemedSignature(UUID playerUUID, List<UUID> signature);

    void uuidkeys$addRedeemedSignature(UUID playerUUID, List<UUID> signature);

    java.util.Map<UUID, java.util.Set<List<UUID>>> uuidkeys$getPlayerSignatures();
}
