package com.maks.trinketsplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ConjurationGUI {
    public static final String TITLE_MAIN = ChatColor.DARK_PURPLE + "Conjuration";
    public static final String TITLE_ITEMS = ChatColor.DARK_PURPLE + "Items Conjuration";
    public static final String TITLE_DISPEL = ChatColor.DARK_PURPLE + "Rune Words Dispel";

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
        inv.setItem(11, createMenuItem(Material.DIAMOND_SWORD, ChatColor.GREEN + "Items Conjuration"));
        inv.setItem(15, createMenuItem(Material.PAPER, ChatColor.YELLOW + "Rune Words Dispel"));
        player.openInventory(inv);
    }

    public static void openItemsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_ITEMS);
        ItemStack filler = createFiller();
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);
        inv.clear(11);
        inv.clear(15);
        inv.setItem(22, createMenuItem(Material.ANVIL, ChatColor.YELLOW + "Conjure (100,000,000$)"));
        player.openInventory(inv);
    }

    public static void openDispelMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_DISPEL);
        ItemStack filler = createFiller();
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);
        inv.clear(13);
        inv.setItem(22, createMenuItem(Material.ANVIL, ChatColor.YELLOW + "Dispel (250,000,000$)"));
        player.openInventory(inv);
    }
}
