package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

/**
 * Represents the different runic words that can be applied to weapons.
 */
public enum RunicWord {
    RUNIC_TETHER("Runic Tether", 20),
    SURGICAL_SEVER("Surgical Sever", 0),
    BLESSING_THEFT("Blessing Theft", 22),
    HUNTERS_MARK("Hunter's Mark", 20),
    RHYTHMIC_DISPLACEMENT("Rhythmic Displacement", 28),
    CROSSHAIR_RATTLE("Crosshair Rattle", 12),
    WHIPLASH_SPRINT("Whiplash Sprint", 14),
    MISCHIEF("Mischief", 16),
    OVEREXTENSION("Overextension", 16);

    private final String displayName;
    private final int cooldown; // cooldown in seconds

    RunicWord(String displayName, int cooldown) {
        this.displayName = displayName;
        this.cooldown = cooldown;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getCooldown() {
        return cooldown;
    }

    /**
     * Attempts to match an ItemStack to a runic word based on its display name.
     *
     * @param item item to inspect
     * @return matching RunicWord or null if the item is not a runic word
     */
    public static RunicWord fromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return null;
        }
        String stripped = ChatColor.stripColor(item.getItemMeta().getDisplayName())
                .replace("â€™", "'")
                .replace("\u2019", "'")
                .replace("`", "'");
        for (RunicWord word : values()) {
            if (stripped.contains(word.displayName)) {
                return word;
            }
        }
        return null;
    }
}
