package fr.crafity.dropper.commands.commandFunction;

import fr.crafity.dropper.Dropper;
import fr.crafity.dropper.game.GameManager;
import fr.crafity.dropper.util.CooldownManager;
import fr.crafity.dropper.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class SkipFunc {

    private static final GameManager game = Dropper.getInstance().getGameManager();
    private static final CooldownManager cooldownManager = Dropper.getInstance().getCooldownManager();
    private static final String COMMAND_NAME = "skip";
    private static final int COOLDOWN_SECONDS = 3;

    public static void skipCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dropper.admin")) {
            MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "Tu n’as pas la permission d’utiliser cette commande.");
            return;
        }

        if (cooldownManager.isOnCooldown(sender, COMMAND_NAME, COOLDOWN_SECONDS)) {
            long remaining = cooldownManager.getRemainingTime(sender, COMMAND_NAME);
            MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "Tu dois attendre " + game.colorMessage + remaining + "s " + game.colorFailed + "avant de réutiliser cette commande.");
            return;
        }

        if (!game.isRunning()) {
            MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "La partie n’est pas en cours.");
            return;
        }

        Player target = null;
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "Ce joueur est introuvable ou hors-ligne.");
                return;
            }
        }

        GameManager.SkipResult result = game.skipPlayer(target);

        switch (result) {
            case SKIPPED_CURRENT -> {
                Player p = target != null ? target : game.getCurrentPlayerInTurn();
                if (p != null) {
                    MessageUtil.broadcast(game.prefix_systeme + game.colorMentionedPlayer + p.getName() + " " + game.colorSuccess + "a été passé !");
                }
            }
            case MOVED_IN_ROTATION -> MessageUtil.broadcast(game.prefix_systeme + game.colorMentionedPlayer + target.getName() + " " + game.colorSuccess + "sera passé au prochain tour.");
            case NOT_IN_ROTATION -> MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "Ce joueur n'est pas dans la rotation.");
            case NO_PLAYER_IN_TURN -> MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "Aucun joueur n’est en train de jouer.");
        }

        cooldownManager.setCooldown(sender, COMMAND_NAME, COOLDOWN_SECONDS);
    }

}
