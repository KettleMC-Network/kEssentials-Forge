package net.kettlemc.kessentialsforge.jda;

import com.mojang.authlib.GameProfile;
import net.kettlemc.kessentialsforge.KEssentialsForge;
import net.kettlemc.kessentialsforge.i18n.Messages;
import net.kettlemc.kessentialsforge.service.LinkService;
import net.kettlemc.kessentialsforge.util.ServerRef;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserWhiteListEntry;

import javax.security.auth.login.LoginException;
import java.awt.Color;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DiscordBot extends ListenerAdapter implements Runnable {
    private static final int DISCORD_MESSAGE_LIMIT = 2000;
    private static final int DISCORD_EMBED_LIMIT = 4096;

    private final Path configFile;

    private String token;
    private String channelId;
    private String statusTemplate;
    private Activity.ActivityType statusActivityType;
    private long statusIntervalSeconds;
    private boolean disableFormatting;
    private boolean useEmbeds;
    private Color embedColor;
    private boolean relayJoins;
    private boolean relayQuits;
    private boolean relayClearLagg;
    private boolean enableStopCommand;
    private boolean enableLinkCommand;

    private volatile String cachedChannelName;
    private volatile boolean startupNotified;

    private JDA jda;
    private final Set<Long> registeredGuilds = ConcurrentHashMap.newKeySet();
    private ScheduledExecutorService statusExecutor;
    private ScheduledFuture<?> statusFuture;
    private String manualStatus;

    public DiscordBot(Path configFile) throws IOException {
        this.configFile = configFile;
        loadConfig();
    }

    private Properties defaultProperties() {
        Properties defaults = new Properties();
        defaults.setProperty("token", "");
        defaults.setProperty("channelId", "");
        defaults.setProperty("statusTemplate", "{online}/{max} online");
        defaults.setProperty("statusActivity", "PLAYING");
        defaults.setProperty("statusIntervalSeconds", "30");
        defaults.setProperty("disableFormatting", "false");
        defaults.setProperty("useEmbeds", "true");
        defaults.setProperty("embedColor", "#5865F2");
        defaults.setProperty("relayJoins", "true");
        defaults.setProperty("relayQuits", "true");
        defaults.setProperty("relayClearLagg", "false");
        defaults.setProperty("enableStopCommand", "true");
        defaults.setProperty("enableLinkCommand", "true");
        return defaults;
    }

    private void loadConfig() throws IOException {
        Properties merged = defaultProperties();
        if (Files.exists(configFile)) {
            Properties existing = new Properties();
            try (Reader reader = Files.newBufferedReader(configFile)) {
                existing.load(reader);
            }
            for (String name : existing.stringPropertyNames()) {
                merged.setProperty(name, existing.getProperty(name));
            }
        }
        try (Writer writer = Files.newBufferedWriter(configFile)) {
            merged.store(writer, "kEssentials Discord config");
        }

        this.token = normalize(merged.getProperty("token"));
        this.channelId = normalize(merged.getProperty("channelId"));
        this.cachedChannelName = null;
        this.statusTemplate = merged.getProperty("statusTemplate", "");
        this.statusActivityType = parseActivityType(merged.getProperty("statusActivity", "PLAYING"));
        this.statusIntervalSeconds = parseLong(merged.getProperty("statusIntervalSeconds"), 30L);
        this.disableFormatting = parseBoolean(merged.getProperty("disableFormatting"));
        this.useEmbeds = parseBoolean(merged.getProperty("useEmbeds", "true"));
        this.embedColor = parseColor(merged.getProperty("embedColor", "#5865F2"));
        this.relayJoins = parseBoolean(merged.getProperty("relayJoins", "true"));
        this.relayQuits = parseBoolean(merged.getProperty("relayQuits", "true"));
        this.relayClearLagg = parseBoolean(merged.getProperty("relayClearLagg", "false"));
        this.enableStopCommand = parseBoolean(merged.getProperty("enableStopCommand", "true"));
        this.enableLinkCommand = parseBoolean(merged.getProperty("enableLinkCommand", "true"));
    }

    private String normalize(String value) {
        if (value == null) return null;
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    private boolean parseBoolean(String raw) {
        return raw != null && Boolean.parseBoolean(raw.trim());
    }

    private long parseLong(String raw, long fallback) {
        if (raw == null) return fallback;
        try {
            return Math.max(5L, Long.parseLong(raw.trim()));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private Color parseColor(String raw) {
        if (raw == null) {
            return Color.getHSBColor(0.64f, 0.63f, 0.95f);
        }
        String cleaned = raw.trim();
        if (cleaned.startsWith("#")) cleaned = cleaned.substring(1);
        try {
            int rgb = (int) Long.parseLong(cleaned, 16);
            return new Color(rgb);
        } catch (NumberFormatException ignored) {
            return Color.getHSBColor(0.64f, 0.63f, 0.95f);
        }
    }

    private Activity.ActivityType parseActivityType(String raw) {
        if (raw == null) return Activity.ActivityType.PLAYING;
        try {
            return Activity.ActivityType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return Activity.ActivityType.PLAYING;
        }
    }

    public boolean isEnabled() {
        return token != null;
    }

    public void start() throws LoginException {
        if (!isEnabled()) return;
        this.startupNotified = false;
        this.jda = JDABuilder.createDefault(token,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(this)
                .build();
        startStatusTask();
    }

    public void setGameStatus(String status) {
        this.manualStatus = status;
        updatePresence();
    }

    public void requestStatusUpdate() {
        updatePresence();
    }

    public void shutdown() {
        stopStatusTask();
        if (jda != null) {
            sendLifecycleMessage("discord_shutdown");
            jda.shutdown();
            jda = null;
        }
        registeredGuilds.clear();
    }

    @Override
    public void run() {
        try {
            if (isEnabled()) {
                start();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        updatePresence();
        if (!startupNotified) {
            startupNotified = true;
            sendLifecycleMessage("discord_startup");
        }
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        if (jda == null) return;
        Guild guild = event.getGuild();
        if (channelId != null && guild.getTextChannelById(channelId) == null) {
            return;
        }
        if (registeredGuilds.add(guild.getIdLong())) {
            List<net.dv8tion.jda.api.interactions.commands.build.CommandData> data = new ArrayList<>();
            data.add(Commands.slash("online", "Zeigt die aktuellen Online-Spieler"));
            if (enableStopCommand) {
                data.add(Commands.slash("stop", "Stoppt den Minecraft-Server"));
            }
            if (enableLinkCommand) {
                data.add(Commands.slash("link", "Verknüpft Discord mit Minecraft")
                        .addOption(OptionType.STRING, "code", "6-stelliger Link-Code", true));
            }
            guild.updateCommands().addCommands(data).queue();
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String name = event.getName();
        switch (name) {
            case "online" -> handleSlashOnline(event);
            case "stop" -> handleSlashStop(event);
            case "link" -> handleSlashLink(event);
            default -> {
            }
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        String raw = event.getMessage().getContentDisplay();

        if (raw.equalsIgnoreCase("!online") && isConfiguredChannel(event)) {
            event.getMessage().reply(formatOnlineList()).queue();
            return;
        }

        if (raw.startsWith("!say ")) {
            String msg = raw.substring(5);
            var server = ServerRef.get();
            if (server != null)
                server.execute(() -> server.getPlayerList().getPlayers().forEach(p -> p.sendMessage(new TextComponent("[Discord] " + msg), net.minecraft.Util.NIL_UUID)));
            return;
        }
        if (raw.startsWith("!kick ")) {
            String[] parts = raw.split(" ", 3);
            if (parts.length >= 2) {
                String target = parts[1];
                String reason = parts.length >= 3 ? parts[2] : "Kicked by Discord";
                var server = ServerRef.get();
                if (server != null)
                    server.execute(() -> {
                        var pl = server.getPlayerList().getPlayerByName(target);
                        if (pl != null)
                            pl.connection.disconnect(new TextComponent(reason));
                        else event.getMessage().reply("Spieler nicht online").queue();
                    });
            }
            return;
        }
        if (raw.startsWith("!link ")) {
            String code = raw.substring(6).trim();
            LinkService links = KEssentialsForge.INSTANCE.links;
            long did = event.getAuthor().getIdLong();
            boolean ok = links.claim(did, code);
            UUID uuid = ok ? links.getLinked(did) : null;
            var server = ServerRef.get();
            if (server != null && uuid != null) {
                GameProfile profile = server.getProfileCache().get(uuid).orElse(new GameProfile(uuid, "Player"));
                var wl = server.getPlayerList().getWhiteList();
                wl.add(new UserWhiteListEntry(profile));
                event.getMessage().reply("Verknüpft & auf die Whitelist gesetzt.").queue();
            } else {
                event.getMessage().reply("Ungültiger Link-Code oder Server offline.").queue();
            }
            return;
        }
        if (channelId != null && event.getChannel().getId().equals(channelId)) {
            var server = ServerRef.get();
            if (server != null)
                server.execute(() -> server.getPlayerList().getPlayers().forEach(p -> p.sendMessage(new TextComponent("[Discord] " + event.getAuthor().getName() + ": " + raw), net.minecraft.Util.NIL_UUID)));
        }
    }

    public void relayChatMessage(ServerPlayer player, Component formatted, String rawMessage) {
        if (player == null) return;
        String author = player.getGameProfile().getName();
        String content;
        if (disableFormatting) {
            content = author + ": " + (rawMessage == null ? "" : rawMessage);
        } else {
            content = formatted != null ? formatted.getString() : author + ": " + (rawMessage == null ? "" : rawMessage);
        }
        sendBridgeMessage(content, author, playerHead(author), null);
    }

    public void relayPlayerJoin(ServerPlayer player, String message) {
        if (!relayJoins || player == null) return;
        sendBridgeMessage(message, player.getGameProfile().getName(), playerHead(player.getGameProfile().getName()), "Spieler beigetreten");
        requestStatusUpdate();
    }

    public void relayPlayerQuit(ServerPlayer player, String message) {
        if (!relayQuits || player == null) return;
        sendBridgeMessage(message, player.getGameProfile().getName(), playerHead(player.getGameProfile().getName()), "Spieler hat das Spiel verlassen");
        requestStatusUpdate();
    }

    public boolean isClearLaggHookEnabled() {
        return relayClearLagg;
    }

    public void relayClearLaggCommand(String actor, String commandLine) {
        if (!relayClearLagg) return;
        String message = (actor == null ? "Unbekannt" : actor) + " führte " + commandLine + " aus.";
        sendBridgeMessage(message, null, null, "ClearLagg");
    }

    private void handleSlashOnline(SlashCommandInteractionEvent event) {
        event.reply(formatOnlineList()).setEphemeral(true).queue();
    }

    private void handleSlashStop(SlashCommandInteractionEvent event) {
        if (!enableStopCommand) {
            event.reply("Dieser Befehl ist deaktiviert.").setEphemeral(true).queue();
            return;
        }
        if (event.getMember() == null || !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("Du benötigst Administrator-Rechte für diesen Befehl.").setEphemeral(true).queue();
            return;
        }
        var server = ServerRef.get();
        if (server == null) {
            event.reply("Server nicht verfügbar.").setEphemeral(true).queue();
            return;
        }
        event.reply("Server wird gestoppt...").setEphemeral(true).queue();
        server.execute(() -> server.halt(false));
    }

    private void handleSlashLink(SlashCommandInteractionEvent event) {
        if (!enableLinkCommand) {
            event.reply("Dieser Befehl ist deaktiviert.").setEphemeral(true).queue();
            return;
        }
        OptionMapping codeOpt = event.getOption("code");
        if (codeOpt == null) {
            event.reply("Es fehlt der Link-Code.").setEphemeral(true).queue();
            return;
        }
        String code = codeOpt.getAsString();
        LinkService links = KEssentialsForge.INSTANCE.links;
        long did = event.getUser().getIdLong();
        boolean ok = links.claim(did, code);
        UUID uuid = ok ? links.getLinked(did) : null;
        var server = ServerRef.get();
        if (!ok || uuid == null || server == null) {
            event.reply("Der Link-Code ist ungültig oder der Server ist offline.").setEphemeral(true).queue();
            return;
        }
        server.execute(() -> {
            GameProfile profile = server.getProfileCache().get(uuid).orElse(new GameProfile(uuid, "Player"));
            var wl = server.getPlayerList().getWhiteList();
            wl.add(new UserWhiteListEntry(profile));
        });
        event.reply("Discord erfolgreich verknüpft.").setEphemeral(true).queue();
    }

    private boolean isConfiguredChannel(MessageReceivedEvent event) {
        return channelId != null && event.getChannel().getId().equals(channelId);
    }

    private void startStatusTask() {
        stopStatusTask();
        if (statusTemplate == null || statusTemplate.isBlank()) {
            updatePresence();
            return;
        }
        statusExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "discord-status");
            t.setDaemon(true);
            return t;
        });
        long interval = Math.max(5L, statusIntervalSeconds);
        statusFuture = statusExecutor.scheduleAtFixedRate(this::safeUpdatePresence, 5L, interval, TimeUnit.SECONDS);
    }

    private void stopStatusTask() {
        if (statusFuture != null) {
            statusFuture.cancel(false);
            statusFuture = null;
        }
        if (statusExecutor != null) {
            statusExecutor.shutdownNow();
            statusExecutor = null;
        }
    }

    private void safeUpdatePresence() {
        try {
            updatePresence();
        } catch (Throwable ignored) {
        }
    }

    private void updatePresence() {
        if (jda == null) return;
        String status = computeStatusText();
        Activity activity = null;
        if (status != null && !status.isBlank()) {
            activity = Activity.of(statusActivityType, status);
        }
        jda.getPresence().setActivity(activity);
    }

    private String computeStatusText() {
        if (statusTemplate != null && !statusTemplate.isBlank()) {
            var server = ServerRef.get();
            int online = 0;
            int max = 0;
            String motd = "";
            if (server != null) {
                online = server.getPlayerCount();
                max = server.getMaxPlayers();
                motd = server.getMotd();
            }
            return statusTemplate
                    .replace("{online}", Integer.toString(online))
                    .replace("{max}", Integer.toString(max))
                    .replace("{motd}", motd == null ? "" : motd);
        }
        return manualStatus;
    }

    private String formatOnlineList() {
        var server = ServerRef.get();
        if (server == null) {
            return "Der Server ist derzeit offline.";
        }
        var players = server.getPlayerList().getPlayers();
        int online = players.size();
        int max = server.getMaxPlayers();
        if (online == 0) {
            return "Niemand ist online (0/" + max + ").";
        }
        String list = players.stream()
                .map(p -> p.getGameProfile().getName())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", "));
        return "Online (" + online + "/" + max + "): " + list;
    }

    private void sendBridgeMessage(String message, String author, String avatar, String title) {
        if (message == null || message.isBlank()) return;
        TextChannel channel = resolveChannel();
        if (channel == null) return;
        String sanitized = sanitize(message);
        if (useEmbeds) {
            EmbedBuilder builder = new EmbedBuilder();
            if (title != null && !title.isBlank()) {
                builder.setTitle(limit(title, 256));
            }
            if (author != null && !author.isBlank()) {
                builder.setAuthor(author, null, avatar);
            }
            builder.setColor(embedColor);
            builder.setTimestamp(Instant.now());
            builder.setDescription(limit(sanitized, DISCORD_EMBED_LIMIT));
            channel.sendMessageEmbeds(builder.build()).queue();
        } else {
            channel.sendMessage(limit(sanitized, DISCORD_MESSAGE_LIMIT)).queue();
        }
    }

    private TextChannel resolveChannel() {
        if (channelId == null || jda == null) return null;
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            cachedChannelName = "#" + channel.getName();
        }
        return channel;
    }

    private void sendLifecycleMessage(String key) {
        if (key == null) return;
        String channelName = getFriendlyChannelName();
        if (channelName == null || channelName.isBlank()) {
            channelName = Messages.get("discord_channel_unknown");
        }
        String message = Messages.get(key, channelName);
        sendBridgeMessage(message, null, null, null);
    }

    public String getFriendlyChannelName() {
        resolveChannel();
        return cachedChannelName;
    }

    private String sanitize(String input) {
        String sanitized = input.replace("@", "@\u200B");
        sanitized = sanitized.replace("`", "`\u200B");
        return sanitized;
    }

    private String limit(String input, int max) {
        if (input.length() <= max) return input;
        if (max <= 3) return input.substring(0, max);
        return input.substring(0, max - 3) + "...";
    }

    private String playerHead(String playerName) {
        return "https://mc-heads.net/avatar/" + playerName + "/128.png";
    }
}
