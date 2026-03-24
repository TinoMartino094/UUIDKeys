package com.tino.keys;

import net.minecraft.world.entity.player.Player;
import java.util.UUID;

public class CraftingPlayerTracker {
    private static final ThreadLocal<Player> CURRENT_PLAYER = new ThreadLocal<>();

    public static void setPlayer(Player player) {
        CURRENT_PLAYER.set(player);
    }

    public static Player getPlayer() {
        return CURRENT_PLAYER.get();
    }

    public static void clear() {
        CURRENT_PLAYER.remove();
    }
}
