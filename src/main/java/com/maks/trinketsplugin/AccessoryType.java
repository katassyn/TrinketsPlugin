package com.maks.trinketsplugin;

import org.bukkit.Material;

public enum AccessoryType {
    RING_1(Material.TNT_MINECART, "Ring 1", 0),
    RING_2(Material.HOPPER_MINECART, "Ring 2", 1),
    NECKLACE(Material.CHEST_MINECART, "Necklace", 2),
    ADORNMENT(Material.FURNACE_MINECART, "Adornment", 3),
    CLOAK(Material.WHITE_BANNER, "Cloak", 4),
    SHIELD(Material.IRON_DOOR, "Shield", 5),
    BELT(Material.MINECART, "Belt", 6),
    GLOVES(Material.LEATHER_HORSE_ARMOR, "Gloves", 7),
    BOSS_SOUL(Material.DRAGON_BREATH, "Boss Soul", 8);

    private Material material;
    private String displayName;
    private int slot;

    AccessoryType(Material material, String displayName, int slot) {
        this.material = material;
        this.displayName = displayName;
        this.slot = slot;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getSlot() {
        return slot;
    }
}

