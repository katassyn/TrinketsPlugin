package com.maks.trinketsplugin;

import com.google.common.collect.Multimap;
import org.bukkit.Material;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.*;

public class PlayerData {
    private Map<AccessoryType, Map<Attribute, AttributeModifier>> appliedModifiers = new HashMap<>();

    private EnumMap<AccessoryType, ItemStack> accessories = new EnumMap<>(AccessoryType.class);

    public ItemStack getAccessory(AccessoryType type) {
        return accessories.get(type);
    }

    public void setAccessory(AccessoryType type, ItemStack item) {
        accessories.put(type, item);
    }

    public void removeAccessory(AccessoryType type) {
        accessories.remove(type);
    }

    public void applyAttributes(Player player, ItemStack item, AccessoryType type) {
        if (item == null) return;

        // Usuń istniejące modyfikatory dla tego typu akcesorium
        removeAttributes(player, type);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Pobierz modyfikatory atrybutów z przedmiotu
        Multimap<Attribute, AttributeModifier> modifiers = meta.getAttributeModifiers();
        if (modifiers == null || modifiers.isEmpty()) return;

        Map<Attribute, AttributeModifier> applied = new HashMap<>();

        for (Attribute attribute : modifiers.keys()) {
            AttributeInstance attributeInstance = player.getAttribute(attribute);
            if (attributeInstance == null) continue;

            Collection<AttributeModifier> attrModifiers = modifiers.get(attribute);
            for (AttributeModifier modifier : attrModifiers) {
                // Tworzymy nowy modyfikator z unikalną nazwą i UUID
                String modifierName = "trinket." + type.name() + "." + modifier.getName();
                AttributeModifier newModifier = new AttributeModifier(UUID.randomUUID(), modifierName, modifier.getAmount(), modifier.getOperation(), modifier.getSlot());
                attributeInstance.addModifier(newModifier);
                applied.put(attribute, newModifier);
            }
        }

        // Zapisujemy zastosowane modyfikatory
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
        // Usuń wszystkie istniejące atrybuty dodane przez plugin
        removeAllAttributes(player);

        // Zastosuj atrybuty z aktualnie założonych akcesoriów
        for (Map.Entry<AccessoryType, ItemStack> entry : accessories.entrySet()) {
            AccessoryType type = entry.getKey();
            ItemStack item = entry.getValue();
            applyAttributes(player, item, type);
        }
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
        return sb.toString();
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


    public void deserialize(String data) {
        String[] entries = data.split(";");
        for (String entry : entries) {
            if (entry.isEmpty()) continue;
            String[] parts = entry.split(":", 2); // Używamy limitu 2, aby uniknąć problemów z ':' w Base64
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
