package dev.tallenpeli.serverHomes;

import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Note;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;

public record EventListener(FileConfiguration config) implements Listener {

    private void cancelTeleport(Player player, String reason) {
        Map<UUID, BukkitTask> activeTeleports = Commands.getActiveTeleports();
        BukkitTask task = activeTeleports.remove(player.getUniqueId());

        if (task != null) {
            boolean soundsEnabled = config.getBoolean("home.enable_sounds");
            task.cancel();
            player.sendMessage(String.format("§cTeleport cancelled §4(%s)", reason));

            if (soundsEnabled) {
                player.playNote(player.getLocation(), Instrument.BASS_GUITAR, new Note(1, Note.Tone.C, false));
            }
        }
    }

    @EventHandler
    public void onDamageTaken(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            // Accessing a hashmap is faster than accessing the config. Will cancel quickly if player does not have active teleports.
            if (Commands.getActiveTeleports().containsKey(player.getUniqueId())) {
                if (config.getBoolean("home.teleport.cancel_events.damage", true)) {
                    cancelTeleport(player, "you took damage");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMoved(PlayerMoveEvent event) {
        final Player player = event.getPlayer();

        // Accessing a hashmap is faster than accessing the config. Will cancel quickly if player does not have active teleports.
        if (Commands.getActiveTeleports().containsKey(player.getUniqueId())) {
            if (config.getBoolean("home.teleport.cancel_events.movement", true)) {
                Location to = event.getTo();
                Location from = event.getFrom();

                assert to != null;
                if (from.getBlockX() != to.getBlockX() ||
                        from.getBlockY() != to.getBlockY() ||
                        from.getBlockZ() != to.getBlockZ()) {
                    cancelTeleport(player, "you moved");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        // We check them one at a time for performance. This could be combined, but I then I would need to evaluate all the conditions.
        if (event.getDamager() instanceof Player player) {
            if (Commands.getActiveTeleports().containsKey(player.getUniqueId())) {
                if (config.getBoolean("home.teleport.cancel_events.attack", true)) {
                    cancelTeleport(player, "you attacked");
                }
            }
        }
    }
}
