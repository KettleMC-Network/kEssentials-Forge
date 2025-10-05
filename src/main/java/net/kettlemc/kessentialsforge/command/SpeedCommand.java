
package net.kettlemc.kessentialsforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public class SpeedCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var speed = dispatcher.register(CommandBuilders.literal("speed")
            .then(CommandBuilders.argument("value", DoubleArgumentType.doubleArg(0.1, 5.0))
                .executes(ctx -> {
                    if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "speed", 2)) return 0;
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    return apply(ctx.getSource(), player, DoubleArgumentType.getDouble(ctx, "value"));
                })
                .then(CommandBuilders.argument("target", EntityArgument.player()).executes(ctx -> {
                    if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "speed", 2)) return 0;
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                    boolean self = CommandUtil.isSelf(ctx.getSource(), target);
                    if (!self && !net.kettlemc.kessentialsforge.perm.Perms.hasOther(ctx.getSource(), "speed", 2)) return 0;
                    return apply(ctx.getSource(), target, DoubleArgumentType.getDouble(ctx, "value"));
                }))));
        CommandBuilders.registerAliases(dispatcher, speed, "geschwindigkeit");
    }

    private static int apply(CommandSourceStack source, ServerPlayer target, double value) {
        var abilities = target.getAbilities();
        abilities.setWalkingSpeed((float) (0.1f * value));
        abilities.setFlyingSpeed((float) (0.05f * value));
        target.onUpdateAbilities();

        String valueText = String.format(Locale.ROOT, "%.2f", value);
        target.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(target,"speed_set", valueText)), false);

        if (!CommandUtil.isSelf(source, target)) {
            CommandUtil.notifySourceLocalized(source, "speed_set_other", target.getGameProfile().getName(), valueText);
        }
        return 1;
    }
}
