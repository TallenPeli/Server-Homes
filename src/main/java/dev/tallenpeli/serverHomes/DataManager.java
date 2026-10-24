package dev.tallenpeli.serverHomes;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataManager {
    // This will be used to save player names and UUIDs locally
    private final Map<String, UUID> userCache = new HashMap<>();
    private File userCacheFile;

    private final File playerDataFolder;
    private final ServerHomes plugin;

    public DataManager(ServerHomes plugin) {
        this.plugin = plugin;
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!playerDataFolder.exists()) playerDataFolder.mkdirs();
    }

    public boolean loadUserCache() {
        userCacheFile = new File(plugin.getDataFolder(), "usercache.yml");
        if (!userCacheFile.exists()) return false;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(userCacheFile);
        ConfigurationSection names = config.getConfigurationSection("names");

        if (names == null) return false;

        userCache.clear();

        for (String name : names.getKeys(false)) {
            String uuidString = names.getString(name);
            if (uuidString != null) {
                try {
                    userCache.put(name.toLowerCase(), UUID.fromString(uuidString));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Skipping invalid UUID for player: " + name);
                }
            }
        }
        return true;
    }

    public void saveUserCache() {
        if (userCacheFile == null) {
            userCacheFile = new File(plugin.getDataFolder(), "usercache.yml");
        }

        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, UUID> entry : userCache.entrySet()) {
            config.set("names." + entry.getKey(), entry.getValue().toString());
        }

        try {
            config.save(userCacheFile);
        } catch (IOException e) {
            plugin.getLogger().severe("CRITICAL: Could not save usercache.yml. Home data may be lost.");
            e.printStackTrace();
        }
    }

    public boolean playerExists(UUID uuid) {
        return userCache.containsKey(uuid.toString());
    }

    public boolean playerExists(String playerName) {
        return userCache.containsKey(playerName);
    }

    @Nullable
    public UUID getPlayerUUID(String playerName) {
        return userCache.get(playerName);
    }

    @Nullable
    public String getPlayerName(OfflinePlayer player) {
        return player.getName();
    }

    @Nullable
    public File getPlayerFile(UUID uuid) {
        return new File(playerDataFolder, uuid.toString() + ".yml");
    }

    @Nullable
    public YamlConfiguration getPlayerConfig(UUID uuid) {
        File playerFile = getPlayerFile(uuid);
        if (playerFile != null) return YamlConfiguration.loadConfiguration(playerFile);

        return null;
    }

    @Nullable
    public YamlConfiguration getConfigFromName(String playerName) {
        if (playerName == null || playerName.isEmpty()) return null;

        UUID uuid = getPlayerUUID(playerName);
        if (uuid != null) return getPlayerConfig(uuid);

        return null;
    }

    public void savePlayerConfig(UUID uuid, YamlConfiguration config) {
        try {
            File playerFile = getPlayerFile(uuid);
            if (playerFile != null) config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config for UUID: " + uuid);
        }
    }
}
