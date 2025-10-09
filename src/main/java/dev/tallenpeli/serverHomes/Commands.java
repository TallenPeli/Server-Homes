package dev.tallenpeli.serverHomes;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Commands implements CommandExecutor {
    private final File playerDataFolder;

    private final ServerHomes plugin;
    private final FileConfiguration config;

    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private static final Map<UUID, BukkitTask> activeTeleports = new ConcurrentHashMap<>();

    public static Map<UUID, BukkitTask> getActiveTeleports() {
        return activeTeleports;
    }

    public void addActiveTeleport(UUID playerUUID, BukkitTask task) {
        activeTeleports.put(playerUUID, task);
    }

    public Commands(ServerHomes plugin) {
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        this.plugin = plugin;
        this.config = this.plugin.getConfig();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        final String homeName = (args.length > 0) ? String.join(" ", args) : "home"; // join the arguments into a house name

        return switch (label.toLowerCase()) {
            case "homes" -> listHomes(player);
            case "sethome" -> setHome(player, homeName, false);
            case "delhome" -> delHome(player, homeName, false);
            case "confirm" -> confirm(player);
            case "home" -> teleportHome(player, homeName);
            default -> false;
        };
    }

    public boolean teleportHome(Player player, String homeName) {
        if (checkCooldown(player)) {
            return true;
        }

        final Location homeLocation = getHome(player, homeName);

        if (homeLocation == null) {
            player.sendMessage(String.format("§cHome §b'%s'§c doesn't exist.", homeName));
            return true;
        }

        World destinationWorld = homeLocation.getWorld();
        World currentWorld = player.getWorld();

        if (destinationWorld == null) {
            player.sendMessage("§cHome world doesn't exist.");
            return true;
        }

        List<String> blockedWorlds = config.getStringList("home.admin.blocked_worlds");
        if (blockedWorlds.contains(destinationWorld.getName())) {
            player.sendMessage("§cYou cannot teleport to a home in that world.");
            return true;
        }

        if(!currentWorld.equals(destinationWorld)) {
            boolean crossWorldAllowed = config.getBoolean("home.admin.allow_cross_world_teleportation");

            if(!crossWorldAllowed) {
                player.sendMessage("§cCross-world teleportation is not allowed.");
                return true;
            }
        }

        int delayTime = config.getInt("home.teleport.delay_seconds");
        boolean soundsEnabled = config.getBoolean("home.enable_sounds");

        BukkitTask teleportTask = new BukkitRunnable() {
            private int remainingTime = delayTime;
            @Override
            public void run() {
                if (!activeTeleports.containsKey(player.getUniqueId())) {
                    this.cancel();
                    return;
                }

                Location currentLocation = player.getLocation();

                if (remainingTime <= 0) {
                    this.cancel();
                    activeTeleports.remove(player.getUniqueId());
                    player.teleport(homeLocation);
                    player.sendMessage(String.format("§a✓ Teleported to §b%s§a!", homeName));
                    if (soundsEnabled) {
                        player.playNote(currentLocation, Instrument.BELL, new Note(1, Note.Tone.A, false));
                    }
                    if (player.hasPermission("tallenpeli.serverHomes.cooldown.bypass")) {
                        return;
                    }
                    setCooldown(player);
                    return;
                }

                player.sendMessage(String.format("§6⏳ Teleporting in §e%d§6 second%s...", remainingTime, remainingTime == 1 ? "" : "s"));
                if (soundsEnabled) {
                    player.playNote(currentLocation, Instrument.PIANO, new Note(1, Note.Tone.A, false));
                }
                remainingTime--;
            }
        }.runTaskTimer(plugin, 1L, 20L);

        addActiveTeleport(player.getUniqueId(), teleportTask);
        return true;
    }

    public boolean setHome(Player player, String homeName, Boolean isConfirmed) {
        Location location = player.getLocation();

        File playerFile = getPlayerConfig(player);
        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

        String homeBasePath = "home." + homeName;

        if (playerConfig.contains(homeBasePath) && !isConfirmed) {
            player.sendMessage("§c⚠ A home named §e'" + homeName + "'§c already exists. Type §e/confirm§c to overwrite it.");
            ServerHomes.PendingAction newAction = new ServerHomes.PendingAction("overrideHome", homeName);
            plugin.addPendingConfirmation(player.getUniqueId(), newAction);
            return true;
        }

        ConfigurationSection homeSection = playerConfig.getConfigurationSection("home");
        final int homeCount = (homeSection != null) ? homeSection.getKeys(false).size() : 0;
        final int maxHomes = config.getInt("home.max_homes", 1);

        if (homeCount >= maxHomes && !isConfirmed) {
            player.sendMessage("§cHome limit reached §7(" + maxHomes + " max)§c. Delete a home with §e/delhome <name>§c.");
            return true;
        }

        playerConfig.set(homeBasePath + ".world", Objects.requireNonNull(location.getWorld()).getName());
        playerConfig.set(homeBasePath + ".x", location.getX());
        playerConfig.set(homeBasePath + ".y", location.getY());
        playerConfig.set(homeBasePath + ".z", location.getZ());

        try {
            playerConfig.save(playerFile);
            player.sendMessage("§a✓ Home §b'" + homeName + "'§a has been saved!");
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning(String.format("Unable to save home for player %s", player.getName()));
            return false;
        }
    }

    private boolean listHomes(Player player) {
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

        boolean exists = playerConfig.contains(basePath);

        if (exists) {
            plugin.getLogger().info(String.format("%s has a home set with name '%s'", player.getUniqueId(), homeName));
        } else {
            plugin.getLogger().info(player.getUniqueId() + " does not have a home with name '" + homeName + "' set.");
        }
        return exists;
    }

    @Nullable
    private Location getHome(Player player, String homeName) {
        File playerFile = getPlayerConfig(player);
        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

        String basePath = "home." + homeName;

        if (checkHome(player, homeName)) {
            String worldName = playerConfig.getString(basePath + ".world");

            assert worldName != null;
            World world = Bukkit.getWorld(worldName);

            if (world == null) {
                player.sendMessage("§c✗ Your home world no longer exists!");
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

    private boolean delHome(Player player, String homeName, Boolean isConfirmed) {
        if (!checkHome(player, homeName)) {
            player.sendMessage(String.format("§cHome §b'%s'§c doesn't exist.", homeName));
            return false;
        }

        File playerFile = getPlayerConfig(player);
        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

        if (!isConfirmed) {
            player.sendMessage(String.format("§c⚠ Are you sure you want to delete §e'%s'§c? Type §e/confirm§c to proceed.", homeName));
            ServerHomes.PendingAction newAction = new ServerHomes.PendingAction("delhome", homeName);
            plugin.addPendingConfirmation(player.getUniqueId(), newAction);
        } else {
            String basePath = "home." + homeName;

            try {
                playerConfig.set(basePath, null);
                player.sendMessage(String.format("§a✓ Home §b'%s'§a has been deleted.", homeName));
                playerConfig.save(playerFile);
            } catch (Exception e) {
                player.sendMessage("§cAn error occurred while deleting your home.");
                plugin.getLogger().info(String.format("Failed to delete home '%s' due to error: %s", homeName, e));
                return false;
            }
        }

        return true;
    }

    private boolean confirm(Player player) {
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

    private boolean checkCooldown(Player player) {
        if (player.hasPermission("tallenpeli.serverHomes.cooldown.bypass")) {
            return false;
        }

        int cooldownSeconds = config.getInt("home.teleport.cooldown_seconds", 0);
        if (cooldownSeconds <= 0) {
            return false;
        }

        long expirationTime = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long currentTime = System.currentTimeMillis();

        if (currentTime < expirationTime) {
            long remainingSeconds = (expirationTime - currentTime) / 1000;
            player.sendMessage(String.format("§cYou must wait §e%d§c seconds before teleporting again.", remainingSeconds + 1));
            return true;
        }

        return false;
    }

    private void setCooldown(Player player) {
        int cooldownSeconds = config.getInt("home.teleport.cooldown_seconds", 0);

        if (cooldownSeconds > 0) {
            long newExpirationTime = System.currentTimeMillis() + (cooldownSeconds * 1000L);
            cooldowns.put(player.getUniqueId(), newExpirationTime);
        }
    }

    private File getPlayerConfig(Player player) {
        return new File(playerDataFolder, player.getUniqueId() + ".yml");
    }
}