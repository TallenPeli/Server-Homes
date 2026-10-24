package dev.tallenpeli.serverHomes;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.Objects;

public class AdminUtils {
    private final HomeManager homeManager;
    private final DataManager dataManager;

    public AdminUtils(HomeManager homeManager, DataManager dataManager) {
        this.homeManager = homeManager;
        this.dataManager = dataManager;
    }

    public boolean listPlayerHomes(Player player, String targetPlayerName) {
        if (targetPlayerName.isEmpty()) {
            player.sendMessage("§cPlease enter a player's name.");
            return false;
        }

        YamlConfiguration targetPlayerConfig = dataManager.getConfigFromName(targetPlayerName);

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

    public boolean deletePlayerHome(Player player, String targetPlayerName, String targetHomeName) {
        if (targetPlayerName.isEmpty()) {
            player.sendMessage("§cPlease enter a player's name.");
            return false;
        }

        YamlConfiguration targetPlayerConfig = dataManager.getConfigFromName(targetPlayerName);
        if (targetPlayerConfig == null) {
            player.sendMessage("§cCould not find data for player: §e" + targetPlayerName);
            return false;
        }

        String path = "homes." + targetHomeName;
        if (!targetPlayerConfig.contains(path)) {
            player.sendMessage("§cPlayer §e" + targetPlayerName + "§c has no home named §b'" + targetHomeName + "'§c.");
            return false;
        }

        return homeManager.deleteHome(Bukkit.getPlayer(targetPlayerName), targetHomeName, true);
    }

    public boolean teleportToPlayerHome(Player player, String targetPlayerName, String homeName) {
        if (targetPlayerName.isEmpty()) {
            player.sendMessage("§cPlease enter a player's name.");
            return false;
        }

        YamlConfiguration targetPlayerConfig = dataManager.getConfigFromName(targetPlayerName);
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
