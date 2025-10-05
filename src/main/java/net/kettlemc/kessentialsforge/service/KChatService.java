package net.kettlemc.kessentialsforge.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KChatService {
    private final Path file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final List<String> DEFAULT_SCOREBOARD_LINES = List.of(
            "&7Rank: &f%luckperms-prefix%",
            "&7World: &f%world_name_overworld%",
            "&7Kills: &f%statistic_minecraft:mob_kills%"
    );

    private static final List<String> DEFAULT_TAB_HEADER = List.of(
            "&f&m                                                &r",
            "&7☽ &9KettleMC Network &e☀",
            "&r&7&l>> &6Welcome &l%player%&7&l! &7&l<<",
            ""
    );

    private static final List<String> DEFAULT_TAB_FOOTER = List.of(
            "",
            "&7&lStoneBlock 3",
            "&6PROTOTYP PROJECT",
            "&f&m                                                &r"
    );

    public static class Config {
        public boolean motdEnabled = true;
        public String motdMiniMessage = "<gray><bold>KettleMC</bold></gray> <yellow>Server</yellow>\n<dark_gray>➜</dark_gray> <white>Join now!</white>";
        public List<String> tabHeader = new ArrayList<>();
        public List<String> tabFooter = new ArrayList<>();
        public HeaderFooterCfg headerFooter = new HeaderFooterCfg();
        public ScoreboardCfg scoreboard = new ScoreboardCfg();
        public String chatFormat = "&r%luckperms-prefix% &8| &7%player% &8» &7%message%";
        public Map<String, List<String>> animations = new HashMap<>();
        public Map<String, AnimationCfg> animationsAdv = new HashMap<>();
        public NameDisplayCfg nameDisplay = new NameDisplayCfg();
    }
    public static class HeaderFooterCfg {
        public boolean enabled = true;
        public List<String> header = new ArrayList<>(DEFAULT_TAB_HEADER);
        public List<String> footer = new ArrayList<>(DEFAULT_TAB_FOOTER);
    }
    public static class ScoreboardCfg {
        public boolean enabled = true;
        public String title = "&6&lKettleMC";
        public List<String> lines = new ArrayList<>(DEFAULT_SCOREBOARD_LINES);
    }
    public static class AnimationCfg {
        public int changeInterval = 20;
        public List<String> texts = new ArrayList<>();
    }
    public static class NameDisplayCfg {
        public boolean enabled = true;
        public String format = "&r%luckperms-prefix% &7%player%";
        public boolean hideWhenVanished = true;
    }

    private Config cfg = new Config();

    private final Map<String, AnimationState> animationStates = new HashMap<>();
    private final Map<UUID, TabState> tabCache = new HashMap<>();
    private final Map<UUID, BoardState> boardCache = new HashMap<>();
    private final Map<UUID, NameState> nameCache = new HashMap<>();
    private long ticks = 0;
    private boolean motdApplied = false;

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final GsonComponentSerializer adventureSerializer = GsonComponentSerializer.gson();

    public KChatService(Path dataDir) { this.file = dataDir.resolve("kchat.json"); }

    public void load() {
        Config loaded = null;
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file)) {
                loaded = gson.fromJson(r, Config.class);
            } catch (IOException ignored) {}
        }
        cfg = loaded != null ? loaded : new Config();
        ensureCollections();
        rebuildAnimations();
        tabCache.clear();
        boardCache.clear();
        nameCache.clear();
        motdApplied = false;
        save();
    }
    public void save() {
        try (Writer w = Files.newBufferedWriter(file)) { gson.toJson(cfg, w); } catch (IOException ignored) {}
    }

    public void apply(ServerPlayer p) {
        updateTab(p, true);
        updateScoreboard(p, true);
        updateNameDisplay(p, true);
        sendExistingNameDisplays(p);
    }

    public void tick(MinecraftServer server) {
        ticks++;
        boolean animationChanged = advanceAnimations();
        if (!motdApplied) {
            applyMotd(server);
        }
        if (ticks % 20 == 0 || animationChanged) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                updateTab(player, false);
                updateScoreboard(player, false);
                updateNameDisplay(player, false);
            }
        }
        if (ticks % (20 * 60) == 0) {
            applyMotd(server);
        }
    }

    public void applyMotd(MinecraftServer server) {
        try {
            if (!cfg.motdEnabled) {
                motdApplied = true;
                return;
            }
            Component motd = deserializeMiniMessage(cfg.motdMiniMessage);
            server.getStatus().setDescription(motd);
            server.getStatus().invalidateJson();
            motdApplied = true;
        } catch (Throwable ignored) {}
    }

    public void onQuit(ServerPlayer player) {
        tabCache.remove(player.getUUID());
        BoardState state = boardCache.remove(player.getUUID());
        if (state != null) {
            state.clear(player);
        }
        clearNameDisplay(player);
    }

    public void reloadAndApply(MinecraftServer server) {
        load();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            apply(player);
        }
        refreshAllNameDisplays(server, true);
        applyMotd(server);
    }

    // ===== Legacy color & simple placeholder handling =====
    private static final Pattern LEGACY = Pattern.compile("&([0-9a-fk-or])", Pattern.CASE_INSENSITIVE);
    private static final Pattern LP_META = Pattern.compile("%lp-meta:([a-z0-9_:-]+)%", Pattern.CASE_INSENSITIVE);
    private static final Pattern ANIMATION = Pattern.compile("%animation:([a-z0-9_:-]+)%", Pattern.CASE_INSENSITIVE);
    private static final Pattern STATISTIC = Pattern.compile("%statistic_([a-z0-9_:/]+)%", Pattern.CASE_INSENSITIVE);
    private static final Pattern WORLD_NAME = Pattern.compile("%world_name_([a-z0-9_:-]+)%", Pattern.CASE_INSENSITIVE);

    private MutableComponent legacyToComponent(String in){
        if (in == null || in.isEmpty()) return new TextComponent("");
        MutableComponent root = new net.minecraft.network.chat.TextComponent("");
        Style style = Style.EMPTY;
        int i = 0;
        while (i < in.length()){
            Matcher m = LEGACY.matcher(in).region(i, in.length());
            if (m.find()){
                int start = m.start();
                if (start > i){
                    root.append(new net.minecraft.network.chat.TextComponent(in.substring(i, start)).withStyle(style));
                }
                String code = m.group(1).toLowerCase();
                if ("r".equals(code)) style = Style.EMPTY;
                else if ("l".equals(code)) style = style.withBold(true);
                else if ("n".equals(code)) style = style.withUnderlined(true);
                else if ("m".equals(code)) style = style.withStrikethrough(true);
                else if ("o".equals(code)) style = style.withItalic(true);
                else {
                    ChatFormatting fmt = ChatFormatting.getByCode(code.charAt(0));
                    if (fmt != null) style = Style.EMPTY.withColor(TextColor.fromLegacyFormat(fmt));
                }
                i = m.end();
            } else {
                root.append(new net.minecraft.network.chat.TextComponent(in.substring(i)).withStyle(style));
                break;
            }
        }
        return root;
    }

    private String replacePlaceholders(ServerPlayer p, String s){
        if (s == null) return "";
        MinecraftServer server = p.getServer();
        String result = s;
        result = result.replace("%player%", p.getGameProfile().getName());
        if (server != null) {
            int visible = 0;
            try {
                var inst = net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE;
                if (inst != null && inst.state != null) {
                    for (var sp : server.getPlayerList().getPlayers()) {
                        if (!inst.state.isVanished(sp.getUUID())) visible++;
                    }
                } else {
                    visible = server.getPlayerCount();
                }
            } catch (Throwable ignored) {
                visible = server.getPlayerCount();
            }
            result = result.replace("%online%", Integer.toString(visible))
                    .replace("%server_max%", Integer.toString(server.getMaxPlayers()));
        }
        result = result.replace("%ping%", Integer.toString(p.latency));

        String dimId = p.level.dimension().location().toString();
        String simpleDim = dimId.contains(":") ? dimId.substring(dimId.indexOf(':') + 1) : dimId;
        result = result.replace("%world%", simpleDim);
        Matcher worldNameMatcher = WORLD_NAME.matcher(result);
        StringBuffer buffer = new StringBuffer();
        while (worldNameMatcher.find()) {
            String key = worldNameMatcher.group(1).toLowerCase(Locale.ROOT);
            worldNameMatcher.appendReplacement(buffer, Matcher.quoteReplacement(worldDisplayName(simpleDim, key)));
        }
        worldNameMatcher.appendTail(buffer);
        result = buffer.toString();

        result = applyLuckPerms(p, result);
        result = applyStatistics(p, result);
        result = applyAnimations(result);
        return result;
    }

    private String applyLuckPerms(ServerPlayer p, String in) {
        try {
            LuckPerms lp = LuckPermsProvider.get();
            CachedMetaData meta = lp.getPlayerAdapter(ServerPlayer.class).getMetaData(p);
            String prefix = sanitizeLegacy(meta.getPrefix());
            String suffix = sanitizeLegacy(meta.getSuffix());
            String result = in.replace("%luckperms-prefix%", prefix)
                    .replace("%rank%", prefix)
                    .replace("%luckperms-suffix%", suffix);
            Matcher matcher = LP_META.matcher(result);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String key = matcher.group(1);
                String value = sanitizeLegacy(meta.getMetaValue(key));
                matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
            }
            matcher.appendTail(sb);
            return sb.toString();
        } catch (Throwable ignored) {
            return in.replace("%luckperms-prefix%", "")
                    .replace("%rank%", "")
                    .replace("%luckperms-suffix%", "");
        }
    }

    private String sanitizeLegacy(String value) {
        if (value == null) return "";
        return value.replace('§', '&');
    }

    private String applyStatistics(ServerPlayer p, String input) {
        Matcher matcher = STATISTIC.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String id = matcher.group(1).toLowerCase(Locale.ROOT);
            String replacement = "0";
            try {
                ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl != null) {
                    Stat<ResourceLocation> stat = Stats.CUSTOM.get(rl);
                    if (stat != null) {
                        replacement = Integer.toString(p.getStats().getValue(stat));
                    }
                }
            } catch (Throwable ignored) {}
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String applyAnimations(String input) {
        Matcher matcher = ANIMATION.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1).toLowerCase(Locale.ROOT);
            AnimationState state = animationStates.get(key);
            String frame = state != null ? state.current() : "";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(frame));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String worldDisplayName(String dim, String placeholderKey) {
        if (placeholderKey.equals(dim)) {
            return switch (dim) {
                case "overworld" -> "Overworld";
                case "the_nether", "nether" -> "Nether";
                case "the_end", "end" -> "End";
                default -> dim;
            };
        }
        return placeholderKey;
    }

    public Component formatChat(ServerPlayer p, String message) {
        String in = cfg.chatFormat.replace("%message%", message);
        return legacyToComponent(replacePlaceholders(p, in));
    }

    private void ensureCollections() {
        if (cfg.tabHeader == null) cfg.tabHeader = new ArrayList<>();
        if (cfg.tabFooter == null) cfg.tabFooter = new ArrayList<>();
        if (cfg.headerFooter == null) {
            cfg.headerFooter = new HeaderFooterCfg();
        } else {
            if (cfg.headerFooter.header == null) {
                cfg.headerFooter.header = new ArrayList<>(DEFAULT_TAB_HEADER);
            } else if (cfg.headerFooter.header.isEmpty() && cfg.tabHeader != null && !cfg.tabHeader.isEmpty()) {
                cfg.headerFooter.header = new ArrayList<>(cfg.tabHeader);
            }
            if (cfg.headerFooter.footer == null) {
                cfg.headerFooter.footer = new ArrayList<>(DEFAULT_TAB_FOOTER);
            } else if (cfg.headerFooter.footer.isEmpty() && cfg.tabFooter != null && !cfg.tabFooter.isEmpty()) {
                cfg.headerFooter.footer = new ArrayList<>(cfg.tabFooter);
            }
        }
        if (cfg.scoreboard == null) {
            cfg.scoreboard = new ScoreboardCfg();
        } else {
            if (cfg.scoreboard.lines == null) {
                cfg.scoreboard.lines = new ArrayList<>(DEFAULT_SCOREBOARD_LINES);
            } else if (cfg.scoreboard.lines.isEmpty()) {
                cfg.scoreboard.lines.addAll(DEFAULT_SCOREBOARD_LINES);
            }
        }
        if (cfg.animations == null) cfg.animations = new HashMap<>();
        if (cfg.animationsAdv == null) cfg.animationsAdv = new HashMap<>();
        if (cfg.nameDisplay == null) {
            cfg.nameDisplay = new NameDisplayCfg();
        } else if (cfg.nameDisplay.format == null) {
            cfg.nameDisplay.format = "&r%luckperms-prefix% &7%player%";
        }
    }

    private void rebuildAnimations() {
        animationStates.clear();
        cfg.animations.forEach((name, frames) -> {
            if (frames != null && !frames.isEmpty()) {
                animationStates.put(name.toLowerCase(Locale.ROOT), new AnimationState(frames, 20));
            }
        });
        cfg.animationsAdv.forEach((name, adv) -> {
            if (adv != null && adv.texts != null && !adv.texts.isEmpty()) {
                int interval = Math.max(1, adv.changeInterval);
                animationStates.put(name.toLowerCase(Locale.ROOT), new AnimationState(adv.texts, interval));
            }
        });
    }

    private boolean advanceAnimations() {
        boolean changed = false;
        for (AnimationState state : animationStates.values()) {
            if (state.tick()) {
                changed = true;
            }
        }
        return changed;
    }

    private Component deserializeMiniMessage(String text) {
        try {
            var adventure = miniMessage.deserialize(text);
            String json = adventureSerializer.serialize(adventure);
            Component component = Component.Serializer.fromJson(json);
            return component != null ? component : new TextComponent("");
        } catch (IllegalArgumentException | JsonParseException ex) {
            return legacyToComponent(text);
        }
    }

    private void updateTab(ServerPlayer player, boolean force) {
        HeaderFooterCfg headerCfg = cfg.headerFooter;
        List<String> headerLines;
        List<String> footerLines;
        if (headerCfg != null && headerCfg.enabled) {
            headerLines = headerCfg.header;
            footerLines = headerCfg.footer;
        } else {
            headerLines = List.of();
            footerLines = List.of();
        }

        String header = formatLines(player, headerLines);
        String footer = formatLines(player, footerLines);
        TabState state = tabCache.computeIfAbsent(player.getUUID(), u -> new TabState());
        if (force || !header.equals(state.lastHeader) || !footer.equals(state.lastFooter)) {
            state.lastHeader = header;
            state.lastFooter = footer;
            player.connection.send(new ClientboundTabListPacket(legacyToComponent(header), legacyToComponent(footer)));
        }
    }

    private void updateScoreboard(ServerPlayer player, boolean force) {
        if (cfg.scoreboard == null || !cfg.scoreboard.enabled || cfg.scoreboard.lines == null || cfg.scoreboard.lines.isEmpty()) {
            BoardState state = boardCache.remove(player.getUUID());
            if (state != null) {
                state.clear(player);
            }
            return;
        }

        BoardState state = boardCache.computeIfAbsent(player.getUUID(), u -> new BoardState());
        Scoreboard scoreboard = state.scoreboard;
        Objective objective = state.objective;
        String titleRaw = replacePlaceholders(player, cfg.scoreboard.title);
        if (force || !titleRaw.equals(state.lastTitle)) {
            state.lastTitle = titleRaw;
            objective.setDisplayName(legacyToComponent(titleRaw));
            if (state.initialized) {
                state.sendObjectiveUpdate(player);
            }
        }

        state.ensureInitialized(player);

        List<String> processed = new ArrayList<>();
        int maxLines = Math.min(cfg.scoreboard.lines.size(), BoardState.LINE_KEYS.length);
        for (int i = 0; i < maxLines; i++) {
            String line = cfg.scoreboard.lines.get(i);
            processed.add(replacePlaceholders(player, line));
        }

        for (int i = 0; i < processed.size(); i++) {
            String key = BoardState.LINE_KEYS[i];
            String raw = processed.get(i);
            Component value = legacyToComponent(raw);
            PlayerTeam team = scoreboard.getPlayerTeam(BoardState.TEAM_NAMES[i]);
            if (team == null) {
                team = scoreboard.addPlayerTeam(BoardState.TEAM_NAMES[i]);
                scoreboard.addPlayerToTeam(key, team);
            }
            boolean changed = force || !raw.equals(state.lastLines[i]);
            team.setPlayerPrefix(value);
            team.setPlayerSuffix(new TextComponent(""));
            if (!state.teamActive[i]) {
                state.teamActive[i] = true;
                state.sendTeamAdd(player, team);
            } else if (changed) {
                state.sendTeamUpdate(player, team);
            }
            if (changed) {
                state.lastLines[i] = raw;
            }
            Score score = scoreboard.getOrCreatePlayerScore(key, objective);
            int valueScore = processed.size() - i;
            score.setScore(valueScore);
            state.sendScoreUpdate(player, score);
        }

        // Remove leftover entries
        for (int i = processed.size(); i < BoardState.LINE_KEYS.length; i++) {
            String key = BoardState.LINE_KEYS[i];
            if (state.lastLines[i] != null) {
                scoreboard.resetPlayerScore(key, objective);
                state.lastLines[i] = null;
                state.sendScoreRemoval(player, key);
            }
            if (state.teamActive[i]) {
                scoreboard.removePlayerFromTeam(key);
                state.teamActive[i] = false;
                state.sendTeamRemoval(player, BoardState.TEAM_NAMES[i]);
            }
        }
    }

    private String formatLines(ServerPlayer player, List<String> lines) {
        if (lines == null || lines.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(replacePlaceholders(player, lines.get(i)));
        }
        return sb.toString();
    }

    private void updateNameDisplay(ServerPlayer player, boolean force) {
        if (player == null || player.getServer() == null) {
            return;
        }
        if (cfg.nameDisplay == null || !cfg.nameDisplay.enabled) {
            clearNameDisplay(player);
            return;
        }

        MinecraftServer server = player.getServer();
        UUID uuid = player.getUUID();
        NameState state = nameCache.computeIfAbsent(uuid, NameState::new);
        state.team.getPlayers().clear();
        state.team.getPlayers().add(player.getScoreboardName());

        String format = cfg.nameDisplay.format != null ? cfg.nameDisplay.format : "&r%luckperms-prefix% &7%player%";
        int idx = format.indexOf("%player%");
        String prefixTemplate;
        String suffixTemplate;
        if (idx < 0) {
            prefixTemplate = format;
            suffixTemplate = "";
        } else {
            prefixTemplate = format.substring(0, idx);
            suffixTemplate = format.substring(idx + "%player%".length());
        }

        String prefixRaw = replacePlaceholders(player, prefixTemplate);
        String suffixRaw = replacePlaceholders(player, suffixTemplate);

        boolean changed = force;
        if (!prefixRaw.equals(state.lastPrefix)) {
            state.lastPrefix = prefixRaw;
            state.team.setPlayerPrefix(legacyToComponent(prefixRaw));
            changed = true;
        }
        if (!suffixRaw.equals(state.lastSuffix)) {
            state.lastSuffix = suffixRaw;
            state.team.setPlayerSuffix(legacyToComponent(suffixRaw));
            changed = true;
        }

        boolean vanished = false;
        if (net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE != null) {
            vanished = net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE.state.isVanished(uuid);
        }
        PlayerTeam.Visibility visibility = (cfg.nameDisplay.hideWhenVanished && vanished)
                ? PlayerTeam.Visibility.NEVER
                : PlayerTeam.Visibility.ALWAYS;
        if (visibility != state.lastVisibility) {
            state.lastVisibility = visibility;
            state.team.setNameTagVisibility(visibility);
            changed = true;
        }

        boolean seeFriendly = !vanished;
        if (seeFriendly != state.lastSeeFriendlyInvisibles) {
            state.lastSeeFriendlyInvisibles = seeFriendly;
            state.team.setSeeFriendlyInvisibles(seeFriendly);
            changed = true;
        }

        if (!state.added) {
            sendTeamPacket(server, state.team, true);
            state.added = true;
        } else if (changed) {
            sendTeamPacket(server, state.team, false);
        }
    }

    private void sendExistingNameDisplays(ServerPlayer target) {
        if (target == null || target.getServer() == null) return;
        if (cfg.nameDisplay == null || !cfg.nameDisplay.enabled) return;
        UUID self = target.getUUID();
        for (Map.Entry<UUID, NameState> entry : nameCache.entrySet()) {
            if (entry.getKey().equals(self)) continue;
            NameState state = entry.getValue();
            if (!state.added) continue;
            target.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(state.team, true));
        }
    }

    private void sendTeamPacket(MinecraftServer server, PlayerTeam team, boolean add) {
        if (server == null) return;
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            viewer.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, add));
        }
    }

    private void sendTeamRemoval(MinecraftServer server, PlayerTeam team) {
        if (server == null) return;
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            viewer.connection.send(ClientboundSetPlayerTeamPacket.createRemovePacket(team));
        }
    }

    private void clearNameDisplay(ServerPlayer player) {
        if (player == null) return;
        NameState state = nameCache.remove(player.getUUID());
        if (state == null) return;
        MinecraftServer server = player.getServer();
        if (state.added) {
            sendTeamRemoval(server, state.team);
        }
    }

    public void onVanishStateChanged(ServerPlayer player, boolean vanished) {
        updateNameDisplay(player, true);
    }

    private void refreshAllNameDisplays(MinecraftServer server, boolean force) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updateNameDisplay(player, force);
        }
    }

    private static class TabState {
        String lastHeader = "";
        String lastFooter = "";
    }

    private static class BoardState {
        private static final String[] LINE_KEYS;
        private static final String[] TEAM_NAMES;
        static {
            LINE_KEYS = new String[15];
            TEAM_NAMES = new String[15];
            ChatFormatting[] colors = ChatFormatting.values();
            for (int i = 0; i < LINE_KEYS.length; i++) {
                LINE_KEYS[i] = i < colors.length ? colors[i].toString() : ("§" + Integer.toHexString(i));
                TEAM_NAMES[i] = "kes_line_" + i;
            }
        }

        final Scoreboard scoreboard = new Scoreboard();
        final Objective objective;
        final String[] lastLines = new String[LINE_KEYS.length];
        final boolean[] teamActive = new boolean[LINE_KEYS.length];
        boolean initialized = false;
        String lastTitle = "";

        BoardState() {
            objective = scoreboard.addObjective("kessentials", ObjectiveCriteria.DUMMY, new TextComponent(""), ObjectiveCriteria.RenderType.INTEGER);
        }

        void ensureInitialized(ServerPlayer player) {
            if (!initialized) {
                initialized = true;
                player.connection.send(new ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_ADD));
                player.connection.send(new ClientboundSetDisplayObjectivePacket(Scoreboard.DISPLAY_SLOT_SIDEBAR, objective));
            }
        }

        void sendObjectiveUpdate(ServerPlayer player) {
            if (initialized) {
                player.connection.send(new ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_CHANGE));
            }
        }

        void sendTeamAdd(ServerPlayer player, PlayerTeam team) {
            player.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true));
        }

        void sendTeamUpdate(ServerPlayer player, PlayerTeam team) {
            player.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, false));
        }

        void sendTeamRemoval(ServerPlayer player, String teamName) {
            PlayerTeam team = scoreboard.getPlayerTeam(teamName);
            if (team != null) {
                player.connection.send(ClientboundSetPlayerTeamPacket.createRemovePacket(team));
            }
        }

        void sendScoreUpdate(ServerPlayer player, Score score) {
            player.connection.send(new ClientboundSetScorePacket(ServerScoreboard.Method.CHANGE, objective.getName(), score.getOwner(), score.getScore()));
        }

        void sendScoreRemoval(ServerPlayer player, String key) {
            player.connection.send(new ClientboundSetScorePacket(ServerScoreboard.Method.REMOVE, objective.getName(), key, 0));
        }

        void clear(ServerPlayer player) {
            if (!initialized) return;
            for (int i = 0; i < LINE_KEYS.length; i++) {
                if (lastLines[i] != null) {
                    sendScoreRemoval(player, LINE_KEYS[i]);
                    scoreboard.resetPlayerScore(LINE_KEYS[i], objective);
                    lastLines[i] = null;
                }
                if (teamActive[i]) {
                    sendTeamRemoval(player, TEAM_NAMES[i]);
                    scoreboard.removePlayerFromTeam(LINE_KEYS[i]);
                    teamActive[i] = false;
                }
            }
            player.connection.send(new ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_REMOVE));
            initialized = false;
            lastTitle = "";
        }
    }

    private static class NameState {
        final Scoreboard scoreboard = new Scoreboard();
        final PlayerTeam team;
        final String teamName;
        String lastPrefix = "";
        String lastSuffix = "";
        PlayerTeam.Visibility lastVisibility = PlayerTeam.Visibility.ALWAYS;
        boolean lastSeeFriendlyInvisibles = true;
        boolean added = false;

        NameState(UUID uuid) {
            String base = uuid.toString().replace("-", "");
            String suffix = base.length() > 12 ? base.substring(0, 12) : base;
            this.teamName = "kes_" + suffix;
            this.team = scoreboard.addPlayerTeam(teamName);
            this.team.setNameTagVisibility(PlayerTeam.Visibility.ALWAYS);
            this.team.setSeeFriendlyInvisibles(true);
        }
    }

    private static class AnimationState {
        private final List<String> frames;
        private final int interval;
        private int index = 0;
        private int tickCounter = 0;

        private AnimationState(List<String> frames, int interval) {
            this.frames = new ArrayList<>(frames);
            this.interval = interval;
        }

        String current() {
            if (frames.isEmpty()) return "";
            return frames.get(index % frames.size());
        }

        boolean tick() {
            if (frames.size() <= 1) return false;
            tickCounter++;
            if (tickCounter >= Math.max(1, interval / 2)) {
                tickCounter = 0;
                index = (index + 1) % frames.size();
                return true;
            }
            return false;
        }
    }
}
