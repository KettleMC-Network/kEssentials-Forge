package net.kettlemc.kessentialsforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kettlemc.kessentialsforge.KEssentialsForge;
import net.kettlemc.kessentialsforge.i18n.Messages;
import net.kettlemc.kessentialsforge.service.TpaService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.UUID;

public class TpaCommands {
    private static final long TTL_MS = 60_000;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var tpa = dispatcher.register(CommandBuilders.literal("tpa")
            .executes(ctx -> {
                CommandUtil.sendUsage(ctx.getSource(), "tpa");
                return 0;
            })
            .then(CommandBuilders.argument("target", EntityArgument.player())
                .executes(ctx -> tpa(ctx, false))));
        CommandBuilders.registerAliases(dispatcher, tpa, "tpanfrage");

        dispatcher.register(CommandBuilders.literal("tpahere")
            .executes(ctx -> {
                CommandUtil.sendUsage(ctx.getSource(), "tpahere");
                return 0;
            })
            .then(CommandBuilders.argument("target", EntityArgument.player())
                .executes(ctx -> tpa(ctx, true))));

        var tpaccept = dispatcher.register(CommandBuilders.literal("tpaccept")
            .executes(TpaCommands::accept)
            .then(CommandBuilders.argument("player", EntityArgument.player())
                .executes(TpaCommands::acceptSpecific)));
        CommandBuilders.registerAliases(dispatcher, tpaccept, "tpakzeptieren");

        var tpdeny = dispatcher.register(CommandBuilders.literal("tpdeny")
            .executes(TpaCommands::deny)
            .then(CommandBuilders.argument("player", EntityArgument.player())
                .executes(TpaCommands::denySpecific)));
        CommandBuilders.registerAliases(dispatcher, tpdeny, "tpablehnen");

        var tplist = dispatcher.register(CommandBuilders.literal("tplist")
            .executes(TpaCommands::list));
        CommandBuilders.registerAliases(dispatcher, tplist, "tpanfragen");

