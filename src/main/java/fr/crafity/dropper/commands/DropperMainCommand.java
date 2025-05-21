package fr.crafity.dropper.commands;

import fr.crafity.dropper.Dropper;
import fr.crafity.dropper.commands.commandFunction.*;
import fr.crafity.dropper.game.GameManager;
import fr.crafity.dropper.util.MessageUtil;
import net.md_5.bungee.api.chat.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DropperMainCommand implements CommandExecutor {

    private final Dropper plugin;
    private final GameManager game;

    public DropperMainCommand(Dropper plugin) {
        this.plugin = plugin;
        this.game = plugin.getGameManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("dropper.admin")) {
            MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "Tu n’as pas la permission.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("§8§m---------------------------------------");
            sender.sendMessage("§6§lDropper - Commandes Admin :");
            sender.sendMessage(" §e/skip, /force <joueur>");
            sender.sendMessage(" §e/sys <level> <type> <action>");
            sender.sendMessage(" §e/dropper start §7- Démarrer le jeu");
            sender.sendMessage(" §e/dropper stop §7- Arrêtez le jeu");
            sender.sendMessage(" §e/dropper pause §7- Pause ou resume le jeu en cours");
            sender.sendMessage(" §e/dropper players add <@a/joueur> §7- Ajoute 1 ou plusieurs joueur(s) au jeu");
            sender.sendMessage(" §e/dropper players remove <@a/joueur> §7- Supprime 1 joueur du jeu §o(Note: '@a' ne supprime pas tous les joueurs connecté, mais tous les joueurs dans la variables)");
            sender.sendMessage(" §e/dropper players force <joueur> §7- Force le tour d’un joueur");
            sender.sendMessage(" §e/dropper players skip (joueur) §7- Skip le joueur en cours ou celui donné");
            sender.sendMessage(" §e/dropper reload <config|levels|plugin> §7- Recharge les fichiers");
            sender.sendMessage(" §e/dropper list §7- Liste les niveaux (cliquables)");
            sender.sendMessage(" §e/dropper debug modify players <joueur> <clé> <valeur>");
            sender.sendMessage("§5§o<> = obligatoire | () = optionnel");
            sender.sendMessage("§8§m---------------------------------------");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (args.length < 2) {
                MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "Usage: /dropper reload <config|levels|plugin>");
                return true;
            }
            if (game.isRunning() || game.isPaused()) {
                MessageUtil.send(sender, game.prefix_systeme + game.colorError + "La config du plugin ne peut pas être rechargée si un jeu est en cours ou en pause !");
                return true;
            }
            switch (args[1].toLowerCase()) {
                case "config" -> {
                    plugin.reloadConfig();
                    MessageUtil.send(sender, game.prefix_systeme + "§aConfiguration rechargée avec succès !");
                }
                case "levels" -> {
                    game.reloadLevels();
                    MessageUtil.send(sender, game.prefix_systeme + "§e@DEPRECATED §aNiveaux rechargés avec succès !");
                }
                case "plugin" -> {
                    plugin.reloadConfig();
                    game.reloadLevels();
                    MessageUtil.send(sender, game.prefix_systeme + "§e@DEPRECATED §aConfiguration & niveaux rechargés !");
                }
                default -> MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "Option invalide. Utilise config, levels ou plugin.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            MessageUtil.send(sender, game.prefix_systeme + game.colorBossBar + "§lListe des niveaux disponibles :");

            var levels = game.getAvailableLevels();

            if (levels.isEmpty()) {
                MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "Aucun niveau n’a encore été créé.");
            } else {
                for (String level : levels) {
                    TextComponent comp = new TextComponent(" §f• §a" + level);
                    comp.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/dropper start " + level));
                    comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder("§eClique pour lancer ce niveau").create()));
                    if (sender instanceof Player p) {
                        p.spigot().sendMessage(comp);
                    } else {
                        sender.sendMessage(" - " + level);
                    }
                }
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> StartFunc.startCommand(sender, args);
            case "stop" -> StopFunc.stopCommand(sender, args);
            case "pause" -> PauseFunc.pauseCommand(sender, args);
            case "players" -> {
                if (args.length < 2) {
                    MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "Usage: /dropper players <add|remove|skip|force> <joueur>");
                    return true;
                }
                switch (args[1].toLowerCase()) {
                    case "add" -> PlayerAddFunc.addPlayer(sender, args);
                    case "remove" -> PlayerRemoveFunc.removePlayer(sender, args);
                    case "skip" -> SkipFunc.skipCommand(sender, args);
                    case "force" -> ForceFunc.forceCommand(sender, args);
                    default -> MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "Sous-commande invalide. Utilise /dropper help");
                }
            }
            case "debug" -> {
                if (!(sender instanceof Player player)) {
                    MessageUtil.send(sender, "§cCommande réservée aux joueurs.");
                    return true;
                }
                if (args.length == 1) {
                    DebugFunc.sendDebugInfo(player);
                    return true;
                }
                switch (args[1].toLowerCase()) {
                    case "players" -> {
                        if (args.length >= 3) DebugFunc.sendPlayerDebug(player, args[2]);
                        else DebugFunc.sendPlayerDebug(player);
                    }
                    case "levels" -> {
                        if (args.length >= 3) DebugFunc.sendLevelDebug(player, args[2]);
                        else DebugFunc.sendLevelDebug(player);
                    }
                    case "modify" -> {
                        if (args.length < 6) {
                            MessageUtil.send(player, game.prefix_systeme + game.colorFailed + "Usage: /dropper debug modify players <joueur> <clé> <valeur>");
                            return true;
                        }
                        if (args[2].equalsIgnoreCase("players")) {
                            DebugFunc.modifyPlayerValue(player, args[3], args[4], args[5]);
                        } else if (args[2].equalsIgnoreCase("game")) {
                            DebugFunc.modifyGameValue(player, args[3], args[4]);
                        } else {
                            MessageUtil.send(player, game.prefix_systeme + game.colorFailed + "Type invalide. Utilise 'players' ou 'game'.");
                        }
                    }
                    default -> MessageUtil.send(sender, "§cSous-commande invalide. Utilise: §e/dropper debug §7|§e players [joueur] §7|§e levels [level] §7|§e modify players <joueur> <clé> <valeur>");
                }
            }
            default -> MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "Sous-commande invalide. Utilise " + game.colorMessage + "/dropper help");
        }
        return true;
    }
}
