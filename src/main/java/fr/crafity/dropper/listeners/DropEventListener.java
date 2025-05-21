package fr.crafity.dropper.listeners;

import fr.crafity.dropper.Dropper;
import fr.crafity.dropper.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class DropEventListener implements Listener {

    private final Dropper plugin;
    private final GameManager game;
    public static final HashSet<UUID> fallingPlayers = new HashSet<>();
    private final Map<UUID, Integer> waterContactTicks = new HashMap<>();
    private final Map<UUID, Double> lastY = new HashMap<>();

    private static final int WATER_CONFIRMATION_TICKS = 2; // Nombre de ticks pour valider le contact avec l'eau

    public DropEventListener(Dropper plugin) {
        this.plugin = plugin;
        this.game = plugin.getGameManager();
        startDetectionLoop();
    }

    private void startDetectionLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!game.isInRotation(player)) continue;

                    UUID uuid = player.getUniqueId();

                    // ✅ Détection du saut
                    double currentY = player.getLocation().getY();
                    double previousY = lastY.getOrDefault(uuid, currentY);

                    if (!fallingPlayers.contains(uuid)
                            && player.getVelocity().getY() < -0.3
                            && player.getFallDistance() > 1.2
                            && !player.isOnGround()
                            && currentY < previousY) {

                        fallingPlayers.add(uuid);
                        game.onPlayerJump(player);
                    }

                    // ✅ Si le joueur est en train de tomber
                    if (fallingPlayers.contains(uuid)) {

                        // ✅ Vérification contact eau (avec confirmation sur X ticks)
                        if (player.isInWater()) {
                            int ticks = waterContactTicks.getOrDefault(uuid, 0) + 1;
                            if (ticks >= WATER_CONFIRMATION_TICKS) {
                                if (game.isPlayerInTurn(player)) {
                                    game.playerLanded(player, true);
                                }
                                fallingPlayers.remove(uuid);
                                waterContactTicks.remove(uuid);
                            } else {
                                waterContactTicks.put(uuid, ticks);
                            }
                            continue;
                        } else {
                            // Pas dans l'eau : reset du compteur de ticks
                            waterContactTicks.remove(uuid);
                        }

                        // ✅ Échec : a touché le sol sans être dans l'eau
                        if (player.isOnGround() && !player.isInWater()) {
                            game.playerLanded(player, false);
                            fallingPlayers.remove(uuid);
                            waterContactTicks.remove(uuid);
                        }
                    }

                    lastY.put(uuid, currentY);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onAnyMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (GameManager.blockedPlayers.contains(uuid)) {
            if (event.getFrom().getX() != event.getTo().getX() ||
                    event.getFrom().getY() != event.getTo().getY() ||
                    event.getFrom().getZ() != event.getTo().getZ()) {
                event.setCancelled(true);
            }
        }
    }
}
