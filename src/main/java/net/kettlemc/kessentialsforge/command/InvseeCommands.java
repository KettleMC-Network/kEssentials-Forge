
package net.kettlemc.kessentialsforge.command;
import com.mojang.brigadier.CommandDispatcher; import com.mojang.brigadier.arguments.BoolArgumentType; import net.minecraft.commands.arguments.EntityArgument;
import net.kettlemc.kessentialsforge.ui.InvseeMenu; import net.minecraft.commands.CommandSourceStack; import net.minecraft.network.chat.Component; import net.minecraft.server.level.ServerPlayer; import net.minecraft.world.entity.player.Inventory; import net.minecraft.world.SimpleMenuProvider;

public class InvseeCommands {
    public static void register(CommandDispatcher<CommandSourceStack> d){
        var invsee = d.register(CommandBuilders.literal("invsee").then(CommandBuilders.argument("target", EntityArgument.player()).executes(ctx -> open(ctx, false)).then(CommandBuilders.argument("modify", BoolArgumentType.bool()).executes(ctx -> open(ctx, BoolArgumentType.getBool(ctx,"modify"))))));
        CommandBuilders.registerAliases(d, invsee, "inventorysee", "inv", "inventory", "inventar");
    }
    public static int open(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, boolean modify) {
        if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "invsee", 2)) return 0;
        ServerPlayer viewer; ServerPlayer target; try { viewer = ctx.getSource().getPlayerOrException(); target = EntityArgument.getPlayer(ctx,"target"); } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) { return 0; } boolean canModify = modify && net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "invsee.modify", 3);
        final net.minecraft.world.entity.player.Inventory targetInv = target.getInventory();
        viewer.openMenu(new SimpleMenuProvider((id, playerInv, player) -> new InvseeMenu(id, playerInv, targetInv, canModify), new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(viewer,"invsee_title", target.getGameProfile().getName()))));
        return 1;
    }
}
