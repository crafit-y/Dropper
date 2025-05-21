package fr.crafity.dropper.data;

import fr.crafity.dropper.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class LevelData {

    private Location jumpPoint;
    private Location limit1;
    private Location limit2;
    private int lives;
    private int jumpTime;
    private String creator;
    private String createdAt;
    private String world;

    // --- Getters & Setters ---
    public Location getJumpPoint() { return jumpPoint; }
    public void setJumpPoint(Location jumpPoint) { this.jumpPoint = jumpPoint; }

    public Location getLimit1() { return limit1; }
    public void setLimit1(Location limit1) { this.limit1 = limit1; }

    public Location getLimit2() { return limit2; }
    public void setLimit2(Location limit2) { this.limit2 = limit2; }

    public int getLives() { return lives; }
    public void setLives(int lives) { this.lives = lives; }

    public int getJumpTime() { return jumpTime; }
    public void setJumpTime(int jumpTime) { this.jumpTime = jumpTime; }

    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getWorld() { return world; }
    public void setWorld(String world) { this.world = world; }

    // --- Loading from File ---
    public static LevelData fromFile(File file) {
        if (!file.exists()) return null;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        LevelData data = new LevelData();

        // ✅ Meta Infos
        data.creator = config.getString("creator", "Unknown");
        data.createdAt = config.getString("created_at", "Unknown");
        data.world = config.getString("world", "world");

        // ✅ Settings
        data.lives = config.getInt("lives", 3);
        data.jumpTime = config.getInt("jump_time", 10);

        // ✅ Locations
        data.jumpPoint = LocationUtil.fromConfig(config.getConfigurationSection("jump"));
        data.limit1 = LocationUtil.fromConfig(config.getConfigurationSection("limit1"));
        data.limit2 = LocationUtil.fromConfig(config.getConfigurationSection("limit2"));

        return data;
    }


    // --- Saving to File ---
    public void toFile(FileConfiguration config) {
        // ✅ Meta Infos
        config.set("creator", creator);
        config.set("created_at", createdAt);
        config.set("world", world);

        // ✅ Settings
        config.set("lives", lives);
        config.set("jump_time", jumpTime);

        // ✅ Locations
        saveLocation(config.createSection("jump"), jumpPoint, true); // Avec yaw & pitch
        saveLocation(config.createSection("limit1"), limit1, false); // Sans yaw & pitch
        saveLocation(config.createSection("limit2"), limit2, false); // Sans yaw & pitch
    }

    private void saveLocation(ConfigurationSection section, Location loc, boolean includeOrientation) {
        if (loc == null) return;

        section.set("world", loc.getWorld().getName());
        section.set("x", loc.getX());
        section.set("y", loc.getY());
        section.set("z", loc.getZ());

        if (includeOrientation) {
            section.set("yaw", loc.getYaw());
            section.set("pitch", loc.getPitch());
        }
    }

}
