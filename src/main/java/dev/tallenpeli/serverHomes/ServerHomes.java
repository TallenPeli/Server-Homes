package dev.tallenpeli.serverHomes;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerHomes extends JavaPlugin {
    public record PendingAction(String commandLabel, String homeName) {
    }

    private static final Map<UUID, PendingAction> pendingConfirmations = new ConcurrentHashMap<>();

    public static Map<UUID, PendingAction> getPendingConfirmations() {
        return pendingConfirmations;
    }

    public void addPendingConfirmation(UUID playerUUID, PendingAction pendingAction) {
        pendingConfirmations.put(playerUUID, pendingAction);
        int confirmationTimeout = this.getConfig().getInt("home.confirmation.timeout_seconds", 30);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingConfirmations.remove(playerUUID, pendingAction)) {
                    Player player = Bukkit.getPlayer(playerUUID);

                    if (player != null && player.isOnline()) {
                        player.sendMessage("§cConfirmation expired.");
                    }
                }
            }
        }.runTaskLater(this, confirmationTimeout * 20L);
    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        HomeTabCompleter tabCompleter = new HomeTabCompleter(new File(getDataFolder(), "playerdata"));

        Commands commandHandler = new Commands(this);

        // Register commands
        this.getCommand("home").setExecutor(commandHandler);
        this.getCommand("homes").setExecutor(commandHandler);
        this.getCommand("sethome").setExecutor(commandHandler);
        this.getCommand("delhome").setExecutor(commandHandler);
        this.getCommand("confirm").setExecutor(commandHandler);
        this.getCommand("cancel").setExecutor(commandHandler);

        // Register tab completers
        this.getCommand("home").setTabCompleter(tabCompleter);
        this.getCommand("delhome").setTabCompleter(tabCompleter);

        // Register event handlers
        getServer().getPluginManager().registerEvents(new EventListener(this.getConfig()), this);
    }

    @Override
    public void onDisable() {
        pendingConfirmations.clear();
    }
}