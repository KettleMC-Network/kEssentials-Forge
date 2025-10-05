
package net.kettlemc.kessentialsforge.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;

public class EnderChestCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var enderchest = dispatcher.register(CommandBuilders.literal("enderchest")
            .executes(ctx -> {
                if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "enderchest", 0)) return 0;
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                return open(ctx.getSource(), player, player);
            })
            .then(CommandBuilders.argument("target", EntityArgument.player()).executes(ctx -> {
                ServerPlayer viewer = ctx.getSource().getPlayerOrException();
                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                boolean self = viewer.getUUID().equals(target.getUUID());
                if (self) {
                    if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "enderchest", 0)) return 0;
                } else {
                    if (!(net.kettlemc.kessentialsforge.perm.Perms.hasOther(ctx.getSource(), "enderchest", 2)
                        || net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "endersee", 2))) return 0;
                }
                return open(ctx.getSource(), viewer, target);
            })));
        CommandBuilders.registerAliases(dispatcher, enderchest, "ec", "enderkiste");
    }

    static int open(CommandSourceStack source, ServerPlayer viewer, ServerPlayer target) {
        boolean self = viewer.getUUID().equals(target.getUUID());
        String targetName = target.getGameProfile().getName();

        viewer.openMenu(new SimpleMenuProvider((id, inv, player) -> ChestMenu.threeRows(id, inv, target.getEnderChestInventory()),
            new net.minecraft.network.chat.TextComponent(self ? "Ender Chest" : net.kettlemc.kessentialsforge.i18n.Messages.get(viewer,"endersee_title", targetName))));

        String key = self ? "enderchest_opened" : "enderchest_opened_other";
        Object[] args = self ? new Object[0] : new Object[]{targetName};
        String viewerMessage = net.kettlemc.kessentialsforge.i18n.Messages.get(viewer, key, args);
        viewer.displayClientMessage(new net.minecraft.network.chat.TextComponent(viewerMessage), false);

        if (!self) {
            target.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(target,"enderchest_accessed", viewer.getGameProfile().getName())), false);
        }

        if (!CommandUtil.isSelf(source, viewer)) {
            String sourceMessage = CommandUtil.resolveMessage(source, key, args);
            CommandUtil.notifySource(source, viewer.getGameProfile().getName() + ": " + sourceMessage);
        }
        return 1;
    }
}
