package net.kettlemc.kessentialsforge.event;

import net.kettlemc.kessentialsforge.service.MotdService;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "kessentials", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MotdEvents {

    /**
     * Fires right after the server reports "Done" in console.
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent e) {
        MotdService.apply(e.getServer());
    }
}