
package net.kettlemc.kessentialsforge.command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

public class FreezeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        var freeze = dispatcher.register(CommandBuilders.literal("freeze")
            .executes(ctx -> {
                if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "freeze", 2)) return 0;
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                return toggle(ctx.getSource(), player);
            })
            .then(CommandBuilders.argument("target", EntityArgument.player()).executes(ctx -> {
                if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "freeze", 2)) return 0;
                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                boolean self = CommandUtil.isSelf(ctx.getSource(), target);
                if (!self && !net.kettlemc.kessentialsforge.perm.Perms.hasOther(ctx.getSource(), "freeze", 2)) return 0;
                return toggle(ctx.getSource(), target);
            })));
        CommandBuilders.registerAliases(dispatcher, freeze, "einfrieren");
        dispatcher.register(CommandBuilders.literal("mute").then(CommandBuilders.argument("target", EntityArgument.player()).executes(ctx -> {
            if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "mute", 2)) return 0;
            ServerPlayer t=EntityArgument.getPlayer(ctx, "target"); boolean on=net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE.state.toggleMuted(t.getUUID());
            ctx.getSource().sendSuccess(new net.minecraft.network.chat.TextComponent("Mute " + t.getGameProfile().getName() + ": " + (on?"ON":"OFF")), false); return 1;
        })));
        dispatcher.register(CommandBuilders.literal("unmute").then(CommandBuilders.argument("target", EntityArgument.player()).executes(ctx -> {
            if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "mute", 2)) return 0;
            ServerPlayer t=EntityArgument.getPlayer(ctx, "target"); if (net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE.state.isMuted(t.getUUID())) net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE.state.toggleMuted(t.getUUID());
            ctx.getSource().sendSuccess(new net.minecraft.network.chat.TextComponent("Unmuted " + t.getGameProfile().getName()), false); return 1;
        })));
    }

    private static int toggle(CommandSourceStack source, ServerPlayer target) {
        boolean enabled = net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE.state.toggleFrozen(target.getUUID());
        String key = enabled ? "freeze_on" : "freeze_off";
        String message = net.kettlemc.kessentialsforge.i18n.Messages.get(target, key, target.getGameProfile().getName());
        target.displayClientMessage(new TextComponent(message), false);
        if (!CommandUtil.isSelf(source, target)) {
            String sourceMessage = CommandUtil.resolveMessage(source, key, target.getGameProfile().getName());
            CommandUtil.notifySource(source, new TextComponent(sourceMessage));
        }
        return 1;
    }
}
