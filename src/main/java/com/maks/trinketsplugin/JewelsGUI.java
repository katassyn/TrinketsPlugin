package com.maks.trinketsplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JewelsGUI {

    private static final int debuggingFlag = 1;

    public static void openJewelsMenu(Player player) {
        if (debuggingFlag == 1) {
            Bukkit.getLogger().info("[JewelsGUI] Opening jewels menu for " + player.getName());
        }

        Inventory gui = Bukkit.createInventory(null, 27, "Jewels Menu");

        // Get player data
        TrinketsPlugin.getInstance().getDatabaseManager().loadPlayerData(player.getUniqueId(), data -> {
            // Fill GUI with placeholders for each jewel type
            int slot = 0;
            for (JewelType type : JewelType.values()) {
                // Skip all JEWEL_OF_FOCUS types as they'll be handled in a separate GUI
                if (type == JewelType.JEWEL_OF_FOCUS || type == JewelType.JEWEL_OF_FOCUS_2 || type == JewelType.JEWEL_OF_FOCUS_3) {
                    continue;
                }

                ItemStack equippedJewel = data.getJewel(type);

                if (equippedJewel != null) {
                    // Clone the jewel and set amount to 1 to prevent showing stacks in GUI
                    ItemStack displayJewel = equippedJewel.clone();
                    displayJewel.setAmount(1);
                    gui.setItem(slot, displayJewel);
                } else {
                    // Slot is empty, add placeholder
                    gui.setItem(slot, createEmptySlot(type));
                }

                slot++;
                if (slot >= 27) break; // Safety check to prevent exceeding inventory size
            }

            // Add Focus Jewels button
            gui.setItem(15, createMenuItem(Material.BLUE_DYE, ChatColor.BLUE + "Focus Jewels", 
                    ChatColor.GRAY + "Manage your Focus Jewels", 
                    ChatColor.GRAY + "You can equip up to 3 Focus Jewels"));

            // Add back button
            gui.setItem(26, createMenuItem(Material.ARROW, ChatColor.YELLOW + "Back", 
                    ChatColor.GRAY + "Return to the main menu"));

            // Open GUI for player
            player.openInventory(gui);

            if (debuggingFlag == 1) {
                Bukkit.getLogger().info("[JewelsGUI] Jewels menu opened successfully for " + player.getName());
            }
        });
    }

    public static void openFocusJewelsMenu(Player player) {
        if (debuggingFlag == 1) {
            Bukkit.getLogger().info("[JewelsGUI] Opening Focus Jewels menu for " + player.getName());
        }

        Inventory gui = Bukkit.createInventory(null, 9, "Focus Jewels");

        // Get player data
        TrinketsPlugin.getInstance().getDatabaseManager().loadPlayerData(player.getUniqueId(), data -> {
            // Add slots for all three Jewel of Focus
            ItemStack cdJewel1 = data.getJewel(JewelType.JEWEL_OF_FOCUS);
            ItemStack cdJewel2 = data.getJewel(JewelType.JEWEL_OF_FOCUS_2);
            ItemStack cdJewel3 = data.getJewel(JewelType.JEWEL_OF_FOCUS_3);

            // Display jewels in slots 0, 1, 2
            if (cdJewel1 != null) {
                ItemStack displayJewel = cdJewel1.clone();
                displayJewel.setAmount(1);
                gui.setItem(0, displayJewel);
            } else {
                gui.setItem(0, createEmptySlot(JewelType.JEWEL_OF_FOCUS));
            }

            if (cdJewel2 != null) {
                ItemStack displayJewel = cdJewel2.clone();
                displayJewel.setAmount(1);
                gui.setItem(1, displayJewel);
            } else {
                gui.setItem(1, createEmptySlot(JewelType.JEWEL_OF_FOCUS_2));
            }

            if (cdJewel3 != null) {
                ItemStack displayJewel = cdJewel3.clone();
                displayJewel.setAmount(1);
                gui.setItem(2, displayJewel);
            } else {
                gui.setItem(2, createEmptySlot(JewelType.JEWEL_OF_FOCUS_3));
            }

            // Add information item
            gui.setItem(4, createMenuItem(Material.BOOK, ChatColor.GOLD + "Focus Jewels Info", 
                    ChatColor.GRAY + "Each Focus Jewel reduces cooldowns",
                    ChatColor.GRAY + "Tier 1: 2% reduction",
                    ChatColor.GRAY + "Tier 2: 3% reduction",
                    ChatColor.GRAY + "Tier 3: 5% reduction",
                    "",
                    ChatColor.YELLOW + "Maximum reduction: 15%"));

            // Add back button
            gui.setItem(8, createMenuItem(Material.ARROW, ChatColor.YELLOW + "Back", 
                    ChatColor.GRAY + "Return to the jewels menu"));

            // Open GUI for player
            player.openInventory(gui);

            if (debuggingFlag == 1) {
                Bukkit.getLogger().info("[JewelsGUI] Focus Jewels menu opened successfully for " + player.getName());
            }
        });
    }

    private static ItemStack createEmptySlot(JewelType type) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.GRAY + type.getDisplayName() + " Slot");

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + "Empty");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Right-click with a " + type.getDisplayName());
            lore.add(ChatColor.YELLOW + "to equip it.");
            lore.add("");
            lore.add(ChatColor.RED + "Required Level: 50");

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack createMenuItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);

            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }

            item.setItemMeta(meta);
        }

        return item;
    }
}
