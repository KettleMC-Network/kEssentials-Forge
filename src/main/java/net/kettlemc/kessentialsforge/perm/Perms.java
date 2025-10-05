
package net.kettlemc.kessentialsforge.perm;

import com.mojang.brigadier.context.CommandContext;
import net.kettlemc.kessentialsforge.KEssentialsForge;
import net.kettlemc.kessentialsforge.service.ConfigService;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

public class Perms {
    private static ConfigService cfg() {
        if (KEssentialsForge.INSTANCE == null) return new ConfigService(java.nio.file.Path.of("config","kessentials"));
        return KEssentialsForge.INSTANCE.config;
    }

    public static boolean has(CommandContext<CommandSourceStack> ctx, String command, int opLevelFallback) {
        return hasNode(ctx, cfg().permission(command), opLevelFallback);
    }

    public static boolean has(CommandSourceStack src, String command, int opLevelFallback) {
        return hasNode(src, cfg().permission(command), opLevelFallback);
    }

    public static boolean hasNode(CommandContext<CommandSourceStack> ctx, String node, int opLevelFallback) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            return checkLuckPerms(player, node);
        } catch (Throwable t) {
            return ctx.getSource().hasPermission(opLevelFallback);
        }
    }

    public static boolean hasNode(CommandSourceStack src, String node, int opLevelFallback) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            return checkLuckPerms(player, node);
        } catch (Throwable t) {
            return src.hasPermission(opLevelFallback);
        }
    }

    public static boolean hasOther(CommandSourceStack src, String command, int opLevelFallback) {
        ConfigService config = cfg();
        return hasNode(src, config.permissionOther(command), opLevelFallback)
            || hasNode(src, config.permissionOthers(command), opLevelFallback);
    }

    public static int resolveHomeLimit(ServerPlayer player) {
        ConfigService config = cfg();
        int limit = config.defaultMaxHomes;
        try {
            LuckPerms lp = LuckPermsProvider.get();
            var adapter = lp.getPlayerAdapter(ServerPlayer.class);
            CachedPermissionData permissions = adapter.getPermissionData(player);
            for (int i = 1; i <= 100; i++) {
                String node = config.permissionHomeLimit(i);
                if (node == null || node.isBlank()) continue;
                if (permissions.checkPermission(node).asBoolean()) {
                    limit = Math.max(limit, i);
                }
            }
            CachedMetaData meta = adapter.getMetaData(player);
            String cap = meta.getMetaValue("kessentials.maxhomes");
            if (cap != null) {
                try {
                    limit = Math.max(limit, Integer.parseInt(cap));
                } catch (NumberFormatException ignored) {}
            }
        } catch (Throwable ignored) {}
        return limit;
    }

    private static boolean checkLuckPerms(ServerPlayer player, String node) {
        if (node == null || node.isBlank()) return false;
        LuckPerms lp = LuckPermsProvider.get();
        return lp.getPlayerAdapter(ServerPlayer.class).getPermissionData(player).checkPermission(node).asBoolean();
    }

    public static boolean has(java.util.UUID uuid, String node) {
        try {
            if (node == null || node.isBlank()) return false;
            LuckPerms lp = LuckPermsProvider.get();
            if (lp == null) return false;
            var userManager = lp.getUserManager();
            var user = userManager.getUser(uuid);
            if (user == null) {
                user = userManager.loadUser(uuid).join();
                if (user == null) return false;
            }
            CachedPermissionData data = user.getCachedData().getPermissionData();
            return data.checkPermission(node).asBoolean();
        } catch (Throwable ignored) {
            return false;
        }
    }

}
