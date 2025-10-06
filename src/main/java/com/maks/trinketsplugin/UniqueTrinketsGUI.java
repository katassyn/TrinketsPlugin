package com.maks.trinketsplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class UniqueTrinketsGUI {

    public static void openUniqueTrinketsMenu(Player player) {
        TrinketsPlugin.getInstance().getDatabaseManager().loadPlayerData(player.getUniqueId(), data -> {
            Inventory gui = Bukkit.createInventory(null, 9, "Unique Trinkets");

            // Display equipped unique trinkets
            for (UniqueTrinketType type : UniqueTrinketType.values()) {
                ItemStack item = data.getUniqueTrinket(type);
                if (item != null) {
                    ItemStack displayItem = item.clone();
                    displayItem.setAmount(1);
                    gui.setItem(type.getSlot(), displayItem);
                } else {
                    gui.setItem(type.getSlot(), createEmptySlot(type));
                }
            }

            // Add back button
            gui.setItem(8, createBackButton());

            // Open the inventory on the main thread
            Bukkit.getScheduler().runTask(TrinketsPlugin.getInstance(), () -> {
                player.openInventory(gui);
            });
        });
    }

    private static ItemStack createEmptySlot(UniqueTrinketType type) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + type.getDisplayName() + " Slot");
        meta.setLore(Arrays.asList(ChatColor.YELLOW + "Right-click a " + type.getDisplayName().toLowerCase(), ChatColor.YELLOW + "to equip it."));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Back");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Return to the main menu"));
        item.setItemMeta(meta);
        return item;
    }
}