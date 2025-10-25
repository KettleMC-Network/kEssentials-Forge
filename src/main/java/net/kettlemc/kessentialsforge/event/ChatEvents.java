
package net.kettlemc.kessentialsforge.event;

import net.kettlemc.kessentialsforge.KEssentialsForge;
import net.kettlemc.kessentialsforge.i18n.Messages;
import net.kettlemc.kessentialsforge.jda.DiscordBot;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Locale;

public class ChatEvents {
    private static DiscordBot discord;

    public static void setDiscord(DiscordBot d) {
        discord = d;
    }

    @SubscribeEvent
    public void onChat(ServerChatEvent e) {
        var player = e.getPlayer();
        if (KEssentialsForge.INSTANCE.state.isMuted(player.getUUID())) {
            e.setCanceled(true);
            player.sendMessage(new TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(player, "muted")), net.minecraft.Util.NIL_UUID);
            return;
        }
        if (discord != null && !e.isCanceled()) {
            var component = KEssentialsForge.INSTANCE.kchat.formatChat(player, e.getMessage());
            discord.relayChatMessage(player, component, e.getMessage());
        }
    }

    @SubscribeEvent
    public void onJoin(PlayerEvent.PlayerLoggedInEvent e) {
        var entity = e.getEntity();
        KEssentialsForge.INSTANCE.state.setSeen(entity.getUUID(), System.currentTimeMillis());
        if (discord != null && entity instanceof ServerPlayer player) {
            boolean vanished = KEssentialsForge.INSTANCE.state.isVanished(player.getUUID());
            if (!vanished) {
                discord.relayPlayerJoin(player, Messages.get(player, "discord_join", player.getGameProfile().getName()));
            }
            String channelName = discord.getFriendlyChannelName();
            if (channelName == null || channelName.isBlank()) {
                channelName = Messages.get(player, "discord_channel_unknown");
            }
            player.sendMessage(new TextComponent(Messages.get(player, "discord_join_hint", channelName)), net.minecraft.Util.NIL_UUID);
        }
    }

    @SubscribeEvent
    public void onQuit(PlayerEvent.PlayerLoggedOutEvent e) {
        var entity = e.getEntity();
        KEssentialsForge.INSTANCE.state.setSeen(entity.getUUID(), System.currentTimeMillis());
        if (discord != null && entity instanceof ServerPlayer player) {
            if (!KEssentialsForge.INSTANCE.state.isVanished(player.getUUID())) {
                discord.relayPlayerQuit(player, Messages.get(player, "discord_leave", player.getGameProfile().getName()));
            }
        }
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        if (discord == null || !discord.isClearLaggHookEnabled()) return;
        String literal = extractRootLiteral(event);
        if (literal == null) return;
        literal = literal.toLowerCase(Locale.ROOT);
        if (!literal.equals("lagg") && !literal.equals("clearlagg")) return;
        CommandSourceStack source = event.getParseResults().getContext().getSource();
        String actor = source.getDisplayName() != null ? source.getDisplayName().getString() : source.getTextName();
        String commandLine = event.getParseResults().getReader().getString().trim();
        if (!commandLine.startsWith("/")) {
            commandLine = "/" + commandLine;
        }
        discord.relayClearLaggCommand(actor, commandLine);
    }

    private String extractRootLiteral(CommandEvent event) {
        var context = event.getParseResults().getContext();
        if (context == null) return null;
        var nodes = context.getNodes();
        if (nodes == null || nodes.isEmpty()) {
            String input = event.getParseResults().getReader().getString().trim();
            if (input.startsWith("/")) input = input.substring(1);
            int space = input.indexOf(' ');
            return space == -1 ? input : input.substring(0, space);
        }
        return nodes.get(0).getNode().getName();
    }
}
