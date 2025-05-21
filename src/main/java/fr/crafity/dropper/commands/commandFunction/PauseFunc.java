package fr.crafity.dropper.commands.commandFunction;

import fr.crafity.dropper.Dropper;
import fr.crafity.dropper.game.GameManager;
import fr.crafity.dropper.util.CooldownManager;
import fr.crafity.dropper.util.MessageUtil;
import org.bukkit.command.CommandSender;

public class PauseFunc {

    private static final GameManager game = Dropper.getInstance().getGameManager();
    private static final CooldownManager cooldownManager = Dropper.getInstance().getCooldownManager();
    private static final String COMMAND_NAME = "pause";
    private static final int COOLDOWN_SECONDS = 5;

    public static boolean pauseCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dropper.admin")) {
            MessageUtil.send(sender,game.prefix_systeme + game.colorFailed + "Tu n’as pas la permission d’utiliser cette commande.");
            return true;
        }

        if (cooldownManager.isOnCooldown(sender, COMMAND_NAME, COOLDOWN_SECONDS)) {
            long remaining = cooldownManager.getRemainingTime(sender, COMMAND_NAME);
            MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "Tu dois attendre " + game.colorMessage + remaining + "s " + game.colorFailed + "avant de réutiliser cette commande.");
            return true;
        }

        if (!game.isRunning()) {
            MessageUtil.send(sender,game.prefix_systeme + game.colorFailed + "Aucune partie en cours.");
            return true;
        }

        if (!game.isPaused()) {
            game.pauseGame();
            MessageUtil.send(sender,game.prefix_systeme + game.colorMessage + "Le jeu est maintenant " + game.colorFailed + "pause.");
        } else {
            game.resumeGame();
            MessageUtil.send(sender,game.prefix_systeme + game.colorMessage + "La rotation reprend.");
        }
        cooldownManager.setCooldown(sender, COMMAND_NAME, COOLDOWN_SECONDS);
        return true;
    }
}
