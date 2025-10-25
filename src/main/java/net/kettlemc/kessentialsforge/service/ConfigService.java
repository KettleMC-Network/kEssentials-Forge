
package net.kettlemc.kessentialsforge.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class ConfigService {
    private final Path file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final List<Long> RESTART_ANNOUNCE_SECONDS = List.of(3600L, 1800L, 900L, 600L, 300L, 120L, 60L, 30L, 10L, 5L, 4L, 3L, 2L, 1L);
    private static final String DEFAULT_PERMISSION_LAYOUT = "system.%command%";
    private static final String DEFAULT_PERMISSION_OTHER_LAYOUT = "system.%command%.%other%";
    private static final String DEFAULT_PERMISSION_HOME_LAYOUT = "system.%command%.limit.%limit%";

    public int defaultMaxHomes = 3;
    public Map<String,Integer> cooldownsSeconds = new HashMap<>();
    public Map<String,Integer> warmupsSeconds = new HashMap<>();
    public List<String> rtpBiomeBlacklist = new ArrayList<>();
    public boolean vanishFakeMessages = true;
    public boolean customJoinLeaveMessages = true;
    public Map<String, String> disabledCommands = new HashMap<>();
    public List<String> onJoinActions = new ArrayList<>();
    public Map<String, List<String>> onCommandActions = new HashMap<>();
    public List<String> restartTimes = new ArrayList<>();
    public String permissionLayout = DEFAULT_PERMISSION_LAYOUT;
    public String permissionLayoutOther = DEFAULT_PERMISSION_OTHER_LAYOUT;
    public String permissionHomeLayout = DEFAULT_PERMISSION_HOME_LAYOUT;

    private ZonedDateTime nextRestart = null;
    private long lastBroadcastSeconds = Long.MIN_VALUE;

    public ConfigService(Path dataDir) { this.file = dataDir.resolve("config.json"); }
    public void load() {
        try (Reader r = Files.newBufferedReader(file)) {
            ConfigService c = gson.fromJson(r, ConfigService.class);
            if (c != null) {
                this.defaultMaxHomes = c.defaultMaxHomes;
                this.cooldownsSeconds = c.cooldownsSeconds != null ? c.cooldownsSeconds : this.cooldownsSeconds;
                this.warmupsSeconds = c.warmupsSeconds != null ? c.warmupsSeconds : this.warmupsSeconds;
                this.rtpBiomeBlacklist = c.rtpBiomeBlacklist != null ? c.rtpBiomeBlacklist : this.rtpBiomeBlacklist;
                this.vanishFakeMessages = c.vanishFakeMessages;
                this.customJoinLeaveMessages = c.customJoinLeaveMessages;
                this.disabledCommands = c.disabledCommands != null ? c.disabledCommands : this.disabledCommands;
                this.onJoinActions = c.onJoinActions != null ? c.onJoinActions : this.onJoinActions;
                this.onCommandActions = c.onCommandActions != null ? c.onCommandActions : this.onCommandActions;
                this.restartTimes = c.restartTimes != null ? c.restartTimes : this.restartTimes;
                this.permissionLayout = sanitizeLayout(c.permissionLayout, DEFAULT_PERMISSION_LAYOUT);
                this.permissionLayoutOther = sanitizeLayout(c.permissionLayoutOther, DEFAULT_PERMISSION_OTHER_LAYOUT);
                this.permissionHomeLayout = sanitizeLayout(c.permissionHomeLayout, DEFAULT_PERMISSION_HOME_LAYOUT);
            }
        } catch (Exception ignored) {}
        if (!cooldownsSeconds.containsKey("home")) cooldownsSeconds.put("home", 3);
        if (!cooldownsSeconds.containsKey("warp")) cooldownsSeconds.put("warp", 3);
        if (!cooldownsSeconds.containsKey("spawn")) cooldownsSeconds.put("spawn", 3);
        if (!cooldownsSeconds.containsKey("rtp"))  cooldownsSeconds.put("rtp", 30);
        if (!warmupsSeconds.containsKey("home")) warmupsSeconds.put("home", 0);
        if (!warmupsSeconds.containsKey("warp")) warmupsSeconds.put("warp", 0);
        if (!warmupsSeconds.containsKey("spawn")) warmupsSeconds.put("spawn", 0);
        if (!warmupsSeconds.containsKey("rtp"))  warmupsSeconds.put("rtp", 3);
        if (rtpBiomeBlacklist.isEmpty()) { rtpBiomeBlacklist.add("OCEAN"); rtpBiomeBlacklist.add("RIVER"); rtpBiomeBlacklist.add("NETHER"); }
        if (restartTimes.isEmpty()) {
            restartTimes.add("04:00");
            restartTimes.add("16:00");
        }
        if (permissionLayout == null || permissionLayout.isBlank()) permissionLayout = DEFAULT_PERMISSION_LAYOUT;
        if (permissionLayoutOther == null || permissionLayoutOther.isBlank()) permissionLayoutOther = DEFAULT_PERMISSION_OTHER_LAYOUT;
        if (permissionHomeLayout == null || permissionHomeLayout.isBlank()) permissionHomeLayout = DEFAULT_PERMISSION_HOME_LAYOUT;
        save();
        refreshNextRestart();
    }
    public void save(){ try (Writer w = Files.newBufferedWriter(file)) { gson.toJson(this, w); } catch (Exception ignored){} }

    private String sanitizeLayout(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value;
    }

    public boolean isCommandDisabled(String command) {
        if (command == null) return false;
        return disabledCommands.containsKey(command.toLowerCase(Locale.ROOT));
    }

    public Optional<String> disabledCommandMessage(String command) {
        if (command == null) return Optional.empty();
        return Optional.ofNullable(disabledCommands.get(command.toLowerCase(Locale.ROOT)));
    }

    public void executeJoinActions(ServerPlayer player) {
        executeActions(player, onJoinActions);
    }

    public void executeCommandActions(CommandSourceStack source, String commandLiteral) {
        if (commandLiteral == null) return;
        List<String> actions = new ArrayList<>();
        List<String> specific = onCommandActions.get(commandLiteral.toLowerCase(Locale.ROOT));
        if (specific != null) actions.addAll(specific);
        List<String> wildcard = onCommandActions.get("*");
        if (wildcard != null) actions.addAll(wildcard);
        if (actions.isEmpty()) return;
        ServerPlayer player = source.getEntity() instanceof ServerPlayer sp ? sp : null;
        executeActions(player, actions, source.getServer(), source);
    }

    private void executeActions(ServerPlayer player, List<String> actions) {
        executeActions(player, actions, player != null ? player.getServer() : null, player != null ? player.createCommandSourceStack() : null);
    }

    private void executeActions(ServerPlayer player, List<String> actions, MinecraftServer server, CommandSourceStack fallbackSource) {
        if (actions == null || actions.isEmpty() || server == null) return;
        CommandSourceStack serverSource = server.createCommandSourceStack();
        for (String raw : actions) {
            if (raw == null || raw.isBlank()) continue;
            String prepared = applyPlaceholders(raw.trim(), player, server);
            CommandSourceStack sourceToUse = serverSource;
            String commandString = prepared;
            if (prepared.regionMatches(true, 0, "player:", 0, 7) && player != null) {
                commandString = prepared.substring(7).trim();
                sourceToUse = player.createCommandSourceStack();
            } else if (prepared.regionMatches(true, 0, "server:", 0, 7)) {
                commandString = prepared.substring(7).trim();
                sourceToUse = serverSource;
            } else if (prepared.regionMatches(true, 0, "source:", 0, 7) && fallbackSource != null) {
                commandString = prepared.substring(7).trim();
                sourceToUse = fallbackSource;
            }

            if (commandString.isBlank()) continue;
            if (commandString.startsWith("/")) {
                commandString = commandString.substring(1);
            }
            try {
                server.getCommands().performCommand(sourceToUse, commandString);
            } catch (Exception ignored) {}
        }
    }

    private String applyPlaceholders(String input, ServerPlayer player, MinecraftServer server) {
        String result = input;
        if (player != null) {
            result = result.replace("{player}", player.getGameProfile().getName());
            result = result.replace("{uuid}", player.getUUID().toString());
        }
        if (server != null) {
            result = result.replace("{online}", Integer.toString(server.getPlayerCount()));
            result = result.replace("{max}", Integer.toString(server.getMaxPlayers()));
        }
        return result;
    }

    public void refreshNextRestart() {
        ZonedDateTime now = ZonedDateTime.now();
        this.nextRestart = calculateNextRestart(now);
        this.lastBroadcastSeconds = Long.MIN_VALUE;
    }

    private ZonedDateTime calculateNextRestart(ZonedDateTime reference) {
        if (restartTimes == null || restartTimes.isEmpty()) return null;
        ZonedDateTime best = null;
        for (String time : restartTimes) {
            LocalTime parsed = parseLocalTime(time);
            if (parsed == null) continue;
            ZonedDateTime candidate = reference.with(parsed);
            if (!candidate.isAfter(reference)) {
                candidate = candidate.plusDays(1);
            }
            if (best == null || candidate.isBefore(best)) {
                best = candidate;
            }
        }
        return best;
    }

    private LocalTime parseLocalTime(String time) {
        if (time == null) return null;
        try {
            return LocalTime.parse(time.trim());
        } catch (Exception ignored) {}
        return null;
    }

    public void tickRestart(MinecraftServer server) {
        if (server == null) return;
        if (nextRestart == null) {
            refreshNextRestart();
        }
        if (nextRestart == null) return;
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        if (!now.isBefore(nextRestart)) {
            broadcast(server, new TextComponent("Server restarting now."));
            server.halt(false);
            return;
        }
        long secondsRemaining = Duration.between(now, nextRestart).getSeconds();
        if (RESTART_ANNOUNCE_SECONDS.contains(secondsRemaining) && secondsRemaining != lastBroadcastSeconds) {
            lastBroadcastSeconds = secondsRemaining;
            sendRestartWarning(server, secondsRemaining);
        }
    }

    private void sendRestartWarning(MinecraftServer server, long secondsRemaining) {
        String formatted = formatRestartCountdown(secondsRemaining);
        Component title = new TextComponent("Automatischer Neustart").withStyle(ChatFormatting.DARK_RED);
        Component subtitle = new TextComponent("In " + formatted + " startet der Server neu.").withStyle(ChatFormatting.RED);
        broadcast(server, new TextComponent("Automatischer Neustart in " + formatted + ".").withStyle(ChatFormatting.RED));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 40, 10));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle.copy()));
            player.connection.send(new ClientboundSetTitleTextPacket(title.copy()));
        }
    }

    private String formatRestartCountdown(long secondsRemaining) {
        if (secondsRemaining >= 3600 && secondsRemaining % 3600 == 0) {
            long hours = secondsRemaining / 3600;
            return hours + " " + (hours == 1 ? "Stunde" : "Stunden");
        }
        if (secondsRemaining >= 60 && secondsRemaining % 60 == 0) {
            long minutes = secondsRemaining / 60;
            return minutes + " " + (minutes == 1 ? "Minute" : "Minuten");
        }
        return secondsRemaining + " " + (secondsRemaining == 1 ? "Sekunde" : "Sekunden");
    }

    private void broadcast(MinecraftServer server, Component component) {
        server.getPlayerList().getPlayers().forEach(p -> p.sendMessage(component, net.minecraft.Util.NIL_UUID));
        System.out.println("[kEssentials] " + component.getString());
    }

    private String sanitizeCommand(String command) {
        if (command == null) return "";
        return command.toLowerCase(Locale.ROOT);
    }

    private String applyPermissionLayout(String layout, String fallback, String command, String otherSuffix, Integer limit) {
        String result = sanitizeLayout(layout, fallback);
        String sanitizedCommand = sanitizeCommand(command);
        if (result.contains("%command%")) {
            result = result.replace("%command%", sanitizedCommand);
        } else if (!sanitizedCommand.isBlank()) {
            if (!result.isEmpty() && !result.endsWith(".")) {
                result = result + "." + sanitizedCommand;
            } else {
                result = result + sanitizedCommand;
            }
        }

        if (otherSuffix != null) {
            if (result.contains("%other%")) {
                result = result.replace("%other%", otherSuffix);
            } else if (!otherSuffix.isBlank()) {
                if (!result.endsWith(".")) {
                    result = result + "." + otherSuffix;
                } else {
                    result = result + otherSuffix;
                }
            }
        } else if (result.contains("%other%")) {
            result = result.replace("%other%", "");
        }

        if (limit != null) {
            String limitString = Integer.toString(limit);
            if (result.contains("%limit%")) {
                result = result.replace("%limit%", limitString);
            } else {
                if (!result.endsWith(".")) {
                    result = result + "." + limitString;
                } else {
                    result = result + limitString;
                }
            }
        }

        return trimDots(result);
    }

    private String trimDots(String input) {
        if (input == null) return null;
        while (input.endsWith(".")) {
            input = input.substring(0, input.length() - 1);
        }
        return input.replace("..", ".");
    }

    public String permission(String command) {
        return applyPermissionLayout(permissionLayout, DEFAULT_PERMISSION_LAYOUT, command, null, null);
    }

    public String permissionOther(String command) {
        return applyPermissionLayout(permissionLayoutOther, DEFAULT_PERMISSION_OTHER_LAYOUT, command, "other", null);
    }

    public String permissionOthers(String command) {
        return applyPermissionLayout(permissionLayoutOther, DEFAULT_PERMISSION_OTHER_LAYOUT, command, "others", null);
    }

    public String permissionHomeLimit(int limit) {
        return applyPermissionLayout(permissionHomeLayout, DEFAULT_PERMISSION_HOME_LAYOUT, "home", null, limit);
    }
}
