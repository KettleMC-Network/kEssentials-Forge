package net.kettlemc.kessentialsforge.event;

import net.kettlemc.kessentialsforge.i18n.Messages;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.server.level.ServerPlayer;

public class VanishHelper {
    public static void applyVanish(ServerPlayer p, boolean on) {
        p.setInvisible(on); p.setSilent(on); p.noPhysics = on;
        if (net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE != null) {
            net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE.kchat.onVanishStateChanged(p, on);
        }
        var list = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList();
        for (ServerPlayer other : list.getPlayers()) {
            if (other.getUUID().equals(p.getUUID())) continue;
            boolean canSee = other.hasPermissions(3);
            if (on && !canSee) {
                other.connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, p));
                other.connection.send(new ClientboundRemoveEntitiesPacket(p.getId()));
            } else if (!on) {
                other.connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, p));
                if (!canSee) {
                    other.connection.send(new ClientboundAddPlayerPacket(p));
                    other.connection.send(new ClientboundRotateHeadPacket(p, (byte) (p.getYHeadRot() * 256.0F / 360.0F)));
                    other.connection.send(new ClientboundMoveEntityPacket.Rot(p.getId(), (byte) (p.getYRot() * 256.0F / 360.0F), (byte) (p.getXRot() * 256.0F / 360.0F), p.isOnGround()));
                }
            }
        }
        if (net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer() != null && net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE.config.vanishFakeMessages) {
            String key = on ? "leave_message" : "join_message";
            String name = p.getGameProfile().getName();
            for (ServerPlayer o : list.getPlayers()) {
                o.sendMessage(new net.minecraft.network.chat.TextComponent(Messages.get(o, key, name)), net.minecraft.Util.NIL_UUID);
            }
        }
    }
}
