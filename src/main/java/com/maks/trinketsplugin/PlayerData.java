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

    // Accumulated blockChance and blockStrength from all equipped accessories
    private int blockChance = 0;   // Total Block Chance (%)
    private int blockStrength = 0; // Total Block Strength (%)

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

    // Recalculate total blockChance and blockStrength from all equipped accessories
    public void recalculateBlockStats() {
        blockChance = 0;
        blockStrength = 0;

        for (ItemStack item : accessories.values()) {
            if (item != null) {
                parseBlockStats(item);
            }
        }
    }

    // Parse blockChance and blockStrength from an item and add to totals
    private void parseBlockStats(ItemStack item) {
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
                            String percentageString = chanceMatcher.group(1);
                            try {
                                int chance = Integer.parseInt(percentageString);
                                chance = Math.max(0, Math.min(100, chance));
                                blockChance += chance;
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                            continue;
                        }

                        // Parse Block Strength (updated pattern)
                        Pattern strengthPattern = Pattern.compile("(?i)block strength:\\s*\\+\\s*(\\d+)%?");
                        Matcher strengthMatcher = strengthPattern.matcher(strippedLine);
                        if (strengthMatcher.find()) {
                            String percentageString = strengthMatcher.group(1);
                            try {
                                int strength = Integer.parseInt(percentageString);
                                strength = Math.max(0, Math.min(100, strength));
                                blockStrength += strength;
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

    public void applyAllAttributes(Player player) {
        // Remove all existing attributes added by the plugin
        removeAllAttributes(player);

        // Apply attributes from currently equipped accessories
        for (Map.Entry<AccessoryType, ItemStack> entry : accessories.entrySet()) {
            AccessoryType type = entry.getKey();
            ItemStack item = entry.getValue();
            applyAttributes(player, item, type);
        }
    }

    public void removeAllAttributes(Player player) {
        for (Attribute attribute : Attribute.values()) {
            AttributeInstance attributeInstance = player.getAttribute(attribute);
            if (attributeInstance != null) {
                for (AttributeModifier modifier : new ArrayList<>(attributeInstance.getModifiers())) {
                    if (modifier.getName().startsWith("trinket.")) {
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
            } else {
                String[] parts = entry.split(":", 2); // Use limit 2 to avoid issues with ':' in Base64
                if (parts.length < 2) continue;
                AccessoryType type = AccessoryType.valueOf(parts[0]);
                try {
                    ItemStack item = ItemSerializationUtils.itemStackFromBase64(parts[1]);
                    accessories.put(type, item);
                } catch (IOException e) {
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
}
