package com.maks.trinketsplugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Arrays;

public class TrinketsGUI {

    public static void openMainMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, "Trinkets Menu");

        // Icons for submenus
        ItemStack accessoriesIcon = createMenuItem(Material.DIAMOND, "Accessories", "Manage your accessories.");
        ItemStack runesIcon = createMenuItem(Material.ENCHANTED_BOOK, "Runes", "Manage your runes.");
        ItemStack gemsIcon = createMenuItem(Material.EMERALD, "Gems", "Manage your gems.");

        // Place icons in the GUI
        gui.setItem(3, accessoriesIcon);
        gui.setItem(4, runesIcon);
        gui.setItem(5, gemsIcon);

        player.openInventory(gui);
    }

    public static void openAccessoriesMenu(Player player) {
        TrinketsPlugin.getInstance().getDatabaseManager().loadPlayerData(player.getUniqueId(), data -> {
            int inventorySize = 9; // Start with size 9

            // Calculate required inventory size based on the highest slot number
            int highestSlot = 0;
            for (AccessoryType type : AccessoryType.values()) {
                if (type.getSlot() > highestSlot) {
                    highestSlot = type.getSlot();
                }
            }

            // Adjust inventory size to fit all slots (must be a multiple of 9)
            while (inventorySize <= highestSlot) {
                inventorySize += 9;
            }

            Inventory gui = Bukkit.createInventory(null, inventorySize, "Accessories");

            // Display equipped accessories
            for (AccessoryType type : AccessoryType.values()) {
                ItemStack item = data.getAccessory(type);
                if (item != null) {
                    gui.setItem(type.getSlot(), item);
                } else {
                    gui.setItem(type.getSlot(), createMenuItem(Material.GRAY_STAINED_GLASS_PANE, type.getDisplayName(), "Empty slot."));
                }
            }

            // Open the inventory on the main thread
            Bukkit.getScheduler().runTask(TrinketsPlugin.getInstance(), () -> {
                player.openInventory(gui);
            });
        });
    }



    private static ItemStack createMenuItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));

        item.setItemMeta(meta);
        return item;
    }
}
