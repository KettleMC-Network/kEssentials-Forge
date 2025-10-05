package net.kettlemc.kessentialsforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.kettlemc.kessentialsforge.event.EnchantingTableEvents;
import net.kettlemc.kessentialsforge.i18n.Messages;
import net.kettlemc.kessentialsforge.perm.Perms;
import net.kettlemc.kessentialsforge.ui.CommandEnchantmentMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

public class EnchantCommand {
    private static final String PERMISSION = "enchantingtable";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var enchantingtable = dispatcher.register(CommandBuilders.literal("enchantingtable")
            .then(CommandBuilders.argument("slot1", IntegerArgumentType.integer(0, 30))
                .then(CommandBuilders.argument("slot2", IntegerArgumentType.integer(0, 30))
                    .then(CommandBuilders.argument("slot3", IntegerArgumentType.integer(0, 30))
                        .executes(ctx -> {
                            if (!Perms.has(ctx.getSource(), PERMISSION, 0)) return 0;
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int slot1 = IntegerArgumentType.getInteger(ctx, "slot1");
                            int slot2 = IntegerArgumentType.getInteger(ctx, "slot2");
                            int slot3 = IntegerArgumentType.getInteger(ctx, "slot3");
                            return openTable(ctx.getSource(), player, slot1, slot2, slot3);
                        })
                        .then(CommandBuilders.argument("target", EntityArgument.player()).executes(ctx -> {
                            if (!Perms.has(ctx.getSource(), PERMISSION, 0)) return 0;
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                            boolean self = CommandUtil.isSelf(ctx.getSource(), target);
                            if (!self && !Perms.hasOther(ctx.getSource(), PERMISSION, 2)) return 0;
                            int slot1 = IntegerArgumentType.getInteger(ctx, "slot1");
                            int slot2 = IntegerArgumentType.getInteger(ctx, "slot2");
                            int slot3 = IntegerArgumentType.getInteger(ctx, "slot3");
                            return openTable(ctx.getSource(), target, slot1, slot2, slot3);
                        }))))));
        CommandBuilders.registerAliases(dispatcher, enchantingtable, "verzauberungstisch", "enchantmenttable");
    }

    private static int openTable(CommandSourceStack source, ServerPlayer target, int slot1, int slot2, int slot3) {
        EnchantingTableEvents.setLevels(target, slot1, slot2, slot3);
        target.openMenu(new SimpleMenuProvider((id, inv, player) -> new CommandEnchantmentMenu(id, inv, (ServerPlayer) player),
            new TextComponent("Enchanting")));

        target.displayClientMessage(new TextComponent(Messages.get(target, "enchantingtable_opened", slot1, slot2, slot3)), false);
        if (!CommandUtil.isSelf(source, target)) {
            CommandUtil.notifySourceLocalized(source, "enchantingtable_opened_other", target.getGameProfile().getName(), slot1, slot2, slot3);
        }
        return 1;
    }
}
