
package net.kettlemc.kessentialsforge.command;
import com.mojang.brigadier.CommandDispatcher; import com.mojang.brigadier.arguments.StringArgumentType; import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.kettlemc.kessentialsforge.KEssentialsForge; import net.kettlemc.kessentialsforge.service.HomeService; import net.kettlemc.kessentialsforge.util.DimUtil;
import net.minecraft.commands.CommandSourceStack; import net.minecraft.server.MinecraftServer; import net.minecraft.server.level.ServerLevel; import net.minecraft.server.level.ServerPlayer;
import static net.kettlemc.kessentialsforge.command.CommandBuilders.argument;
public class HomeCommands {
    private static final SuggestionProvider<CommandSourceStack> WARP_SUGGEST = (ctx, b) -> {
        ServerPlayer p;
        try {
            p = ctx.getSource().getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) {
            // Bei fehlendem Player einfach "nichts" vorschlagen, aber Future zurückgeben
            return b.buildFuture();
        }

        for (var n : KEssentialsForge.INSTANCE.homes.list(p.getUUID())) {
            b.suggest(n);
        }
        return b.buildFuture();
    };
    private static final com.mojang.brigadier.suggestion.SuggestionProvider<net.minecraft.commands.CommandSourceStack>
            HOME_SUGGEST = WARP_SUGGEST;
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        var sethome = d.register(CommandBuilders.literal("sethome").then(argument("name", StringArgumentType.word()).executes(ctx -> {
            if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "home", 2)) return 0;
            ServerPlayer p;
            try { p = ctx.getSource().getPlayerOrException(); } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) { return 0; }
            int maxHomes = net.kettlemc.kessentialsforge.perm.Perms.resolveHomeLimit(p);
            if (KEssentialsForge.INSTANCE.homes.list(p.getUUID()).size() >= maxHomes) { p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"maxhomes_reached", maxHomes)), false); return 0; }
            String name=StringArgumentType.getString(ctx,"name"); KEssentialsForge.INSTANCE.homes.setHome(p, name);
            p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"home_set", name)), false); return 1;
        })).executes(ctx -> {
            if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "home", 2)) return 0;
            ServerPlayer p;
            try { p = ctx.getSource().getPlayerOrException(); } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) { return 0; }
            int maxHomes = net.kettlemc.kessentialsforge.perm.Perms.resolveHomeLimit(p);
            if (KEssentialsForge.INSTANCE.homes.list(p.getUUID()).size() >= maxHomes) { p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"maxhomes_reached", maxHomes)), false); return 0; }
            KEssentialsForge.INSTANCE.homes.setHome(p, "home");
            p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"home_set", "home")), false); return 1;
        }));
        CommandBuilders.registerAliases(d, sethome, "newhome", "delhomes", "newhomes", "zuhausefestlegen");

        var home = CommandBuilders.literal("home")
            .then(CommandBuilders.literal("list").executes(HomeCommands::listHomes))
            .then(argument("name", StringArgumentType.word()).suggests(HOME_SUGGEST).executes(ctx -> goHome(ctx, StringArgumentType.getString(ctx,"name"))))
            .executes(ctx -> goHome(ctx, "home"));
        var homeNode = d.register(home);
        CommandBuilders.registerAliases(d, homeNode, "zuhause");

        var homesList = d.register(CommandBuilders.literal("homes").executes(HomeCommands::listHomes));
        CommandBuilders.registerAliases(d, homesList, "listhomes", "homelist", "zuhaeuser");

        var delhome = d.register(CommandBuilders.literal("delhome").then(argument("name", StringArgumentType.word()).suggests(HOME_SUGGEST).executes(ctx -> {
            if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "home", 2)) return 0;
            ServerPlayer p;
            try { p = ctx.getSource().getPlayerOrException(); } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) { return 0; } String name=StringArgumentType.getString(ctx,"name");
            boolean ok=KEssentialsForge.INSTANCE.homes.delHome(p.getUUID(), name);
            p.displayClientMessage(new net.minecraft.network.chat.TextComponent(ok? net.kettlemc.kessentialsforge.i18n.Messages.get(p,"home_deleted", name) : net.kettlemc.kessentialsforge.i18n.Messages.get(p,"home_not_found")), false); return 1;
        })));
        CommandBuilders.registerAliases(d, delhome, "deletehome", "removehome", "removehomes", "zuhauseloeschen");
    }

    private static int listHomes(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "home", 2)) return 0;
        ServerPlayer p;
        try { p = ctx.getSource().getPlayerOrException(); } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) { return 0; }
        var names = KEssentialsForge.INSTANCE.homes.list(p.getUUID());
        p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"homes_list", String.join(", ", names), false)), false);
        return 1;
    }
    private static int goHome(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String name){
        if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "home", 2)) return 0;
        ServerPlayer p;
            try { p = ctx.getSource().getPlayerOrException(); } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) { return 0; } HomeService.Home h=KEssentialsForge.INSTANCE.homes.getHome(p.getUUID(), name);
        if (h==null){ p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"home_none", name)), false); return 0; }
        MinecraftServer server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer(); ServerLevel lvl=DimUtil.levelById(server, h.dim);
        if (lvl==null){ p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"dim_not_found", h.dim)), false); return 0; }
        var cd=KEssentialsForge.INSTANCE.cd; var cfg=KEssentialsForge.INSTANCE.config; int warm=cfg.warmupsSeconds.getOrDefault("home",0); int cool=cfg.cooldownsSeconds.getOrDefault("home",0);
        if (cd.isOnCooldown(p.getUUID(),"home",cool)){ long left=cd.cooldownLeft(p.getUUID(),"home",cool); p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"cooldown_active", left)), false); return 0; }
        if (warm>0){ p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"warmup_started", warm)), false);
            cd.startWarmup(p,"home",warm,()->{ KEssentialsForge.INSTANCE.back.store(p); p.teleportTo(lvl,h.x,h.y,h.z,h.yaw,h.pitch); p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"home_tp", name)), false); cd.markUsed(p.getUUID(),"home"); });
        } else { KEssentialsForge.INSTANCE.back.store(p); p.teleportTo(lvl,h.x,h.y,h.z,h.yaw,h.pitch); p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"home_tp", name)), false); KEssentialsForge.INSTANCE.cd.markUsed(p.getUUID(),"home"); }
        return 1;
    }
}
