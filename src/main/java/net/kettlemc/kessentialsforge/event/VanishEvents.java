
package net.kettlemc.kessentialsforge.event;

import net.kettlemc.kessentialsforge.KEssentialsForge;
import net.minecraftforge.event.TickEvent; import net.minecraftforge.eventbus.api.SubscribeEvent; import net.minecraft.server.level.ServerPlayer;

public class VanishEvents {
    @SubscribeEvent public void onTick(TickEvent.PlayerTickEvent e) {
        if (e.player instanceof ServerPlayer p && e.phase == TickEvent.Phase.END) {
            boolean vanished = KEssentialsForge.INSTANCE.state.isVanished(p.getUUID());
            p.setInvisible(vanished); p.setSilent(vanished); p.noPhysics = vanished;
        }
    }
}
