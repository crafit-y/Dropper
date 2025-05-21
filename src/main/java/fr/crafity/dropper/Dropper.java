package fr.crafity.dropper;

import fr.crafity.dropper.commands.*;
import fr.crafity.dropper.game.GameManager;
import fr.crafity.dropper.listeners.*;
import fr.crafity.dropper.util.CooldownManager;
import fr.crafity.dropper.util.DropperTabCompleter;
import fr.crafity.dropper.util.VersionChecker;
import org.bukkit.plugin.java.JavaPlugin;

public class Dropper extends JavaPlugin {

    private static Dropper instance;
    private GameManager gameManager;
    private CooldownManager cooldownManager;

    // Codes ANSI pour la coloration de la console
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";

    @Override
    public void onEnable() {
        getLogger().info(ANSI_YELLOW + "---------------------------------------" + ANSI_RESET);
        getLogger().info(ANSI_YELLOW + "[Dropper] Démarrage..." + ANSI_RESET);
        instance = this;
        saveDefaultConfig();
        getLogger().info(ANSI_YELLOW + "[Dropper] Recherche de mis à jours..." + ANSI_RESET);
        VersionChecker.checkForUpdate(this);
        // Initialisation du GameManager
        getLogger().info(ANSI_YELLOW + "---------------------------------------" + ANSI_RESET);
        getLogger().info(ANSI_YELLOW + "[Dropper] Création des managers..." + ANSI_RESET);
        gameManager = new GameManager(this);
        getLogger().info(ANSI_GREEN + "[Dropper] ✔ Game manager chargé avec succès." + ANSI_RESET);
        cooldownManager = new CooldownManager(); // Idéalement défini en attribut si utilisé ailleurs
        getLogger().info(ANSI_GREEN + "[Dropper] ✔ Cooldown manager chargé avec succès." + ANSI_RESET);
        getLogger().info(ANSI_YELLOW + "---------------------------------------" + ANSI_RESET);
        getLogger().info(ANSI_YELLOW + "[Dropper] Chargement du système..." + ANSI_RESET);

        // Commandes et Tab Completion
        registerCommands();
        registerTabCompleters();


        // Événements
        getServer().getPluginManager().registerEvents(new DropEventListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerMovementListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInventoryInteractListener(this), this);

        // Chargement des niveaux
        // SUPPRESSED -----
        getLogger().info(ANSI_GREEN + "[Dropper] ✔ Chargé avec succès." + ANSI_RESET);
        getLogger().info(ANSI_YELLOW + "---------------------------------------" + ANSI_RESET);
        getLogger().info(ANSI_GREEN + "[Dropper] Plugin activé avec succès !" + ANSI_RESET);
        getLogger().info(ANSI_YELLOW + "---------------------------------------" + ANSI_RESET);
    }

    @Override
    public void onDisable() {
        getLogger().info(ANSI_YELLOW + "---------------------------------------" + ANSI_RESET);
        getLogger().info(ANSI_YELLOW + "[Dropper] Début de la sauvegarde..." + ANSI_RESET);
        if (gameManager != null) {
            if (gameManager.isRunning() || gameManager.isPaused()) gameManager.stopGame(false);
        }
        getLogger().info(ANSI_GREEN + "[Dropper] ✔ Level réinitialisé avec succès." + ANSI_RESET);
        //getLogger().info(ANSI_GREEN + "[Dropper] ✔ Tous les niveaux sauvegardés avec succès." + ANSI_RESET);
        getLogger().info(ANSI_YELLOW + "---------------------------------------" + ANSI_RESET);
        getLogger().info(ANSI_GREEN + "[Dropper] Plugin désactivé." + ANSI_RESET);
        getLogger().info(ANSI_YELLOW + "---------------------------------------" + ANSI_RESET);
    }

    private void registerCommands() {
        getCommand("dropper").setExecutor(new DropperMainCommand(this));
        getCommand("sys").setExecutor(new SysCommand(this));

    }

    private void registerTabCompleters() {
        DropperTabCompleter tabCompleter = new DropperTabCompleter(this);

        String[] commands = {"dropper", "sys", "force", "skip"};
        for (String cmd : commands) {
            if (getCommand(cmd) != null) {
                getCommand(cmd).setTabCompleter(tabCompleter);
            }
        }
    }

    public static Dropper getInstance() {
        return instance;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

}
