
package net.kettlemc.kessentialsforge.command;
import com.mojang.brigadier.CommandDispatcher; import net.kettlemc.kessentialsforge.KEssentialsForge; import net.kettlemc.kessentialsforge.service.BackService; import net.kettlemc.kessentialsforge.util.DimUtil;
import net.minecraft.commands.CommandSourceStack; import net.minecraft.network.chat.Component; import net.minecraft.server.level.ServerLevel; import net.minecraft.server.level.ServerPlayer;
public class BackCommand {
    public static void register(CommandDispatcher<CommandSourceStack> d){ d.register(CommandBuilders.literal("back").executes(ctx -> {
        if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "back", 2)) return 0;
        ServerPlayer p=ctx.getSource().getPlayerOrException(); BackService.Loc l=KEssentialsForge.INSTANCE.back.pop(p.getUUID());
        if (l==null){ p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"no_prev_location")), false); return 0; }
        ServerLevel lvl=DimUtil.levelById(net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer(), l.dim); if (lvl==null){ p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"dim_not_found", l.dim)), false); return 0; }
        p.teleportTo(lvl,l.x,l.y,l.z,l.yaw,l.pitch); return 1;
    })); }
}
