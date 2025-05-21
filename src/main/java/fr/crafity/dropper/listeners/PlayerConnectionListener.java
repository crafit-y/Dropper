package fr.crafity.dropper.listeners;

import fr.crafity.dropper.Dropper;
import fr.crafity.dropper.game.GameManager;
import fr.crafity.dropper.data.LevelData;
import fr.crafity.dropper.data.PlayerData;
import fr.crafity.dropper.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerConnectionListener implements Listener {

    private final Dropper plugin;
    private final GameManager game;

    public PlayerConnectionListener(Dropper plugin) {
        this.plugin = plugin;
        this.game = plugin.getGameManager();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        PlayerData data = game.getPlayerData(uuid);
        String playerName = Bukkit.getOfflinePlayer(uuid).getName();

        if (!game.isRunning() || game.isPaused()) return;

        if (data == null || !game.isInRotation(player)) return;

        int max_disconnection = plugin.getConfig().getInt("max_disconnection", 2);
        int max_missed_turn = plugin.getConfig().getInt("max_missed_turn", 3);

        if (max_disconnection == -1) return;

        // ✅ Gestion des déconnexions
        data.incrementDisconnects();

        if (data.getDisconnectCount() >= max_disconnection) {
            game.getPlayerData(player.getUniqueId()).setEliminated(true);
            data.setPendingSuperJump(false);
            game.disconnectedPlayers.remove(uuid);
            event.setQuitMessage("");
            MessageUtil.broadcast(game.prefix_systeme + game.colorMentionedPlayer + playerName + " " + game.colorError + "été éliminé après " + game.colorMessage + max_disconnection + " déconnexion" + game.sss(max_disconnection) + game.colorError + ".");
        } else {
            game.disconnectedPlayers.add(uuid);
            event.setQuitMessage("");
            MessageUtil.broadcast(game.prefix_systeme + game.colorMentionedPlayer + playerName + " " + game.colorFailed + "a quitté, il sera éliminé après " + game.colorMessage + max_missed_turn + " tour" + game.sss(max_missed_turn) + game.colorFailed + " manqués.");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        PlayerData data = game.getPlayerData(uuid);

        if (!game.isRunning() || game.isPaused()) return;

        // ✅ Mettre directement en mode spectateur et téléporter
        player.setGameMode(GameMode.SPECTATOR);

        if (game.getCurrentLevel() != null) {
            LevelData level = game.loadLevel(game.getCurrentLevel());
            if (level != null && level.getJumpPoint() != null) {
                player.teleport(level.getJumpPoint());
            }
        }

        if (data != null) {

            int missedTurns = data.getMissedTurns();

            event.setJoinMessage("");
            game.disconnectedPlayers.remove(uuid);
            if (data.hasPendingSuperJump()) {
                MessageUtil.broadcast(game.prefix_systeme + game.colorMentionedPlayer + player.getName() + game.colorSuccess + " est de retour ! Son prochain saut sera un §dSuper Jump" + game.colorSuccess + " ! §7Du fais qu'il ait manqué " + game.colorMessage + missedTurns + " §7tour" + game.sss(missedTurns) +".");
            } else {
                MessageUtil.broadcast(game.prefix_systeme + game.colorMentionedPlayer + player.getName() + game.colorSuccess + " est de retour !");
            }
            data.resetMissedTurns();
            // ✅ Reset des déconnexions à la reconnexion
            //data.resetDisconnects(); // Décommente si tu veux reset
        }
    }

}
