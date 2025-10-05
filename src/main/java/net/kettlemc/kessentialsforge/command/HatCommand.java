
package net.kettlemc.kessentialsforge.command;
import com.mojang.brigadier.CommandDispatcher; import net.minecraft.commands.CommandSourceStack; import net.minecraft.server.level.ServerPlayer; import net.minecraft.world.entity.EquipmentSlot; import net.minecraft.world.item.ItemStack;
public class HatCommand {
    public static void register(CommandDispatcher<CommandSourceStack> d){
        d.register(CommandBuilders.literal("hat").executes(ctx -> { if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "hat",0)) return 0;
            ServerPlayer p=ctx.getSource().getPlayerOrException(); ItemStack hand=p.getMainHandItem(); ItemStack old=p.getItemBySlot(EquipmentSlot.HEAD); p.setItemSlot(EquipmentSlot.HEAD, hand); p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, old);
            p.displayClientMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(p,"hat_swapped")), false); return 1; }));
    }
}
