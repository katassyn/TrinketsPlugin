package com.maks.trinketsplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GemActionsGUI {
    public static final String TITLE_MAIN = ChatColor.DARK_PURPLE + "Gem Actions";
    public static final String TITLE_INSERT = ChatColor.DARK_PURPLE + "Insert Gem";
    public static final String TITLE_EXTRACT = ChatColor.DARK_PURPLE + "Extract Gem";

    private static ItemStack createFiller() {
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }
        return glass;
    }

    private static ItemStack createMenuItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_MAIN);
        ItemStack filler = createFiller();
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);
        inv.setItem(11, createMenuItem(Material.EMERALD, ChatColor.GREEN + "Insert Gem"));
        inv.setItem(15, createMenuItem(Material.REDSTONE, ChatColor.RED + "Extract Gem"));
        player.openInventory(inv);
    }

    public static void openInsertMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_INSERT);
        ItemStack filler = createFiller();
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);
        inv.clear(11);
        inv.clear(15);
        inv.setItem(22, createMenuItem(Material.ANVIL, ChatColor.YELLOW + "Confirm"));
        player.openInventory(inv);
    }

    public static void openExtractMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_EXTRACT);
        ItemStack filler = createFiller();
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);
        inv.clear(13);
        inv.setItem(22, createMenuItem(Material.ANVIL, ChatColor.YELLOW + "Confirm (50,000,000$)"));
        player.openInventory(inv);
    }
}
