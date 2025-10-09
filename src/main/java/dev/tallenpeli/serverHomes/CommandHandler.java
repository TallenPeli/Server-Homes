package dev.tallenpeli.serverHomes;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandHandler implements CommandExecutor {
    private final HomeManager homeManager;
    private final TeleportManager teleportManager;

    private final ServerHomes plugin;


    public CommandHandler(HomeManager homeManager, TeleportManager teleportManager, ServerHomes plugin) {
        this.homeManager = homeManager;
        this.teleportManager = teleportManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        final String homeName = (args.length > 0) ? String.join(" ", args) : "home"; // join the arguments into a house name

        return switch (label.toLowerCase()) {
            case "homes" -> homeManager.listHomes(player);
            case "sethome" -> homeManager.setHome(player, homeName, false);
            case "delhome" -> homeManager.delHome(player, homeName, false);
            case "confirm" -> homeManager.confirm(player);
            case "cancel" -> homeManager.cancel(player);
            case "home" -> teleportManager.teleportHome(player, homeName);
            default -> false;
        };
    }
}