package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
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
            } else if (itemName.equals("Unique Trinkets")) {
                UniqueTrinketsGUI.openUniqueTrinketsMenu(player);
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

            // Handle back button
            if (clickedItem.getType() == Material.ARROW && event.getSlot() == 17) {
                TrinketsGUI.openMainMenu(player);
                return;
            }

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
        } else if (title.equals("Unique Trinkets")) {
            // Allow players to interact with their own inventory for equipping items
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {
                return;
            }

            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            int slot = event.getSlot();
            
            // Handle back button
            if (slot == 8 && clickedItem != null && clickedItem.getType() == Material.ARROW) {
                TrinketsGUI.openMainMenu(player);
                return;
            }
            
            // Handle unique trinket slots (slots 0 and 1)
            UniqueTrinketType slotType = getSlotTypeFromSlot(slot);
            if (slotType == null) return;
            
            ItemStack cursorItem = event.getCursor();
            
            if (clickedItem != null && !clickedItem.getType().toString().contains("GLASS_PANE")) {
                // Player is trying to unequip an item
                unequipUniqueTrinketItem(player, slotType);
                UniqueTrinketsGUI.openUniqueTrinketsMenu(player);
            }
        } else if (org.bukkit.ChatColor.stripColor(title).equals("Your Stats")) {
            // Prevent taking items from the stats GUI; allow arrow to close
            event.setCancelled(true);
            org.bukkit.inventory.ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() == org.bukkit.Material.ARROW) {
                player.closeInventory();
            }
        }
    }

    private UniqueTrinketType getSlotTypeFromSlot(int slot) {
        for (UniqueTrinketType type : UniqueTrinketType.values()) {
            if (type.getSlot() == slot) {
                return type;
            }
        }
        return null;
    }


    private void equipUniqueTrinketItem(Player player, UniqueTrinketType type, ItemStack item) {
        PlayerData data = TrinketsPlugin.getInstance().getDatabaseManager().getPlayerData(player.getUniqueId());
        
        // Check for existing trinket and return it to player
        ItemStack existing = data.getUniqueTrinket(type);
        if (existing != null) {
            player.getInventory().addItem(existing);
        }
        
        // Equip new trinket
        data.setUniqueTrinket(type, item.clone());
        
        // For team relics, check if player is in party and is leader
        if (type == UniqueTrinketType.TEAM_RELIC) {
            if (!PartyAPIIntegration.isPartyLeader(player)) {
                player.sendMessage("§eTeam Relic equipped, but you must be a party leader for it to work!");
            } else {
                player.sendMessage("§aTeam Relic equipped and active!");
            }
        } else {
            player.sendMessage("§aUnique trinket equipped!");
        }
        
        // Save data
        TrinketsPlugin.getInstance().getDatabaseManager().savePlayerData(player.getUniqueId(), data);
    }

    private void unequipUniqueTrinketItem(Player player, UniqueTrinketType type) {
        PlayerData data = TrinketsPlugin.getInstance().getDatabaseManager().getPlayerData(player.getUniqueId());
        ItemStack item = data.getUniqueTrinket(type);
        
        if (item != null) {
            // Remove attributes first
            data.removeUniqueTrinketAttributes(player, type);
            
            // Remove from data
            data.removeUniqueTrinket(type);
            player.getInventory().addItem(item);
            player.sendMessage("§eUnique trinket unequipped!");
            
            // Save data
            TrinketsPlugin.getInstance().getDatabaseManager().savePlayerData(player.getUniqueId(), data);
        }
    }
}
