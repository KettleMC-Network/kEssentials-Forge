package net.kettlemc.kessentialsforge.service;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.QueryOptions;
import net.minecraft.server.level.ServerPlayer;

public class LPService {

    private static LuckPerms api;

    public static LuckPerms api() {
        if (api != null) return api;
        try { api = LuckPermsProvider.get(); } catch (Throwable ignored) {}
        return api;
    }

    /** Determine tab weight:
     *  1) Meta overrides: tab.weight / tab_priority / tabweight
     *  2) Permission-style nodes: weight.X (highest X wins)
     *  3) LuckPerms group weight: highest effective across inherited groups
     *  4) Fallback: primary group weight, else 0
     */
    public static int weight(ServerPlayer p) {
        try {
            LuckPerms lp = api(); if (lp == null) return 0;
            User user = lp.getUserManager().getUser(p.getUUID());
            if (user == null) {
                user = lp.getUserManager().loadUser(p.getUUID()).join();
                if (user == null) return 0;
            }

            QueryOptions queryOptions = lp.getContextManager().getQueryOptions(user)
                    .orElse(lp.getContextManager().getStaticQueryOptions());

            // 1) Meta override
            try {
                CachedMetaData meta = user.getCachedData().getMetaData();
                String override = meta.getMetaValue("tab.weight");
                if (override == null) override = meta.getMetaValue("tab_priority");
                if (override == null) override = meta.getMetaValue("tabweight");
                if (override != null) {
                    try { return Integer.parseInt(override.trim()); } catch (NumberFormatException ignored) {}
                }
            } catch (Throwable ignored) {}

            // 2) Permission-style nodes: weight.X (true)
            int permWeight = 0;
            for (Node n : user.resolveInheritedNodes(queryOptions)) {
                if (n.getType() == NodeType.PERMISSION && n.getValue()) {
                    String key = n.getKey(); // e.g. "weight.100"
                    if (key.startsWith("weight.")) {
                        try {
                            int w = Integer.parseInt(key.substring("weight.".length()));
                            if (w > permWeight) permWeight = w;
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            if (permWeight > 0) return permWeight;

            // 3) Highest effective group weight across inherited groups (with active contexts)
            int best = 0;
            for (Group g : user.getInheritedGroups(queryOptions)) {
                best = Math.max(best, g.getWeight().orElse(0));
            }

            // 4) Fallback: primary group
            if (best == 0) {
                String pg = user.getPrimaryGroup();
                Group g = lp.getGroupManager().getGroup(pg);
                if (g != null) best = g.getWeight().orElse(0);
            }
            return best;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public static String prefix(ServerPlayer p) {
        try {
            LuckPerms lp = api(); if (lp == null) return "";
            User user = lp.getUserManager().getUser(p.getUUID());
            if (user == null) {
                user = lp.getUserManager().loadUser(p.getUUID()).join();
                if (user == null) return "";
            }
            CachedMetaData meta = user.getCachedData().getMetaData();
            String pref = meta.getPrefix();
            return pref == null ? "" : pref;
        } catch (Throwable ignored) {
            return "";
        }
    }

    public static String suffix(ServerPlayer p) {
        try {
            LuckPerms lp = api(); if (lp == null) return "";
            User user = lp.getUserManager().getUser(p.getUUID());
            if (user == null) {
                user = lp.getUserManager().loadUser(p.getUUID()).join();
                if (user == null) return "";
            }
            CachedMetaData meta = user.getCachedData().getMetaData();
            String suff = meta.getSuffix();
            return suff == null ? "" : suff;
        } catch (Throwable ignored) {
            return "";
        }
    }
}
