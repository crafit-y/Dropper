package fr.crafity.dropper.listeners;

import fr.crafity.dropper.Dropper;
import fr.crafity.dropper.game.GameManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    private final Dropper plugin;
    private final GameManager game;

    public PlayerDeathListener(Dropper plugin) {
        this.plugin = plugin;
        this.game = plugin.getGameManager();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (!game.isInRotation(player)) return;

        // Vérifie si c’est actuellement son tour (il est en GameMode.ADVENTURE)
        //(player.getGameMode() == org.bukkit.GameMode.ADVENTURE || player.getGameMode() == GameMode.SURVIVAL) &&
        if (player.getGameMode() == org.bukkit.GameMode.ADVENTURE || player.getGameMode() == GameMode.SURVIVAL) {
            event.setCancelled(true);
            game.playerLanded(player, false); // ⚠️ considéré comme échec
        }
    }
}
