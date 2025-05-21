package fr.crafity.dropper.listeners;

import fr.crafity.dropper.Dropper;
import fr.crafity.dropper.game.GameManager;
import fr.crafity.dropper.data.LevelData;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.Random;

public class PlayerMovementListener implements Listener {

    private final Dropper plugin;
    private final Random random = new Random();

    public PlayerMovementListener(Dropper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        GameManager game = plugin.getGameManager();

        if (!game.isRunning() || player.getGameMode() != GameMode.SPECTATOR) return;
        if (!game.isInRotation(player)) return;

        LevelData level = game.loadLevel(game.getCurrentLevel()); //game.getLevel(game.getCurrentLevel());
        if (level == null || level.getLimit1() == null || level.getLimit2() == null) return;

        Location loc = player.getLocation();
        Location min = level.getLimit1();
        Location max = level.getLimit2();

        double minX = Math.min(min.getX(), max.getX());
        double maxX = Math.max(min.getX(), max.getX());
        double minY = Math.min(min.getY(), max.getY());
        double maxY = Math.max(min.getY(), max.getY());
        double minZ = Math.min(min.getZ(), max.getZ());
        double maxZ = Math.max(min.getZ(), max.getZ());

        boolean outOfBounds = loc.getX() < minX || loc.getX() > maxX
                || loc.getY() < minY || loc.getY() > maxY
                || loc.getZ() < minZ || loc.getZ() > maxZ;

        if (outOfBounds) {
            // Repoussement vers le centre de la zone
            double centerX = (minX + maxX) / 2;
            double centerY = (minY + maxY) / 2;
            double centerZ = (minZ + maxZ) / 2;
            Location center = new Location(loc.getWorld(), centerX, centerY, centerZ);

            Vector pushBack = center.toVector().subtract(loc.toVector()).normalize().multiply(0.4); // Avant : 1.5, maintenant plus doux
            player.setVelocity(pushBack);


            // Feedback sonore et visuel
            player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
        }

        // ✅ Affichage du mur dynamique en approche
        showWallParticles(player, loc, minX, maxX, minY, maxY, minZ, maxZ);
    }

    private void showWallParticles(Player player, Location loc, double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        double maxDistance = 2.0;
        double distance = distanceToZone(loc, minX, maxX, minY, maxY, minZ, maxZ);

        // Affiche les particules seulement si on est à moins de 2 blocs du mur
        if (distance > maxDistance) return;

        // Calcul de la proximité (1 = collé au mur, 0 = à 2 blocs)
        double proximity = 1.0 - (distance / maxDistance);
        double minRadius = 0.2;
        double maxRadius = 1.5;
        double radius = minRadius + (maxRadius - minRadius) * proximity;

        int minDensity = 10;
        int maxDensity = 50;
        int density = (int) (minDensity + (maxDensity - minDensity) * proximity);

        Particle.DustOptions dust = new Particle.DustOptions(Color.RED, 1.0f);

        // Calcul du mur le plus proche
        double dx = Math.min(Math.abs(loc.getX() - minX), Math.abs(loc.getX() - maxX));
        double dy = Math.min(Math.abs(loc.getY() - minY), Math.abs(loc.getY() - maxY));
        double dz = Math.min(Math.abs(loc.getZ() - minZ), Math.abs(loc.getZ() - maxZ));

        Location wallCenter;
        String plane;

        if (dx <= dy && dx <= dz) {
            double wallX = (Math.abs(loc.getX() - minX) < Math.abs(loc.getX() - maxX)) ? minX : maxX;
            wallCenter = new Location(loc.getWorld(), wallX, loc.getY() + 1.0, loc.getZ());
            plane = "YZ";
        } else if (dy <= dx && dy <= dz) {
            double wallY = (Math.abs(loc.getY() - minY) < Math.abs(loc.getY() - maxY)) ? minY : maxY;
            wallCenter = new Location(loc.getWorld(), loc.getX(), wallY, loc.getZ());
            plane = "XZ";
        } else {
            double wallZ = (Math.abs(loc.getZ() - minZ) < Math.abs(loc.getZ() - maxZ)) ? minZ : maxZ;
            wallCenter = new Location(loc.getWorld(), loc.getX(), loc.getY() + 1.0, wallZ);
            plane = "XY";
        }

        for (int i = 0; i < density; i++) {
            double r = radius * Math.sqrt(random.nextDouble());
            double angle = 2 * Math.PI * random.nextDouble();
            double offsetX = Math.cos(angle) * r;
            double offsetY = Math.sin(angle) * r;

            Location particleLoc;
            switch (plane) {
                case "YZ" -> particleLoc = wallCenter.clone().add(0, offsetX, offsetY);
                case "XZ" -> particleLoc = wallCenter.clone().add(offsetX, 0, offsetY);
                case "XY" -> particleLoc = wallCenter.clone().add(offsetX, offsetY, 0);
                default -> {
                    continue;
                }
            }

            player.spawnParticle(Particle.DUST, particleLoc, 1, dust);
        }
    }

    private double distanceToZone(Location loc, double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        double dx = Math.max(0, Math.max(minX - loc.getX(), loc.getX() - maxX));
        double dy = Math.max(0, Math.max(minY - loc.getY(), loc.getY() - maxY));
        double dz = Math.max(0, Math.max(minZ - loc.getZ(), loc.getZ() - maxZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
