
package net.kettlemc.kessentialsforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.kettlemc.kessentialsforge.perm.Perms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public class TeleportCommands {
    public static void register(CommandDispatcher<CommandSourceStack> d){
        d.register(CommandBuilders.literal("tp").executes(ctx -> {
            CommandUtil.sendUsage(ctx.getSource(), "tp");
            return 0;
        }).then(CommandBuilders.argument("target", EntityArgument.player()).executes(ctx -> {
            if (!Perms.has(ctx.getSource(), "tp", 2)) return 0;
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerPlayer target = EntityArgument.getPlayer(ctx,"target");
            return teleport(ctx.getSource(), player, target);
        })).then(CommandBuilders.argument("from", EntityArgument.player()).then(CommandBuilders.argument("to", EntityArgument.player()).executes(ctx -> {
            if (!Perms.hasOther(ctx.getSource(), "tp", 2)) return 0;
            ServerPlayer from = EntityArgument.getPlayer(ctx,"from");
            ServerPlayer to = EntityArgument.getPlayer(ctx,"to");
            return teleport(ctx.getSource(), from, to);
        }))));
        d.register(CommandBuilders.literal("tphere").executes(ctx -> {
            CommandUtil.sendUsage(ctx.getSource(), "tphere");
            return 0;
        }).then(CommandBuilders.argument("target", EntityArgument.player()).executes(ctx -> {
            if (!Perms.hasOther(ctx.getSource(), "tp", 2)) return 0;
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerPlayer target = EntityArgument.getPlayer(ctx,"target");
            return teleport(ctx.getSource(), target, player);
        })));
        d.register(CommandBuilders.literal("tppos").executes(ctx -> {
            CommandUtil.sendUsage(ctx.getSource(), "tppos");
            return 0;
        }).then(CommandBuilders.argument("x", IntegerArgumentType.integer()).then(CommandBuilders.argument("y", IntegerArgumentType.integer()).then(CommandBuilders.argument("z", IntegerArgumentType.integer()).executes(ctx -> {
            if (!Perms.has(ctx.getSource(), "tp", 2)) return 0;
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            int x = IntegerArgumentType.getInteger(ctx,"x");
            int y = IntegerArgumentType.getInteger(ctx,"y");
            int z = IntegerArgumentType.getInteger(ctx,"z");
            player.teleportTo(player.getLevel(), x + 0.5, y, z + 0.5, player.getYHeadRot(), player.getXRot());
            CommandUtil.notifySourceLocalized(ctx.getSource(), "teleport_done", String.format(Locale.ROOT, "%d %d %d", x, y, z));
            return 1;
        })))));
    }

    static int teleport(CommandSourceStack source, ServerPlayer teleported, ServerPlayer destination) {
        teleported.teleportTo(destination.getLevel(), destination.getX(), destination.getY(), destination.getZ(), destination.getYHeadRot(), destination.getXRot());
        if (CommandUtil.isSelf(source, teleported)) {
            CommandUtil.notifySourceLocalized(source, "teleport_done", destination.getGameProfile().getName());
        } else {
            CommandUtil.notifySourceLocalized(source, "teleport_done_other", teleported.getGameProfile().getName(), destination.getGameProfile().getName());
            CommandUtil.notifySourceLocalized(teleported.createCommandSourceStack(), "teleport_done", destination.getGameProfile().getName());
        }
        return 1;
    }
}
