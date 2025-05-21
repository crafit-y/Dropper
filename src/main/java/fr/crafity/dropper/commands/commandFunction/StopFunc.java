package fr.crafity.dropper.commands.commandFunction;

import fr.crafity.dropper.Dropper;
import fr.crafity.dropper.game.GameManager;
import fr.crafity.dropper.util.MessageUtil;

import org.bukkit.command.CommandSender;

public class StopFunc {

    private static final GameManager game = Dropper.getInstance().getGameManager();

    public static void stopCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dropper.admin")) {
            MessageUtil.send(sender,game.prefix_systeme + game.colorFailed + "Tu n’as pas la permission d’utiliser cette commande.");
            return;
        }

        if (!game.isRunning() && !game.isPaused()) {
            MessageUtil.send(sender,game.prefix_systeme + game.colorFailed + "Aucune partie en cours.");
            return;
        }

        game.stopGame(true);
        MessageUtil.send(sender,game.prefix_systeme + game.colorMessage + "Jeu Dropper arrêté. Vous pouvez redémarrer avec /start <level>.");
    }
}