
package net.kettlemc.kessentialsforge.command;
import com.mojang.brigadier.CommandDispatcher; import net.minecraft.commands.CommandSourceStack; import net.minecraft.network.chat.Component; import net.minecraft.server.level.ServerLevel; import net.minecraft.server.level.ServerPlayer;
import net.kettlemc.kessentialsforge.KEssentialsForge; import net.kettlemc.kessentialsforge.service.SpawnService; import net.kettlemc.kessentialsforge.util.DimUtil;
public class SpawnCommands {
    public static void register(CommandDispatcher<CommandSourceStack> d){
        d.register(CommandBuilders.literal("spawn").executes(ctx -> { ServerPlayer p=ctx.getSource().getPlayerOrException(); var s=KEssentialsForge.INSTANCE.spawn.get();
            if (s==null){ p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"spawn_none")), false); return 0; }
            ServerLevel lvl=DimUtil.levelById(net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer(), s.dim); if (lvl==null){ p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"dim_not_found", s.dim)), false); return 0; }
            var cd=KEssentialsForge.INSTANCE.cd; var cfg=KEssentialsForge.INSTANCE.config; int warm=cfg.warmupsSeconds.getOrDefault("spawn",0); int cool=cfg.cooldownsSeconds.getOrDefault("spawn",0);
            if (cd.isOnCooldown(p.getUUID(),"spawn",cool)){ long left=cd.cooldownLeft(p.getUUID(),"spawn",cool); p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"cooldown_active", left)), false); return 0; }
            if (warm>0){ p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"warmup_started", warm)), false);
                cd.startWarmup(p,"spawn",warm,()->{ KEssentialsForge.INSTANCE.back.store(p); p.teleportTo(lvl, s.x, s.y, s.z, s.yaw, s.pitch); p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"spawn_tp")), false); cd.markUsed(p.getUUID(),"spawn"); });
            } else { KEssentialsForge.INSTANCE.back.store(p); p.teleportTo(lvl, s.x, s.y, s.z, s.yaw, s.pitch); p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"spawn_tp")), false); KEssentialsForge.INSTANCE.cd.markUsed(p.getUUID(),"spawn"); }
            return 1; }));
        d.register(CommandBuilders.literal("setspawn").executes(ctx -> { if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "setspawn",2)) return 0;
            ServerPlayer p=ctx.getSource().getPlayerOrException(); SpawnService.Spawn s=new SpawnService.Spawn(); s.dim=p.getLevel().dimension().location().toString(); s.x=p.getX(); s.y=p.getY(); s.z=p.getZ(); s.yaw=p.getYRot(); s.pitch=p.getXRot();
            KEssentialsForge.INSTANCE.spawn.set(s); p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"spawn_set")), false); return 1; }));
    }
}
