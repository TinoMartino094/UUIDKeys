package com.tino.keys;

import java.util.Set;
import java.util.UUID;

public interface TrialSpawnerTracker {
    void uuidkeys$addParticipant(UUID playerUUID);

    Set<UUID> uuidkeys$getParticipants();

    void uuidkeys$clearParticipants();
}
