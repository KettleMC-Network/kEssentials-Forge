
package net.kettlemc.kessentialsforge.command;
import com.mojang.brigadier.CommandDispatcher; import net.minecraft.commands.CommandSourceStack; import net.minecraft.server.level.ServerPlayer;
public class ExtinguishCommand {
    public static void register(CommandDispatcher<CommandSourceStack> d){
        d.register(CommandBuilders.literal("ext").executes(ctx -> { if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(),"extinguish",0)) return 0;
            ServerPlayer p=ctx.getSource().getPlayerOrException(); p.clearFire(); p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"extinguish_done")), false); return 1; }));
    }
}
