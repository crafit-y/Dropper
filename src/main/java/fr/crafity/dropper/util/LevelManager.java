package fr.crafity.dropper.util;

import fr.crafity.dropper.Dropper;
import fr.crafity.dropper.data.LevelData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class LevelManager {

    static Dropper plugin = Dropper.getInstance();

    public static void saveLevel(String levelName, LevelData data) {
        File file = new File(plugin.getDataFolder(), "levels/" + levelName + ".yml");
        FileConfiguration config = new YamlConfiguration();
        data.toFile(config);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder le niveau : " + levelName);
        }
    }

    public static void deleteLevel(String levelName) {
        File file = new File(plugin.getDataFolder(), "levels/" + levelName + ".yml");
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Impossible de supprimer le niveau : " + levelName);
        }
    }
}
