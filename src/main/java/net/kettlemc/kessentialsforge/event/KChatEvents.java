
package net.kettlemc.kessentialsforge.event;

import net.kettlemc.kessentialsforge.KEssentialsForge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class KChatEvents {
    @SubscribeEvent public void onJoin(PlayerEvent.PlayerLoggedInEvent e){
        if (e.getEntity() instanceof ServerPlayer p) {
            KEssentialsForge.INSTANCE.kchat.apply(p);
        }
    }
    @SubscribeEvent public void onQuit(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            KEssentialsForge.INSTANCE.kchat.onQuit(p);
        }
    }
    @SubscribeEvent public void onTick(TickEvent.ServerTickEvent e){
        if (e.phase == TickEvent.Phase.END) KEssentialsForge.INSTANCE.kchat.tick(net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer());
    }
    @SubscribeEvent public void onServerStarted(ServerStartedEvent e) {
        KEssentialsForge.INSTANCE.kchat.applyMotd(e.getServer());
    }
    @SubscribeEvent public void onChat(ServerChatEvent e){
        // Let mute handling happen in ChatEvents first; we only format if not canceled.
        if (e.isCanceled()) return;
        var p = e.getPlayer();
        var comp = KEssentialsForge.INSTANCE.kchat.formatChat(p, e.getMessage());
        e.setComponent(comp);
    }
}
