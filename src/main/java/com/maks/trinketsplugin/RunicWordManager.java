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
            String stripped = ChatColor.stripColor(line)
                    .replace("â€™", "'")
                    .replace("\u2019", "'")
                    .replace("`", "'");
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
        // ensure only one runic word line
        lore.removeIf(line -> ChatColor.stripColor(line).startsWith("Runic Word:"));
        String runicLore = LORE_PREFIX + word.getDisplayName() + ChatColor.RESET;
        int insertIndex = lore.size();
        for (int i = 0; i < lore.size(); i++) {
            if (ChatColor.stripColor(lore.get(i)).toLowerCase().contains("rarity:")) {
                insertIndex = i;
                break;
            }
        }
        lore.add(insertIndex, runicLore);
        normalizeRarityLine(lore);
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
        normalizeRarityLine(lore);
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /**
     * Ensures the rarity line retains formatting and duplicates are removed.
     */
    private static void normalizeRarityLine(List<String> lore) {
        boolean found = false;
        for (int i = 0; i < lore.size();) {
            String stripped = ChatColor.stripColor(lore.get(i)).trim().toLowerCase();
            if (stripped.startsWith("rarity:")) {
                if (!found) {
                    String line = lore.get(i);
                    int idx = line.toLowerCase().indexOf("rarity:");
                    String suffix = line.substring(idx + "rarity:".length());
                    if (suffix.startsWith(ChatColor.RESET.toString())) {
                        suffix = suffix.substring(ChatColor.RESET.toString().length());
                    }
                    lore.set(i, ChatColor.WHITE.toString() + ChatColor.BOLD + "Rarity:" + ChatColor.RESET + suffix);
                    found = true;
                    i++;
                } else {
                    lore.remove(i);
                }
            } else {
                i++;
            }
        }
    }
}
