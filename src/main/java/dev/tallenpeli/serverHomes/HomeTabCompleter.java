package dev.tallenpeli.serverHomes;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HomeTabCompleter implements TabCompleter {
    private final HomeManager homeManager;

    public HomeTabCompleter(HomeManager homeManager) {
        this.homeManager = homeManager;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        String cmd = command.getName().toLowerCase();
        String partialInput = args[args.length - 1].toLowerCase();

        switch (cmd) {
            case "home", "delhome" -> {
                if (args.length == 1) {
                    return homeManager.getPlayerHomes(player).stream()
                            .filter(name -> name.toLowerCase().startsWith(partialInput))
                            .collect(Collectors.toList());
                }
            }
            case "sethome" -> {
                if (args.length == 1) {
                    return args[0].isEmpty() ? Collections.singletonList("home") : Collections.singletonList(args[0]);
                }
            }
            case "homeadmin" -> {
                if (args.length == 1) {
                    return Stream.of("list", "tp", "delhome")
                            .filter(s -> s.startsWith(partialInput))
                            .collect(Collectors.toList());
                }

                if (args.length == 2) {
                    return getOnlinePlayerNames(player, partialInput);
                }

                if (args.length == 3) {
                    if (args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("delhome")) {
                        YamlConfiguration targetConfig = homeManager.getConfigFromName(args[1]);
                        if (targetConfig == null) return Collections.emptyList();

                        ConfigurationSection homeSection = targetConfig.getConfigurationSection("homes");
                        if (homeSection == null) return Collections.emptyList();

                        return homeSection.getKeys(false).stream()
                                .filter(home -> home.toLowerCase().startsWith(partialInput))
                                .collect(Collectors.toList());
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * Helper to get online player names the sender is allowed to see.
     */
    private List<String> getOnlinePlayerNames(Player sender, String partialInput) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(sender::canSee)
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partialInput.toLowerCase()))
                .collect(Collectors.toList());
    }
}