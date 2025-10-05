package net.kettlemc.kessentialsforge.i18n;

import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Messages {
    private static final Map<String, Properties> bundles = new HashMap<>();
    private static final Pattern FILE_PATTERN = Pattern.compile("messages(?:_(?<tag>[A-Za-z0-9_\\-]+))?\\.properties");
    private static final List<String> BUILTIN_LOCALES = List.of("de", "en");
    private static Path configDir;
    private static Locale defaultLocale = Locale.forLanguageTag("de");
    private static LocaleProvider localeProvider = id -> defaultLocale;

    static {
        reloadInternal(null);
    }

    public static synchronized void setConfigDir(Path dir) {
        configDir = dir;
        if (configDir != null) {
            try {
                Files.createDirectories(configDir.resolve("lang"));
            } catch (IOException ignored) {
            }
        }
        reload();
    }

    public static synchronized void setLocaleProvider(LocaleProvider provider) {
        localeProvider = provider != null ? provider : id -> defaultLocale;
    }

    public static synchronized void setDefaultLocale(Locale locale) {
        if (locale != null) {
            defaultLocale = locale;
        }
    }

    public static synchronized void reload() {
        reloadInternal(configDir);
    }

    public static String get(String key, Object... args) {
        return format(null, key, args);
    }

    public static String get(UUID playerId, String key, Object... args) {
        Locale locale = localeProvider != null ? localeProvider.getLocale(playerId) : defaultLocale;
        return format(locale, key, args);
    }

    public static String get(ServerPlayer player, String key, Object... args) {
        return player != null ? get(player.getUUID(), key, args) : get(key, args);
    }

    public static Locale localeFromString(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        trimmed = trimmed.replace('_', '-');
        Locale locale = Locale.forLanguageTag(trimmed);
        if (locale == null) {
            return null;
        }
        if (locale.getLanguage().isEmpty()) {
            return null;
        }
        return locale;
    }

    public static String toLanguageTag(Locale locale) {
        if (locale == null) {
            return "";
        }
        String tag = locale.toLanguageTag();
        if (tag == null) {
            return "";
        }
        if ("und".equalsIgnoreCase(tag)) {
            return locale.getLanguage() != null ? locale.getLanguage() : "";
        }
        return tag;
    }

    private static String format(Locale locale, String key, Object... args) {
        String raw = lookup(locale, key);
        return MessageFormat.format(raw, args);
    }

    private static String lookup(Locale locale, String key) {
        for (String candidate : candidates(locale)) {
            Properties props = bundles.get(candidate);
            if (props != null && props.containsKey(key)) {
                return props.getProperty(key);
            }
        }
        if (locale == null || !locale.equals(defaultLocale)) {
            for (String candidate : candidates(defaultLocale)) {
                Properties props = bundles.get(candidate);
                if (props != null && props.containsKey(key)) {
                    return props.getProperty(key);
                }
            }
        }
        return key;
    }

    private static List<String> candidates(Locale locale) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (locale != null) {
            String tag = toLanguageTag(locale);
            if (!tag.isBlank()) {
                String normalized = normalizeTag(tag);
                tags.add(normalized);
                int sep = normalized.indexOf('-');
                while (sep > 0) {
                    tags.add(normalized.substring(0, sep));
                    sep = normalized.indexOf('-', sep + 1);
                }
            }
            if (!locale.getLanguage().isBlank()) {
                tags.add(locale.getLanguage().toLowerCase(Locale.ROOT));
            }
        }
        tags.add("");
        return new ArrayList<>(tags);
    }

    private static void reloadInternal(Path dir) {
        bundles.clear();
        bundles.put("", new Properties());

        loadFromClasspath("messages", "");
        for (String locale : BUILTIN_LOCALES) {
            loadFromClasspath("messages_" + locale, locale);
        }

        if (dir != null) {
            Path langDir = dir.resolve("lang");
            if (Files.isDirectory(langDir)) {
                try (var paths = Files.list(langDir)) {
                    paths.filter(Files::isRegularFile).forEach(Messages::loadOverrideFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static void loadFromClasspath(String baseName, String locale) {
        String resource = "lang/" + baseName + ".properties";
        try (InputStream in = Messages.class.getClassLoader().getResourceAsStream(resource)) {
            if (in != null) {
                Properties props = bundles.computeIfAbsent(normalizeTag(locale), key -> new Properties());
                try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    props.load(reader);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static void loadOverrideFile(Path file) {
        String name = file.getFileName().toString();
        Matcher matcher = FILE_PATTERN.matcher(name);
        if (!matcher.matches()) {
            return;
        }
        String tag = matcher.group("tag");
        String normalized = normalizeTag(tag == null ? "" : tag);
        Properties props = bundles.computeIfAbsent(normalized, key -> new Properties());
        try (InputStream in = Files.newInputStream(file)) {
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                props.load(reader);
            }
        } catch (IOException ignored) {
        }
    }

    private static String normalizeTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return "";
        }
        return tag.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    @FunctionalInterface
    public interface LocaleProvider {
        Locale getLocale(UUID playerId);
    }
}
