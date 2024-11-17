package com.maks.trinketsplugin;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public class InventoryClickListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        Inventory inv = event.getInventory();
        Player player = (Player) event.getWhoClicked();
        InventoryView inventoryView = event.getView(); // Get the inventory view
        String title = inventoryView.getTitle(); // Get the inventory title
        if (title.equals("Trinkets Menu")) {
            event.setCancelled(true); // Prevent item movement

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null) return;

            String itemName = clickedItem.getItemMeta().getDisplayName();

            if (itemName.equals("Accessories")) {
                TrinketsGUI.openAccessoriesMenu(player);
            } else if (itemName.equals("Runes")) {
                // Future implementation
            } else if (itemName.equals("Gems")) {
                // Future implementation
            }

        } else if (title.equals("Accessories")) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR || clickedItem.getType().toString().contains("GLASS_PANE")) return;

            // Unequip the accessory
            TrinketsPlugin.getInstance().getDatabaseManager().unequipAccessory(player, clickedItem);
            player.closeInventory();
        }
    }
}
