package com.tino.keys.mixin;

import net.minecraft.world.level.block.entity.vault.VaultServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(VaultServerData.class)
public interface VaultServerDataAccessor {
    @Accessor("isDirty")
    @Mutable
    void uuidkeys$setDirty(boolean dirty);
}
