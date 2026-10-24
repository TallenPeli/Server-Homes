package dev.tallenpeli.serverHomes;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class HomeManager {
    private final File playerDataFolder;
    private final ServerHomes plugin;
    private final DataManager dataManager;
    private final FileConfiguration config;

    public HomeManager(ServerHomes plugin, DataManager dataManager) {
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.config = plugin.getConfig();
    }

    public List<String> getPlayerHomes(Player player) {
        File playerFile = new File(playerDataFolder, player.getUniqueId() + ".yml");

        if (!playerFile.exists()) {
            return Collections.emptyList();
        }

        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
        ConfigurationSection homeSection = playerConfig.getConfigurationSection("homes");

        if (homeSection == null) {
            return Collections.emptyList();
        }

        return new ArrayList<>(homeSection.getKeys(false));
    }

    public boolean setHome(Player player, String homeName, boolean isConfirmed) {
        Location location = player.getLocation();

        YamlConfiguration playerConfig = dataManager.getPlayerConfig(player.getUniqueId());

        String homeBasePath = "homes." + homeName;

        if (playerConfig.contains(homeBasePath) && !isConfirmed) {
            player.sendMessage("§c⚠ A home named §e'" + homeName + "'§c already exists. Type §e/confirm§c to overwrite, §e/cancel§c to cancel.");
            ServerHomes.PendingAction newAction = new ServerHomes.PendingAction("overrideHome", homeName);
            plugin.addPendingConfirmation(player.getUniqueId(), newAction);
            return true;
        }

        ConfigurationSection homeSection = playerConfig.getConfigurationSection("homes");

        final int homeCount = (homeSection != null ? homeSection.getKeys(false).size() : 0);
        final int maxHomes = player.hasPermission("tallenpeli.serverHomes.vip") ?
                config.getInt("home.max_homes_vip", 1) : config.getInt("home.max_homes");

        if (homeCount >= maxHomes && !isConfirmed) {
            player.sendMessage("§cHome limit reached §7(" + maxHomes + " max)§c. Delete a home with §e/delhome <name>§c.");
            return true;
        }

        playerConfig.set(homeBasePath + ".world", Objects.requireNonNull(location.getWorld()).getName());
        playerConfig.set(homeBasePath + ".x", location.getX());
        playerConfig.set(homeBasePath + ".y", location.getY());
        playerConfig.set(homeBasePath + ".z", location.getZ());

        dataManager.savePlayerConfig(player.getUniqueId(), playerConfig);
        player.sendMessage("§a✔ Home §b'" + homeName + "'§a has been saved!");
        return true;
    }

    /// Returns true if a home is found with that name
    @Nullable
    public Location getHome(Player player, String homeName) {
        YamlConfiguration playerConfig = dataManager.getPlayerConfig(player.getUniqueId());

        String basePath = "homes." + homeName;

        if (hasHome(player, homeName)) {
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

    public boolean deleteHome(Player player, String homeName, Boolean isConfirmed) {
        if (!hasHome(player, homeName)) {
            player.sendMessage(String.format("§cHome §b'%s'§c doesn't exist.", homeName));
            return false;
        }

        YamlConfiguration playerConfig = dataManager.getPlayerConfig(player.getUniqueId());

        if (!isConfirmed) {
            player.sendMessage(String.format("§c⚠ Are you sure you want to delete §e'%s'§c? Type §e/confirm§c to proceed, §e/cancel§c to cancel.", homeName));
            ServerHomes.PendingAction newAction = new ServerHomes.PendingAction("delhome", homeName);
            plugin.addPendingConfirmation(player.getUniqueId(), newAction);
        } else {
            String basePath = "homes." + homeName;

            try {
                playerConfig.set(basePath, null);
                dataManager.savePlayerConfig(player.getUniqueId(), playerConfig);
                player.sendMessage(String.format("§a✔ Home §b'%s'§a has been deleted.", homeName));
            } catch (Exception e) {
                player.sendMessage("§cAn error occurred while deleting home.");
                return false;
            }
        }

        return true;
    }

    public boolean listHomes(Player player) {
        YamlConfiguration playerConfig = dataManager.getPlayerConfig(player.getUniqueId());

        if (!playerConfig.contains("homes") || Objects.requireNonNull(playerConfig.getConfigurationSection("homes")).getKeys(false).isEmpty()) {
            player.sendMessage("§7No homes set yet. Use §e/sethome§7 to create one.");
            return true;
        }

        ConfigurationSection homeSection = playerConfig.getConfigurationSection("homes");
        assert homeSection != null;
        player.sendMessage("§8§m                §r §6Your Homes §8§m                ");
        for (String homeName : homeSection.getKeys(false)) {
            double x = homeSection.getDouble(homeName + ".x");
            double y = homeSection.getDouble(homeName + ".y");
            double z = homeSection.getDouble(homeName + ".z");

            String world;
            switch (homeSection.getString(homeName + ".world")) {
                case "world_nether" -> world = "the_nether";
                case "world_end" -> world = "the_end";
                case null, default -> world = "overworld";
            }

            TextComponent message = new TextComponent("§e - " + homeName + " ");

            String formattedCoords = String.format("§7[%s: %.1f, %.1f, %.1f]", world, x, y, z);
            TextComponent coords = new TextComponent(formattedCoords);

            String tpCommand = String.format("/home %s", homeName);
            coords.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand));
            coords.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§aClick to teleport to " + homeName)));

            player.spigot().sendMessage(message, coords);
        }
        return true;
    }

    private boolean hasHome(Player player, String homeName) {
        YamlConfiguration playerConfig = dataManager.getPlayerConfig(player.getUniqueId());
        return playerConfig.contains("homes." + homeName);
    }

    public boolean confirm(Player player) {
        UUID playerUUID = player.getUniqueId();
        Map<UUID, ServerHomes.PendingAction> confirmations = ServerHomes.getPendingConfirmations();

        if (confirmations.containsKey(playerUUID)) {
            ServerHomes.PendingAction pendingAction = confirmations.remove(playerUUID);

            String commandLabel = pendingAction.commandLabel();
            String homeName = pendingAction.homeName();

            if (commandLabel.equalsIgnoreCase("delhome")) {
                deleteHome(player, homeName, true);
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