
package net.kettlemc.kessentialsforge.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public class FeedCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var feed = dispatcher.register(CommandBuilders.literal("feed")
            .executes(ctx -> {
                if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "feed", 2)) return 0;
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                return feed(ctx.getSource(), player);
            })
            .then(CommandBuilders.argument("target", EntityArgument.player()).executes(ctx -> {
                if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "feed", 2)) return 0;
                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                boolean self = CommandUtil.isSelf(ctx.getSource(), target);
                if (!self && !net.kettlemc.kessentialsforge.perm.Perms.hasOther(ctx.getSource(), "feed", 2)) return 0;
                return feed(ctx.getSource(), target);
            })));
        CommandBuilders.registerAliases(dispatcher, feed, "essen");
    }

    private static int feed(CommandSourceStack source, ServerPlayer target) {
        target.getFoodData().setFoodLevel(20);
        target.getFoodData().setSaturation(20f);

        String message = net.kettlemc.kessentialsforge.i18n.Messages.get(target, "fed");
        target.displayClientMessage(new net.minecraft.network.chat.TextComponent(message), false);

        if (!CommandUtil.isSelf(source, target)) {
            String sourceMessage = CommandUtil.resolveMessage(source, "fed");
            CommandUtil.notifySource(source, target.getGameProfile().getName() + ": " + sourceMessage);
        }
        return 1;
    }
}
