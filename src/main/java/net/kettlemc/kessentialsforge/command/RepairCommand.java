
package net.kettlemc.kessentialsforge.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class RepairCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var repair = dispatcher.register(CommandBuilders.literal("repair")
            .executes(ctx -> {
                if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "repair", 2)) return 0;
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                return repair(ctx.getSource(), player);
            })
            .then(CommandBuilders.argument("target", EntityArgument.player()).executes(ctx -> {
                if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "repair", 2)) return 0;
                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                boolean self = CommandUtil.isSelf(ctx.getSource(), target);
                if (!self && !net.kettlemc.kessentialsforge.perm.Perms.hasOther(ctx.getSource(), "repair", 2)) return 0;
                return repair(ctx.getSource(), target);
            })));
        CommandBuilders.registerAliases(dispatcher, repair, "reparieren");
    }

    private static int repair(CommandSourceStack source, ServerPlayer target) {
        ItemStack stack = target.getMainHandItem();
        if (stack.isEmpty()) {
            sendMessage(source, target, "repair_no_item");
            return 0;
        }

        if (!stack.isDamageableItem()) {
            sendMessage(source, target, "repair_not_repairable");
            return 0;
        }

        if (stack.isDamaged()) {
            stack.setDamageValue(0);
        }

        sendMessage(source, target, "repair_done");
        return 1;
    }

    private static void sendMessage(CommandSourceStack source, ServerPlayer target, String key) {
        String message = net.kettlemc.kessentialsforge.i18n.Messages.get(target, key);
        target.displayClientMessage(new TextComponent(message), false);

        if (!CommandUtil.isSelf(source, target)) {
            String sourceMessage = CommandUtil.resolveMessage(source, key);
            CommandUtil.notifySource(source, target.getGameProfile().getName() + ": " + sourceMessage);
        }
    }
}
