package fr.crafity.dropper.commands.commandFunction;

import fr.crafity.dropper.Dropper;
import fr.crafity.dropper.game.GameManager;
import fr.crafity.dropper.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerRemoveFunc {

    private static final GameManager game = Dropper.getInstance().getGameManager();

    public static boolean removePlayer(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dropper.admin")) {
            MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "Tu n’as pas la permission d’utiliser cette commande.");
            return true;
        }

        if (args.length <= 2) {
            MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "Utilisation : /dropper players remove <pseudo|@a>");
            return true;
        }

        if (args[2].equalsIgnoreCase("All")) {
            int removed = 0;

            // Copie des UUIDs pour éviter la modification concurrente
            List<UUID> toRemove = new ArrayList<>(game.getRotation());

            if (toRemove.size() <= 0) {
                MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "Il n'y a aucun joueurs dans la rotation !");
                return true;
            } else {
                removed = toRemove.size();
                game.clearAllPlayers();
                MessageUtil.send(sender, game.prefix_systeme + game.colorMessage + "§aSuppression de " + game.colorMentionedPlayer + removed + game.colorMessage +" joueur(s) de la rotation.");
                return true;
            }

        }
        else {
            Player target = Bukkit.getPlayer(args[2]);

            if (target == null) {
                MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "§Joueur introuvable ou hors ligne.");
                return true;
            }

            if (!game.isInRotation(target)) {
                MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "§Ce joueur n’est pas dans la rotation.");
                return true;
            }

            game.removePlayer(target);
            MessageUtil.send(sender, game.prefix_systeme + game.colorMentionedPlayer + target.getName() + game.colorMessage + " retiré de la rotation.");
            MessageUtil.send(target, game.prefix_systeme + game.colorMessage + "Tu as été retiré du jeu du Dropper.");
            return true;
        }
    }
}
