package net.kettlemc.kessentialsforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TimeWeatherCommands {
    private static final String ARG_TICKS = "ticks";
    private static final String ARG_DIMENSION = "dimension";

    private static final DynamicCommandExceptionType UNKNOWN_DIMENSION =
            new DynamicCommandExceptionType(id -> new TranslatableComponent("commands.locate.dimension.not_found", id));

    private TimeWeatherCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                CommandBuilders.literal("time")
                        .then(CommandBuilders.literal("set")
                                .then(CommandBuilders.argument(ARG_TICKS, IntegerArgumentType.integer(0))
                                        .executes(ctx -> setTime(ctx, IntegerArgumentType.getInteger(ctx, ARG_TICKS)))
                                        .then(CommandBuilders.argument(ARG_DIMENSION, ResourceLocationArgument.id())
                                                .suggests(TimeWeatherCommands::suggestDimensions)
                                                .executes(ctx -> setTime(ctx, IntegerArgumentType.getInteger(ctx, ARG_TICKS))))))
                        .then(CommandBuilders.literal("add")
                                .then(CommandBuilders.argument(ARG_TICKS, IntegerArgumentType.integer(0))
                                        .executes(ctx -> addTime(ctx, IntegerArgumentType.getInteger(ctx, ARG_TICKS)))
                                        .then(CommandBuilders.argument(ARG_DIMENSION, ResourceLocationArgument.id())
                                                .suggests(TimeWeatherCommands::suggestDimensions)
                                                .executes(ctx -> addTime(ctx, IntegerArgumentType.getInteger(ctx, ARG_TICKS))))))
        );

        dispatcher.register(
                CommandBuilders.literal("weather")
                        .then(CommandBuilders.literal("clear")
                                .executes(ctx -> setWeather(ctx, 12000, 0, false, false))
                                .then(CommandBuilders.argument(ARG_DIMENSION, ResourceLocationArgument.id())
                                        .suggests(TimeWeatherCommands::suggestDimensions)
                                        .executes(ctx -> setWeather(ctx, 12000, 0, false, false))))
                        .then(CommandBuilders.literal("rain")
                                .executes(ctx -> setWeather(ctx, 0, 12000, true, false))
                                .then(CommandBuilders.argument(ARG_DIMENSION, ResourceLocationArgument.id())
                                        .suggests(TimeWeatherCommands::suggestDimensions)
                                        .executes(ctx -> setWeather(ctx, 0, 12000, true, false))))
                        .then(CommandBuilders.literal("thunder")
                                .executes(ctx -> setWeather(ctx, 0, 12000, true, true))
                                .then(CommandBuilders.argument(ARG_DIMENSION, ResourceLocationArgument.id())
                                        .suggests(TimeWeatherCommands::suggestDimensions)
                                        .executes(ctx -> setWeather(ctx, 0, 12000, true, true))))
        );

        var morning = dispatcher.register(
                CommandBuilders.literal("morning")
                        .executes(ctx -> setFixedTime(ctx, 0))
                        .then(CommandBuilders.argument(ARG_DIMENSION, ResourceLocationArgument.id())
                                .suggests(TimeWeatherCommands::suggestDimensions)
                                .executes(ctx -> setFixedTime(ctx, 0)))
        );
        CommandBuilders.registerAliases(dispatcher, morning, "morgen");

        var day = dispatcher.register(
                CommandBuilders.literal("day")
                        .executes(ctx -> setFixedTime(ctx, 1000))
                        .then(CommandBuilders.argument(ARG_DIMENSION, ResourceLocationArgument.id())
                                .suggests(TimeWeatherCommands::suggestDimensions)
                                .executes(ctx -> setFixedTime(ctx, 1000)))
        );
        CommandBuilders.registerAliases(dispatcher, day, "tag");

        var midday = dispatcher.register(
                CommandBuilders.literal("midday")
                        .executes(ctx -> setFixedTime(ctx, 6000))
                        .then(CommandBuilders.argument(ARG_DIMENSION, ResourceLocationArgument.id())
                                .suggests(TimeWeatherCommands::suggestDimensions)
                                .executes(ctx -> setFixedTime(ctx, 6000)))
        );
        CommandBuilders.registerAliases(dispatcher, midday, "mittag");

        var evening = dispatcher.register(
                CommandBuilders.literal("evening")
                        .executes(ctx -> setFixedTime(ctx, 12000))
                        .then(CommandBuilders.argument(ARG_DIMENSION, ResourceLocationArgument.id())
                                .suggests(TimeWeatherCommands::suggestDimensions)
                                .executes(ctx -> setFixedTime(ctx, 12000)))
        );
        CommandBuilders.registerAliases(dispatcher, evening, "abend");

        var night = dispatcher.register(
                CommandBuilders.literal("night")
                        .executes(ctx -> setFixedTime(ctx, 18000))
                        .then(CommandBuilders.argument(ARG_DIMENSION, ResourceLocationArgument.id())
                                .suggests(TimeWeatherCommands::suggestDimensions)
                                .executes(ctx -> setFixedTime(ctx, 18000)))
        );
        CommandBuilders.registerAliases(dispatcher, night, "nacht");
    }

    private static int setTime(CommandContext<CommandSourceStack> ctx, int ticks) throws CommandSyntaxException {
        if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "time", 2)) {
            return 0;
        }
        resolveLevel(ctx).setDayTime(ticks);
        return 1;
    }

    private static int addTime(CommandContext<CommandSourceStack> ctx, int ticks) throws CommandSyntaxException {
        if (!net.kettlemc.kessentialsforge.perm.Perms.has(ctx.getSource(), "time", 2)) {
            return 0;
        }
        ServerLevel level = resolveLevel(ctx);
        level.setDayTime(level.getDayTime() + ticks);
        return 1;
    }

    private static int setFixedTime(CommandContext<CommandSourceStack> ctx, int ticks) throws CommandSyntaxException {
        resolveLevel(ctx).setDayTime(ticks);
        return 1;
    }

    private static int setWeather(CommandContext<CommandSourceStack> ctx,
                                  int clearDuration,
                                  int rainDuration,
                                  boolean raining,
                                  boolean thundering) throws CommandSyntaxException {
        resolveLevel(ctx).setWeatherParameters(clearDuration, rainDuration, raining, thundering);
        return 1;
    }

    private static ServerLevel resolveLevel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ResourceLocation id;
        try {
            id = ResourceLocationArgument.getId(ctx, ARG_DIMENSION);
        } catch (IllegalArgumentException ignored) {
            return source.getLevel();
        }

        ResourceKey<Level> key = ResourceKey.create(Registry.DIMENSION_REGISTRY, id);
        ServerLevel level = source.getServer().getLevel(key);
        if (level == null) {
            throw UNKNOWN_DIMENSION.create(id);
        }
        return level;
    }

    private static CompletableFuture<Suggestions> suggestDimensions(CommandContext<CommandSourceStack> ctx,
                                                                    SuggestionsBuilder builder) {
        Iterable<ResourceKey<Level>> keys = ctx.getSource().getServer().levelKeys();
        List<ResourceLocation> ids = new ArrayList<>();
        for (ResourceKey<Level> key : keys) {
            ids.add(key.location());
        }
        return SharedSuggestionProvider.suggestResource(ids, builder);
    }
}
