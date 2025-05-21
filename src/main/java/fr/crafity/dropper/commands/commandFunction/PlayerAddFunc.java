package fr.crafity.dropper.commands.commandFunction;

import fr.crafity.dropper.Dropper;
import fr.crafity.dropper.game.GameManager;
import fr.crafity.dropper.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class PlayerAddFunc {

    private static final GameManager game = Dropper.getInstance().getGameManager();

    public static boolean addPlayer(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dropper.admin")) {
            MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "Tu n’as pas la permission d’utiliser cette commande.");
            return true;
        }

        if (args.length <= 2) {
            sender.sendMessage(game.prefix_systeme + game.colorFailed + "Utilisation : /dropper players add <pseudo>");
            return true;
        }

        if (args.length >= 1 && args[2].equalsIgnoreCase("@a")) {
            int added = 0;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!game.isInRotation(online)) {
                    game.addPlayer(online);
                    MessageUtil.send(sender,game.prefix_systeme + game.colorMentionedPlayer + online.getName() + game.colorMessage + " ajouté à la rotation.");
                    MessageUtil.send(online,game.prefix_systeme + game.colorMessage + "Tu as été ajouté au jeu du Dropper !");
                    added++;
                }
            }
            MessageUtil.send(sender,game.prefix_systeme + game.colorMessage + "Ajout de " + game.colorMentionedPlayer + added + game.colorMessage + " joueur(s) à la rotation.");
            return true;
        } else {
            Player target = Bukkit.getPlayer(args[2]);

            if (target == null) {
                MessageUtil.send(sender,game.prefix_systeme + game.colorMentionedPlayer + "Joueur introuvable ou hors ligne.");
                return true;
            }

            if (game.isInRotation(target)) {
                MessageUtil.send(sender,game.prefix_systeme + game.colorMentionedPlayer + "Ce joueur est déjà dans la rotation.");
                return true;
            }


            game.addPlayer(target);
            MessageUtil.send(sender,game.prefix_systeme + game.colorMentionedPlayer + target.getName() + game.colorMessage + " ajouté à la rotation.");
            MessageUtil.send(target,game.prefix_systeme + game.colorMessage + "Tu as été ajouté au jeu Dropper !");
            return false;
        }
    }
}