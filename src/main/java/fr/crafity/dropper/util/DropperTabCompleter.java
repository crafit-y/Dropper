package fr.crafity.dropper.util;

import fr.crafity.dropper.Dropper;
import fr.crafity.dropper.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.io.File;
import java.util.*;

public class DropperTabCompleter implements TabCompleter {

    private final GameManager game;
    private final Dropper plugin;

    public DropperTabCompleter(Dropper plugin) {
        this.plugin = plugin;
        this.game = plugin.getGameManager();
    }

    public static List<String> getAvailableLevels(Dropper plugin) {
        List<String> levels = new ArrayList<>();
        File levelsDir = new File(plugin.getDataFolder(), "levels");
        if (levelsDir.exists() && levelsDir.isDirectory()) {
            for (File file : Objects.requireNonNull(levelsDir.listFiles())) {
                if (file.isFile() && file.getName().endsWith(".yml")) {
                    levels.add(file.getName().replace(".yml", ""));
                }
            }
        }
        return levels;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = command.getName().toLowerCase();

        switch (cmd) {
            case "dropper" -> {
                if (args.length == 1) {
                    return match(args[0], Arrays.asList("help", "reload", "list", "players", "start", "stop", "pause", "debug"));
                }

                if (args.length == 2) {
                    return switch (args[0].toLowerCase()) {
                        case "players" -> match(args[1], Arrays.asList("add", "remove", "force", "skip"));
                        case "start" -> match(args[1], getAvailableLevels(plugin));
                        case "reload" -> match(args[1], Arrays.asList("config", "levels", "plugin"));
                        case "debug" -> match(args[1], Arrays.asList("players", "levels", "game", "modify"));
                        default -> List.of();
                    };
                }

                if (args.length == 3) {
                    if (args[0].equalsIgnoreCase("players")) {
                        List<String> names = new ArrayList<>();
                        if (args[1].equalsIgnoreCase("add")) {
                            names.add("@a");
                            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
                        } else if (args[1].equalsIgnoreCase("remove")) {
                            names.add("All");
                            for (UUID uuid : game.getPlayers().keySet()) {
                                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                                if (op.getName() != null) names.add(op.getName());
                            }
                        } else {
                            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
                        }
                        return match(args[2], names);
                    }

                    if (args[0].equalsIgnoreCase("debug")) {
                        if (args[1].equalsIgnoreCase("players")) {
                            List<String> names = new ArrayList<>();
                            for (UUID uuid : game.getPlayers().keySet()) {
                                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                                if (op.getName() != null) names.add(op.getName());
                            }
                            return match(args[2], names);
                        } else if (args[1].equalsIgnoreCase("levels")) {
                            return match(args[2], getAvailableLevels(plugin));
                        }
                    }

                    if (args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("modify")) {
                        return match(args[2], Arrays.asList("players", "game"));
                    }
                }

                if (args.length == 4 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("modify")) {
                    if (args[2].equalsIgnoreCase("players")) {
                        List<String> names = new ArrayList<>();
                        for (UUID uuid : game.getPlayers().keySet()) {
                            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                            if (op.getName() != null) names.add(op.getName());
                        }
                        return match(args[3], names);
                    } else if (args[2].equalsIgnoreCase("game")) {
                        return match(args[3], Arrays.asList("@DEPRECATED", "@DEPRECATED", "@DEPRECATED"));
                    }
                }

                if (args.length == 5 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("modify")) {
                    if (args[2].equalsIgnoreCase("players")) {
                        return match(args[4], Arrays.asList("lives", "jumps", "perfect", "superjump", "eliminated", "missed"));
                    }
                }

                if (args.length == 6 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("modify")) {
                    if (args[2].equalsIgnoreCase("players")) {
                        return switch (args[4].toLowerCase()) {
                            case "lives", "jumps", "perfect", "missed" -> match(args[5], List.of("0", "1", "2", "3", "5", "10"));
                            case "eliminated", "superjump" -> match(args[5], List.of("true", "false"));
                            default -> List.of();
                        };
                    }
                }
            }

            case "sys" -> {
                if (args.length == 1) return match(args[0], getAvailableLevels(plugin));
                if (args.length == 2)
                    return match(args[1], Arrays.asList("manage", "jump", "lives", "time", "limit1", "limit2"));
                if (args.length == 3) {
                    return match(args[2], args[1].equalsIgnoreCase("manage") ?
                            Arrays.asList("create", "delete") : Arrays.asList("set"));
                }
                if (args.length == 4 && args[1].equalsIgnoreCase("jump") && args[2].equalsIgnoreCase("set")) {
                    return match(args[3], List.of("-py"));
                }
            }
        }

        return List.of();
    }

    private List<String> match(String input, List<String> options) {
        String lower = input.toLowerCase();
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lower)) matches.add(option);
        }
        return matches;
    }
}
