
package net.kettlemc.kessentialsforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public class FlyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var fly = dispatcher.register(CommandBuilders.literal("fly")
            .executes(ctx -> {
                if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "fly", 2)) return 0;
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                return toggle(ctx, player);
            })
            .then(CommandBuilders.argument("target", EntityArgument.player()).executes(ctx -> {
                if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "fly", 2)) return 0;
                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                boolean self = CommandUtil.isSelf(ctx.getSource(), target);
                if (!self && !net.kettlemc.kessentialsforge.perm.Perms.hasOther(ctx.getSource(), "fly", 2)) return 0;
                return toggle(ctx, target);
            })));
        CommandBuilders.registerAliases(dispatcher, fly, "flight", "flug", "flugmodus", "fliegen");
    }

    private static int toggle(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        var abilities = target.getAbilities();
        abilities.mayfly = !abilities.mayfly;
        if (!abilities.mayfly) abilities.flying = false;
        target.onUpdateAbilities();

        String message = net.kettlemc.kessentialsforge.i18n.Messages.get(target, abilities.mayfly ? "fly_on" : "fly_off");
        target.displayClientMessage(new net.minecraft.network.chat.TextComponent(message), false);

        if (!CommandUtil.isSelf(ctx.getSource(), target)) {
            String sourceMessage = CommandUtil.resolveMessage(ctx.getSource(), abilities.mayfly ? "fly_on" : "fly_off");
            CommandUtil.notifySource(ctx.getSource(), target.getGameProfile().getName() + ": " + sourceMessage);
        }
        return 1;
    }
}
