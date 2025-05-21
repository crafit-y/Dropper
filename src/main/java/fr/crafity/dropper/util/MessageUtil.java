package fr.crafity.dropper.util;

import fr.crafity.dropper.Dropper;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MessageUtil {

    private static final Dropper plugin = Dropper.getInstance();

    public static String getPrefix() {
        return plugin.getConfig().getString("prefix", "§6[§eDropper§6]§r ");
    }

    // Version pour Player directement (facilite l'appel si tu as déjà un Player)
    public static void send(Player player, String message) {
        player.sendMessage(getPrefix() + message);
    }

    // Version générique pour tout CommandSender (Player ou Console)
    public static void send(CommandSender sender, String message) {
        String fullMessage = getPrefix() + message;

        if (sender instanceof Player player) {
            player.sendMessage(fullMessage);
        } else {
            sender.sendMessage(fullMessage.replaceAll("§.", ""));
        }
    }


    public static void broadcast(String message) {
        plugin.getServer().broadcastMessage(getPrefix() + message);
    }

    public static void actionBar(Player player, String message) {
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent(getPrefix() + message));
    }
}
