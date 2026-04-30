package dev.tallenpeli.serverHomes;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
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

    private File getPlayerFile(UUID uuid) {
        return new File(playerDataFolder, uuid.toString() + ".yml");
    }

    private @Nullable YamlConfiguration getPlayerConfig(UUID uuid) {
        File file = getPlayerFile(uuid);
        if (!file.exists()) return null;
        return YamlConfiguration.loadConfiguration(file);
    }

    @Nullable YamlConfiguration getConfigFromName(String playerName) {
        if (playerName == null || playerName.isEmpty()) return null;

        Player online = Bukkit.getPlayer(playerName);
        if (online != null) {
            return getPlayerConfig(online.getUniqueId());
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerName);

        if (offline.hasPlayedBefore()) {
            return getPlayerConfig(offline.getUniqueId());
        }

        return null;
    }

    private void savePlayerConfig(UUID uuid, YamlConfiguration config) {
        try {
            config.save(getPlayerFile(uuid));
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config for UUID: " + uuid);
            e.printStackTrace();
        }
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

        YamlConfiguration playerConfig = getPlayerConfig(player.getUniqueId());

        String homeBasePath = "homes." + homeName;

        assert playerConfig != null;
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

        savePlayerConfig(player.getUniqueId(), playerConfig);
        player.sendMessage("§a✔ Home §b'" + homeName + "'§a has been saved!");
        return true;
    }

    /// Returns true if a home is found with that name
    @Nullable
    public Location getHome(Player player, String homeName) {
        YamlConfiguration playerConfig = getPlayerConfig(player.getUniqueId());

        String basePath = "homes." + homeName;

        if (checkHome(player, homeName)) {
            assert playerConfig != null;
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

        YamlConfiguration playerConfig = getPlayerConfig(player.getUniqueId());

        if (!isConfirmed) {
            player.sendMessage(String.format("§c⚠ Are you sure you want to delete §e'%s'§c? Type §e/confirm§c to proceed, §e/cancel§c to cancel.", homeName));
            ServerHomes.PendingAction newAction = new ServerHomes.PendingAction("delhome", homeName);
            plugin.addPendingConfirmation(player.getUniqueId(), newAction);
        } else {
            String basePath = "homes." + homeName;

            try {
                assert playerConfig != null;
                playerConfig.set(basePath, null);
                savePlayerConfig(player.getUniqueId(), playerConfig);
                player.sendMessage(String.format("§a✔ Home §b'%s'§a has been deleted.", homeName));
            } catch (Exception e) {
                player.sendMessage("§cAn error occurred while deleting your home.");
                return false;
            }
        }

        return true;
    }

    public boolean listHomes(Player player) {
        YamlConfiguration playerConfig = getPlayerConfig(player.getUniqueId());

        assert playerConfig != null;
        if (!playerConfig.contains("homes") || Objects.requireNonNull(playerConfig.getConfigurationSection("homes")).getKeys(false).isEmpty()) {
            player.sendMessage("§7No homes set yet. Use §e/sethome§7 to create one.");
            return true;
        }

        ConfigurationSection homeSection = playerConfig.getConfigurationSection("homes");
        assert homeSection != null;
        player.sendMessage("§8§m                §r §6Your Homes §8§m                ");
        for (String home : homeSection.getKeys(false)) {
            player.sendMessage("§e - " + home);
        }
        return true;
    }

    private boolean checkHome(Player player, String homeName) {
        YamlConfiguration playerConfig = getPlayerConfig(player.getUniqueId());

        String basePath = "homes." + homeName;

        assert playerConfig != null;
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

    public boolean adminListHomes(Player player, String targetPlayerName) {
        if (targetPlayerName.isEmpty()) {
            player.sendMessage("§cPlease enter a player's name.");
            return false;
        }

        YamlConfiguration targetPlayerConfig = getConfigFromName(targetPlayerName);

        assert targetPlayerConfig != null;
        if (!targetPlayerConfig.contains("homes") || Objects.requireNonNull(targetPlayerConfig.getConfigurationSection("homes")).getKeys(false).isEmpty()) {
            player.sendMessage("§7Player has no homes set.");
            return true;
        }

        ConfigurationSection homeSection = targetPlayerConfig.getConfigurationSection("homes");
        assert homeSection != null;
        player.sendMessage(String.format("§8§m                §r §6%s's Homes §8§m                ", targetPlayerName));
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

            String tpCommand = String.format("/homeadmin tp %s %s", targetPlayerName, homeName);
            coords.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand));
            coords.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§aClick to teleport to " + homeName)));

            player.spigot().sendMessage(message, coords);
        }

        return true;
    }

    public boolean adminHomeTp(Player player, String targetPlayerName, String homeName) {
        if (targetPlayerName.isEmpty()) {
            player.sendMessage("§cPlease enter a player's name.");
            return false;
        }

        YamlConfiguration targetPlayerConfig = getConfigFromName(targetPlayerName);
        if (targetPlayerConfig == null) {
            player.sendMessage("§cCould not find data for player: §e" + targetPlayerName);
            return false;
        }

        String path = "homes." + homeName;
        if (!targetPlayerConfig.contains(path)) {
            player.sendMessage("§cPlayer §e" + targetPlayerName + "§c has no home named §b'" + homeName + "'§c.");
            return false;
        }

        String worldName = targetPlayerConfig.getString(path + ".world");
        World world = (worldName != null) ? Bukkit.getWorld(worldName) : null;

        if (world == null) {
            player.sendMessage("§cError: The world §e" + worldName + "§c is not currently loaded.");
            return false;
        }

        double x = targetPlayerConfig.getDouble(path + ".x");
        double y = targetPlayerConfig.getDouble(path + ".y");
        double z = targetPlayerConfig.getDouble(path + ".z");

        Location targetLocation = new Location(world, x, y, z, player.getLocation().getYaw(), player.getLocation().getPitch());

        player.teleport(targetLocation);
        player.sendMessage("§a✔ Teleporting to §e" + targetPlayerName + "'s §ahome: §b" + homeName);

        return true;
    }
}