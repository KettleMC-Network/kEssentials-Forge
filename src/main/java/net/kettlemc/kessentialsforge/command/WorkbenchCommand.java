
package net.kettlemc.kessentialsforge.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.CraftingMenu;

public class WorkbenchCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var workbench = dispatcher.register(CommandBuilders.literal("workbench")
            .executes(ctx -> {
                if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "workbench", 0)) return 0;
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                return openWorkbench(ctx.getSource(), player);
            })
            .then(CommandBuilders.argument("target", EntityArgument.player()).executes(ctx -> {
                if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "workbench", 0)) return 0;
                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                boolean self = CommandUtil.isSelf(ctx.getSource(), target);
                if (!self && !net.kettlemc.kessentialsforge.perm.Perms.hasOther(ctx.getSource(), "workbench", 2)) return 0;
                return openWorkbench(ctx.getSource(), target);
            })));
        CommandBuilders.registerAliases(dispatcher, workbench, "werkbank", "craftingtable", "craft", "crafting");
    }

    private static int openWorkbench(CommandSourceStack source, ServerPlayer target) {
        target.openMenu(new SimpleMenuProvider((id, inv, player) -> new CraftingMenu(id, inv), new net.minecraft.network.chat.TextComponent("Workbench")));
        String message = net.kettlemc.kessentialsforge.i18n.Messages.get(target, "workbench_opened");
        target.displayClientMessage(new net.minecraft.network.chat.TextComponent(message), false);

        if (!CommandUtil.isSelf(source, target)) {
            CommandUtil.notifySourceLocalized(source, "workbench_opened_other", target.getGameProfile().getName());
        }
        return 1;
    }
}
