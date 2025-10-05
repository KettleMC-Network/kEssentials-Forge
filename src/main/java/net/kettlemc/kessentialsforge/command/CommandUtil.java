package net.kettlemc.kessentialsforge.command;

import net.kettlemc.kessentialsforge.i18n.Messages;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

public final class CommandUtil {
    private CommandUtil() {
    }

    public static boolean isSelf(CommandSourceStack source, ServerPlayer target) {
        return source.getEntity() instanceof ServerPlayer player && player.getUUID().equals(target.getUUID());
    }

    public static void notifySource(CommandSourceStack source, Component message) {
        if (source.getEntity() instanceof ServerPlayer player) {
            player.displayClientMessage(message, false);
        } else {
            source.sendSuccess(message, false);
        }
    }

    public static void notifySource(CommandSourceStack source, String message) {
        notifySource(source, new TextComponent(message));
    }

    public static void notifySourceLocalized(CommandSourceStack source, String key, Object... args) {
        if (source.getEntity() instanceof ServerPlayer player) {
            notifySource(source, Messages.get(player, key, args));
        } else {
            notifySource(source, Messages.get(key, args));
        }
    }

    public static void sendUsage(CommandSourceStack source, String messageKey, Object... args) {
        notifySourceLocalized(source, "usage_" + messageKey, args);
    }

    public static String resolveMessage(CommandSourceStack source, String key, Object... args) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return Messages.get(player, key, args);
        }
        return Messages.get(key, args);
    }
}
