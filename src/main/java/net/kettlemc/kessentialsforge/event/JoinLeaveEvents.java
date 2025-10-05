package net.kettlemc.kessentialsforge.event;

import net.kettlemc.kessentialsforge.KEssentialsForge;
import net.kettlemc.kessentialsforge.i18n.Messages;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class JoinLeaveEvents {
    @SubscribeEvent public void onJoin(PlayerEvent.PlayerLoggedInEvent e) {
        var p = (ServerPlayer)e.getEntity();
        var state = KEssentialsForge.INSTANCE.state;
        if (KEssentialsForge.INSTANCE.config.customJoinLeaveMessages && !state.isVanished(p.getUUID())) {
            var name = p.getGameProfile().getName();
            var list = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList();
            list.getPlayers().forEach(other -> other.sendMessage(new net.minecraft.network.chat.TextComponent(Messages.get(other, "join_message", name)), net.minecraft.Util.NIL_UUID));
        }
        // Hide already vanished players from the joiner
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == p) continue;
            if (state.isVanished(other.getUUID())) {
                p.connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, other));
                p.connection.send(new ClientboundRemoveEntitiesPacket(other.getId()));
            }
        }
        KEssentialsForge.INSTANCE.config.executeJoinActions(p);
    }
    @SubscribeEvent public void onQuit(PlayerEvent.PlayerLoggedOutEvent e) {
        var p = (ServerPlayer)e.getEntity();
        if (KEssentialsForge.INSTANCE.config.customJoinLeaveMessages && !KEssentialsForge.INSTANCE.state.isVanished(p.getUUID())) {
            var name = p.getGameProfile().getName();
            var list = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList();
            list.getPlayers().forEach(other -> other.sendMessage(new net.minecraft.network.chat.TextComponent(Messages.get(other, "leave_message", name)), net.minecraft.Util.NIL_UUID));
        }
    }
}
