package net.kettlemc.kessentialsforge.service;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class MotdService {

    private static final Pattern AMP_CODES = Pattern.compile("&([0-9a-fk-orA-FK-OR])");
    private static final Path MOTD_PATH = Paths.get("config", "kessentials", "motd.txt");

    /**
     * Apply the custom MOTD to the server list ping.
     * Called once after the server is fully started (i.e. when "Done" appears).
     */
    public static void apply(MinecraftServer server) {
        try {
            String motd = readMotd();
            if (motd == null) return;
            String colored = AMP_CODES.matcher(motd).replaceAll("\u00A7$1");
            if (server.getStatus() != null) {
                server.getStatus().setDescription(new TextComponent(colored));
            }
        } catch (Throwable ignored) {}
    }

    private static String readMotd() {
        try {
            if (!Files.exists(MOTD_PATH)) {
                Files.createDirectories(MOTD_PATH.getParent());
                String def = "&aKettleMC &7| &fWillkommen!\n&7Setze deine MOTD in config/kessentials/motd.txt";
                Files.write(MOTD_PATH, def.getBytes(StandardCharsets.UTF_8));
            }
            byte[] data = Files.readAllBytes(MOTD_PATH);
            return new String(data, StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (Throwable t) {
            return null;
        }
    }
}