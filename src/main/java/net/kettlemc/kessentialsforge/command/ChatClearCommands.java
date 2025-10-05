package net.kettlemc.kessentialsforge.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

public class ChatClearCommands {
    public static void register(CommandDispatcher<CommandSourceStack> d){
        var chatClear = d.register(CommandBuilders.literal("chatclear").executes(ctx -> {
            if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "chatclear", 2)) return 0;
            var list = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList();
            for (int i=0;i<100;i++) {
                for (ServerPlayer p : list.getPlayers()) {
                    p.sendMessage(new net.minecraft.network.chat.TextComponent(" "), net.minecraft.Util.NIL_UUID);
                }
            }
            for (ServerPlayer p : list.getPlayers()) {
                p.sendMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"chat_cleared")), net.minecraft.Util.NIL_UUID);
            }
            return 1;
        }));
        CommandBuilders.registerAliases(d, chatClear, "cc", "chatleeren");

        var f3d = d.register(CommandBuilders.literal("f3d").executes(ctx -> {
            var list = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList();
            for (ServerPlayer p : list.getPlayers()) {
                p.sendMessage(new net.minecraft.network.chat.TextComponent(" "), net.minecraft.Util.NIL_UUID);
            }
            return 1;
        }));
        CommandBuilders.registerAliases(d, f3d, "clearmychat", "meinenchatleeren");
    }
}
