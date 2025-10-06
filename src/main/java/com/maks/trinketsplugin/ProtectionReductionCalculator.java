package com.maks.trinketsplugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public final class ProtectionReductionCalculator {

    private ProtectionReductionCalculator() {
    }

    public static ProtectionStats calculate(Player player) {
        Objects.requireNonNull(player, "player");

        TrinketsPlugin plugin = TrinketsPlugin.getInstance();
        FileConfiguration config = plugin.getConfig();

        int baseLevel = Math.max(0, config.getInt("protection-curve.base-level", 16));
        double baseReduction = clamp01(config.getDouble("protection-curve.base-reduction", 0.50));
        double maxCap = clamp01(config.getDouble("protection-curve.max-cap", 0.80));
        int maxTotal = Math.max(0, config.getInt("protection-curve.max-total-level", 800));
        double kFactor = Math.max(0.0, config.getDouble("protection-curve.k-factor", 0.1913));

        int totalProtection = Math.max(0, getTotalProtectionLevel(player));
        int cappedTotalProtection = maxTotal > 0 ? Math.min(totalProtection, maxTotal) : totalProtection;

        double perLevelReduction;
        if (baseLevel > 0) {
            perLevelReduction = baseReduction / baseLevel;
        } else {
            perLevelReduction = 0.0;
        }

        double baseReductionApplied;
        if (baseLevel > 0) {
            baseReductionApplied = Math.min(baseReduction, cappedTotalProtection * perLevelReduction);
        } else {
            baseReductionApplied = 0.0;
        }

        int extraLevels = Math.max(0, cappedTotalProtection - baseLevel);
        double multiplier = 1.0;
        if (extraLevels > 0 && kFactor > 0.0) {
            multiplier = 100.0 / (100.0 + kFactor * extraLevels);
        }

        double damageMultiplierBeforeCap = (1.0 - baseReductionApplied) * multiplier;
        double minDamageMultiplier = 1.0 - maxCap;
        double finalDamageMultiplier = Math.max(damageMultiplierBeforeCap, minDamageMultiplier);

        double totalReduction = 1.0 - finalDamageMultiplier;
        totalReduction = Math.min(totalReduction, maxCap);
        totalReduction = clamp01(totalReduction);

        double extraReduction = Math.max(0.0, totalReduction - baseReductionApplied);

        return new ProtectionStats(totalProtection, cappedTotalProtection, baseLevel, extraLevels,
                baseReductionApplied, extraReduction, totalReduction, multiplier, finalDamageMultiplier);
    }

    public static double applyToDamage(Player player, double baseDamage) {
        if (baseDamage <= 0.0) {
            return 0.0;
        }
        ProtectionStats stats = calculate(player);
        // The Bukkit damage event already contains the vanilla protection reduction, so we
        // compensate for it here and only apply the additional scaling from the custom curve.
        double vanillaMultiplier = 1.0 - stats.baseReductionApplied();
        if (vanillaMultiplier <= 0.0) {
            return 0.0;
        }
        double adjustedMultiplier = stats.finalDamageMultiplier() / vanillaMultiplier;
        return Math.max(0.0, baseDamage * adjustedMultiplier);
    }

    public static int getTotalProtectionLevel(Player player) {
        int total = 0;
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null) {
                total += armor.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL);
            }
        }
        return total;
    }

    private static double clamp01(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    public static final class ProtectionStats {
        private final int totalProtection;
        private final int cappedTotalProtection;
        private final int baseLevel;
        private final int extraLevels;
        private final double baseReductionApplied;
        private final double extraReduction;
        private final double totalReduction;
        private final double multiplier;
        private final double finalDamageMultiplier;

        private ProtectionStats(int totalProtection, int cappedTotalProtection, int baseLevel, int extraLevels,
                                double baseReductionApplied, double extraReduction, double totalReduction,
                                double multiplier, double finalDamageMultiplier) {
            this.totalProtection = totalProtection;
            this.cappedTotalProtection = cappedTotalProtection;
            this.baseLevel = baseLevel;
            this.extraLevels = extraLevels;
            this.baseReductionApplied = baseReductionApplied;
            this.extraReduction = extraReduction;
            this.totalReduction = totalReduction;
            this.multiplier = multiplier;
            this.finalDamageMultiplier = finalDamageMultiplier;
        }

        public int totalProtection() {
            return totalProtection;
        }

        public int cappedTotalProtection() {
            return cappedTotalProtection;
        }

        public int baseLevel() {
            return baseLevel;
        }

        public int extraLevels() {
            return extraLevels;
        }

        public double baseReductionApplied() {
            return baseReductionApplied;
        }

        public double extraReduction() {
            return extraReduction;
        }

        public double totalReduction() {
            return totalReduction;
        }

        public double multiplier() {
            return multiplier;
        }

        public double finalDamageMultiplier() {
            return finalDamageMultiplier;
        }

        public double apply(double damage) {
            return Math.max(0.0, damage * finalDamageMultiplier);
        }
    }
}
