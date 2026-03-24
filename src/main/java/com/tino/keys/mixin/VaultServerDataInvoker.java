package com.tino.keys.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.vault.VaultServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(VaultServerData.class)
public interface VaultServerDataInvoker {
    @Invoker("hasRewardedPlayer")
    boolean invokeHasRewardedPlayer(Player player);
}
