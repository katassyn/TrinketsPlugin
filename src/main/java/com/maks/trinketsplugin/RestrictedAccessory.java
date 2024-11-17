package com.maks.trinketsplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class RestrictedAccessory {

    private final Material material;
    private final String displayName;
    private final int requiredLevel;
    private final AccessoryType accessoryType;

    public RestrictedAccessory(String materialName, String displayName, int requiredLevel) {
        this.material = Material.matchMaterial(materialName);
        if (this.material == null) {
            throw new IllegalArgumentException("Invalid material name: " + materialName);
        }
        this.displayName = ChatColor.translateAlternateColorCodes('&', displayName);
        this.requiredLevel = requiredLevel;
        this.accessoryType = getAccessoryTypeByMaterial(this.material);
    }

    private AccessoryType getAccessoryTypeByMaterial(Material material) {
        for (AccessoryType type : AccessoryType.values()) {
            if (type.getMaterial() == material) {
                return type;
            }
        }
        return null;
    }

    public AccessoryType getAccessoryType() {
        return accessoryType;
    }

    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != material) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        String itemDisplayName = ChatColor.stripColor(meta.getDisplayName());
        String expectedDisplayName = ChatColor.stripColor(displayName);
        boolean matches = itemDisplayName.equals(expectedDisplayName);
        // Wiadomość debugująca
 //       Bukkit.getLogger().info("Comparing stripped display names. Expected: '" + expectedDisplayName + "', Found: '" + itemDisplayName + "'. Matches: " + matches);
        return matches;
    }



    public int getRequiredLevel() {
        return requiredLevel;
    }

    // Dodajemy getter dla displayName
    public String getDisplayName() {
        return displayName;
    }
}
