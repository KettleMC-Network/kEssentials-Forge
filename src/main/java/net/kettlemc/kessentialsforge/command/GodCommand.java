
package net.kettlemc.kessentialsforge.command;
import com.mojang.brigadier.CommandDispatcher; import net.minecraft.commands.CommandSourceStack; import net.minecraft.network.chat.Component; import net.minecraft.server.level.ServerPlayer;
public class GodCommand {
    public static void register(CommandDispatcher<CommandSourceStack> d){ d.register(CommandBuilders.literal("god").executes(ctx -> {
        if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "god", 2)) return 0;
        ServerPlayer p=ctx.getSource().getPlayerOrException(); boolean on=net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE.state.toggleGod(p.getUUID());
        p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,on? "god_on":"god_off")), false); return 1;
    })); }
}
