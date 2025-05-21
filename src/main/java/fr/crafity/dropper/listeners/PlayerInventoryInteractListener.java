package fr.crafity.dropper.listeners;

import fr.crafity.dropper.Dropper;
import fr.crafity.dropper.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;


public class PlayerInventoryInteractListener implements Listener {

    private final Dropper plugin;
    private final GameManager game;

    public PlayerInventoryInteractListener(Dropper plugin) {
        this.plugin = plugin;
        this.game = plugin.getGameManager();
    }

    @EventHandler
    public void onHelmetRemove(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();

        // Si le jeu ne tourne pas ou que le joueur ne participe pas, on ne bloque pas
        if (!game.isRunning() || !game.getRotation().contains(uuid)) return;

        // Slot 39 = casque dans l'inventaire du joueur
        if (event.getSlot() == 39 || event.getRawSlot() == 39) {
            ItemStack item = event.getCurrentItem();

            if (item == null || item.getType().isAir()) return;

            // Si le nom est défini et correspond au bloc "casque" utilisé pour l'identification
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                    item.getItemMeta().getDisplayName().equals(game.blockStr)) {

                event.setCancelled(true);
                player.sendMessage("§cTu ne peux pas enlever ce bloc pendant le jeu !");
            }
        }
    }

    @EventHandler
    public void onHelmetDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();

        if (!game.isRunning() || !game.getRotation().contains(uuid)) return;

        // Slot 39 = casque dans l'inventaire
        if (event.getRawSlots().contains(39)) {
            ItemStack item = event.getOldCursor();
            if (item == null || item.getType().isAir()) return;

            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                    item.getItemMeta().getDisplayName().equals(game.blockStr)) {

                event.setCancelled(true);
                player.sendMessage("§cTu ne peux pas enlever ce bloc pendant le jeu !");
            }
        }
    }

    @EventHandler
    public void onHelmetSwap(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!game.isRunning() || !game.getRotation().contains(uuid)) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) return;

        // Si l'objet est un casque et qu'il a le displayName bloqué
        if (item.getType().name().endsWith("_HELMET") || item.getType().isBlock()) {
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                    item.getItemMeta().getDisplayName().equals(game.blockStr)) {

                // Si le joueur n’a pas déjà cet item sur la tête
                ItemStack helmet = player.getInventory().getHelmet();
                if (helmet == null || !helmet.isSimilar(item)) {
                    event.setCancelled(true);
                    player.sendMessage("§cTu ne peux pas changer ce bloc pendant le jeu !");
                }
            }
        }
    }

    @EventHandler
    public void onHelmetDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!game.isRunning() || !game.getRotation().contains(uuid)) return;

        ItemStack dropped = event.getItemDrop().getItemStack();

        if (dropped != null && dropped.hasItemMeta() && dropped.getItemMeta().hasDisplayName() &&
                dropped.getItemMeta().getDisplayName().equals(game.blockStr)) {

            event.setCancelled(true);
            player.sendMessage("§cTu ne peux pas jeter ce bloc pendant le jeu !");
        }
    }

    @EventHandler
    public void onHelmetKeepOnDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();

        if (!game.isRunning() || !game.getRotation().contains(uuid)) return;

        ItemStack helmet = player.getInventory().getHelmet();

        if (helmet != null && helmet.hasItemMeta() && helmet.getItemMeta().hasDisplayName() &&
                helmet.getItemMeta().getDisplayName().equals(game.blockStr)) {

            // Supprime le casque de la liste des drops
            event.getDrops().removeIf(item ->
                    item != null &&
                            item.hasItemMeta() &&
                            item.getItemMeta().hasDisplayName() &&
                            item.getItemMeta().getDisplayName().equals(game.blockStr)
            );

            // Réattribue le casque après la mort si nécessaire
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.getInventory().setHelmet(helmet);
            }, 1L);
        }
    }


}
