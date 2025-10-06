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
        ItemStack uniqueTrinketsIcon = createMenuItem(Material.EMERALD, "Unique Trinkets", "Manage your unique trinkets.");
        ItemStack jewelsIcon = createMenuItem(Material.FEATHER, "Jewels", "Manage your jewels.");

        // Place icons in the GUI
        gui.setItem(2, accessoriesIcon);
        gui.setItem(3, runesIcon);
        gui.setItem(5, uniqueTrinketsIcon);
        gui.setItem(6, jewelsIcon);

        player.openInventory(gui);
    }

    public static void openAccessoriesMenu(Player player) {
        TrinketsPlugin.getInstance().getDatabaseManager().loadPlayerData(player.getUniqueId(), data -> {
            // Fixed inventory size of 18 slots (2 rows)
            int inventorySize = 18;

            Inventory gui = Bukkit.createInventory(null, inventorySize, "Accessories");

            // Display equipped accessories
            for (AccessoryType type : AccessoryType.values()) {
                ItemStack item = data.getAccessory(type);
                if (item != null) {
                    // Clone the accessory and set amount to 1 to prevent showing stacks in GUI
                    ItemStack displayItem = item.clone();
                    displayItem.setAmount(1);
                    gui.setItem(type.getSlot(), displayItem);
                } else {
                    gui.setItem(type.getSlot(), createMenuItem(Material.GRAY_STAINED_GLASS_PANE, type.getDisplayName(), "Empty slot."));
                }
            }

            // Add back button in the bottom right corner
            gui.setItem(17, createMenuItem(Material.ARROW, "§eBack", "§7Return to the main menu"));

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
