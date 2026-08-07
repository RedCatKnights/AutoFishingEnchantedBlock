package wm.vdr.autofishing;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class Messages {
    private final AutoFishing plugin;
    private final File file;
    private YamlConfiguration config;

    Messages(AutoFishing plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        reload();
    }

    void reload() {
        config = YamlConfiguration.loadConfiguration(file);

        InputStream defaultsStream = plugin.getResource("messages.yml");
        if (defaultsStream != null) {
            try (InputStreamReader reader = new InputStreamReader(defaultsStream, StandardCharsets.UTF_8)) {
                config.setDefaults(YamlConfiguration.loadConfiguration(reader));
            } catch (Exception exception) {
                plugin.getLogger().warning("Could not load the default messages.yml: " + exception.getMessage());
            }
        }
    }

    String get(String path, String... replacements) {
        String message = config.getString(path);
        if (message == null) {
            plugin.getLogger().warning("Missing message in messages.yml: " + path);
            return path;
        }

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return color(message);
    }

    List<String> getList(String path) {
        List<String> messages = new ArrayList<>();
        for (String line : config.getStringList(path)) messages.add(color(line));
        return messages;
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
