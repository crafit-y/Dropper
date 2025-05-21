package fr.crafity.dropper.commands.commandFunction;

import fr.crafity.dropper.Dropper;
import fr.crafity.dropper.game.GameManager;
import fr.crafity.dropper.data.LevelData;
import fr.crafity.dropper.util.MessageUtil;
import org.bukkit.command.*;

public class StartFunc {

    private static final GameManager game = Dropper.getInstance().getGameManager();

    public static void startCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dropper.admin")) {
            MessageUtil.send(sender,game.prefix_systeme + game.colorFailed + "Tu n’as pas la permission d’utiliser cette commande.");
            return;
        }

        if (args.length <= 1) {
            MessageUtil.send(sender,game.prefix_systeme + game.colorFailed + "Utilisation : /dropper start <level>");
            return;
        }

        String levelName = args[1];

        LevelData level = game.loadLevel(levelName);
        if (level == null) {
            MessageUtil.send(sender,game.prefix_systeme + game.colorFailed + "Le niveau " + game.colorMentionedPlayer + levelName + game.colorFailed + " §cn'existe pas ou n'a pas pu être chargé.");
            return;
        }

        if (level.getJumpPoint() == null) {
            MessageUtil.send(sender,game.prefix_systeme + game.colorFailed + "Erreur : Le niveau " + game.colorMentionedPlayer + levelName + game.colorFailed + " n'est pas correctement configuré (point de saut manquant).");
            return;
        }

        if (game.getRotation().isEmpty()) {
            MessageUtil.send(sender,game.prefix_systeme + game.colorFailed + "§cAucun joueur n’a été ajouté à la rotation !");
            return;
        }

        if (game.isRunning()) {
            if (game.isPaused()) {
                MessageUtil.send(sender,game.prefix_systeme + game.colorFailed + "Une partie est en pause. Utilise §6/pause§e pour la reprendre.");
            } else {
                MessageUtil.send(sender,game.prefix_systeme + game.colorFailed + "Une partie est déjà en cours !");
            }
            return;
        }

        game.startGame(levelName);
        MessageUtil.send(sender,game.prefix_systeme + game.colorMessage + "Le jeu a été lancé sur le niveau : " + game.colorMentionedPlayer + levelName);
    }
}
