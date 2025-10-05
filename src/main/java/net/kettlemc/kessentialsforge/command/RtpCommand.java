package net.kettlemc.kessentialsforge.command;

import com.mojang.brigadier.CommandDispatcher;
import net.kettlemc.kessentialsforge.i18n.Messages;
import net.kettlemc.kessentialsforge.util.DimUtil;
import net.kettlemc.kessentialsforge.util.TeleportUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.ThreadLocalRandom;

public class RtpCommand {
    public static void register(CommandDispatcher<CommandSourceStack> d){
        d.register(CommandBuilders.literal("rtp").executes(ctx -> {
            if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "rtp", 2)) return 0;
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            var cd = net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE.cd;
            var cfg = net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE.config;
            var chat = net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE.chat;
            int warm = cfg.warmupsSeconds.getOrDefault("rtp", 3);
            int cool = cfg.cooldownsSeconds.getOrDefault("rtp", 30);
            if (cd.isOnCooldown(p.getUUID(), "rtp", cool)){
                long left = cd.cooldownLeft(p.getUUID(), "rtp", cool);
                p.displayClientMessage(new net.minecraft.network.chat.TextComponent(Messages.get(p,"cooldown_active", left)), false);
                return 0;
            }
            p.displayClientMessage(new net.minecraft.network.chat.TextComponent(Messages.get(p,"rtp_searching")), false);
            ServerLevel lvl = p.getLevel();
            final int tries = 32;
            BlockPos found = null;
            var rnd = ThreadLocalRandom.current();
            long rawMin = chat.getRtpMin();
            long rawMax = chat.getRtpMax();
            int configuredMin = rawMin >= Integer.MAX_VALUE ? Integer.MAX_VALUE - 1 : (int) Math.max(0, rawMin);
            int configuredMax = rawMax >= Integer.MAX_VALUE ? Integer.MAX_VALUE - 1 : (int) Math.max(0, rawMax);
            boolean validRange = configuredMax > 0 && configuredMax >= configuredMin;
            for (int i=0;i<tries;i++){
                int x;
                int z;
                if (validRange){
                    x = randomCoordinate(rnd, configuredMin, configuredMax);
                    z = randomCoordinate(rnd, configuredMin, configuredMax);
                } else {
                    int fallbackRange = configuredMax > 0 ? configuredMax : 1;
                    x = rnd.nextInt(-fallbackRange, fallbackRange);
                    z = rnd.nextInt(-fallbackRange, fallbackRange);
                }
                lvl.getChunkAt(new BlockPos(x>>4,0,z>>4));
                var top = DimUtil.highestSafeY(lvl, x, z);
                if (top != null) {
                    found = TeleportUtil.findSafe(lvl, top);
                    if (found != null) break;
                }
            }
            if (found == null) found = DimUtil.highestSafeY(lvl, 0, 0);
            var dest = found;
            if (warm > 0){
                p.displayClientMessage(new net.minecraft.network.chat.TextComponent(Messages.get(p,"warmup_started", warm)), false);
                final BlockPos finalDest = dest;
                cd.startWarmup(p, "rtp", warm, () -> {
                    net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE.back.store(p);
                    p.teleportTo(lvl, finalDest.getX()+0.5, finalDest.getY(), finalDest.getZ()+0.5, p.getYHeadRot(), p.getXRot());
                    p.displayClientMessage(new net.minecraft.network.chat.TextComponent(Messages.get(p,"rtp_done")), false);
                    cd.markUsed(p.getUUID(), "rtp");
                });
            } else {
                net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE.back.store(p);
                p.teleportTo(lvl, dest.getX()+0.5, dest.getY(), dest.getZ()+0.5, p.getYHeadRot(), p.getXRot());
                p.displayClientMessage(new net.minecraft.network.chat.TextComponent(Messages.get(p,"rtp_done")), false);
                cd.markUsed(p.getUUID(), "rtp");
            }
            return 1;
        }));
    }

    private static int randomCoordinate(ThreadLocalRandom rnd, int min, int max) {
        if (min == 0 && max == 0) return 0;
        int magnitude = rnd.nextInt(min, max + 1);
        return rnd.nextBoolean() ? magnitude : -magnitude;
    }
}
