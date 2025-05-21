package fr.crafity.dropper.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class LocationUtil {

    // Récupère l'emplacement exact du bloc visé par le joueur (max 100 blocs)
    public static Location getTargetBlockLocation(Player player) {
        return player.getTargetBlockExact(100).getLocation();
    }

    // Convertit une section de config en Location
    public static Location fromConfig(ConfigurationSection section) {
        if (section == null) return null;

        World world = Bukkit.getWorld(section.getString("world", "world"));
        if (world == null) {
            Bukkit.getLogger().warning("[Dropper] ⚠ World manquant dans la configuration de location : " + section.getCurrentPath());
            return null;
        }

        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw");
        float pitch = (float) section.getDouble("pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }

    // Enregistre une Location dans une section de config
    public static void toConfig(ConfigurationSection section, Location loc, boolean includeAngles) {
        section.set("world", loc.getWorld().getName());
        section.set("x", loc.getX());
        section.set("y", loc.getY());
        section.set("z", loc.getZ());
        if (includeAngles) {
            section.set("yaw", loc.getYaw());
            section.set("pitch", loc.getPitch());
        }
    }

}
