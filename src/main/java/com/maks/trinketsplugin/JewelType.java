package com.maks.trinketsplugin;

import org.bukkit.Material;

public enum JewelType {
    DAMAGE("Emberfang Jewel", Material.BROWN_DYE, "Grants increased damage", 3),
    MOVE_SPEED("Windstep Sapphire", Material.WHITE_DYE, "Grants increased movement speed", 3),
    ATTACK_SPEED("Whirlwind Opal", Material.LIGHT_GRAY_DYE, "Grants increased attack speed", 3),
    HEALTH("Heartroot Ruby", Material.GRAY_DYE, "Grants additional health", 3),
    ARMOR_TOUGHNESS("Stonehide Garnet", Material.YELLOW_DYE, "Grants armor toughness", 3),
    LASTING_HEALING("Lasting Healing Jewel", Material.LIME_DYE, "Heals after killing enemies", 3),
    AMPLIFIED_HEALING("Amplified Healing Jewel", Material.CYAN_DYE, "Increases damage after healing", 3),
    JEWEL_OF_FOCUS("Jewel of Focus", Material.BLUE_DYE, "Reduces cooldowns", 3),
    JEWEL_OF_FOCUS_2("Jewel of Focus", Material.BLUE_DYE, "Reduces cooldowns", 3),
    JEWEL_OF_FOCUS_3("Jewel of Focus", Material.BLUE_DYE, "Reduces cooldowns", 3),
    JEWEL_OF_RAGE("Jewel of Rage", Material.PURPLE_DYE, "Grants fury and critical chance", 3),
    STEAM_SALE("Steam Sale", Material.MAGENTA_DYE, "Cheaper crafting and better selling", 3),
    PHOENIX("Phoenix Egg", Material.PINK_DYE, "Cheat death", 3),
    ANDERMANT("Shadowvein Crystal", Material.GUNPOWDER, "Chance to duplicate Andermant", 3),
    CLOVER("Sunspire Amber", Material.GLOWSTONE_DUST, "Get additional clovers", 3),
    DRAKENMELON("Melonbane Prism", Material.SUGAR, "Get additional drakenmelons", 3),
    LOCKPICK("Shadowpick Onyx", Material.FEATHER, "Get additional lockpicks", 3),
    INGREDIENT("Ingredient Jewel", Material.PRISMARINE_SHARD, "Get additional ingredients", 3),
    COLLECTOR("Deathcut Garnet", Material.GHAST_TEAR, "Finishes off low health enemies", 3),
    GOLDEN_FISH("Golden Fish Jewel", Material.WHEAT, "Chance to double fishing loot", 3);

    private final String displayName;
    private final Material material;
    private final String description;
    private final int maxTier;
    private final int maxEquipped;

    JewelType(String displayName, Material material, String description, int maxTier) {
        this(displayName, material, description, maxTier, 1); // Default max equipped is 1
    }

    JewelType(String displayName, Material material, String description, int maxTier, int maxEquipped) {
        this.displayName = displayName;
        this.material = material;
        this.description = description;
        this.maxTier = maxTier;
        this.maxEquipped = maxEquipped;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDescription() {
        return description;
    }

    public int getMaxTier() {
        return maxTier;
    }

    public int getMaxEquipped() {
        return maxEquipped;
    }
}
