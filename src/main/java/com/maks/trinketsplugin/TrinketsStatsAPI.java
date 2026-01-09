package com.maks.trinketsplugin;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Public API for other plugins to access Trinkets stats.
 * Usage example:
 *   double xpBoost = TrinketsStatsAPI.getXpBoost(player);
 *   double biologistBonus = TrinketsStatsAPI.getBiologistChance(player);
 */
public final class TrinketsStatsAPI {

    private static final double LUCK_SOFT_CAP = 30.0;
    private static final double LUCK_SOFT_CAP_MULTIPLIER = 0.1;

    // Stat hard caps
    public static final double EVASION_CAP = 25.0;
    public static final double ARMOR_PENETRATION_CAP = 35.0;
    public static final double CRITICAL_CHANCE_CAP = 50.0;

    private TrinketsStatsAPI() {
        // Utility class - no instantiation
    }

    /**
     * Get player's XP Boost percentage from equipped trinkets and armor.
     * @param player The player
     * @return XP boost percentage (e.g., 5.0 for +5%)
     */
    public static double getXpBoost(Player player) {
        double total = 0.0;
        PlayerData data = getPlayerData(player);
        if (data != null) {
            total += data.getXpBoost();
        }
        // Also scan player's equipped armor
        total += getArmorCustomStats(player).xpBoost;
        return total;
    }

    /**
     * Get player's Biologist Time bonus from equipped trinkets and armor.
     * This value should be ADDED to the base drop_chance.
     * Example: if drop_chance is 0.05 (5%) and this returns 3.0,
     * the new chance should be 0.05 + 0.03 = 0.08 (8%)
     * @param player The player
     * @return Biologist chance bonus as percentage (e.g., 3.0 for +3%)
     */
    public static double getBiologistChance(Player player) {
        double total = 0.0;
        PlayerData data = getPlayerData(player);
        if (data != null) {
            total += data.getBiologistChance();
        }
        // Also scan player's equipped armor
        total += getArmorCustomStats(player).biologistChance;
        return total;
    }

    /**
     * Get player's Biologist Time bonus as decimal for direct addition to drop_chance.
     * Example: if drop_chance is 0.05 and this returns 0.03, new chance = 0.08
     * @param player The player
     * @return Biologist chance bonus as decimal (e.g., 0.03 for +3%)
     */
    public static double getBiologistChanceDecimal(Player player) {
        return getBiologistChance(player) / 100.0;
    }

    /**
     * Get player's Evasion Chance percentage from equipped trinkets and armor (CAPPED).
     * @param player The player
     * @return Evasion chance percentage capped at EVASION_CAP (e.g., 5.0 for 5% chance to evade all damage)
     */
    public static double getEvasionChance(Player player) {
        return Math.min(getRawEvasionChance(player), EVASION_CAP);
    }

    /**
     * Get player's raw Evasion Chance before cap is applied.
     * @param player The player
     * @return Raw evasion chance percentage (uncapped)
     */
    public static double getRawEvasionChance(Player player) {
        double total = 0.0;
        PlayerData data = getPlayerData(player);
        if (data != null) {
            total += data.getEvasionChance();
        }
        // Also scan player's equipped armor
        total += getArmorCustomStats(player).evasionChance;
        return total;
    }

    /**
     * Get player's Critical Chance bonus from equipped trinkets and armor (CAPPED).
     * This should be added to the CriticalStrikeSystem base chance.
     * @param player The player
     * @return Critical chance bonus percentage capped at CRITICAL_CHANCE_CAP (e.g., 5.0 for +5%)
     */
    public static double getCritChance(Player player) {
        return Math.min(getRawCritChance(player), CRITICAL_CHANCE_CAP);
    }

    /**
     * Get player's raw Critical Chance bonus before cap is applied.
     * @param player The player
     * @return Raw critical chance bonus percentage (uncapped)
     */
    public static double getRawCritChance(Player player) {
        double total = 0.0;
        PlayerData data = getPlayerData(player);
        if (data != null) {
            total += data.getCritChance();
        }
        // Also scan player's equipped armor
        total += getArmorCustomStats(player).critChance;
        return total;
    }

    /**
     * Get player's Critical Damage bonus from equipped trinkets and armor.
     * This should be added to the CriticalStrikeSystem damage multiplier.
     * Example: if base multiplier is 2.0 (200%) and this returns 20.0,
     * the new multiplier should be 2.0 + 0.20 = 2.20 (220%)
     * @param player The player
     * @return Critical damage bonus percentage (e.g., 20.0 for +20%)
     */
    public static double getCritDamage(Player player) {
        double total = 0.0;
        PlayerData data = getPlayerData(player);
        if (data != null) {
            total += data.getCritDamage();
        }
        // Also scan player's equipped armor
        total += getArmorCustomStats(player).critDamage;
        return total;
    }

