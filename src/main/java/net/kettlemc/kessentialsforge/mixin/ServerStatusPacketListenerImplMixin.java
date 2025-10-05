package net.kettlemc.kessentialsforge.mixin;

import net.kettlemc.kessentialsforge.KEssentialsForge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerStatusPacketListenerImpl;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ensures our custom MOTD (from KChatService config) is applied to the
 * server list ping, even if vanilla resets the description from server.properties
 * right before sending the response.
 *
 * Target: ServerStatusPacketListenerImpl#handleStatusRequest(ServerboundStatusRequestPacket)
 * Env: SERVER (Forge 1.18.2, Mojang mappings)
 */
@Mixin(ServerStatusPacketListenerImpl.class)
public abstract class ServerStatusPacketListenerImplMixin {

    @Shadow private MinecraftServer server;

    @Inject(method = "handleStatusRequest", at = @At("TAIL"))
    private void kessentials$applyCustomMotd(ServerboundStatusRequestPacket packet, CallbackInfo ci) {
        try {
            var inst = KEssentialsForge.INSTANCE;
            if (inst != null && inst.kchat != null && this.server != null) {
                // Re-apply our MOTD after vanilla sets it from server.properties
                inst.kchat.applyMotd(this.server);
            }
        } catch (Throwable ignored) {}
    }
}
