package net.kettlemc.kessentialsforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class KEssentialsCommand {
    private static final String VERSION = "0.3.1";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var kessentials = dispatcher.register(CommandBuilders.literal("kessentials")
            .then(CommandBuilders.literal("reload")
                .requires(src -> net.kettlemc.kessentialsforge.perm.Perms.has(src, "reload", 2))
                .executes(KEssentialsCommand::reload))
            .executes(ctx -> {
                ctx.getSource().sendSuccess(new TextComponent(CommandUtil.resolveMessage(ctx.getSource(), "kessentials_info", VERSION)), false);
                return 1;
            }));
        CommandBuilders.registerAliases(dispatcher, kessentials, "essentials", "kessential");
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var instance = net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE;
        var server = ServerLifecycleHooks.getCurrentServer();
        List<String> failures = new ArrayList<>();

        try {
            instance.config.load();
        } catch (Exception ex) {
            failures.add("ConfigService");
            ex.printStackTrace();
        }

        try {
            instance.chat.load();
        } catch (Exception ex) {
            failures.add("ChatService");
            ex.printStackTrace();
        }

        try {
            if (server != null) {
                instance.kchat.reloadAndApply(server);
            } else {
                instance.kchat.load();
            }
        } catch (Exception ex) {
            failures.add("KChatService");
            ex.printStackTrace();
        }

        try {
            instance.locales.load();
            net.kettlemc.kessentialsforge.i18n.Messages.setDefaultLocale(instance.locales.getDefaultLocale());
        } catch (Exception ex) {
            failures.add("LocaleService");
            ex.printStackTrace();
        }

        try {
            net.kettlemc.kessentialsforge.i18n.Messages.reload();
        } catch (Exception ex) {
            failures.add("Messages");
            ex.printStackTrace();
        }

        if (failures.isEmpty()) {
            CommandUtil.notifySourceLocalized(source, "kessentials_reload_success");
            return 1;
        }

        String details = failures.stream().distinct().collect(Collectors.joining(", "));
        source.sendFailure(new TextComponent(CommandUtil.resolveMessage(source, "kessentials_reload_failure", details)));
        return 0;
    }
}
