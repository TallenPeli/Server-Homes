package dev.tallenpeli.serverHomes;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeTabCompleter implements TabCompleter {
    private final File playerDataFolder;

    public HomeTabCompleter(File playerDataFolder) {
        this.playerDataFolder = playerDataFolder;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        // Only provide completions for the first argument (home name)
        if (args.length == 1) {
            return getPlayerHomes(player);
        }

        return Collections.emptyList();
    }

    private List<String> getPlayerHomes(Player player) {
        File playerFile = new File(playerDataFolder, player.getUniqueId().toString() + ".yml");

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
}