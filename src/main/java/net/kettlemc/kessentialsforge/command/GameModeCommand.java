
package net.kettlemc.kessentialsforge.command;
import com.mojang.brigadier.CommandDispatcher; import com.mojang.brigadier.arguments.StringArgumentType; import net.minecraft.commands.arguments.EntityArgument;
import net.kettlemc.kessentialsforge.i18n.Messages; import net.kettlemc.kessentialsforge.perm.Perms; import net.minecraft.commands.CommandSourceStack; import net.minecraft.network.chat.TextComponent; import net.minecraft.server.level.ServerPlayer; import net.minecraft.world.level.GameType;
public class GameModeCommand {
    private static GameType parse(String s){ s=s.toLowerCase(); return switch(s){ case "0","s","survival"->GameType.SURVIVAL; case "1","c","creative"->GameType.CREATIVE; case "2","a","adventure"->GameType.ADVENTURE; case "3","sp","spectator"->GameType.SPECTATOR; default->null; }; }
    public static void register(CommandDispatcher<CommandSourceStack> d){
        var gamemode = d.register(CommandBuilders.literal("gamemode").then(CommandBuilders.argument("mode", StringArgumentType.word()).executes(ctx -> {
            if (!Perms.has(ctx.getSource(), "gamemode", 2)) return 0;
            ServerPlayer p=ctx.getSource().getPlayerOrException(); var gt=parse(StringArgumentType.getString(ctx,"mode")); if(gt==null) return 0; return apply(ctx.getSource(), p, gt);
        }).then(CommandBuilders.argument("player", EntityArgument.player()).executes(ctx -> {
            if (!Perms.has(ctx.getSource(), "gamemode", 2)) return 0;
            ServerPlayer t=EntityArgument.getPlayer(ctx,"player"); var gt=parse(StringArgumentType.getString(ctx,"mode")); if(gt==null) return 0; boolean self=CommandUtil.isSelf(ctx.getSource(), t); if(!self && !Perms.hasOther(ctx.getSource(), "gamemode", 2)) return 0; return apply(ctx.getSource(), t, gt);
        }))));
        CommandBuilders.registerAliases(d, gamemode, "gm");
    }

    private static int apply(CommandSourceStack source, ServerPlayer target, GameType gameType) {
        target.setGameMode(gameType);
        String modeName=gameType.getName();
        target.displayClientMessage(new TextComponent(Messages.get(target,"gamemode_set", modeName)), false);
        if(!CommandUtil.isSelf(source, target)){
            CommandUtil.notifySourceLocalized(source, "gamemode_set_other", target.getGameProfile().getName(), modeName);
        }
        return 1;
    }
}
