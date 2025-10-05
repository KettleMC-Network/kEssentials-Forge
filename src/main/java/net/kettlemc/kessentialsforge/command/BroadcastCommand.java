package net.kettlemc.kessentialsforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

public class BroadcastCommand {
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(CommandBuilders.literal("broadcast")
            .then(CommandBuilders.argument("msg", StringArgumentType.greedyString())
                .executes(ctx -> {
                    if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "broadcast", 2)) return 0;
                    String m = StringArgumentType.getString(ctx, "msg");
                    var list = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList();
                    for (ServerPlayer sp : list.getPlayers()) {
                        sp.sendMessage(new net.minecraft.network.chat.TextComponent(m), net.minecraft.Util.NIL_UUID);
                    }
                    return 1;
                })
            )
        );
    }
}
