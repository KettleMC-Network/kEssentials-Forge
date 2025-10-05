
package net.kettlemc.kessentialsforge.command;
import com.mojang.brigadier.CommandDispatcher; import net.minecraft.commands.CommandSourceStack; import net.minecraft.network.chat.Component; import net.minecraft.server.level.ServerPlayer;
public class LinkCommand {
    public static void register(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> d) {
        d.register(CommandBuilders.literal("link").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            String code = net.kettlemc.kessentialsforge.KEssentialsForge.INSTANCE.links.createCode(p.getUUID());
            p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"link_code", code)), false);
            return 1;
        }));
    }
}
