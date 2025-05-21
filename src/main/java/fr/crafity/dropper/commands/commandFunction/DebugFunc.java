package fr.crafity.dropper.commands.commandFunction;

import fr.crafity.dropper.Dropper;
import fr.crafity.dropper.game.GameManager;
import fr.crafity.dropper.data.LevelData;
import fr.crafity.dropper.data.PlayerData;
import fr.crafity.dropper.util.MessageUtil;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DebugFunc {

    private static final GameManager game = Dropper.getInstance().getGameManager();

    // 🔧 DEBUG GLOBAL
    public static void sendDebugInfo(Player player) {
        player.sendMessage("§8§m-------[ ⚙ DEBUG GLOBAL ]-------");
        player.sendMessage("§eÉtat: " + (game.isRunning() ? (game.isPaused() ? "§6Pause" : "§aEn cours") : "§cArrêté"));
        player.sendMessage("§eNiveau actuel: §b" + (game.getCurrentLevel() != null ? game.getCurrentLevel() : "Aucun"));
        player.sendMessage("§eJoueur actuel: §b" + (game.getCurrentPlayerInTurn() != null ? game.getCurrentPlayerInTurn().getName() : "Aucun"));
        player.sendMessage("§eRotation: §7" + game.getRotation().size() + " joueur" + game.sss(game.getRotation().size()));
        player.sendMessage("§eDéconnectés: §7" + game.disconnectedPlayers.size());
        player.sendMessage("§eBossBar actif: " + (game.bossBar != null ? "§aOui" : "§cNon"));
        player.sendMessage("§eTour forcé: " + (game.getCurrentPlayerInTurn() != null ? game.getCurrentPlayerInTurn().getName() : "§7Aucun"));
        player.sendMessage("§8§m-------------------------------");
    }

    // 🔧 DEBUG JOUEUR CIBLÉ
    public static void sendPlayerDebug(Player sender, String targetStr) {
        if (targetStr == null) {
            sender.sendMessage("§cCe joueur est introuvable ou hors-ligne.");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetStr);
        UUID uuid = target.getUniqueId();
        PlayerData data = game.getPlayerData(uuid);
        if (data == null) {
            sender.sendMessage("§cCe joueur ne fait pas partie de la rotation.");
            return;
        }

        TextComponent comp = new TextComponent("§7- §eUUID: §f" + uuid);
        comp.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, uuid.toString()));
        comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("§eClique pour copier le uuid").create()));

        sender.sendMessage("§8§m------[ ⚙ DEBUG " + target.getName() + " ]------");
        sender.spigot().sendMessage(comp);
        sender.sendMessage("§7- §eÉliminé: " + (data.isEliminated() ? "§a✔ Oui" : "§7✘ Non"));
        sender.sendMessage("§7- §eTour actuel: " + (game.isPlayerInTurn(target) ? "§a✔ Oui" : "§7✘ Non"));
        sender.sendMessage("§7- §eDéconnecté: " + (game.disconnectedPlayers.contains(uuid) ? "§a✔ Oui" : "§7✘ Non"));
        sender.sendMessage("§7- §eSuper Jump: " + (data.hasPendingSuperJump() ? "§a✔ Oui" : "§7✘ Non"));
        sender.sendMessage("§7- §eVies restantes: §c" + data.getLives());
        sender.sendMessage("§7- §eJumps ce tour: §e" + data.getJumpsThisTurn());
        sender.sendMessage("§7- §ePerfect Drops ce tour: §6" + data.getPerfectDropsThisTurn());
        sender.sendMessage("§7- §eTours manqués: §7" + data.getMissedTurns());
        sender.sendMessage("§8§m-------------------------------");
    }

    // 🔧 DEBUG TOUS LES JOUEURS
    public static void sendPlayerDebug(Player sender) {
        List<UUID> rotation = game.getRotation(); // ✅ compatible avec ArrayList
        if (rotation.isEmpty()) {
            sender.sendMessage("§cAucun joueur dans la rotation.");
            return;
        }

        sender.sendMessage("§8§m------[ ⚙ DEBUG PLAYERS ]------");
        for (UUID uuid : rotation) {
            PlayerData data = game.getPlayerData(uuid);
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : "§oInconnu";

            String status = data != null
                    ? (data.isEliminated() ? "§cÉliminé" : "§aActif")
                    : "§7Inconnu";

            boolean isCurrent = game.isPlayerInTurn(offlinePlayer.getPlayer());

            sender.sendMessage("§7- §f" + name + " §8(" + uuid.toString().substring(0, 6) + "…): " + status + (isCurrent ? " §6(Actuellement)" : ""));
        }
        sender.sendMessage("§8§m-------------------------------");
    }

    // 🔧 DEBUG NIVEAU CIBLÉ
    public static void sendLevelDebug(Player sender, String levelName) {
        LevelData data = game.loadLevel(levelName);
        if (data == null) {
            sender.sendMessage("§cLe niveau §e" + levelName + " §cn'existe pas ou est corrompu.");
            return;
        }

        sender.sendMessage("§8§m------[ ⚙ DEBUG " + levelName + " ]------");
        sender.sendMessage("§7- §ePoint de saut: " + (data.getJumpPoint() != null ? "§a✔" : "§c✘"));
        sender.sendMessage("§7- §eLimite 1: " + (data.getLimit1() != null ? "§a✔" : "§c✘"));
        sender.sendMessage("§7- §eLimite 2: " + (data.getLimit2() != null ? "§a✔" : "§c✘"));
        sender.sendMessage("§7- §eVies: §c" + data.getLives());
        sender.sendMessage("§7- §eTemps max de saut: §e" + data.getJumpTime() + "s");

        boolean valid = data.getJumpPoint() != null &&
                data.getLimit1() != null &&
                data.getLimit2() != null &&
                data.getLives() > 0 &&
                data.getJumpTime() > 0;

        sender.sendMessage("§7- §eValide: " + (valid ? "§aOui" : "§cNon"));
        sender.sendMessage("§8§m-------------------------------");
    }

    // 🔧 DEBUG TOUS LES NIVEAUX
    public static void sendLevelDebug(Player sender) {
        Set<String> levels = game.getAvailableLevels();

        if (levels.isEmpty()) {
            sender.sendMessage("§cAucun niveau trouvé.");
            return;
        }

        sender.sendMessage("§8§m------[ ⚙ DEBUG LEVELS ]------");
        for (String level : levels) {
            LevelData data = game.loadLevel(level);
            boolean valid = data != null &&
                    data.getJumpPoint() != null &&
                    data.getLimit1() != null &&
                    data.getLimit2() != null &&
                    data.getLives() > 0 &&
                    data.getJumpTime() > 0;

            sender.sendMessage("§7- §b" + level + ": " + (valid ? "§a✔ OK" : "§c✘ Invalide"));
        }
        sender.sendMessage("§8§m-------------------------------");
    }

    public static void modifyPlayerValue(Player sender, String targetStr, String key, String value) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetStr);

        if (false) {
            MessageUtil.send(sender, game.prefix_systeme + "§cJoueur introuvable.");
            return;
        }

        PlayerData data = GameManager.getPlayers().get(target.getUniqueId());
        if (data == null) {
            MessageUtil.send(sender, game.prefix_systeme + "§cPas de données pour ce joueur.");
            return;
        }

        try {
            switch (key.toLowerCase()) {
                case "lives" -> data.setLives(Integer.parseInt(value));
                case "jumps" -> data.setJumpsThisTurn(Integer.parseInt(value));
                case "perfect" -> data.setPerfectDropsThisTurn(Integer.parseInt(value));
                case "missed" -> data.setMissedTurns(Integer.parseInt(value));
                case "superjump" -> data.setPendingSuperJump(Boolean.parseBoolean(value));
                case "eliminated" -> data.setEliminated(Boolean.parseBoolean(value));
                default -> MessageUtil.send(sender, game.prefix_systeme + "§cClé inconnue: " + key);
            }
        } catch (Exception e) {
            MessageUtil.send(sender, game.prefix_systeme + "§cErreur de conversion: " + e.getMessage());
            return;
        }

        MessageUtil.send(sender, game.prefix_systeme + "§aValeur mise à jour: " + key + " -> " + value);
    }

    @Deprecated
    public static void modifyGameValue(Player sender, String key, String value) {
//        try {
//            switch (key.toLowerCase()) {
//                case "currentlevel" -> game.getClass().getDeclaredField("currentLevel").set(game, value);
//                case "isrunning" -> game.getClass().getDeclaredField("isRunning").setBoolean(game, Boolean.parseBoolean(value));
//                case "ispaused" -> game.getClass().getDeclaredField("isPaused").setBoolean(game, Boolean.parseBoolean(value));
//                default -> {
//                    sender.sendMessage("§cClé inconnue: " + key);
//                    return;
//                }
//            }
//        } catch (Exception e) {
//            sender.sendMessage("§cErreur lors de la modification: " + e.getMessage());
//            return;
//        }
        MessageUtil.send(sender, game.prefix_systeme + "§4@DEPRECATED");
    }
}
