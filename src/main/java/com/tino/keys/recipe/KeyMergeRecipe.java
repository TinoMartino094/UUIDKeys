package com.tino.keys.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class KeyMergeRecipe extends CustomRecipe {
    public KeyMergeRecipe() {
        super();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int count = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if ((stack.is(Items.TRIAL_KEY) || stack.is(Items.OMINOUS_TRIAL_KEY)) && stack.getCount() == 1) {
                    count++;
                } else {
                    return false;
                }
            }
        }
        return count == 2;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        List<ItemStack> keys = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            if (!input.getItem(i).isEmpty()) {
                keys.add(input.getItem(i));
            }
        }

        if (keys.size() != 2) return ItemStack.EMPTY;

        ItemStack k1 = keys.get(0);
        ItemStack k2 = keys.get(1);

        if (!k1.is(k2.getItem())) return ItemStack.EMPTY;

        List<UUID> s1 = getSignature(k1);
        List<UUID> s2 = getSignature(k2);
        
        if (s1 == null || s2 == null) return ItemStack.EMPTY;

        int m1 = getOriginalMax(k1);
        int m2 = getOriginalMax(k2);
        int finalMax = Math.min(m1, m2);

        Set<UUID> mergedSet = new HashSet<>(s1);
        mergedSet.addAll(s2);
        
        if (mergedSet.size() > finalMax) return ItemStack.EMPTY;

        List<UUID> mergedList = new ArrayList<>(mergedSet);
        ItemStack result = k1.copyWithCount(1);
        
        Player player = com.tino.keys.CraftingPlayerTracker.getPlayer();
        Level level = (player != null) ? player.level() : null;
        applySignature(result, mergedList, finalMax, level);

        return result;
    }

    private int getOriginalMax(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        return tag.getIntOr("UUIDKeys_OriginalMaxSignatures", 0);
    }

    private void applySignature(ItemStack stack, List<UUID> uuids, int max, Level level) {
        Collections.sort(uuids);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            ListTag list = new ListTag();
            for (UUID u : uuids) {
                list.add(new net.minecraft.nbt.IntArrayTag(net.minecraft.core.UUIDUtil.uuidToIntArray(u)));
            }
            tag.put("UUIDKeys_CombinatorialSignature", list);
            tag.putInt("UUIDKeys_OriginalMaxSignatures", max);
        });

        List<Component> loreLines = new ArrayList<>();
        loreLines.add(Component.literal("Obtained by: ").withStyle(net.minecraft.ChatFormatting.GOLD));
        for (UUID uuid : uuids) {
            String name = null;
            if (level != null && !level.isClientSide() && level.getServer() != null) {
                // Try online player first
                Player p = level.getPlayerByUUID(uuid);
                if (p != null) {
                    name = p.getScoreboardName();
                } else {
                    // Try UserCache (nameToIdCache in this version) for offline names
                    net.minecraft.server.players.UserNameToIdResolver cache = level.getServer().services().nameToIdCache();
                    if (cache != null) {
                        java.util.Optional<net.minecraft.server.players.NameAndId> profile = cache.get(uuid);
                        if (profile.isPresent()) {
                            name = profile.get().name();
                        }
                    }
                }
            }
            if (name == null) {
                name = "Player " + uuid.toString().substring(0, 8);
            }
            loreLines.add(Component.literal("- " + name).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        if (uuids.size() < max) {
            loreLines.add(Component.literal("Max signatures: " + max).withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
        }
        stack.set(DataComponents.LORE, new ItemLore(loreLines));
    }

    private List<UUID> getSignature(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag.contains("UUIDKeys_CombinatorialSignature")) {
            List<UUID> signature = new ArrayList<>();
            ListTag list = tag.getListOrEmpty("UUIDKeys_CombinatorialSignature");
            for (int i = 0; i < list.size(); i++) {
                int[] intArray = list.getIntArray(i).orElse(new int[4]);
                signature.add(net.minecraft.core.UUIDUtil.uuidFromIntArray(intArray));
            }
            return signature;
        }
        return null;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return com.tino.keys.UUIDKeys.KEY_MERGE_SERIALIZER;
    }
}
