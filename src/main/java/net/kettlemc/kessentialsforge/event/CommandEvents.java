package net.kettlemc.kessentialsforge.event;

import net.kettlemc.kessentialsforge.KEssentialsForge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Locale;

public class CommandEvents {
    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        CommandSourceStack source = event.getParseResults().getContext().getSource();
        String literal = extractRootLiteral(event);
        if (literal == null || literal.isEmpty()) {
            return;
        }
        literal = literal.toLowerCase(Locale.ROOT);
        var config = KEssentialsForge.INSTANCE.config;
        if (config.isCommandDisabled(literal)) {
            event.setCanceled(true);
            String message = config.disabledCommandMessage(literal)
                    .filter(s -> !s.isBlank())
                    .orElse("Dieser Command ist deaktiviert.");
            source.sendFailure(new TextComponent(message));
            return;
        }
        config.executeCommandActions(source, literal);
    }

    private String extractRootLiteral(CommandEvent event) {
        var context = event.getParseResults().getContext();
        if (context == null) return null;
        var nodes = context.getNodes();
        if (nodes == null || nodes.isEmpty()) {
            String input = event.getParseResults().getReader().getString().trim();
            if (input.startsWith("/")) input = input.substring(1);
            int space = input.indexOf(' ');
            return space == -1 ? input : input.substring(0, space);
        }
        return nodes.get(0).getNode().getName();
    }
}
