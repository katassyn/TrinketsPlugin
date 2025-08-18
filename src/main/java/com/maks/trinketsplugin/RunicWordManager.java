package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for reading and writing runic word information on items.
 */
public class RunicWordManager {
    private static final String LORE_PREFIX = ChatColor.GOLD + "Runic Word: " + ChatColor.YELLOW;

    /**
     * Returns the runic word applied to the given item, or null if none.
     */
    public static RunicWord getRunicWord(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) {
            return null;
        }
        for (String line : meta.getLore()) {
            String stripped = ChatColor.stripColor(line);
            if (stripped.startsWith("Runic Word: ")) {
                String name = stripped.substring("Runic Word: ".length()).trim();
                for (RunicWord word : RunicWord.values()) {
                    if (name.equalsIgnoreCase(word.getDisplayName())) {
                        return word;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Adds a runic word lore line to the item.
     */
    public static void applyRunicWord(ItemStack item, RunicWord word) {
        if (item == null || word == null) return;
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta != null && meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add(LORE_PREFIX + word.getDisplayName());
        if (meta != null) {
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }

    /**
     * Removes any runic word lore line from the item.
     */
    public static void removeRunicWord(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return;
        List<String> lore = new ArrayList<>(meta.getLore());
        lore.removeIf(line -> ChatColor.stripColor(line).startsWith("Runic Word:"));
        meta.setLore(lore);
        item.setItemMeta(meta);
    }
}
