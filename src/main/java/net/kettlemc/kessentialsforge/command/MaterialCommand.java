package net.kettlemc.kessentialsforge.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;

public class MaterialCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var material = dispatcher.register(CommandBuilders.literal("material").executes(ctx -> {
            var player = ctx.getSource().getPlayerOrException();
            var item = player.getMainHandItem().getItem();
            ResourceLocation id = net.minecraft.core.Registry.ITEM.getKey(item);
            player.sendMessage(new net.minecraft.network.chat.TextComponent(net.kettlemc.kessentialsforge.i18n.Messages.get(player,"item_info", id)), net.minecraft.Util.NIL_UUID);
            return 1;
        }));
        CommandBuilders.registerAliases(dispatcher, material, "mat");
    }
}
