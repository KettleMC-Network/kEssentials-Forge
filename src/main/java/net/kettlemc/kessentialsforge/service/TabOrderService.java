package net.kettlemc.kessentialsforge.service;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Group-based TAB ordering for Forge 1.18.2:
 * - One scoreboard team per LuckPerms primary group.
 * - Team key: "g" + AAA..ZZZZZ (derived from inverted weight) + "_" + short group id (<=16 total).
 *   -> Highest weight => 'AAAAA' => sorts to the top (ascending lexicographic).
 * - Team prefix from group/user meta; exactly one space at end; suffix = §r.
 */
public class TabOrderService {

    private static final Pattern AMP_CODES = Pattern.compile("&([0-9a-fk-orA-FK-OR])");
    private static final Pattern TRAILING_RESET_OR_SPACE = Pattern.compile("(?:\\s|\\u00A7r)+$");
    private static final Pattern GROUP_ID_SAFE = Pattern.compile("[^a-z0-9]+");

    public static void apply(ServerPlayer p) {
        try {
            final MinecraftServer server = p.getServer();
            if (server == null) return;
            Scoreboard sb = server.getScoreboard();

            final String playerName = p.getScoreboardName();

            GroupInfo gi = resolveGroupInfo(p.getUUID());
            final int weight = gi.weight.orElse(0);
            final String groupId = gi.groupName.orElse("default");

            final String teamKey = groupTeamKey(weight, groupId);

            PlayerTeam team = sb.getPlayerTeam(teamKey);
            if (team == null) {
                team = sb.addPlayerTeam(teamKey);
            }

            String prefix = gi.prefix.orElse("");
            setTeamPrefixSuffix(team, prefix);

            PlayerTeam current = sb.getPlayersTeam(playerName);
            if (current == null || !current.getName().equals(teamKey)) {
                if (current != null) {
                    sb.removePlayerFromTeam(playerName, current);
                }
                sb.addPlayerToTeam(playerName, team);
            }
        } catch (Throwable ignored) {
            // fail-safe
        }
    }

    public static void applyAll(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            apply(p);
        }
    }

    // ---------------- helpers ----------------

    private static class GroupInfo {
        final Optional<String> groupName;
        final Optional<Integer> weight;
        final Optional<String> prefix;
        GroupInfo(Optional<String> groupName, Optional<Integer> weight, Optional<String> prefix) {
            this.groupName = groupName;
            this.weight = weight;
            this.prefix = prefix;
        }
    }

    private static GroupInfo resolveGroupInfo(UUID uuid) {
        try {
            LuckPerms lp = LuckPermsProvider.get();
            User user = lp.getUserManager().getUser(uuid);
            if (user == null) user = lp.getUserManager().loadUser(uuid).join();
            if (user == null) return new GroupInfo(Optional.empty(), Optional.empty(), Optional.empty());

            QueryOptions qo = lp.getContextManager().getQueryOptions(user).orElse(QueryOptions.defaultContextualOptions());

            String primary = user.getPrimaryGroup();
            Group g = lp.getGroupManager().getGroup(primary);

            Optional<Integer> wOpt = Optional.empty();
            Optional<String> prefOpt = Optional.empty();

            if (g != null) {
                OptionalInt w = g.getWeight();
                if (w.isPresent()) wOpt = Optional.of(w.getAsInt());

                // prefer group prefix if present, else fall back to user prefix
                String gp = g.getCachedData().getMetaData(qo).getPrefix();
                if (gp != null) prefOpt = Optional.of(gp);
            }

            if (!prefOpt.isPresent()) {
                String up = user.getCachedData().getMetaData(qo).getPrefix();
                if (up != null) prefOpt = Optional.of(up);
            }

            return new GroupInfo(Optional.ofNullable(primary), wOpt, prefOpt);
        } catch (Throwable t) {
            return new GroupInfo(Optional.empty(), Optional.empty(), Optional.empty());
        }
    }

    private static String groupTeamKey(int weight, String groupName) {
        // Highest weight top: invert to make 'AAAAA' top and 'ZZZZZ' bottom
        int w = Math.max(0, Math.min(99999, weight));
        int inv = 99999 - w;

        String letters = toLetters(inv); // fixed 5 letters, A..Z
        String shortId = sanitizeGroupId(groupName);
        int maxSuffix = 16 - (1 + 5 + 1); // 'g' + letters + '_' = 7 chars
        if (maxSuffix < 0) maxSuffix = 0;
        if (shortId.length() > maxSuffix) shortId = shortId.substring(0, maxSuffix);

        return "g" + letters + "_" + shortId;
    }

    private static String toLetters(int value) {
        // Map 0..(26^5-1) to 'AAAAA'..'ZZZZZ'. Clamp to range.
        int max = (int) Math.pow(26, 5) - 1; // 11,881,375
        int v = Math.max(0, Math.min(max, value));
        char[] arr = new char[5];
        for (int i = 4; i >= 0; i--) {
            int rem = v % 26;
            arr[i] = (char) ('A' + rem);
            v /= 26;
        }
        return new String(arr);
    }

    private static String sanitizeGroupId(String s) {
        String x = s == null ? "group" : s.toLowerCase(Locale.ROOT);
        x = GROUP_ID_SAFE.matcher(x).replaceAll("");
        if (x.isEmpty()) x = "group";
        return x;
    }

    private static void setTeamPrefixSuffix(PlayerTeam team, String rawPrefix) {
        String norm = normalizePrefix(rawPrefix);
        team.setPlayerPrefix(new TextComponent(norm));
        team.setPlayerSuffix(new TextComponent("\u00A7r"));
    }

    private static String normalizePrefix(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String s = AMP_CODES.matcher(raw).replaceAll("\u00A7$1"); // & -> §
        s = TRAILING_RESET_OR_SPACE.matcher(s).replaceAll("");    // trim trailing spaces/§r
        return s + " ";                                           // exactly one space; reset in suffix
    }
}