package net.kettlemc.kessentialsforge.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.enchanting.EnchantmentLevelSetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class EnchantingTableEvents {
    private static final String LEVEL_TAG = "kessentialsEnchantLevels";
    private static final ThreadLocal<ServerPlayer> ACTIVE_PLAYER = new ThreadLocal<>();

    public static void pushPlayer(ServerPlayer player) {
        ACTIVE_PLAYER.set(player);
    }

    public static void popPlayer() {
        ACTIVE_PLAYER.remove();
    }

    public static void setLevels(ServerPlayer player, int level1, int level2, int level3) {
        CompoundTag data = player.getPersistentData();
        data.putIntArray(LEVEL_TAG, new int[]{level1, level2, level3});
    }

    public static void clearLevels(ServerPlayer player) {
        player.getPersistentData().remove(LEVEL_TAG);
    }

    private static int[] getLevels(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(LEVEL_TAG, Tag.TAG_INT_ARRAY)) {
            return null;
        }
        int[] values = data.getIntArray(LEVEL_TAG);
        return values.length == 3 ? values : null;
    }

    @SubscribeEvent
    public void onEnchantmentLevelSet(EnchantmentLevelSetEvent event) {
        ServerPlayer player = ACTIVE_PLAYER.get();
        if (player == null) {
            return;
        }
        int[] levels = getLevels(player);
        if (levels == null) {
            return;
        }
        int idx = event.getEnchantRow() - 1;
        if (idx < 0 || idx >= levels.length) {
            return;
        }
        event.setLevel(levels[idx]);
    }
}
