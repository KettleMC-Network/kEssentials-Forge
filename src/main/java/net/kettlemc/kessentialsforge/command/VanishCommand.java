
package net.kettlemc.kessentialsforge.command;

import com.mojang.brigadier.CommandDispatcher;
import net.kettlemc.kessentialsforge.event.VanishHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public class VanishCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var vanish = dispatcher.register(CommandBuilders.literal("vanish")
            .executes(ctx -> {
                if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "vanish", 2)) return 0;
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                return toggle(ctx.getSource(), player);
            })
            .then(CommandBuilders.argument("target", EntityArgument.player()).executes(ctx -> {
                if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "vanish", 2)) return 0;
                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                boolean self = CommandUtil.isSelf(ctx.getSource(), target);
                if (!self && !net.kettlemc.kessentialsforge.perm.Perms.hasOther(ctx.getSource(), "vanish", 2)) return 0;
                return toggle(ctx.getSource(), target);
            })));
        CommandBuilders.registerAliases(dispatcher, vanish, "verschwinden", "v");
    }

    private static int toggle(CommandSourceStack source, ServerPlayer target) {
        boolean enabled = net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE.state.toggleVanish(target.getUUID());
        VanishHelper.applyVanish(target, enabled);

        String message = net.kettlemc.kessentialsforge.i18n.Messages.get(target, enabled ? "vanish_on" : "vanish_off");
        target.displayClientMessage(new net.minecraft.network.chat.TextComponent(message), false);

        if (!CommandUtil.isSelf(source, target)) {
            String sourceMessage = CommandUtil.resolveMessage(source, enabled ? "vanish_on" : "vanish_off");
            CommandUtil.notifySource(source, target.getGameProfile().getName() + ": " + sourceMessage);
        }
        return 1;
    }
}
