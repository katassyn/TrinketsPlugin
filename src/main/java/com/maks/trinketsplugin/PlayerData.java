package com.maks.trinketsplugin;

import com.google.common.collect.Multimap;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerData {
    private Map<AccessoryType, Map<Attribute, AttributeModifier>> appliedModifiers = new HashMap<>();

    private EnumMap<AccessoryType, ItemStack> accessories = new EnumMap<>(AccessoryType.class);

    // Added field for jewels
    private EnumMap<JewelType, ItemStack> jewels = new EnumMap<>(JewelType.class);

    // List of equipped runes (up to 9 slots)
    private List<ItemStack> runes = new ArrayList<>();

    // Added field for unique trinkets
    private EnumMap<UniqueTrinketType, ItemStack> uniqueTrinkets = new EnumMap<>(UniqueTrinketType.class);

    // Accumulated blockChance and blockStrength from all equipped accessories
    private int blockChance = 0;   // Total Block Chance (%)
    private int blockStrength = 0; // Total Block Strength (%)

    // New custom stats from item lore
    private double xpBoost = 0.0;           // XP Boost (%) - integrates with MyExperiencePlugin
    private double biologistChance = 0.0;   // Biologist Time (%) - integrates with BiologPlugin
    private double evasionChance = 0.0;     // Evasion Chance (%) - 100% dodge
    private double critChance = 0.0;        // Critical Chance (%) - integrates with CriticalStrikeSystem
    private double critDamage = 0.0;        // Critical Damage (%) - bonus crit multiplier
    private double armorPenetration = 0.0;  // Armor Penetration (%) - reduces enemy protection effectiveness

    private static final int debuggingFlag = 0;

    public ItemStack getAccessory(AccessoryType type) {
        return accessories.get(type);
    }

    public void setAccessory(AccessoryType type, ItemStack item) {
        accessories.put(type, item);
        recalculateBlockStats();
    }

    public void removeAccessory(AccessoryType type) {
        accessories.remove(type);
        recalculateBlockStats();
    }

    // Methods for jewels
    public ItemStack getJewel(JewelType type) {
        return jewels.get(type);
    }

    public void setJewel(JewelType type, ItemStack item) {
        jewels.put(type, item);
    }

    public void removeJewel(JewelType type) {
        jewels.remove(type);
    }

    public Map<JewelType, ItemStack> getAllJewels() {
        return Collections.unmodifiableMap(jewels);
    }

    public int getJewelCount() {
        return jewels.size();
    }

    public int getJewelCountByType(JewelType type) {
        int count = 0;
        for (JewelType jewelType : jewels.keySet()) {
            if (jewelType == type) {
                count++;
            }
        }
        return count;
    }

    // Methods for runes
    public List<ItemStack> getRunes() {
        return Collections.unmodifiableList(runes);
    }

    public void addRune(ItemStack item) {
        if (runes.size() < 9) {
            runes.add(item);
        }
    }

    public void removeRune(int index) {
        if (index >= 0 && index < runes.size()) {
            runes.remove(index);
        }
    }

    // Methods for unique trinkets
    public ItemStack getUniqueTrinket(UniqueTrinketType type) {
        return uniqueTrinkets.get(type);
    }

    public void setUniqueTrinket(UniqueTrinketType type, ItemStack item) {
        uniqueTrinkets.put(type, item);
    }

    public void removeUniqueTrinket(UniqueTrinketType type) {
        uniqueTrinkets.remove(type);
    }

    public Map<UniqueTrinketType, ItemStack> getAllUniqueTrinkets() {
        return Collections.unmodifiableMap(uniqueTrinkets);
    }

    // Apply unique trinket attributes
    public void applyUniqueTrinketAttributes(Player player) {
        for (Map.Entry<UniqueTrinketType, ItemStack> entry : uniqueTrinkets.entrySet()) {
            UniqueTrinketType type = entry.getKey();
            ItemStack item = entry.getValue();
            applyUniqueTrinketAttribute(player, item, type);
        }
    }

    public void applyUniqueTrinketAttribute(Player player, ItemStack item, UniqueTrinketType type) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName()).toLowerCase();
        
        // Remove existing modifiers for this unique trinket type
        removeUniqueTrinketAttributes(player, type);
        
        // Apply specific effects based on the trinket
        if (type == UniqueTrinketType.BOSS_HEART) {
            if (displayName.contains("heart of zeus")) {
                // +10% Health (multiplicative, applied after all other modifiers)
                AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (healthAttr != null) {
                    AttributeModifier modifier = new AttributeModifier(
                        UUID.randomUUID(), 
                        "unique_trinket.boss_heart.health_boost", 
                        0.10, // 10% as decimal
                        AttributeModifier.Operation.ADD_SCALAR
                    );
                    healthAttr.addModifier(modifier);
                }
            } else if (displayName.contains("heart of hades")) {
                // +30 Armor
                AttributeInstance armorAttr = player.getAttribute(Attribute.GENERIC_ARMOR);
                if (armorAttr != null) {
                    AttributeModifier modifier = new AttributeModifier(
                        UUID.randomUUID(), 
                        "unique_trinket.boss_heart.armor", 
                        30.0, 
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    armorAttr.addModifier(modifier);
                }
            } else if (displayName.contains("heart of poseidon")) {
                // +10 Luck (flat) + 10% Luck (multiplicative)
                AttributeInstance luckAttr = player.getAttribute(Attribute.GENERIC_LUCK);
                if (luckAttr != null) {
                    // First add flat +10 luck
                    AttributeModifier flatModifier = new AttributeModifier(
                        UUID.randomUUID(), 
                        "unique_trinket.boss_heart.luck_flat", 
                        10.0, 
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    luckAttr.addModifier(flatModifier);
                    
                    // Then add 10% luck multiplicatively
                    AttributeModifier percentModifier = new AttributeModifier(
                        UUID.randomUUID(), 
                        "unique_trinket.boss_heart.luck_percent", 
                        0.10, // 10% as decimal
                        AttributeModifier.Operation.ADD_SCALAR
                    );
                    luckAttr.addModifier(percentModifier);
                }
            }
        } else if (type == UniqueTrinketType.FULL_MOON_SHARD) {
            if (displayName.contains("curse shard")) {
                double healthBonus = 50.0;
                double damageBonus = 200.0;

                // Check if it's the superior version (T2)
                if (displayName.contains("superior")) {
                    healthBonus = 100.0;
                    damageBonus = 400.0;
                }

                // Apply Health bonus
                AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (healthAttr != null) {
                    AttributeModifier healthModifier = new AttributeModifier(
                        UUID.randomUUID(),
                        "unique_trinket.full_moon_shard.health",
                        healthBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    healthAttr.addModifier(healthModifier);
                }

                // Apply Damage bonus
                AttributeInstance damageAttr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
                if (damageAttr != null) {
                    AttributeModifier damageModifier = new AttributeModifier(
                        UUID.randomUUID(),
                        "unique_trinket.full_moon_shard.damage",
                        damageBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    );
                    damageAttr.addModifier(damageModifier);
                }
            }
        } else if (type == UniqueTrinketType.NEW_MOON_SHARD) {
            // Black Lord: high damage, low HP
            // White Lord: high HP, low damage
            double healthModifier = 0.0;
            double damageModifier = 0.0;

            if (displayName.contains("heart of the black lord") ||
                (displayName.contains("black lord") && displayName.contains("malachai"))) {
                // Black Lord path
                if (displayName.contains("superior")) {
                    healthModifier = -0.35; // -35% HP
                    damageModifier = 0.35;  // +35% Damage
                } else {
                    healthModifier = -0.25; // -25% HP
                    damageModifier = 0.25;  // +25% Damage
                }
            } else if (displayName.contains("heart of the white lord") ||
                       (displayName.contains("white lord") && displayName.contains("silvanus"))) {
                // White Lord path
                if (displayName.contains("superior")) {
                    healthModifier = 0.35;  // +35% HP
                    damageModifier = -0.35; // -35% Damage
                } else {
                    healthModifier = 0.25;  // +25% HP
                    damageModifier = -0.25; // -25% Damage
                }
            }

            // Apply Health modifier
            AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (healthAttr != null) {
                AttributeModifier healthMod = new AttributeModifier(
                    UUID.randomUUID(),
                    "unique_trinket.new_moon_shard.health",
                    healthModifier,
                    AttributeModifier.Operation.ADD_SCALAR
                );
                healthAttr.addModifier(healthMod);
            }

            // Apply Damage modifier
            AttributeInstance damageAttr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (damageAttr != null) {
                AttributeModifier damageMod = new AttributeModifier(
                    UUID.randomUUID(),
                    "unique_trinket.new_moon_shard.damage",
                    damageModifier,
                    AttributeModifier.Operation.ADD_SCALAR
                );
                damageAttr.addModifier(damageMod);
            }
        }
    }

    public void removeUniqueTrinketAttributes(Player player, UniqueTrinketType type) {
        for (Attribute attribute : Attribute.values()) {
            AttributeInstance attributeInstance = player.getAttribute(attribute);
            if (attributeInstance != null) {
                for (AttributeModifier modifier : new ArrayList<>(attributeInstance.getModifiers())) {
                    if (modifier.getName().startsWith("unique_trinket." + type.name().toLowerCase() + ".")) {
                        attributeInstance.removeModifier(modifier);
                    }
                }
            }
        }
    }

    public void removeAllUniqueTrinketAttributes(Player player) {
        for (Attribute attribute : Attribute.values()) {
            AttributeInstance attributeInstance = player.getAttribute(attribute);
            if (attributeInstance != null) {
                for (AttributeModifier modifier : new ArrayList<>(attributeInstance.getModifiers())) {
                    if (modifier.getName().startsWith("unique_trinket.")) {
                        attributeInstance.removeModifier(modifier);
                    }
                }
            }
        }
    }

    // Recalculate total blockChance, blockStrength and all custom stats from all equipped accessories
    public void recalculateBlockStats() {
        blockChance = 0;
        blockStrength = 0;
        xpBoost = 0.0;
        biologistChance = 0.0;
        evasionChance = 0.0;
        critChance = 0.0;
        critDamage = 0.0;
        armorPenetration = 0.0;

        for (ItemStack item : accessories.values()) {
            if (item != null) {
                parseBlockStats(item);
            }
        }
    }

    // Parse blockChance, blockStrength and all custom stats from an item and add to totals
    private void parseBlockStats(ItemStack item) {
        int[] blockStats = parseBlockStatsFromItem(item);
        blockChance += blockStats[0];
        blockStrength += blockStats[1];
        parseCustomStats(item);
    }

    /**
     * Static method to parse block chance and block strength from any item.
     * Can be used to parse armor items as well as trinkets.
     * @param item The item to parse
     * @return int array [blockChance, blockStrength]
     */
    public static int[] parseBlockStatsFromItem(ItemStack item) {
        int parsedBlockChance = 0;
        int parsedBlockStrength = 0;

        if (item == null) return new int[]{0, 0};

        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            if (lore != null) {
                for (String line : lore) {
                    String strippedLine = ChatColor.stripColor(line);
                    if (strippedLine != null) {
                        // Parse Block Chance
                        Pattern chancePattern = Pattern.compile("(?i)block chance:\\s*(\\d+)%?");
                        Matcher chanceMatcher = chancePattern.matcher(strippedLine);
                        if (chanceMatcher.find()) {
                            try {
                                int chance = Integer.parseInt(chanceMatcher.group(1));
                                chance = Math.max(0, Math.min(100, chance));
                                parsedBlockChance += chance;
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }

                        // Parse Block Strength
                        Pattern strengthPattern = Pattern.compile("(?i)block strength:\\s*\\+\\s*(\\d+)%?");
                        Matcher strengthMatcher = strengthPattern.matcher(strippedLine);
                        if (strengthMatcher.find()) {
                            try {
                                int strength = Integer.parseInt(strengthMatcher.group(1));
                                strength = Math.max(0, Math.min(100, strength));
                                parsedBlockStrength += strength;
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        return new int[]{parsedBlockChance, parsedBlockStrength};
    }

    /**
     * Static method to parse all custom stats from any item.
     * Can be used to parse armor items as well as trinkets.
     * @param item The item to parse
     * @return CustomStats object with parsed values
     */
    public static CustomStats parseCustomStatsFromItem(ItemStack item) {
        CustomStats stats = new CustomStats();
        if (item == null) return stats;

        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            if (lore != null) {
                for (String line : lore) {
                    String strippedLine = ChatColor.stripColor(line);
                    if (strippedLine != null) {
                        // Parse XP Boost
                        Pattern xpBoostPattern = Pattern.compile("(?i)xp boost:\\s*\\+?(\\d+(?:\\.\\d+)?)%?");
                        Matcher xpBoostMatcher = xpBoostPattern.matcher(strippedLine);
                        if (xpBoostMatcher.find()) {
                            try {
                                stats.xpBoost += Double.parseDouble(xpBoostMatcher.group(1));
                            } catch (NumberFormatException ignored) {}
                        }

                        // Parse Biologist Time
                        Pattern biologistPattern = Pattern.compile("(?i)biologist time:\\s*\\+?(\\d+(?:\\.\\d+)?)%?");
                        Matcher biologistMatcher = biologistPattern.matcher(strippedLine);
                        if (biologistMatcher.find()) {
                            try {
                                stats.biologistChance += Double.parseDouble(biologistMatcher.group(1));
                            } catch (NumberFormatException ignored) {}
                        }

                        // Parse Evasion Chance
                        Pattern evasionPattern = Pattern.compile("(?i)evasion chance:\\s*\\+?(\\d+(?:\\.\\d+)?)%?");
                        Matcher evasionMatcher = evasionPattern.matcher(strippedLine);
                        if (evasionMatcher.find()) {
                            try {
                                stats.evasionChance += Double.parseDouble(evasionMatcher.group(1));
                            } catch (NumberFormatException ignored) {}
                        }

                        // Parse Critical Chance
                        Pattern critChancePattern = Pattern.compile("(?i)critical chance:\\s*\\+?(\\d+(?:\\.\\d+)?)%?");
                        Matcher critChanceMatcher = critChancePattern.matcher(strippedLine);
                        if (critChanceMatcher.find()) {
                            try {
                                stats.critChance += Double.parseDouble(critChanceMatcher.group(1));
                            } catch (NumberFormatException ignored) {}
                        }

                        // Parse Critical Damage
                        Pattern critDamagePattern = Pattern.compile("(?i)critical damage:\\s*\\+?(\\d+(?:\\.\\d+)?)%?");
                        Matcher critDamageMatcher = critDamagePattern.matcher(strippedLine);
                        if (critDamageMatcher.find()) {
                            try {
                                stats.critDamage += Double.parseDouble(critDamageMatcher.group(1));
                            } catch (NumberFormatException ignored) {}
                        }

                        // Parse Armor Penetration
                        Pattern armorPenPattern = Pattern.compile("(?i)armor penetration:\\s*\\+?(\\d+(?:\\.\\d+)?)%?");
                        Matcher armorPenMatcher = armorPenPattern.matcher(strippedLine);
                        if (armorPenMatcher.find()) {
                            try {
                                stats.armorPenetration += Double.parseDouble(armorPenMatcher.group(1));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
        }
        return stats;
    }

    /**
     * Helper class for custom stats from items.
     */
    public static class CustomStats {
        public double xpBoost = 0.0;
        public double biologistChance = 0.0;
        public double evasionChance = 0.0;
        public double critChance = 0.0;
        public double critDamage = 0.0;
        public double armorPenetration = 0.0;

        public void add(CustomStats other) {
            this.xpBoost += other.xpBoost;
            this.biologistChance += other.biologistChance;
            this.evasionChance += other.evasionChance;
            this.critChance += other.critChance;
            this.critDamage += other.critDamage;
            this.armorPenetration += other.armorPenetration;
        }
    }

    // Parse custom stats (XP boost, biologist, evasion, etc.) from an item
    private void parseCustomStats(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            if (lore != null) {
                for (String line : lore) {
                    String strippedLine = ChatColor.stripColor(line);
                    if (strippedLine != null) {
                        // Parse XP Boost: "+X% ⭐" or "XP Boost: +X%"
                        Pattern xpBoostPattern = Pattern.compile("(?i)xp boost:\\s*\\+?(\\d+(?:\\.\\d+)?)%?");
                        Matcher xpBoostMatcher = xpBoostPattern.matcher(strippedLine);
                        if (xpBoostMatcher.find()) {
                            try {
                                double value = Double.parseDouble(xpBoostMatcher.group(1));
                                xpBoost += value;
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                            continue;
                        }

                        // Parse Biologist Time: "+X% 🔬" or "Biologist Time: +X%"
                        Pattern biologistPattern = Pattern.compile("(?i)biologist time:\\s*\\+?(\\d+(?:\\.\\d+)?)%?");
                        Matcher biologistMatcher = biologistPattern.matcher(strippedLine);
                        if (biologistMatcher.find()) {
                            try {
                                double value = Double.parseDouble(biologistMatcher.group(1));
                                biologistChance += value;
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                            continue;
                        }

                        // Parse Evasion Chance: "X% 💨" or "Evasion Chance: X%"
                        Pattern evasionPattern = Pattern.compile("(?i)evasion chance:\\s*\\+?(\\d+(?:\\.\\d+)?)%?");
                        Matcher evasionMatcher = evasionPattern.matcher(strippedLine);
                        if (evasionMatcher.find()) {
                            try {
                                double value = Double.parseDouble(evasionMatcher.group(1));
                                evasionChance += value;
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                            continue;
                        }

                        // Parse Critical Chance: "+X% 🎯" or "Critical Chance: +X%"
                        Pattern critChancePattern = Pattern.compile("(?i)critical chance:\\s*\\+?(\\d+(?:\\.\\d+)?)%?");
                        Matcher critChanceMatcher = critChancePattern.matcher(strippedLine);
                        if (critChanceMatcher.find()) {
                            try {
                                double value = Double.parseDouble(critChanceMatcher.group(1));
                                critChance += value;
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                            continue;
                        }

                        // Parse Critical Damage: "+X% 💥" or "Critical Damage: +X%"
                        Pattern critDamagePattern = Pattern.compile("(?i)critical damage:\\s*\\+?(\\d+(?:\\.\\d+)?)%?");
                        Matcher critDamageMatcher = critDamagePattern.matcher(strippedLine);
                        if (critDamageMatcher.find()) {
                            try {
                                double value = Double.parseDouble(critDamageMatcher.group(1));
                                critDamage += value;
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                            continue;
                        }

                        // Parse Armor Penetration: "+X% 🗡" or "Armor Penetration: +X%"
                        Pattern armorPenPattern = Pattern.compile("(?i)armor penetration:\\s*\\+?(\\d+(?:\\.\\d+)?)%?");
                        Matcher armorPenMatcher = armorPenPattern.matcher(strippedLine);
                        if (armorPenMatcher.find()) {
                            try {
                                double value = Double.parseDouble(armorPenMatcher.group(1));
                                armorPenetration += value;
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }


    public void applyAttributes(Player player, ItemStack item, AccessoryType type) {
        if (item == null) return;

        // Remove existing modifiers for this accessory type
        removeAttributes(player, type);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Get attribute modifiers from the item
        Multimap<Attribute, AttributeModifier> modifiers = meta.getAttributeModifiers();
        if (modifiers == null || modifiers.isEmpty()) return;

        Map<Attribute, AttributeModifier> applied = new HashMap<>();

        for (Attribute attribute : modifiers.keys()) {
            AttributeInstance attributeInstance = player.getAttribute(attribute);
            if (attributeInstance == null) continue;

            Collection<AttributeModifier> attrModifiers = modifiers.get(attribute);
            for (AttributeModifier modifier : attrModifiers) {
                // Create a new modifier with a unique name and UUID
                String modifierName = "trinket." + type.name() + "." + modifier.getName();
                AttributeModifier newModifier = new AttributeModifier(UUID.randomUUID(), modifierName, modifier.getAmount(), modifier.getOperation(), modifier.getSlot());
                attributeInstance.addModifier(newModifier);
                applied.put(attribute, newModifier);
            }
        }

        // Save applied modifiers
        appliedModifiers.put(type, applied);
    }

    public void removeAttributes(Player player, AccessoryType type) {
        for (Attribute attribute : Attribute.values()) {
            AttributeInstance attributeInstance = player.getAttribute(attribute);
            if (attributeInstance != null) {
                for (AttributeModifier modifier : new ArrayList<>(attributeInstance.getModifiers())) {
                    if (modifier.getName().startsWith("trinket." + type.name() + ".")) {
                        attributeInstance.removeModifier(modifier);
                    }
                }
            }
        }
        appliedModifiers.remove(type);
    }

// Dodaj tę metodę do klasy PlayerData.java

// Dodaj tę metodę do klasy PlayerData.java

    public void applyAllAttributes(Player player) {
        // Apply accessory attributes
        for (AccessoryType type : AccessoryType.values()) {
            ItemStack accessory = accessories.get(type);
            if (accessory != null) {
                applyAttributes(player, accessory, type);
            }
        }

        // Apply jewel attributes
        TrinketsPlugin.getInstance().getJewelManager().applyJewelAttributes(player, this);

        // Apply unique trinket attributes - POPRAWIONE
        applyUniqueTrinketAttributes(player);

        // Apply set bonuses - NOWA LINIA
        TrinketsPlugin.getInstance().getSetBonusManager().updatePlayerSetBonuses(player, this);
    }

    public void removeAllAttributes(Player player) {
        for (Attribute attribute : Attribute.values()) {
            AttributeInstance attributeInstance = player.getAttribute(attribute);
            if (attributeInstance != null) {
                for (AttributeModifier modifier : new ArrayList<>(attributeInstance.getModifiers())) {
                    if (modifier.getName().startsWith("trinket.") || modifier.getName().startsWith("set_bonus.")) {
                        attributeInstance.removeModifier(modifier);
                    }
                }
            }
        }
        appliedModifiers.clear();
    }

    // Serialization and deserialization methods
    public String serialize() {
        StringBuilder sb = new StringBuilder();

        // Serialize accessories
        for (Map.Entry<AccessoryType, ItemStack> entry : accessories.entrySet()) {
            AccessoryType type = entry.getKey();
            ItemStack item = entry.getValue();
            try {
                String itemData = ItemSerializationUtils.itemStackToBase64(item);
                sb.append(type.name()).append(":").append(itemData).append(";");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Serialize jewels
        for (Map.Entry<JewelType, ItemStack> entry : jewels.entrySet()) {
            JewelType type = entry.getKey();
            ItemStack item = entry.getValue();
            try {
                String itemData = ItemSerializationUtils.itemStackToBase64(item);
                sb.append("JEWEL_").append(type.name()).append(":").append(itemData).append(";");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Serialize runes
        for (int i = 0; i < runes.size(); i++) {
            ItemStack rune = runes.get(i);
            try {
                String itemData = ItemSerializationUtils.itemStackToBase64(rune);
                sb.append("RUNE_").append(i).append(":").append(itemData).append(";");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Serialize unique trinkets
        for (Map.Entry<UniqueTrinketType, ItemStack> entry : uniqueTrinkets.entrySet()) {
            UniqueTrinketType type = entry.getKey();
            ItemStack item = entry.getValue();
            try {
                String itemData = ItemSerializationUtils.itemStackToBase64(item);
                sb.append("UNIQUE_").append(type.name()).append(":").append(itemData).append(";");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Serialize blockChance and blockStrength
        sb.append("blockChance=").append(blockChance).append(";");
        sb.append("blockStrength=").append(blockStrength).append(";");

        return sb.toString();
    }

    public void deserialize(String data) {
        String[] entries = data.split(";");
        for (String entry : entries) {
            if (entry.isEmpty()) continue;

            if (entry.startsWith("blockChance=")) {
                // Deserialize blockChance
                String value = entry.substring("blockChance=".length());
                try {
                    blockChance = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    blockChance = 0;
                }
            } else if (entry.startsWith("blockStrength=")) {
                // Deserialize blockStrength
                String value = entry.substring("blockStrength=".length());
                try {
                    blockStrength = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    blockStrength = 0;
                }
            } else if (entry.startsWith("JEWEL_")) {
                // Deserialize jewels
                String[] parts = entry.split(":", 2);
                if (parts.length < 2) continue;

                String jewelTypeName = parts[0].substring("JEWEL_".length());
                try {
                    JewelType jewelType = JewelType.valueOf(jewelTypeName);
                    ItemStack item = ItemSerializationUtils.itemStackFromBase64(parts[1]);
                    jewels.put(jewelType, item);

                    if (debuggingFlag == 1) {
                        System.out.println("[PlayerData] Deserialized jewel: " + jewelType);
                    }
                } catch (IllegalArgumentException | IOException e) {
                    e.printStackTrace();
                }
            } else if (entry.startsWith("RUNE_")) {
                // Deserialize runes
                String[] parts = entry.split(":", 2);
                if (parts.length < 2) continue;

                try {
                    ItemStack item = ItemSerializationUtils.itemStackFromBase64(parts[1]);
                    runes.add(item);
                } catch (IllegalArgumentException | IOException e) {
                    e.printStackTrace();
                }
            } else if (entry.startsWith("UNIQUE_")) {
                // Deserialize unique trinkets
                String[] parts = entry.split(":", 2);
                if (parts.length < 2) continue;

                String trinketTypeName = parts[0].substring("UNIQUE_".length());
                try {
                    UniqueTrinketType trinketType = UniqueTrinketType.valueOf(trinketTypeName);
                    ItemStack item = ItemSerializationUtils.itemStackFromBase64(parts[1]);
                    uniqueTrinkets.put(trinketType, item);

                    if (debuggingFlag == 1) {
                        System.out.println("[PlayerData] Deserialized unique trinket: " + trinketType);
                    }
                } catch (IllegalArgumentException | IOException e) {
                    e.printStackTrace();
                }
            } else {
                // Deserialize accessories
                String[] parts = entry.split(":", 2); // Use limit 2 to avoid issues with ':' in Base64
                if (parts.length < 2) continue;

                try {
                    AccessoryType type = AccessoryType.valueOf(parts[0]);
                    ItemStack item = ItemSerializationUtils.itemStackFromBase64(parts[1]);
                    accessories.put(type, item);
                } catch (IllegalArgumentException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Getters for blockChance and blockStrength
    public int getBlockChance() {
        return blockChance;
    }

    public int getBlockStrength() {
        return blockStrength;
    }

    // Getters for new custom stats
    public double getXpBoost() {
        return xpBoost;
    }

    public double getBiologistChance() {
        return biologistChance;
    }

    public double getEvasionChance() {
        return evasionChance;
    }

    public double getCritChance() {
        return critChance;
    }

    public double getCritDamage() {
        return critDamage;
    }

    public double getArmorPenetration() {
        return armorPenetration;
    }
}
