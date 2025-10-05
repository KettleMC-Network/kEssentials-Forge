package net.kettlemc.kessentialsforge.command;

import com.mojang.brigadier.CommandDispatcher;
import net.kettlemc.kessentialsforge.perm.Perms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public class TeleportPlayerAlias {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var teleportPlayer = dispatcher.register(CommandBuilders.literal("teleportplayer")
            .then(CommandBuilders.argument("from", EntityArgument.player())
                .then(CommandBuilders.argument("to", EntityArgument.player()).executes(ctx -> {
                    if (!Perms.hasOther(ctx.getSource(), "tp", 2)) return 0;
                    ServerPlayer from = EntityArgument.getPlayer(ctx, "from");
                    ServerPlayer to = EntityArgument.getPlayer(ctx, "to");
                    return TeleportCommands.teleport(ctx.getSource(), from, to);
                }))));
        CommandBuilders.registerAliases(dispatcher, teleportPlayer, "tpp");
    }
}
