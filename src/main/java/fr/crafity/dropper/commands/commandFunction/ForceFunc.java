package fr.crafity.dropper.commands.commandFunction;

import fr.crafity.dropper.Dropper;
import fr.crafity.dropper.game.GameManager;
import fr.crafity.dropper.util.CooldownManager;
import fr.crafity.dropper.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ForceFunc {

    private static final GameManager game = Dropper.getInstance().getGameManager();
    private static final CooldownManager cooldownManager = Dropper.getInstance().getCooldownManager();
    private static final String COMMAND_NAME = "force";
    private static final int COOLDOWN_SECONDS = 3;

    public static void forceCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dropper.admin")) {
            MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "Tu n’as pas la permission d’utiliser cette commande.");
            return;
        }

        if (cooldownManager.isOnCooldown(sender, COMMAND_NAME, COOLDOWN_SECONDS)) {
            long remaining = cooldownManager.getRemainingTime(sender, COMMAND_NAME);
            MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "Tu dois attendre " + game.colorMessage + remaining + "s " + game.colorFailed + "avant de réutiliser cette commande.");
            return;
        }

        if (args.length <= 2) {
            MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "Utilisation : /dropper players force <player>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "Joueur introuvable ou hors-ligne.");
            return;
        }

        GameManager.ForceTurnResult result = game.forceTurn(target);

        switch (result) {
            case SUCCESS -> MessageUtil.broadcast(game.prefix_systeme + game.colorMessage + "Tour forcé pour " + game.colorMentionedPlayer + target.getName() + game.colorMessage + " !");
            case NOT_IN_ROTATION -> MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "Ce joueur n’est pas dans la rotation.");
            case ALREADY_TURN -> MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "Ce joueur est déjà en train de jouer.");
            case NO_GAME_RUNNING -> MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "Le jeu n’est pas en cours.");
        }

        cooldownManager.setCooldown(sender, COMMAND_NAME, COOLDOWN_SECONDS);
    }

}

