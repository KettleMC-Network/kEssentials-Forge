
package net.kettlemc.kessentialsforge.command;
import com.mojang.brigadier.CommandDispatcher; import net.minecraft.commands.arguments.EntityArgument; import net.minecraft.commands.CommandSourceStack; import net.minecraft.server.level.ServerPlayer;
public class ClearCommand {
    public static void register(CommandDispatcher<CommandSourceStack> d){
        d.register(CommandBuilders.literal("clear").executes(ctx -> { if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "clear",2)) return 0; ServerPlayer p=ctx.getSource().getPlayerOrException(); p.getInventory().clearContent(); return 1; })
            .then(CommandBuilders.argument("target", EntityArgument.player()).executes(ctx -> { if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "clear",2)) return 0; ServerPlayer t=EntityArgument.getPlayer(ctx,"target"); t.getInventory().clearContent(); return 1; })));
    }
}
