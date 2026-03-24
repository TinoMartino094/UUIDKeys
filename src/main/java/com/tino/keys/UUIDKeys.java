package com.tino.keys;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.codec.StreamCodec;
import com.tino.keys.recipe.KeySplitRecipe;
import com.tino.keys.recipe.KeyMergeRecipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UUIDKeys implements ModInitializer {
	public static final String MOD_ID = "uuidkeys";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static RecipeSerializer<KeySplitRecipe> KEY_SPLIT_SERIALIZER;
	public static RecipeSerializer<KeyMergeRecipe> KEY_MERGE_SERIALIZER;

	@Override
	public void onInitialize() {
		LOGGER.info("UUIDKeys initializing...");

		KEY_SPLIT_SERIALIZER = Registry.register(
				BuiltInRegistries.RECIPE_SERIALIZER,
				Identifier.fromNamespaceAndPath(MOD_ID, "key_split"),
				new RecipeSerializer<>(MapCodec.unit(new KeySplitRecipe()), StreamCodec.unit(new KeySplitRecipe())));

		KEY_MERGE_SERIALIZER = Registry.register(
				BuiltInRegistries.RECIPE_SERIALIZER,
				Identifier.fromNamespaceAndPath(MOD_ID, "key_merge"),
				new RecipeSerializer<>(MapCodec.unit(new KeyMergeRecipe()), StreamCodec.unit(new KeyMergeRecipe())));

		LOGGER.info("UUIDKeys initialized with recipe serializers.");
	}
}