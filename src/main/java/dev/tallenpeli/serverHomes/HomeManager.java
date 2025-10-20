package dev.tallenpeli.serverHomes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class HomeManager {
    private final File playerDataFolder;
    private final ServerHomes plugin;
    private final FileConfiguration config;

    public HomeManager(ServerHomes plugin) {
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    private File getPlayerConfig(Player player) {
        return new File(playerDataFolder, player.getUniqueId() + ".yml");
    }

    public List<String> getPlayerHomes(Player player) {
        File playerFile = new File(playerDataFolder, player.getUniqueId() + ".yml");

        if (!playerFile.exists()) {
            return Collections.emptyList();
        }

        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
        ConfigurationSection homeSection = playerConfig.getConfigurationSection("home");

        if (homeSection == null) {
            return Collections.emptyList();
        }

        return new ArrayList<>(homeSection.getKeys(false));
    }

    public boolean setHome(Player player, String homeName, boolean isConfirmed) {
        Location location = player.getLocation();

        File playerFile = getPlayerConfig(player);
        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

        String homeBasePath = "home." + homeName;

        if (playerConfig.contains(homeBasePath) && !isConfirmed) {
            player.sendMessage("§c⚠ A home named §e'" + homeName + "'§c already exists. Type §e/confirm§c to overwrite, §e/cancel§c to cancel.");
            ServerHomes.PendingAction newAction = new ServerHomes.PendingAction("overrideHome", homeName);
            plugin.addPendingConfirmation(player.getUniqueId(), newAction);
            return true;
        }

        ConfigurationSection homeSection = playerConfig.getConfigurationSection("home");

        // VIP players bypass home limits
        if (!player.hasPermission("tallenpeli.serverHomes.vip")) {
            final int homeCount = (homeSection != null) ? homeSection.getKeys(false).size() : 0;
            final int maxHomes = config.getInt("home.max_homes", 1);

            if (homeCount >= maxHomes && !isConfirmed) {
                player.sendMessage("§cHome limit reached §7(" + maxHomes + " max)§c. Delete a home with §e/delhome <name>§c.");
                return true;
            }
        }

        playerConfig.set(homeBasePath + ".world", Objects.requireNonNull(location.getWorld()).getName());
        playerConfig.set(homeBasePath + ".x", location.getX());
        playerConfig.set(homeBasePath + ".y", location.getY());
        playerConfig.set(homeBasePath + ".z", location.getZ());

        try {
            playerConfig.save(playerFile);
            player.sendMessage("§a✔ Home §b'" + homeName + "'§a has been saved!");
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning(String.format("Unable to save home for player %s", player.getName()));
            return false;
        }
    }

    /// Returns true if a home is found with that name
    @Nullable
    public Location getHome(Player player, String homeName) {
        File playerFile = getPlayerConfig(player);
        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

        String basePath = "home." + homeName;

        if (checkHome(player, homeName)) {
            String worldName = playerConfig.getString(basePath + ".world");

            assert worldName != null;
            World world = Bukkit.getWorld(worldName);

            if (world == null) {
                player.sendMessage("§cYour home world no longer exists!");
                return null;
            }

            double x = playerConfig.getDouble(basePath + ".x");
            double y = playerConfig.getDouble(basePath + ".y");
            double z = playerConfig.getDouble(basePath + ".z");

            float yaw = player.getLocation().getYaw();
            float pitch = player.getLocation().getPitch();

            return new Location(world, x, y, z, yaw, pitch);
        } else {
            return null;
        }
    }

    public boolean delHome(Player player, String homeName, Boolean isConfirmed) {
        if (!checkHome(player, homeName)) {
            player.sendMessage(String.format("§cHome §b'%s'§c doesn't exist.", homeName));
            return false;
        }

        File playerFile = getPlayerConfig(player);
        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

        if (!isConfirmed) {
            player.sendMessage(String.format("§c⚠ Are you sure you want to delete §e'%s'§c? Type §e/confirm§c to proceed, §e/cancel§c to cancel.", homeName));
            ServerHomes.PendingAction newAction = new ServerHomes.PendingAction("delhome", homeName);
            plugin.addPendingConfirmation(player.getUniqueId(), newAction);
        } else {
            String basePath = "home." + homeName;

            try {
                playerConfig.set(basePath, null);
                player.sendMessage(String.format("§a✔ Home §b'%s'§a has been deleted.", homeName));
                playerConfig.save(playerFile);
            } catch (Exception e) {
                player.sendMessage("§cAn error occurred while deleting your home.");
                plugin.getLogger().info(String.format("Failed to delete home '%s' due to error: %s", homeName, e));
                return false;
            }
        }

        return true;
    }

    public boolean listHomes(Player player) {
        File playerFile = getPlayerConfig(player);
        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

        if (!playerConfig.contains("home") || Objects.requireNonNull(playerConfig.getConfigurationSection("home")).getKeys(false).isEmpty()) {
            player.sendMessage("  §7No homes set yet. Use §e/sethome§7 to create one.");
            return true;
        }

        ConfigurationSection homeSection = playerConfig.getConfigurationSection("home");
        assert homeSection != null;
        player.sendMessage("§8§m                §r §6Your Homes §8§m                ");
        for (String home : homeSection.getKeys(false)) {
            player.sendMessage("§e - " + home);
        }
        return true;
    }

    private boolean checkHome(Player player, String homeName) {
        File playerFile = getPlayerConfig(player);
        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

        String basePath = "home." + homeName;

        return playerConfig.contains(basePath);
    }

    public boolean confirm(Player player) {
        UUID playerUUID = player.getUniqueId();
        Map<UUID, ServerHomes.PendingAction> confirmations = ServerHomes.getPendingConfirmations();

        if (confirmations.containsKey(playerUUID)) {
            ServerHomes.PendingAction pendingAction = confirmations.remove(playerUUID);

            String commandLabel = pendingAction.commandLabel();
            String homeName = pendingAction.homeName();

            if (commandLabel.equalsIgnoreCase("delhome")) {
                delHome(player, homeName, true);
            } else if (commandLabel.equalsIgnoreCase("overrideHome")) {
                setHome(player, homeName, true);
            }
        } else {
            player.sendMessage("§cYou have no pending confirmations.");
        }
        return true;
    }

    public boolean cancel(Player player) {
        UUID playerUUID = player.getUniqueId();
        Map<UUID, ServerHomes.PendingAction> confirmations = ServerHomes.getPendingConfirmations();

        if (confirmations.containsKey(playerUUID)) {
            confirmations.remove(playerUUID);
            player.sendMessage("§a✔ Cancelled confirmation request.");
        } else {
            player.sendMessage("§cNo pending confirmation request.");
        }
        return true;
    }
}