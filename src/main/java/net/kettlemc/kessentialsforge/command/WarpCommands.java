
package net.kettlemc.kessentialsforge.command;
import com.mojang.brigadier.CommandDispatcher; import com.mojang.brigadier.arguments.StringArgumentType; import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.kettlemc.kessentialsforge.KEssentialsForge; import net.kettlemc.kessentialsforge.service.WarpService; import net.kettlemc.kessentialsforge.util.DimUtil;
import net.minecraft.commands.CommandSourceStack; import net.minecraft.network.chat.Component; import net.minecraft.server.MinecraftServer; import net.minecraft.server.level.ServerLevel; import net.minecraft.server.level.ServerPlayer;
import static net.kettlemc.kessentialsforge.command.CommandBuilders.argument;
public class WarpCommands {
    private static final SuggestionProvider<CommandSourceStack> WARP_SUGGEST = (ctx, b)->{ for (var n:KEssentialsForge.INSTANCE.warps.list()) b.suggest(n); return b.buildFuture(); };
    public static void register(CommandDispatcher<CommandSourceStack> d){
        var setwarp = d.register(CommandBuilders.literal("setwarp").executes(ctx -> {
            CommandUtil.sendUsage(ctx.getSource(), "setwarp");
            return 0;
        }).then(argument("name", StringArgumentType.word()).executes(ctx -> {
            if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "warp", 2)) return 0;
            ServerPlayer p=ctx.getSource().getPlayerOrException(); WarpService.Warp w=new WarpService.Warp();
            w.dim=p.getLevel().dimension().location().toString(); w.x=p.getX(); w.y=p.getY(); w.z=p.getZ(); w.yaw=p.getYRot(); w.pitch=p.getXRot();
            String name=StringArgumentType.getString(ctx,"name"); KEssentialsForge.INSTANCE.warps.set(name, w);
            p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"warp_set", name)), false); return 1;
        })));
        CommandBuilders.registerAliases(d, setwarp, "newwarp", "addwarp");

        var warp = CommandBuilders.literal("warp")
            .then(CommandBuilders.literal("list").executes(WarpCommands::listWarps))
            .then(argument("name", StringArgumentType.word()).suggests(WARP_SUGGEST).executes(ctx -> {
            if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "warp", 2)) return 0;
            ServerPlayer p=ctx.getSource().getPlayerOrException(); String name=StringArgumentType.getString(ctx,"name");
            WarpService.Warp w=KEssentialsForge.INSTANCE.warps.get(name);
            if (w==null){ p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"warp_none")), false); return 0; }
            MinecraftServer server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer(); ServerLevel lvl=DimUtil.levelById(server, w.dim);
            if (lvl==null){ p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"dim_not_found", w.dim)), false); return 0; }
            var cd=KEssentialsForge.INSTANCE.cd; var cfg=KEssentialsForge.INSTANCE.config; int warm=cfg.warmupsSeconds.getOrDefault("warp",0); int cool=cfg.cooldownsSeconds.getOrDefault("warp",0);
            if (cd.isOnCooldown(p.getUUID(),"warp",cool)){ long left=cd.cooldownLeft(p.getUUID(),"warp",cool); p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"cooldown_active", left)), false); return 0; }
            if (warm>0){ p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"warmup_started", warm)), false);
                cd.startWarmup(p,"warp",warm,()->{ KEssentialsForge.INSTANCE.back.store(p); p.teleportTo(lvl,w.x,w.y,w.z,w.yaw,w.pitch); p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"warp_tp", name)), false); cd.markUsed(p.getUUID(),"warp"); });
            } else { KEssentialsForge.INSTANCE.back.store(p); p.teleportTo(lvl,w.x,w.y,w.z,w.yaw,w.pitch); p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"warp_tp", name)), false); KEssentialsForge.INSTANCE.cd.markUsed(p.getUUID(),"warp"); }
            return 1; }));
        var warpNode = d.register(warp);
        CommandBuilders.registerAliases(d, warpNode, "warps");

        var delwarp = d.register(CommandBuilders.literal("delwarp").executes(ctx -> {
            CommandUtil.sendUsage(ctx.getSource(), "delwarp");
            return 0;
        }).then(argument("name", StringArgumentType.word()).suggests(WARP_SUGGEST).executes(ctx -> {
            if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "warp", 2)) return 0;
            String name=StringArgumentType.getString(ctx,"name"); boolean ok=KEssentialsForge.INSTANCE.warps.del(name);
            String key = ok ? "warp_deleted" : "warp_not_found";
            ctx.getSource().sendSuccess(new net.minecraft.network.chat.TextComponent(CommandUtil.resolveMessage(ctx.getSource(), key, name)), false); return 1;
        })));
        CommandBuilders.registerAliases(d, delwarp, "deletewarp", "removewarp");
    }

    private static int listWarps(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "warp", 2)) return 0;
        var names=KEssentialsForge.INSTANCE.warps.list();
        ctx.getSource().sendSuccess(new net.minecraft.network.chat.TextComponent(CommandUtil.resolveMessage(ctx.getSource(), "warps_list", String.join(", ", names), false)), false);
        return 1;
    }
}
