package fr.crafity.dropper.util;

import fr.crafity.dropper.Dropper;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class Animations {

    public static void startPerfectDrop(Location center, Particle particleType) {
        World world = center.getWorld();
        final long intervalTicks = 1L;

        // ✅ Centre précisément sur le bloc (au milieu du bloc)
        Location centeredLocation = center.clone().add(0.5, 0, 0.5);

        if (centeredLocation.getBlock().getType() != Material.AIR) {
            Location fireworkLoc = centeredLocation.clone().add(0.5, 1, 0.5); // Un bloc au-dessus du coin
            Firework firework = (Firework) world.spawnEntity(fireworkLoc, EntityType.FIREWORK_ROCKET);

            FireworkMeta meta = firework.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder()
                    .withColor(Color.RED, Color.ORANGE)
                    .withFade(Color.YELLOW)
                    .with(FireworkEffect.Type.BURST)
                    .trail(true)
                    .flicker(true)
                    .build());
            meta.setPower(1);
            firework.setFireworkMeta(meta);
            firework.setTicksToDetonate(15);
            //firework.detonate(); // Instant detonation
        }

        new BukkitRunnable() {
            int currentStep = 0;
            final int totalSteps = 40;
            final double finalHeight = 2.0;
            final double growthRate = 0.025;
            final int pointsPerCircle = 15;
            double radius = 0.2;
            double y = 0;

            @Override
            public void run() {
                if (currentStep >= totalSteps) {
                    //world.getPlayers().forEach(p -> p.sendMessage("§b✨ Une fine spirale magique s'élève vers le ciel..."));
                    cancel();
                    return;
                }

                double angle = currentStep * (2 * Math.PI / pointsPerCircle);
                double xOffset = Math.cos(angle) * radius;
                double zOffset = Math.sin(angle) * radius;

                Location particleLoc = centeredLocation.clone().add(xOffset, y, zOffset);
                world.spawnParticle(particleType, particleLoc, 6, 0, 0, 0, 0);

                y += finalHeight / totalSteps;
                radius += growthRate;
                currentStep++;
            }
        }.runTaskTimer(Dropper.getInstance(), 0L, intervalTicks);
    }

    public static void startSuperJump(Location center, Particle particleType) {
        World world = center.getWorld();
        Location baseCenter = center.clone().add(0.5, 0.8, 0.5); // Centre du bloc

        // ✅ 1. Feux d'artifice sur les 4 coins si blocs présents
        int[][] offsets = {{1, 1}, {-1, 1}, {1, -1}, {-1, -1}};
        for (int[] offset : offsets) {
            Location loc = center.clone().add(offset[0], 0, offset[1]);
            if (loc.getBlock().getType() != Material.AIR) {
                Location fireworkLoc = loc.clone().add(0.5, 1, 0.5); // Un bloc au-dessus du coin
                Firework firework = (Firework) world.spawnEntity(fireworkLoc, EntityType.FIREWORK_ROCKET);

                FireworkMeta meta = firework.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .withColor(Color.PURPLE, Color.BLUE)
                        .withFade(Color.RED)
                        .with(FireworkEffect.Type.BURST)
                        .trail(true)
                        .flicker(true)
                        .build());
                meta.setPower(1);
                firework.setFireworkMeta(meta);
                firework.setTicksToDetonate(15);
                //firework.detonate(); // Instant detonation
            }
        }

        // ✅ 2. Cercles de particules qui grandissent avec impulsions et sons
        final int totalImpulses = 3;
        final double maxRadius = 3.5;
        final long interval = 10L; // 0.5s entre impulsions

        new BukkitRunnable() {
            int impulse = 0;

            @Override
            public void run() {
                if (impulse >= totalImpulses) {
                    cancel();
                    return;
                }

                double currentRadius = (impulse + 1) * (maxRadius / totalImpulses);
                int points = 40;

                for (int i = 0; i < points; i++) {
                    double angle = 2 * Math.PI * i / points;
                    double x = Math.cos(angle) * currentRadius;
                    double z = Math.sin(angle) * currentRadius;

                    Location particleLoc = baseCenter.clone().add(x, 0.1, z);
                    world.spawnParticle(
                            particleType,
                            particleLoc,
                            3,
                            0, 0, 0, 0
                    );
                }

                impulse++;
            }
        }.runTaskTimer(Dropper.getInstance(), 0L, interval);
    }

}