        dispatcher.register(CommandBuilders.literal("tptoggle")
            .executes(TpaCommands::toggle));
    }

    private static int tpa(CommandContext<CommandSourceStack> ctx, boolean here) {
        if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "tpa", 0)) {
            return 0;
        }

        ServerPlayer from;
        ServerPlayer to;
        try {
            from = ctx.getSource().getPlayerOrException();
            to = EntityArgument.getPlayer(ctx, "target");
        } catch (CommandSyntaxException ex) {
            return 0;
        }

        if (from.getUUID().equals(to.getUUID())) {
            from.displayClientMessage(new TextComponent(Messages.get(from, "tpa_self")), false);
            return 0;
        }

        if (KEssentialsForge.INSTANCE.state.isTpaBlocked(to.getUUID())) {
            from.displayClientMessage(new TextComponent(Messages.get(from, "tpa_blocked")), false);
            return 0;
        }

        KEssentialsForge.INSTANCE.tpas.create(
            from.getUUID(),
            to.getUUID(),
            from.getGameProfile().getName(),
            here,
            System.currentTimeMillis(),
            TTL_MS);

        if (here) {
            to.displayClientMessage(new TextComponent(Messages.get(to, "tpahere_to_you", from.getGameProfile().getName())), false);
        } else {
            to.displayClientMessage(new TextComponent(Messages.get(to, "tpa_to_you", from.getGameProfile().getName())), false);
        }
        from.displayClientMessage(new TextComponent(Messages.get(from, "tpa_sent", to.getGameProfile().getName())), false);
        return 1;
    }

    private static int accept(CommandContext<CommandSourceStack> ctx) {
        return handleAccept(ctx, null);
    }

    private static int acceptSpecific(CommandContext<CommandSourceStack> ctx) {
        UUID fromId;
        try {
            fromId = EntityArgument.getPlayer(ctx, "player").getUUID();
        } catch (CommandSyntaxException ex) {
            return 0;
        }
        return handleAccept(ctx, fromId);
    }

    private static int handleAccept(CommandContext<CommandSourceStack> ctx, UUID fromId) {
        ServerPlayer to;
        try {
            to = ctx.getSource().getPlayerOrException();
        } catch (CommandSyntaxException ex) {
            return 0;
        }

        TpaService.Req req = KEssentialsForge.INSTANCE.tpas.pull(to.getUUID(), fromId);
        if (req == null) {
            to.displayClientMessage(new TextComponent(Messages.get(to, "tpa_none")), false);
            return 0;
        }

        ServerPlayer from = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(req.from);
        if (from == null) {
            to.displayClientMessage(new TextComponent(Messages.get(to, "player_offline")), false);
            return 0;
        }

        if (req.here) {
            KEssentialsForge.INSTANCE.back.store(to);
            to.teleportTo(from.getLevel(), from.getX(), from.getY(), from.getZ(), from.getYHeadRot(), from.getXRot());
        } else {
            KEssentialsForge.INSTANCE.back.store(from);
            from.teleportTo(to.getLevel(), to.getX(), to.getY(), to.getZ(), to.getYHeadRot(), to.getXRot());
        }

        to.displayClientMessage(new TextComponent(Messages.get(to, "tpa_accepted")), false);
        from.displayClientMessage(new TextComponent(Messages.get(from, "tpa_done")), false);
        return 1;
    }

    private static int deny(CommandContext<CommandSourceStack> ctx) {
        return handleDeny(ctx, null);
    }

    private static int denySpecific(CommandContext<CommandSourceStack> ctx) {
        UUID fromId;
        try {
            fromId = EntityArgument.getPlayer(ctx, "player").getUUID();
        } catch (CommandSyntaxException ex) {
            return 0;
        }
        return handleDeny(ctx, fromId);
    }

    private static int handleDeny(CommandContext<CommandSourceStack> ctx, UUID fromId) {
        ServerPlayer to;
        try {
            to = ctx.getSource().getPlayerOrException();
        } catch (CommandSyntaxException ex) {
            return 0;
        }

        TpaService.Req req = KEssentialsForge.INSTANCE.tpas.pull(to.getUUID(), fromId);
        if (req == null) {
            to.displayClientMessage(new TextComponent(Messages.get(to, "tpa_none")), false);
            return 0;
        }

        to.displayClientMessage(new TextComponent(Messages.get(to, "tpa_denied")), false);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer to;
        try {
            to = ctx.getSource().getPlayerOrException();
        } catch (CommandSyntaxException ex) {
            return 0;
        }

        List<TpaService.Req> requests = KEssentialsForge.INSTANCE.tpas.listFor(to.getUUID());
        if (requests.isEmpty()) {
            to.displayClientMessage(new TextComponent(Messages.get(to, "tpa_none")), false);
            return 1;
        }

        long now = System.currentTimeMillis();
        to.displayClientMessage(new TextComponent(Messages.get(to, "tpa_list_header", requests.size())), false);

        int index = 1;
        for (TpaService.Req req : requests) {
            long remainingSeconds = Math.max(0, (req.expiresAt - now + 999) / 1000);
            String direction = Messages.get(to, req.here ? "tpa_direction_here" : "tpa_direction_to_you");
            String requester = req.fromName != null ? req.fromName : req.from.toString();
            Component line = new TextComponent(Messages.get(
                to,
                "tpa_list_entry",
                index++,
                requester,
                direction,
                remainingSeconds));
            to.displayClientMessage(line, false);
        }
        return 1;
    }

    private static int toggle(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (CommandSyntaxException ex) {
            return 0;
        }
        boolean blocked = KEssentialsForge.INSTANCE.state.toggleTpaBlocked(player.getUUID());
        player.displayClientMessage(new TextComponent("TPA " + (blocked ? "deaktiviert (blockiert)" : "aktiviert")), false);
        return 1;
    }
}
