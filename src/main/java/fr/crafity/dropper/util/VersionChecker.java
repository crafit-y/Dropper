package fr.crafity.dropper.util;

import fr.crafity.dropper.Dropper;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class VersionChecker {

    private static final String VERSION_URL = "https://raw.githubusercontent.com/crafit-y/Dropper/main/version.txt";
    private static final String CURRENT_VERSION = "1.0.0"; // à mettre à jour à chaque release

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";

    public static void checkForUpdate(JavaPlugin plugin) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(VERSION_URL);
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                String latestVersion = reader.readLine().trim();
                reader.close();

                if (!CURRENT_VERSION.equals(latestVersion)) {
                    Bukkit.getLogger().warning(ANSI_YELLOW + "[Dropper] Nouvelle version disponible : §e" + latestVersion + " §7(vous utilisez §c" + CURRENT_VERSION + "§7)" + ANSI_RESET);
                } else {
                    Bukkit.getLogger().info(ANSI_GREEN + "§[Dropper] Vous utilisez la dernière version (" + CURRENT_VERSION + ")" + ANSI_RESET);
                }

            } catch (Exception e) {
                Bukkit.getLogger().warning(ANSI_RED + "[Dropper] Impossible de vérifier les mises à jour : " + e.getMessage() + ANSI_RESET);
            }
        });
    }
}
