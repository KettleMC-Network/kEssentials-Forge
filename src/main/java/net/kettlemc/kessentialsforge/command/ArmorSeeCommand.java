package net.kettlemc.kessentialsforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kettlemc.kessentialsforge.ui.ArmorSeeMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;

public class ArmorSeeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var armorsee = dispatcher.register(CommandBuilders.literal("armorsee")
            .then(CommandBuilders.argument("target", EntityArgument.player())
                .executes(ctx -> open(ctx, false))
                .then(CommandBuilders.argument("modify", BoolArgumentType.bool())
                    .executes(ctx -> open(ctx, BoolArgumentType.getBool(ctx, "modify"))))));
        CommandBuilders.registerAliases(dispatcher, armorsee, "armor", "armsee", "ruestung");
    }

    private static int open(CommandContext<CommandSourceStack> ctx, boolean modify) {
        if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "armorsee", 2)) {
            return 0;
        }

        ServerPlayer viewer;
        ServerPlayer target;
        try {
            viewer = ctx.getSource().getPlayerOrException();
            target = EntityArgument.getPlayer(ctx, "target");
        } catch (CommandSyntaxException ex) {
            return 0;
        }

        boolean canModify = modify && net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "armorsee.modify", 3);
        Inventory targetInv = target.getInventory();
        Component title = new TextComponent("Armor von " + target.getGameProfile().getName());
        viewer.openMenu(new SimpleMenuProvider((id, inv, player) -> new ArmorSeeMenu(id, inv, targetInv, canModify), title));
        return 1;
    }
}
