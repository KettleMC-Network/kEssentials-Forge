package net.kettlemc.kessentialsforge.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kettlemc.kessentialsforge.i18n.Messages;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class LocaleService {
    private final Path file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Locale defaultLocale = Locale.forLanguageTag("de");
    private final Map<UUID, Locale> playerLocales = new HashMap<>();

    public LocaleService(Path dataDir) {
        Path langDir = dataDir.resolve("lang");
        try {
            Files.createDirectories(langDir);
        } catch (Exception ignored) {
        }
        this.file = langDir.resolve("locales.json");
    }

    public void load() {
        playerLocales.clear();
        defaultLocale = Locale.forLanguageTag("de");
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                if (parsed != null && parsed.isJsonObject()) {
                    JsonObject obj = parsed.getAsJsonObject();
                    if (obj.has("defaultLocale")) {
                        Locale loadedDefault = Messages.localeFromString(obj.get("defaultLocale").getAsString());
                        if (loadedDefault != null) {
                            defaultLocale = loadedDefault;
                        }
                    }
                    if (obj.has("playerLocales") && obj.get("playerLocales").isJsonObject()) {
                        JsonObject players = obj.getAsJsonObject("playerLocales");
                        for (Map.Entry<String, JsonElement> entry : players.entrySet()) {
                            try {
                                UUID id = UUID.fromString(entry.getKey());
                                Locale locale = Messages.localeFromString(entry.getValue().getAsString());
                                if (locale != null) {
                                    playerLocales.put(id, locale);
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        save();
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(file)) {
            JsonObject obj = new JsonObject();
            obj.addProperty("defaultLocale", Messages.toLanguageTag(defaultLocale));
            JsonObject players = new JsonObject();
            for (Map.Entry<UUID, Locale> entry : playerLocales.entrySet()) {
                players.addProperty(entry.getKey().toString(), Messages.toLanguageTag(entry.getValue()));
            }
            obj.add("playerLocales", players);
            gson.toJson(obj, writer);
        } catch (Exception ignored) {
        }
    }

    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(Locale locale) {
        if (locale != null) {
            this.defaultLocale = locale;
            save();
        }
    }

    public Locale getLocale(UUID playerId) {
        if (playerId == null) {
            return defaultLocale;
        }
        return playerLocales.getOrDefault(playerId, defaultLocale);
    }

    public void setLocale(UUID playerId, Locale locale) {
        if (playerId == null) {
            return;
        }
        if (locale == null) {
            playerLocales.remove(playerId);
        } else {
            playerLocales.put(playerId, locale);
        }
        save();
    }
}
