package com.maks.trinketsplugin;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import java.util.List;

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
                RunesGUI.openRunesMenu(player);
            } else if (itemName.equals("Gems")) {
                // Future implementation
            } else if (itemName.equals("Jewels")) {
                JewelsGUI.openJewelsMenu(player);
            }

        } else if (title.equals("Accessories")) {
            // Allow players to interact with their own inventory without triggering unequip logic
            if (event.getClickedInventory() == null || event.getClickedInventory().equals(player.getInventory())) {
                return;
            }

            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            // Handle back button
            if (clickedItem.getType() == Material.ARROW) {
                TrinketsGUI.openMainMenu(player);
                return;
            }

            // If it's a glass pane (empty slot), do nothing
            if (clickedItem.getType().toString().contains("GLASS_PANE")) return;

            // Unequip the accessory
            TrinketsPlugin.getInstance().getDatabaseManager().unequipAccessory(player, clickedItem);
            // Refresh the accessories menu instead of closing it
            TrinketsGUI.openAccessoriesMenu(player);
        } else if (title.equals("Jewels Menu")) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            // Handle back button
            if (clickedItem.getType() == Material.ARROW) {
                TrinketsGUI.openMainMenu(player);
                return;
            }

            // Handle Focus Jewels button
            if (clickedItem.getType() == Material.BLUE_DYE && 
                    clickedItem.getItemMeta().getDisplayName().contains("Focus Jewels")) {
                JewelsGUI.openFocusJewelsMenu(player);
                return;
            }

            // If it's a glass pane (empty slot), do nothing
            if (clickedItem.getType().toString().contains("GLASS_PANE")) return;

            // Get the jewel type from the clicked item
            JewelType type = TrinketsPlugin.getInstance().getJewelManager().getJewelType(clickedItem);
            if (type != null) {
                // Unequip the jewel
                TrinketsPlugin.getInstance().getJewelManager().unequipJewel(player, type);
                // Refresh the jewels menu
                JewelsGUI.openJewelsMenu(player);
            }
        } else if (title.equals("Focus Jewels")) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            // Handle back button
            if (clickedItem.getType() == Material.ARROW) {
                JewelsGUI.openJewelsMenu(player);
                return;
            }

            // If it's a glass pane (empty slot) or book (info), do nothing
            if (clickedItem.getType().toString().contains("GLASS_PANE") || 
                clickedItem.getType() == Material.BOOK) return;

            // Determine which Focus Jewel slot was clicked
            int slot = event.getSlot();
            JewelType type = null;

            if (slot == 0) type = JewelType.JEWEL_OF_FOCUS;
            else if (slot == 1) type = JewelType.JEWEL_OF_FOCUS_2;
            else if (slot == 2) type = JewelType.JEWEL_OF_FOCUS_3;

            if (type != null) {
                // Unequip the jewel
                TrinketsPlugin.getInstance().getJewelManager().unequipJewel(player, type);
                // Refresh the focus jewels menu
                JewelsGUI.openFocusJewelsMenu(player);
            }
        } else if (title.equals("Runes")) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            // If it's a locked or empty slot, do nothing
            if (clickedItem.getType().toString().contains("GLASS_PANE") ||
                clickedItem.getType() == Material.BARRIER) return;

            int slot = event.getSlot();
            PlayerData data = TrinketsPlugin.getInstance().getDatabaseManager().getPlayerData(player.getUniqueId());
            List<ItemStack> runes = new java.util.ArrayList<>(data.getRunes());

                if (slot < runes.size()) {
                    ItemStack rune = runes.remove(slot);
                    player.getInventory().addItem(rune);
                    data.removeRune(slot);
                    TrinketsPlugin.getInstance().getDatabaseManager().savePlayerData(player.getUniqueId(), data);
                    TrinketsPlugin.getInstance().getRuneEffectsListener().updateLuck(player);
                    RunesGUI.openRunesMenu(player);
                }

            }
        }
    }
