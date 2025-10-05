
package net.kettlemc.kessentialsforge.command;
import com.mojang.brigadier.CommandDispatcher; import com.mojang.brigadier.arguments.StringArgumentType; import net.minecraft.commands.CommandSourceStack; import net.minecraft.commands.arguments.EntityArgument; import net.minecraft.network.chat.Component; import net.minecraft.server.level.ServerPlayer;
public class MsgReplyCommands {
    public static void register(CommandDispatcher<CommandSourceStack> d){
        d.register(CommandBuilders.literal("msg").then(CommandBuilders.argument("target", EntityArgument.player()).then(CommandBuilders.argument("message", StringArgumentType.greedyString()).executes(ctx -> {
            if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "msg", 0)) return 0;
            ServerPlayer from=ctx.getSource().getPlayerOrException(); ServerPlayer to=EntityArgument.getPlayer(ctx,"target"); String msg=StringArgumentType.getString(ctx,"message");
            from.sendMessage(new net.minecraft.network.chat.TextComponent("-> " + to.getGameProfile().getName() + ": " + msg), net.minecraft.Util.NIL_UUID); to.sendMessage(new net.minecraft.network.chat.TextComponent(from.getGameProfile().getName() + " -> you: " + msg), net.minecraft.Util.NIL_UUID);
            net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE.chat.setLastMsg(from.getUUID(), to.getUUID());
            for (ServerPlayer p : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) if (net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE.state.isSpy(p.getUUID()) && p!=from && p!=to) p.sendMessage(new net.minecraft.network.chat.TextComponent("[Spy] " + from.getGameProfile().getName() + " -> " + to.getGameProfile().getName() + ": " + msg), net.minecraft.Util.NIL_UUID);
            return 1;
        }))));
        d.register(CommandBuilders.literal("reply").then(CommandBuilders.argument("message", StringArgumentType.greedyString()).executes(ctx -> {
            if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "msg", 0)) return 0;
            ServerPlayer from=ctx.getSource().getPlayerOrException(); var id=net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE.chat.getLastMsg(from.getUUID());
            if (id==null){ from.sendMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(from,"no_reply_target")), net.minecraft.Util.NIL_UUID); return 0; }
            ServerPlayer to=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(id); if (to==null){ from.sendMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(from,"player_offline")), net.minecraft.Util.NIL_UUID); return 0; }
            String msg=StringArgumentType.getString(ctx,"message"); from.sendMessage(new net.minecraft.network.chat.TextComponent("-> " + to.getGameProfile().getName() + ": " + msg), net.minecraft.Util.NIL_UUID); to.sendMessage(new net.minecraft.network.chat.TextComponent(from.getGameProfile().getName() + " -> you: " + msg), net.minecraft.Util.NIL_UUID);
            for (ServerPlayer p : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) if (net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE.state.isSpy(p.getUUID()) && p!=from && p!=to) p.sendMessage(new net.minecraft.network.chat.TextComponent("[Spy] " + from.getGameProfile().getName() + " -> " + to.getGameProfile().getName() + ": " + msg), net.minecraft.Util.NIL_UUID);
            return 1;
        })));
    }
}
