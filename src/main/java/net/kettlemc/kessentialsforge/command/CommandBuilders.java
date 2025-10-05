package net.kettlemc.kessentialsforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;

public final class CommandBuilders {
    private CommandBuilders() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> literal(String name) {
        return LiteralArgumentBuilder.<CommandSourceStack>literal(name);
    }

    public static <T> RequiredArgumentBuilder<CommandSourceStack, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public static void registerAliases(CommandDispatcher<CommandSourceStack> dispatcher,
                                       LiteralCommandNode<CommandSourceStack> base,
                                       String... aliases) {
        for (String alias : aliases) {
            LiteralArgumentBuilder<CommandSourceStack> aliasBuilder = literal(alias);

            if (base.getRequirement() != null) {
                aliasBuilder.requires(base.getRequirement());
            }

            aliasBuilder.redirect(base);

            if (base.getCommand() != null) {
                aliasBuilder.executes(base.getCommand());
            }

            dispatcher.register(aliasBuilder);
        }
    }
}