    /**
     * Get player's Critical Damage bonus as decimal for direct addition to multiplier.
     * Example: if multiplier is 2.0 and this returns 0.20, new multiplier = 2.20
     * @param player The player
     * @return Critical damage bonus as decimal (e.g., 0.20 for +20%)
     */
    public static double getCritDamageDecimal(Player player) {
        return getCritDamage(player) / 100.0;
    }

    /**
     * Get player's Armor Penetration percentage from equipped trinkets and armor (CAPPED).
     * This reduces the effectiveness of enemy protection.
     * @param player The player (attacker)
     * @return Armor penetration percentage capped at ARMOR_PENETRATION_CAP (e.g., 10.0 for 10% armor penetration)
     */
    public static double getArmorPenetration(Player player) {
        return Math.min(getRawArmorPenetration(player), ARMOR_PENETRATION_CAP);
    }

    /**
     * Get player's raw Armor Penetration before cap is applied.
     * @param player The player (attacker)
     * @return Raw armor penetration percentage (uncapped)
     */
    public static double getRawArmorPenetration(Player player) {
        double total = 0.0;
        PlayerData data = getPlayerData(player);
        if (data != null) {
            total += data.getArmorPenetration();
        }
        // Also scan player's equipped armor and weapon
        total += getArmorCustomStats(player).armorPenetration;
        // Also check main hand weapon
        if (player != null) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand != null) {
                total += PlayerData.parseCustomStatsFromItem(mainHand).armorPenetration;
            }
        }
        return total;
    }

    /**
     * Get player's Armor Penetration as decimal.
     * @param player The player (attacker)
     * @return Armor penetration as decimal (e.g., 0.10 for 10%)
     */
    public static double getArmorPenetrationDecimal(Player player) {
        return getArmorPenetration(player) / 100.0;
    }

    /**
     * Get player's Block Chance from equipped trinkets and armor.
     * @param player The player
     * @return Block chance percentage (0-100)
     */
    public static int getBlockChance(Player player) {
        int total = 0;
        PlayerData data = getPlayerData(player);
        if (data != null) {
            total += data.getBlockChance();
        }
        // Also scan player's equipped armor
        total += getArmorBlockStats(player)[0];
        return Math.min(total, 100); // Capped at 100%
    }

    /**
     * Get player's Block Strength from equipped trinkets and armor.
     * @param player The player
     * @return Block strength bonus percentage
     */
    public static int getBlockStrength(Player player) {
        int total = 0;
        PlayerData data = getPlayerData(player);
        if (data != null) {
            total += data.getBlockStrength();
        }
        // Also scan player's equipped armor
        total += getArmorBlockStats(player)[1];
        return total;
    }

    /**
     * Get player's effective Luck with soft-cap applied.
     * Soft-cap at 30: every point above 30 counts as 10% (0.1).
     * ADD_SCALAR (%) modifiers are applied after the soft-cap.
     *
     * Example: 40 base Luck + 8% = 31 + 8% = 33.48 effective Luck
     * - Base: 0
     * - Flat bonuses: 40
     * - After soft-cap: 30 + (40-30)*0.1 = 31
     * - With 8% scalar: 31 * 1.08 = 33.48
     *
     * @param player The player
     * @return Effective Luck value after soft-cap and all modifiers
     */
    public static double getEffectiveLuck(Player player) {
        if (player == null) return 0.0;

        AttributeInstance luckAttr = player.getAttribute(Attribute.GENERIC_LUCK);
        if (luckAttr == null) return 0.0;

        double baseValue = luckAttr.getBaseValue();

        // Collect all ADD_NUMBER modifiers
        double flatBonus = 0.0;
        // Collect all ADD_SCALAR modifiers
        double scalarMultiplier = 0.0;

        for (AttributeModifier modifier : luckAttr.getModifiers()) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADD_NUMBER) {
                flatBonus += modifier.getAmount();
            } else if (modifier.getOperation() == AttributeModifier.Operation.ADD_SCALAR) {
                scalarMultiplier += modifier.getAmount();
            }
        }

        // Apply soft-cap to flat bonus
        double effectiveFlatBonus = flatBonus;
        if (flatBonus > LUCK_SOFT_CAP) {
            effectiveFlatBonus = LUCK_SOFT_CAP + (flatBonus - LUCK_SOFT_CAP) * LUCK_SOFT_CAP_MULTIPLIER;
        }

        // Apply scalar multiplier after soft-cap
        double effectiveLuck = (baseValue + effectiveFlatBonus) * (1.0 + scalarMultiplier);

        return effectiveLuck;
    }

    /**
     * Get player's raw Luck value (before soft-cap).
     * @param player The player
     * @return Raw Luck value from Minecraft attributes
     */
    public static double getRawLuck(Player player) {
        if (player == null) return 0.0;

        AttributeInstance luckAttr = player.getAttribute(Attribute.GENERIC_LUCK);
        if (luckAttr == null) return 0.0;

        return luckAttr.getValue();
    }

    /**
     * Get detailed Luck breakdown for display purposes.
     * @param player The player
     * @return LuckBreakdown object with detailed statistics
     */
    public static LuckBreakdown getLuckBreakdown(Player player) {
        if (player == null) return new LuckBreakdown(0, 0, 0, 0, 0);

        AttributeInstance luckAttr = player.getAttribute(Attribute.GENERIC_LUCK);
        if (luckAttr == null) return new LuckBreakdown(0, 0, 0, 0, 0);

        double baseValue = luckAttr.getBaseValue();

        double flatBonus = 0.0;
        double scalarMultiplier = 0.0;

        for (AttributeModifier modifier : luckAttr.getModifiers()) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADD_NUMBER) {
                flatBonus += modifier.getAmount();
            } else if (modifier.getOperation() == AttributeModifier.Operation.ADD_SCALAR) {
                scalarMultiplier += modifier.getAmount();
            }
        }

        double effectiveFlatBonus = flatBonus;
        if (flatBonus > LUCK_SOFT_CAP) {
            effectiveFlatBonus = LUCK_SOFT_CAP + (flatBonus - LUCK_SOFT_CAP) * LUCK_SOFT_CAP_MULTIPLIER;
        }

        double effectiveLuck = (baseValue + effectiveFlatBonus) * (1.0 + scalarMultiplier);

        return new LuckBreakdown(baseValue, flatBonus, scalarMultiplier, effectiveFlatBonus, effectiveLuck);
    }

    /**
     * Breakdown of Luck calculation.
     */
    public static class LuckBreakdown {
        public final double baseValue;
        public final double rawFlatBonus;
        public final double scalarMultiplier;
        public final double effectiveFlatBonus;
        public final double effectiveLuck;

        public LuckBreakdown(double baseValue, double rawFlatBonus, double scalarMultiplier,
                           double effectiveFlatBonus, double effectiveLuck) {
            this.baseValue = baseValue;
            this.rawFlatBonus = rawFlatBonus;
            this.scalarMultiplier = scalarMultiplier;
            this.effectiveFlatBonus = effectiveFlatBonus;
            this.effectiveLuck = effectiveLuck;
        }
    }

    /**
     * Check if TrinketsPlugin is enabled and available.
     * @return true if TrinketsPlugin is available
     */
    public static boolean isAvailable() {
        return TrinketsPlugin.getInstance() != null &&
               TrinketsPlugin.getInstance().isEnabled();
    }

    private static PlayerData getPlayerData(Player player) {
        if (player == null) return null;
        TrinketsPlugin plugin = TrinketsPlugin.getInstance();
        if (plugin == null || plugin.getDatabaseManager() == null) return null;
        return plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
    }

    /**
     * Get combined custom stats from player's equipped armor.
     */
    private static PlayerData.CustomStats getArmorCustomStats(Player player) {
        PlayerData.CustomStats combined = new PlayerData.CustomStats();
        if (player == null) return combined;

        for (ItemStack armorPiece : player.getInventory().getArmorContents()) {
            if (armorPiece != null) {
                combined.add(PlayerData.parseCustomStatsFromItem(armorPiece));
            }
        }
        return combined;
    }

    /**
     * Get combined block stats from player's equipped armor.
     * @return int array [blockChance, blockStrength]
     */
    private static int[] getArmorBlockStats(Player player) {
        int blockChance = 0;
        int blockStrength = 0;
        if (player == null) return new int[]{0, 0};

        for (ItemStack armorPiece : player.getInventory().getArmorContents()) {
            if (armorPiece != null) {
                int[] stats = PlayerData.parseBlockStatsFromItem(armorPiece);
                blockChance += stats[0];
                blockStrength += stats[1];
            }
        }
        return new int[]{blockChance, blockStrength};
    }
}
