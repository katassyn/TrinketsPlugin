package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Random;

public enum QualityHoney {
    BASIC("&9[ I ] Honey Bottle", -10.0, 10.0),
    RARE("&5[ II ] Honey Bottle", 0.0, 20.0),
    LEGENDARY("&6[ III ] Honey Bottle", 10.0, 30.0);

    private final String display;
    private final double min;
    private final double max;

    QualityHoney(String display, double min, double max) {
        this.display = ChatColor.translateAlternateColorCodes('&', display);
        this.min = min;
        this.max = max;
    }

    public double roll() {
        return min + (new Random().nextDouble() * (max - min));
    }

    public static QualityHoney fromItem(ItemStack item) {
        if (item == null || item.getType() != Material.HONEY_BOTTLE) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return null;
        String name = ChatColor.stripColor(meta.getDisplayName());
        for (QualityHoney q : values()) {
            if (ChatColor.stripColor(q.display).equals(name)) {
                return q;
            }
        }
        return null;
    }

    public String getDisplay() {
        return display;
    }
}
