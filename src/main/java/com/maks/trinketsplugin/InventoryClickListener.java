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
            // Allow players to interact with their own inventory, but block transfers into the GUI
            if (event.getClickedInventory() == null) {
                return;
            }
            if (event.getClickedInventory().equals(player.getInventory())) {
                // Cancel shift-click and other transfer actions that would move items into the GUI
                org.bukkit.event.inventory.InventoryAction action = event.getAction();
                org.bukkit.event.inventory.ClickType click = event.getClick();
                if (event.isShiftClick()
                        || action == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY
                        || action == org.bukkit.event.inventory.InventoryAction.COLLECT_TO_CURSOR
                        || action == org.bukkit.event.inventory.InventoryAction.HOTBAR_SWAP
                        || action == org.bukkit.event.inventory.InventoryAction.HOTBAR_MOVE_AND_READD
                        || click == org.bukkit.event.inventory.ClickType.NUMBER_KEY) {
                    event.setCancelled(true);
                }
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
            // Block all item movement inside this GUI to prevent stacking/dupes
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            int slot = event.getSlot();

            // If clicking in the player's own inventory while this GUI is open, do nothing
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {
                return;
            }

            // Back button
            if (slot == 8 && clickedItem != null && clickedItem.getType() == Material.ARROW) {
                TrinketsGUI.openMainMenu(player);
                return;
            }

            // Unequip from defined unique trinket slots
            UniqueTrinketType slotType = getSlotTypeFromSlot(slot);
            if (slotType == null) return;

            if (clickedItem != null && !clickedItem.getType().toString().contains("GLASS_PANE")) {
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
        if (item == null) return;

        // Enforce unstacked unique trinkets when equipping via GUI logic
        if (item.getAmount() > 1) {
            player.sendMessage("§cUnique trinkets must be unstacked (amount = 1) before equipping.");
            return;
        }
        
        // Check for existing trinket and return it to player
        ItemStack existing = data.getUniqueTrinket(type);
        if (existing != null) {
            data.removeUniqueTrinketAttributes(player, type);
            java.util.Map<Integer, ItemStack> leftovers = player.getInventory().addItem(existing);
            if (!leftovers.isEmpty()) {
                leftovers.values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
            }
        }
        
        // Equip new trinket
        data.setUniqueTrinket(type, item.clone());

        // Apply attributes for the new unique trinket
        data.applyUniqueTrinketAttribute(player, item.clone(), type);
        
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

    @org.bukkit.event.EventHandler
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title.equals("Unique Trinkets")
                || title.equals("Accessories")
                || title.equals("Jewels Menu")
                || title.equals("Focus Jewels")
                || title.equals("Runes")
                || title.equals("Trinkets Menu")
                || org.bukkit.ChatColor.stripColor(title).equals("Your Stats")) {
            // Prevent dragging items into/out of any of the plugin GUIs
            event.setCancelled(true);
        }
    }
}
