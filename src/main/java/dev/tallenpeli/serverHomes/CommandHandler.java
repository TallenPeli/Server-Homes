package dev.tallenpeli.serverHomes;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class CommandHandler implements CommandExecutor {
    private final HomeManager homeManager;
    private final TeleportManager teleportManager;

    public CommandHandler(HomeManager homeManager, TeleportManager teleportManager) {
        this.homeManager = homeManager;
        this.teleportManager = teleportManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use these commands.");
            return true;
        }

        String cmd = command.getName().toLowerCase();

        if (cmd.equals("homeadmin")) {
            if (!player.hasPermission("serverhomes.admin")) {
                player.sendMessage("§cNo permission.");
                return true;
            }

            if (args.length == 0) {
                player.sendMessage("§6§lServerHomes Admin Help");
                player.sendMessage("§e/ha list <player> §7- List homes");
                player.sendMessage("§e/ha tp <player> <home> §7- Teleport to home");
                return true;
            }

            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "list" -> {
                    if (args.length < 2) {
                        player.sendMessage("§cUsage: /ha list <player>");
                        return true;
                    }
                    return homeManager.adminListHomes(player, args[1]);
                }
                case "tp" -> {
                    if (args.length < 3) {
                        player.sendMessage("§cUsage: /ha tp <player> <home>");
                        return true;
                    }
                    String homeName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    return homeManager.adminHomeTp(player, args[1], homeName);
                }
                default -> {
                    player.sendMessage("§cUnknown sub-command.");
                    return true;
                }
            }
        }

        String homeName = (args.length > 0) ? String.join(" ", args) : "home";
        return switch (cmd) {
            case "homes" -> homeManager.listHomes(player);
            case "sethome" -> homeManager.setHome(player, homeName, false);
            case "delhome" -> homeManager.delHome(player, homeName, false);
            case "home" -> teleportManager.teleportHome(player, homeName);
            case "confirm" -> homeManager.confirm(player);
            case "cancel" -> homeManager.cancel(player);
            default -> false;
        };
    }
}