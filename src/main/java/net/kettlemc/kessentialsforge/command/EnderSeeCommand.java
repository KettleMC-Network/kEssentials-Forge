
package net.kettlemc.kessentialsforge.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public class EnderSeeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(CommandBuilders.literal("endersee").then(CommandBuilders.argument("target", EntityArgument.player()).executes(ctx -> {
            if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "endersee", 2)) return 0;
            ServerPlayer viewer = ctx.getSource().getPlayerOrException();
            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
            return EnderChestCommand.open(ctx.getSource(), viewer, target);
        })));
    }
}
