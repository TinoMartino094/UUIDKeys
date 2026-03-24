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
import java.util.List;
import java.util.UUID;

public class KeySplitRecipe extends CustomRecipe {
    public KeySplitRecipe() {
        super();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack key = ItemStack.EMPTY;
        int count = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if ((stack.is(Items.TRIAL_KEY) || stack.is(Items.OMINOUS_TRIAL_KEY)) && stack.getCount() == 1) {
                    key = stack;
                    count++;
                } else {
                    return false;
                }
            }
        }

        if (count != 1) return false;

        List<UUID> signature = getSignature(key);
        boolean result = signature != null && signature.size() >= 2;
        if (count == 1 && !key.isEmpty()) {
            System.out.println("UUIDKeys: Recipe Match check: count=" + count + ", signatureSize=" + (signature != null ? signature.size() : "null") + ", result=" + result);
        }
        return result;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        Player player = com.tino.keys.CraftingPlayerTracker.getPlayer();
        if (player == null) return ItemStack.EMPTY;

        ItemStack original = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            if (!input.getItem(i).isEmpty()) {
                original = input.getItem(i);
                break;
            }
        }

        List<UUID> signature = getSignature(original);
        if (signature == null || !signature.contains(player.getUUID())) return ItemStack.EMPTY;

        ItemStack result = original.copyWithCount(1);
        int max = getOriginalMax(original);
        applySignature(result, Collections.singletonList(player.getUUID()), max, player.level());

        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        
        // We need to find the player again because the Tracker might have been cleared if this is called separate from assemble
        Player player = com.tino.keys.CraftingPlayerTracker.getPlayer();
        if (player == null) return remaining;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && (stack.is(Items.TRIAL_KEY) || stack.is(Items.OMINOUS_TRIAL_KEY))) {
                List<UUID> signature = getSignature(stack);
                if (signature != null && signature.contains(player.getUUID()) && signature.size() > 1) {
                    ItemStack kept = stack.copyWithCount(1);
                    List<UUID> others = new ArrayList<>(signature);
                    others.remove(player.getUUID());
                    applySignature(kept, others, getOriginalMax(stack), player.level());
                    remaining.set(i, kept);
                }
            }
        }
        return remaining;
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
        // In some cases in 26.1, the component system prefers explicit ItemLore wrapping or list-based setting
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
        return com.tino.keys.UUIDKeys.KEY_SPLIT_SERIALIZER;
    }
}
