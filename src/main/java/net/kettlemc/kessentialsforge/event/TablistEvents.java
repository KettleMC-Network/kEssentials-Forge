package net.kettlemc.kessentialsforge.event;

import net.kettlemc.kessentialsforge.service.TabOrderService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(modid = "kessentials", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TablistEvents {

    private static final List<Pending> pending = new ArrayList<>();

    private static class Pending {
        final String name;
        final int atTick;
        Pending(String name, int atTick) { this.name = name; this.atTick = atTick; }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getPlayer() instanceof ServerPlayer sp) {
            MinecraftServer server = sp.getServer();
            TabOrderService.apply(sp);
            if (server != null) {
                pending.add(new Pending(sp.getGameProfile().getName(), server.getTickCount() + 40)); // ~2s
            }
        }
    }

    @SubscribeEvent
    public static void onChangeDim(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getPlayer() instanceof ServerPlayer sp) {
            TabOrderService.apply(sp);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getPlayer() instanceof ServerPlayer sp) {
            TabOrderService.apply(sp);
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.END) {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;

            Iterator<Pending> it = pending.iterator();
            while (it.hasNext()) {
                Pending p = it.next();
                if (server.getTickCount() >= p.atTick) {
                    ServerPlayer sp = server.getPlayerList().getPlayerByName(p.name);
                    if (sp != null) {
                        TabOrderService.apply(sp);
                    }
                    it.remove();
                }
            }

            if (server.getTickCount() % 200 == 0) { // ~10s safety
                TabOrderService.applyAll(server);
            }
        }
    }
}