
package net.kettlemc.kessentialsforge.event;

import net.kettlemc.kessentialsforge.KEssentialsForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ProtectionEvents {
    @SubscribeEvent public void onAttack(LivingAttackEvent e) {
        if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) {
            if (KEssentialsForge.INSTANCE.state.isGod(p.getUUID())) e.setCanceled(true);
        }
    }
}
