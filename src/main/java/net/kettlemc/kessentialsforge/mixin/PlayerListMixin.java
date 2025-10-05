package net.kettlemc.kessentialsforge.mixin;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;
import java.net.SocketAddress;
import com.mojang.authlib.GameProfile;

/**
 * Forge 1.18.2:
 * Suppress vanilla "Player joined/left the game" system messages.
 * - Cancel the central broadcast call when it carries the vanilla keys.
 * - Redirect the two call-sites used for join/leave to no-op, as a safety net.
 */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    private static boolean kessentials$isJoinLeave(Component message) {
        if (message instanceof TranslatableComponent) {
            String key = ((TranslatableComponent) message).getKey();
            return "multiplayer.player.joined".equals(key)
                    || "multiplayer.player.left".equals(key)
                    || "multiplayer.player.joined.renamed".equals(key);
        }
        return false;
    }

    // Central broadcast used by 1.18.2: void broadcastMessage(Component, ChatType, UUID)
    @Inject(
            method = "broadcastMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/ChatType;Ljava/util/UUID;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void kessentials$hideJoinLeave(Component message, ChatType chatType, UUID sender, CallbackInfo ci) {
        if (kessentials$isJoinLeave(message)) {
            System.out.println("[kEssentials][debug] cancelled join/leave vanilla system message");
            ci.cancel();
        }
    }

    // Safety net: kill the exact calls from placeNewPlayer/remove so nothing leaks through

    @Redirect(
            method = "placeNewPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/ChatType;Ljava/util/UUID;)V"
            )
    )
    private void kessentials$noJoinBroadcast(PlayerList instance, Component message, ChatType chatType, UUID sender) {
        if (kessentials$isJoinLeave(message)) {
            System.out.println("[kEssentials][debug] blocked vanilla join broadcast call-site");
            return;
        }
        instance.broadcastMessage(message, chatType, sender);
    }

    @Redirect(
            method = "remove",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/ChatType;Ljava/util/UUID;)V"
            )
    )
    private void kessentials$noLeaveBroadcast(PlayerList instance, Component message, ChatType chatType, UUID sender) {
        if (kessentials$isJoinLeave(message)) {
            System.out.println("[kEssentials][debug] blocked vanilla leave broadcast call-site");
            return;
        }
        instance.broadcastMessage(message, chatType, sender);
    }

    // Priority & VIP join handling + dynamic +1 slot per staff (permission-based)
    @Inject(
            method = "canPlayerLogin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/network/chat/Component;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void kessentials$priorityJoin(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Component> cir) {
        try {
            PlayerList pl = (PlayerList) (Object) this;
            int online = pl.getPlayerCount();
            int max = pl.getMaxPlayers();

            boolean hasPlusOne = net.kettlemc.kessentialsforge.perm.Perms.has(profile.getId(), "join.plusone");
            boolean hasPriority = net.kettlemc.kessentialsforge.perm.Perms.has(profile.getId(), "join.priority");

            // Only the joining player benefits from +1 (no global expansion for others)
            int personalAllowed = max + (hasPlusOne ? 1 : 0);

            if (online < personalAllowed) {
                // Enough room for this player (including personal +1 if present)
                cir.setReturnValue(null);
                return;
            }

            // If base capacity is full and player has priority, kick a non-priority/non-plusone player (lowest LP weight)
            if (hasPriority) {
                net.minecraft.server.level.ServerPlayer victim = null;
                int worstWeight = Integer.MAX_VALUE;
                for (var sp : pl.getPlayers()) {
                    if (net.kettlemc.kessentialsforge.perm.Perms.has(sp.getUUID(), "join.priority")) continue;
                    if (net.kettlemc.kessentialsforge.perm.Perms.has(sp.getUUID(), "join.plusone")) continue;
                    int w = 0;
                    try { w = net.kettlemc.kessentialsforge.service.LPService.weight(sp); } catch (Throwable ignored) {}
                    if (w < worstWeight) { worstWeight = w; victim = sp; }
                }
                if (victim != null) {
                    victim.connection.disconnect(new net.minecraft.network.chat.TextComponent("Du wurdest entfernt, um einem VIP den Beitritt zu ermöglichen."));
                    cir.setReturnValue(null);
                    return;
                }
            }

            // Otherwise, let vanilla deny (server full)
        } catch (Throwable ignored) { }
    }

}
