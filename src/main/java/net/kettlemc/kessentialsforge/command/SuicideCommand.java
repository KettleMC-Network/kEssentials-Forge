package net.kettlemc.kessentialsforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

public class SuicideCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        var suicide = dispatcher.register(CommandBuilders.literal("suicide").executes(ctx -> {
            ServerPlayer player;
            try { player = ctx.getSource().getPlayerOrException(); }
            catch (CommandSyntaxException ex) { return 0; }
            if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "suicide", 2)) return 0;
            player.kill();
            player.displayClientMessage(new TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(player, "suicide")), false);
            return 1;
        }));
        CommandBuilders.registerAliases(dispatcher, suicide, "suizid", "selbstmord");
    }
}
