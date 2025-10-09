package dev.tallenpeli.serverHomes;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

        String partialName = args[0].toLowerCase();

        if (label.equalsIgnoreCase("home") || label.equalsIgnoreCase("delhome")) {
            if (args.length == 1) {
                List<String> allHomes = homeManager.getPlayerHomes(player);

                return allHomes.stream()
                        .filter(name -> name.toLowerCase().startsWith(partialName))
                        .collect(Collectors.toList());
            }

        } else if (label.equalsIgnoreCase("sethome")) {
            if (args[0].isEmpty()) {
                return Collections.singletonList("home");
            } else {
                return Collections.singletonList(args[0]);
            }
        }

        return Collections.emptyList();
    }
}