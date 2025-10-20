package dev.tallenpeli.serverHomes;

import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Note;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportManager {
    private final HomeManager homeManager;

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

    public TeleportManager(HomeManager homeManager, ServerHomes plugin) {
        this.homeManager = homeManager;
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public boolean teleportHome(Player player, String homeName) {
        if (checkCooldown(player)) {
            return true;
        }

        final Location homeLocation = homeManager.getHome(player, homeName);

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

        // VIP players bypass teleport restrictions
        if (!player.hasPermission("tallenpeli.serverHomes.vip")) {
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
        }

        int delayTime = config.getInt("home.teleport.delay_seconds");
        boolean soundsEnabled = config.getBoolean("home.enable_sounds");

        BukkitTask teleportTask = new BukkitRunnable() {
            private int remainingTime = player.hasPermission("tallenpeli.serverHomes.vip") ? 0 : delayTime;
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
                    player.sendMessage(String.format("§a✔ Teleported to §b%s§a!", homeName));
                    if (soundsEnabled) {
                        // Call the player.getLocation() again because this location is updated after the teleport.
                        player.playNote(player.getLocation(), Instrument.BELL, new Note(1, Note.Tone.A, false));
                    }
                    if (player.hasPermission("tallenpeli.serverHomes.vip")) {
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

    private void setCooldown(Player player) {
        int cooldownSeconds = config.getInt("home.teleport.cooldown_seconds", 0);

        if (cooldownSeconds > 0) {
            long newExpirationTime = System.currentTimeMillis() + (cooldownSeconds * 1000L);
            cooldowns.put(player.getUniqueId(), newExpirationTime);
        }
    }

    private boolean checkCooldown(Player player) {
        if (player.hasPermission("tallenpeli.serverHomes.vip")) {
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
}