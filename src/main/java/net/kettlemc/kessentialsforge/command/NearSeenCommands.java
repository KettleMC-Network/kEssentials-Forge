package net.kettlemc.kessentialsforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class NearSeenCommands {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    public static void register(CommandDispatcher<CommandSourceStack> d){
        d.register(CommandBuilders.literal("near")
            .executes(ctx -> listNear(ctx, 100))
            .then(CommandBuilders.argument("radius", IntegerArgumentType.integer(1, 100000))
                .executes(ctx -> listNear(ctx, IntegerArgumentType.getInteger(ctx, "radius")))
            )
        );
        d.register(CommandBuilders.literal("seen").then(CommandBuilders.argument("player", StringArgumentType.word()).executes(ctx -> {
            String name = StringArgumentType.getString(ctx, "player");
            var srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            var profile = srv.getProfileCache().get(name).orElse(null);
            if (profile == null){
                ctx.getSource().sendFailure(new TextComponent("Unbekannter Spieler."));
                return 0;
            }
            var ts = net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE.state.getSeen(profile.getId());
            if (ts == null){
                ctx.getSource().sendSuccess(new net.minecraft.network.chat.TextComponent("Keine Daten."), false);
                return 1;
            }
            ctx.getSource().sendSuccess(new net.minecraft.network.chat.TextComponent("Zuletzt gesehen: " + FMT.format(Instant.ofEpochMilli(ts))), false);
            return 1;
        })));
    }

    private static int listNear(CommandContext<CommandSourceStack> ctx, int r){
        ServerPlayer p; try { p = ctx.getSource().getPlayerOrException(); } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) { return 0; }
        var list = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList();
        List<String> names = list.getPlayers().stream()
                .filter(o -> !o.getUUID().equals(p.getUUID()))
                .filter(o -> o.distanceTo(p) <= r)
                .map(o -> o.getGameProfile().getName()).collect(Collectors.toList());
        p.sendMessage(new net.minecraft.network.chat.TextComponent("In der Nähe ("+r+" Blöcke): " + String.join(", ", names)), net.minecraft.Util.NIL_UUID);
        return 1;
    }
}
