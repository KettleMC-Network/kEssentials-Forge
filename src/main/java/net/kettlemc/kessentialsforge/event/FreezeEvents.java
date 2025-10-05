
package net.kettlemc.kessentialsforge.event;

import net.kettlemc.kessentialsforge.KEssentialsForge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class FreezeEvents {
    @SubscribeEvent public void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (e.player instanceof ServerPlayer p) {
            if (KEssentialsForge.INSTANCE.state.isFrozen(p.getUUID())) {
                p.setDeltaMovement(new Vec3(0,0,0)); p.hurtMarked = true;
            }
        }
    }

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!KEssentialsForge.INSTANCE.state.isFrozen(player.getUUID())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!KEssentialsForge.INSTANCE.state.isFrozen(player.getUUID())) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!KEssentialsForge.INSTANCE.state.isFrozen(player.getUUID())) return;
        event.setCanceled(true);
    }
}
