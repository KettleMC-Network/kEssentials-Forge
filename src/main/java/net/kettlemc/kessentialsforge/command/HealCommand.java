
package net.kettlemc.kessentialsforge.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public class HealCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var heal = dispatcher.register(CommandBuilders.literal("heal")
            .executes(ctx -> {
                if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "heal", 2)) return 0;
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                return heal(ctx.getSource(), player);
            })
            .then(CommandBuilders.argument("target", EntityArgument.player()).executes(ctx -> {
                if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "heal", 2)) return 0;
                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                boolean self = CommandUtil.isSelf(ctx.getSource(), target);
                if (!self && !net.kettlemc.kessentialsforge.perm.Perms.hasOther(ctx.getSource(), "heal", 2)) return 0;
                return heal(ctx.getSource(), target);
            })));
        CommandBuilders.registerAliases(dispatcher, heal, "heilen");
    }

    private static int heal(CommandSourceStack source, ServerPlayer target) {
        target.setHealth(target.getMaxHealth());
        target.getFoodData().setFoodLevel(20);
        target.getFoodData().setSaturation(20f);
        target.removeAllEffects();
        target.clearFire();

        String message = net.kettlemc.kessentialsforge.i18n.Messages.get(target, "healed");
        target.displayClientMessage(new net.minecraft.network.chat.TextComponent(message), false);

        if (!CommandUtil.isSelf(source, target)) {
            String sourceMessage = CommandUtil.resolveMessage(source, "healed");
            CommandUtil.notifySource(source, target.getGameProfile().getName() + ": " + sourceMessage);
        }
        return 1;
    }
}
