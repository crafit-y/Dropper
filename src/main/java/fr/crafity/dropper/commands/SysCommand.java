package fr.crafity.dropper.commands;

import fr.crafity.dropper.Dropper;
import fr.crafity.dropper.game.GameManager;
import fr.crafity.dropper.data.LevelData;
import fr.crafity.dropper.util.LocationUtil;
import fr.crafity.dropper.util.MessageUtil;
import fr.crafity.dropper.util.LevelManager;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SysCommand implements CommandExecutor {

    private final GameManager game;
    private final Dropper plugin;

    public SysCommand(Dropper plugin) {
        this.plugin = plugin;
        this.game = plugin.getGameManager();
    }

    private float getPolarYaw(Location loc) {
        float yaw = loc.getYaw();
        yaw = (yaw % 360 + 360) % 360;

        if (yaw >= 315 || yaw < 45) return 180f;
        if (yaw >= 45 && yaw < 135) return -90f;
        if (yaw >= 135 && yaw < 225) return 0f;
        return 90f;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender,game.prefix_systeme + game.colorFailed + "Commande réservée aux joueurs.");
            return true;
        }

        if (!sender.hasPermission("dropper.admin")) {
            MessageUtil.send(sender, game.prefix_systeme + game.colorFailed + "Tu n’as pas la permission.");
            return true;
        }

        if (args.length < 3) {
            MessageUtil.send(sender,game.prefix_systeme + game.colorFailed + "Utilisation: /sys <levelName> <type> <create/set/delete>");
            return true;
        }

        String levelName = args[0];
        String type = args[1].toLowerCase();
        String action = args[2].toLowerCase();

        switch (type) {
            case "manage" -> {
                if (action.equals("create")) {
                    if (game.loadLevel(levelName) != null) {
                        MessageUtil.send(sender,game.prefix_systeme + game.colorFailed + "Ce niveau existe déjà.");
                        return true;
                    }

                    Location raw = player.getLocation();
                    Location jump = new Location(
                            raw.getWorld(),
                            Math.floor(raw.getX()) + 0.5,
                            Math.floor(raw.getY()),
                            Math.floor(raw.getZ()) + 0.5,
                            raw.getYaw(),
                            raw.getPitch()
                    );

                    int lives = plugin.getConfig().getInt("default_lives", 3);
                    int time = plugin.getConfig().getInt("default_jump_time", 10);

                    try {
                        if (args.length >= 4) lives = Integer.parseInt(args[3]);
                        if (args.length >= 5) time = Integer.parseInt(args[4]);
                        if (lives <= 0 || time <= 0) throw new NumberFormatException();
                    } catch (NumberFormatException e) {
                        suggestFix(player, levelName, lives, time);
                        return true;
                    }

                    jump.setYaw(getPolarYaw(jump));

                    LevelData data = new LevelData();
                    data.setJumpPoint(jump);
                    data.setLives(lives);
                    data.setJumpTime(time);
                    data.setCreator(player.getName());
                    data.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                    data.setWorld(player.getWorld().getName());

                    LevelManager.saveLevel(levelName, data);
                    MessageUtil.send(sender,game.prefix_systeme + game.colorSuccess + "Niveau " + game.colorMentionedPlayer + levelName + game.colorSuccess + " créé.");
                    return true;
                }

                if (action.equals("delete")) {
                    if (game.loadLevel(levelName) == null) {
                        MessageUtil.send(sender,game.prefix_systeme + game.colorFailed + "Ce niveau n'existe pas.");
                        return true;
                    }

                    LevelManager.deleteLevel(levelName);
                    MessageUtil.send(sender,game.prefix_systeme + game.colorSuccess + "Niveau " + game.colorMentionedPlayer + levelName + game.colorSuccess + " supprimé.");
                    return true;
                }
            }

            case "jump" -> {
                if (!action.equals("set")) return false;
                LevelData level = game.loadLevel(levelName);
                if (level == null) {
                    MessageUtil.send(sender,game.prefix_systeme + game.colorFailed + "Ce niveau n'existe pas.");
                    return true;
                }

                Location raw = player.getLocation();
                Location jump = new Location(
                        raw.getWorld(),
                        Math.floor(raw.getX()) + 0.5,
                        Math.floor(raw.getY()),
                        Math.floor(raw.getZ()) + 0.5,
                        raw.getYaw(),
                        raw.getPitch()
                );

                if (args.length >= 4 && args[3].equalsIgnoreCase("-py")) {
                    jump.setYaw(getPolarYaw(jump));
                }

                level.setJumpPoint(jump);
                LevelManager.saveLevel(levelName, level);
                MessageUtil.send(sender,game.prefix_systeme + game.colorSuccess + "Point de saut mis à jour pour " + game.colorMentionedPlayer + levelName);
                return true;
            }

            case "limit1", "limit2" -> {
                if (!action.equals("set")) return false;
                LevelData level = game.loadLevel(levelName);
                if (level == null) {
                    MessageUtil.send(sender,game.prefix_systeme + game.colorFailed + "Ce niveau n'existe pas.");
                    return true;
                }

                Location limit = LocationUtil.getTargetBlockLocation(player);
                if (type.equals("limit1")) {
                    level.setLimit1(limit);
                    MessageUtil.send(sender,game.prefix_systeme + game.colorSuccess + "Point limite 1 défini.");
                } else {
                    level.setLimit2(limit);
                    MessageUtil.send(sender,game.prefix_systeme + game.colorSuccess + "Point limite 2 défini.");
                }

                LevelManager.saveLevel(levelName, level);
                return true;
            }

            case "lives", "time" -> {
                if (args.length < 4) {
                    MessageUtil.send(sender,game.prefix_systeme + game.colorFailed + "Utilisation : /sys <levelName> <lives|time> set <value>");
                    return true;
                }

                LevelData level = game.loadLevel(levelName);
                if (level == null) {
                    MessageUtil.send(sender,game.prefix_systeme + game.colorFailed + "Ce niveau n'existe pas.");
                    return true;
                }

                int value;
                try {
                    value = Integer.parseInt(args[3]);
                    if (value <= 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    suggestFix(player, levelName,
                            plugin.getConfig().getInt("default_lives", 3),
                            plugin.getConfig().getInt("default_jump_time", 10));
                    return true;
                }

                if (type.equals("lives")) {
                    level.setLives(value);
                    MessageUtil.send(sender,game.prefix_systeme + game.colorSuccess + "Vies mises à jour : " + game.colorFailed + value + " ♥");
                } else {
                    level.setJumpTime(value);
                    MessageUtil.send(sender,game.prefix_systeme + game.colorSuccess + "Temps de saut mis à jour : " + game.colorMessage + value + " secondes.");
                }

                LevelManager.saveLevel(levelName, level);
                return true;
            }

            default -> MessageUtil.send(sender,game.prefix_systeme + game.colorSuccess + "Type inconnu. Utilise : manage, jump, lives, time, limit1, limit2.");
        }
        return true;
    }

    private void suggestFix(Player player, String levelName, int defaultLives, int defaultTime) {
        String suggestedCommand = "/sys " + levelName + " manage create " + defaultLives + " " + defaultTime;
        TextComponent suggestion = new TextComponent("§e[✔ Clique ici pour corriger]");
        suggestion.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggestedCommand));
        suggestion.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("§7Clique pour insérer la commande corrigée.").create()));

        player.spigot().sendMessage(suggestion);
    }
}
