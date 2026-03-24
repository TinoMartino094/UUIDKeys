package com.tino.keys.mixin;

import net.minecraft.world.level.block.entity.vault.VaultSharedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(VaultSharedData.class)
public interface VaultSharedDataAccessor {
    @Accessor("isDirty")
    @Mutable
    void uuidkeys$setDirty(boolean dirty);

    @Accessor("connectedPlayers")
    @Mutable
    void uuidkeys$setConnectedPlayers(java.util.Set<java.util.UUID> players);

    @Accessor("connectedPlayers")
    java.util.Set<java.util.UUID> uuidkeys$getConnectedPlayers();
}
