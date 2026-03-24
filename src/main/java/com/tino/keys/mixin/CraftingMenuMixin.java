package com.tino.keys.mixin;

import com.tino.keys.CraftingPlayerTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
public class CraftingMenuMixin {

    @Inject(method = "slotChangedCraftingGrid", at = @At("HEAD"))
    private static void onSlotChangedCraftingGrid(net.minecraft.world.inventory.AbstractContainerMenu menu, net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.inventory.CraftingContainer container, net.minecraft.world.inventory.ResultContainer resultSlots, net.minecraft.world.item.crafting.@org.jspecify.annotations.Nullable RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe> recipeHint,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        com.tino.keys.CraftingPlayerTracker.setPlayer(player);
    }

    // We don't clear on RETURN because getRemainingItems is often called immediately after assemble
    // and needs the same player context. The tracker will be overwritten on the next slot change anyway.
}
