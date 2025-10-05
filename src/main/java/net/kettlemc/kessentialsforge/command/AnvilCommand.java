package net.kettlemc.kessentialsforge.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AnvilMenu;

public class AnvilCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var anvil = dispatcher.register(CommandBuilders.literal("anvil").executes(ctx -> {
            if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "anvil", 0)) return 0;
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> new AnvilMenu(id, inv), new net.minecraft.network.chat.TextComponent("Anvil")));
            return 1;
        }));
        CommandBuilders.registerAliases(dispatcher, anvil, "amboss");
    }
}
