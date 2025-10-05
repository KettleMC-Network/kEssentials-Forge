package net.kettlemc.kessentialsforge.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.server.ServerLifecycleHooks;

public class KChatCommand {
    public static void register(CommandDispatcher<CommandSourceStack> d){
        d.register(CommandBuilders.literal("kchat")
            .requires(src -> net.kettlemc.kessentialsforge.perm.Perms.has(src, "kchat", 2))
            .then(CommandBuilders.literal("reload").executes(ctx -> {
                var server = ServerLifecycleHooks.getCurrentServer();
                net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE.kchat.reloadAndApply(server);
                ctx.getSource().sendSuccess(new net.minecraft.network.chat.TextComponent("kChat-Konfiguration neu geladen."), false);
                return 1;
            }))
            .executes(ctx -> {
                var server = ServerLifecycleHooks.getCurrentServer();
                net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE.kchat.applyMotd(server);
                ctx.getSource().sendSuccess(new net.minecraft.network.chat.TextComponent("MOTD aktualisiert."), false);
                return 1;
            }));
    }
}
