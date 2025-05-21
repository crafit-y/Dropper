package fr.crafity.dropper.util;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final Map<String, Map<String, Long>> cooldowns = new HashMap<>();

    /**
     * Vérifie si le sender est en cooldown pour une commande spécifique.
     * @param sender Le CommandSender (Player ou Console)
     * @param commandName Nom unique de la commande (ex: "force", "skip")
     * @param cooldownInSeconds Durée du cooldown en secondes
     * @return true si en cooldown, false sinon
     */
    public boolean isOnCooldown(CommandSender sender, String commandName, int cooldownInSeconds) {
        String id = getId(sender);
        cooldowns.putIfAbsent(id, new HashMap<>());
        Map<String, Long> senderCooldowns = cooldowns.get(id);

        long currentTime = System.currentTimeMillis();
        long expireTime = senderCooldowns.getOrDefault(commandName, 0L);

        if (currentTime < expireTime) return true;

        // Cooldown expiré, on le retire
        senderCooldowns.remove(commandName);
        return false;
    }

    /**
     * Définit le cooldown pour un sender et une commande spécifique.
     * @param sender Le CommandSender (Player ou Console)
     * @param commandName Nom de la commande
     * @param cooldownInSeconds Durée en secondes
     */
    public void setCooldown(CommandSender sender, String commandName, int cooldownInSeconds) {
        String id = getId(sender);
        cooldowns.putIfAbsent(id, new HashMap<>());
        cooldowns.get(id).put(commandName, System.currentTimeMillis() + cooldownInSeconds * 1000L);
    }

    /**
     * Récupère le temps restant du cooldown.
     * @param sender Le CommandSender
     * @param commandName Nom de la commande
     * @return Temps restant en secondes
     */
    public long getRemainingTime(CommandSender sender, String commandName) {
        String id = getId(sender);
        Map<String, Long> senderCooldowns = cooldowns.getOrDefault(id, new HashMap<>());
        return Math.max(0, (senderCooldowns.getOrDefault(commandName, 0L) - System.currentTimeMillis()) / 1000);
    }

    private String getId(CommandSender sender) {
        return (sender instanceof Player) ? ((Player) sender).getUniqueId().toString() : "CONSOLE";
    }
}
