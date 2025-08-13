package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.google.common.collect.Multimap;

import java.util.*;

public enum GemType {
    RUBY_I(Material.COOKED_MUTTON, "&9[ I ] Ruby", "&6⚔ Weapon Socket: &a+1% Damage", "&b🛡 Armor Socket: &a+20 Damage"),
    RUBY_II(Material.COOKED_MUTTON, "&5[ II ] Ruby", "&6⚔ Weapon Socket: &a+2% Damage", "&b🛡 Armor Socket: &a+30 Damage"),
    RUBY_III(Material.COOKED_MUTTON, "&6[ III ] Ruby", "&6⚔ Weapon Socket: &a+3% Damage", "&b🛡 Armor Socket: &a+40 Damage"),

    AMETHYST_I(Material.COOKED_BEEF, "&9[ I ] Amethyst", "&6⚔ Weapon Socket: &a+1% Max Health", "&b🛡 Armor Socket: &a+5 Health"),
    AMETHYST_II(Material.COOKED_BEEF, "&5[ II ] Amethyst", "&6⚔ Weapon Socket: &a+2% Max Health", "&b🛡 Armor Socket: &a+10 Health"),
    AMETHYST_III(Material.COOKED_BEEF, "&6[ III ] Amethyst", "&6⚔ Weapon Socket: &a+3% Max Health", "&b🛡 Armor Socket: &a+15 Health"),

    CYIANITE_I(Material.PORKCHOP, "&9[ I ] Cyianite", "&6⚔ Weapon Socket: &a+1% Damage Reduction", "&b🛡 Armor Socket: &a+2 Armor"),
    CYIANITE_II(Material.PORKCHOP, "&5[ II ] Cyianite", "&6⚔ Weapon Socket: &a+2% Damage Reduction", "&b🛡 Armor Socket: &a+3 Armor"),
    CYIANITE_III(Material.PORKCHOP, "&6[ III ] Cyianite", "&6⚔ Weapon Socket: &a+3% Damage Reduction", "&b🛡 Armor Socket: &a+4 Armor"),

    ZIRCON_I(Material.COOKED_CHICKEN, "&9[ I ] Zircon", "&6⚔ Weapon Socket: &a+0.25 Attack Speed", "&b🛡 Armor Socket: &a+5% Attack Speed"),
    ZIRCON_II(Material.COOKED_CHICKEN, "&5[ II ] Zircon", "&6⚔ Weapon Socket: &a+0.5 Attack Speed", "&b🛡 Armor Socket: &a+10% Attack Speed"),
    ZIRCON_III(Material.COOKED_CHICKEN, "&6[ III ] Zircon", "&6⚔ Weapon Socket: &a+0.75 Attack Speed", "&b🛡 Armor Socket: &a+15% Attack Speed"),

    DIAMOND_I(Material.RABBIT, "&9[ I ] Diamond", "&6⚔ Weapon Socket: &a+3% Armor Penetration", "&b🛡 Armor Socket: &a+1 Armor Penetration"),
    DIAMOND_II(Material.RABBIT, "&5[ II ] Diamond", "&6⚔ Weapon Socket: &a+4% Armor Penetration", "&b🛡 Armor Socket: &a+2 Armor Penetration"),
    DIAMOND_III(Material.RABBIT, "&6[ III ] Diamond", "&6⚔ Weapon Socket: &a+5% Armor Penetration", "&b🛡 Armor Socket: &a+3 Armor Penetration"),

    RHODOLITE_I(Material.COOKED_RABBIT, "&9[ I ] Rhodolite", "&6⚔ Weapon Socket: &a+0.05 Move Speed", "&b🛡 Armor Socket: &a+1% Move Speed"),
    RHODOLITE_II(Material.COOKED_RABBIT, "&5[ II ] Rhodolite", "&6⚔ Weapon Socket: &a+0.1 Move Speed", "&b🛡 Armor Socket: &a+2% Move Speed"),
    RHODOLITE_III(Material.COOKED_RABBIT, "&6[ III ] Rhodolite", "&6⚔ Weapon Socket: &a+0.15 Move Speed", "&b🛡 Armor Socket: &a+3% Move Speed"),

    ONYX_I(Material.COD, "&9[ I ] Onyx", "&6⚔ Weapon Socket: &a+1% Luck", "&b🛡 Armor Socket: &a+1 Luck"),
    ONYX_II(Material.COD, "&5[ II ] Onyx", "&6⚔ Weapon Socket: &a+2% Luck", "&b🛡 Armor Socket: &a+2 Luck"),
    ONYX_III(Material.COD, "&6[ III ] Onyx", "&6⚔ Weapon Socket: &a+3% Luck", "&b🛡 Armor Socket: &a+3 Luck");

    private final Material material;
    private final String display;
    private final String weaponLore;
    private final String armorLore;

    GemType(Material material, String display, String weaponLore, String armorLore) {
        this.material = material;
        this.display = display;
        this.weaponLore = weaponLore;
        this.armorLore = armorLore;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplay() {
        return ChatColor.translateAlternateColorCodes('&', display);
    }

    public String getWeaponLore() {
        return ChatColor.translateAlternateColorCodes('&', weaponLore);
    }

    public String getArmorLore() {
        return ChatColor.translateAlternateColorCodes('&', armorLore);
    }

    public ItemStack createItem() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(getDisplay());
            meta.setLore(Arrays.asList(getWeaponLore(), getArmorLore()));
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }
        item.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
        return item;
    }

    public String buildSocketLore(boolean weapon) {
        String source = weapon ? weaponLore : armorLore;
        String[] parts = source.split(":", 2);
        String bonus = parts.length > 1 ? parts[1].trim() : source;
        return ChatColor.translateAlternateColorCodes('&', display + " Socketed " + bonus) + ChatColor.RESET;
    }

    public void applyAttributes(ItemMeta meta, boolean weapon, EquipmentSlot slot) {
        if (meta == null) return;
        Attribute attribute;
        double amount;
        AttributeModifier.Operation op;
        switch (this) {
            case RUBY_I:
                attribute = Attribute.GENERIC_ATTACK_DAMAGE;
                amount = weapon ? 0.01 : 20;
                op = weapon ? AttributeModifier.Operation.MULTIPLY_SCALAR_1 : AttributeModifier.Operation.ADD_NUMBER;
                break;
            case RUBY_II:
                attribute = Attribute.GENERIC_ATTACK_DAMAGE;
                amount = weapon ? 0.02 : 30;
                op = weapon ? AttributeModifier.Operation.MULTIPLY_SCALAR_1 : AttributeModifier.Operation.ADD_NUMBER;
                break;
            case RUBY_III:
                attribute = Attribute.GENERIC_ATTACK_DAMAGE;
                amount = weapon ? 0.03 : 40;
                op = weapon ? AttributeModifier.Operation.MULTIPLY_SCALAR_1 : AttributeModifier.Operation.ADD_NUMBER;
                break;

            case AMETHYST_I:
                attribute = Attribute.GENERIC_MAX_HEALTH;
                amount = weapon ? 0.01 : 5;
                op = weapon ? AttributeModifier.Operation.MULTIPLY_SCALAR_1 : AttributeModifier.Operation.ADD_NUMBER;
                break;
            case AMETHYST_II:
                attribute = Attribute.GENERIC_MAX_HEALTH;
                amount = weapon ? 0.02 : 10;
                op = weapon ? AttributeModifier.Operation.MULTIPLY_SCALAR_1 : AttributeModifier.Operation.ADD_NUMBER;
                break;
            case AMETHYST_III:
                attribute = Attribute.GENERIC_MAX_HEALTH;
                amount = weapon ? 0.03 : 15;
                op = weapon ? AttributeModifier.Operation.MULTIPLY_SCALAR_1 : AttributeModifier.Operation.ADD_NUMBER;
                break;

            case CYIANITE_I:
                attribute = Attribute.GENERIC_ARMOR;
                amount = weapon ? 0.01 : 2;
                op = weapon ? AttributeModifier.Operation.MULTIPLY_SCALAR_1 : AttributeModifier.Operation.ADD_NUMBER;
                break;
            case CYIANITE_II:
                attribute = Attribute.GENERIC_ARMOR;
                amount = weapon ? 0.02 : 3;
                op = weapon ? AttributeModifier.Operation.MULTIPLY_SCALAR_1 : AttributeModifier.Operation.ADD_NUMBER;
                break;
            case CYIANITE_III:
                attribute = Attribute.GENERIC_ARMOR;
                amount = weapon ? 0.03 : 4;
                op = weapon ? AttributeModifier.Operation.MULTIPLY_SCALAR_1 : AttributeModifier.Operation.ADD_NUMBER;
                break;

            case ZIRCON_I:
                attribute = Attribute.GENERIC_ATTACK_SPEED;
                amount = weapon ? 0.25 : 0.05;
                op = weapon ? AttributeModifier.Operation.ADD_NUMBER : AttributeModifier.Operation.MULTIPLY_SCALAR_1;
                break;
            case ZIRCON_II:
                attribute = Attribute.GENERIC_ATTACK_SPEED;
                amount = weapon ? 0.5 : 0.10;
                op = weapon ? AttributeModifier.Operation.ADD_NUMBER : AttributeModifier.Operation.MULTIPLY_SCALAR_1;
                break;
            case ZIRCON_III:
                attribute = Attribute.GENERIC_ATTACK_SPEED;
                amount = weapon ? 0.75 : 0.15;
                op = weapon ? AttributeModifier.Operation.ADD_NUMBER : AttributeModifier.Operation.MULTIPLY_SCALAR_1;
                break;

            case DIAMOND_I:
                attribute = Attribute.GENERIC_ATTACK_DAMAGE;
                amount = weapon ? 0.03 : 1;
                op = weapon ? AttributeModifier.Operation.MULTIPLY_SCALAR_1 : AttributeModifier.Operation.ADD_NUMBER;
                break;
            case DIAMOND_II:
                attribute = Attribute.GENERIC_ATTACK_DAMAGE;
                amount = weapon ? 0.04 : 2;
                op = weapon ? AttributeModifier.Operation.MULTIPLY_SCALAR_1 : AttributeModifier.Operation.ADD_NUMBER;
                break;
            case DIAMOND_III:
                attribute = Attribute.GENERIC_ATTACK_DAMAGE;
                amount = weapon ? 0.05 : 3;
                op = weapon ? AttributeModifier.Operation.MULTIPLY_SCALAR_1 : AttributeModifier.Operation.ADD_NUMBER;
                break;

            case RHODOLITE_I:
                attribute = Attribute.GENERIC_MOVEMENT_SPEED;
                amount = weapon ? 0.05 : 0.01;
                op = weapon ? AttributeModifier.Operation.ADD_NUMBER : AttributeModifier.Operation.MULTIPLY_SCALAR_1;
                break;
            case RHODOLITE_II:
                attribute = Attribute.GENERIC_MOVEMENT_SPEED;
                amount = weapon ? 0.1 : 0.02;
                op = weapon ? AttributeModifier.Operation.ADD_NUMBER : AttributeModifier.Operation.MULTIPLY_SCALAR_1;
                break;
            case RHODOLITE_III:
                attribute = Attribute.GENERIC_MOVEMENT_SPEED;
                amount = weapon ? 0.15 : 0.03;
                op = weapon ? AttributeModifier.Operation.ADD_NUMBER : AttributeModifier.Operation.MULTIPLY_SCALAR_1;
                break;

            case ONYX_I:
                attribute = Attribute.GENERIC_LUCK;
                amount = weapon ? 0.01 : 1;
                op = weapon ? AttributeModifier.Operation.MULTIPLY_SCALAR_1 : AttributeModifier.Operation.ADD_NUMBER;
                break;
            case ONYX_II:
                attribute = Attribute.GENERIC_LUCK;
                amount = weapon ? 0.02 : 2;
                op = weapon ? AttributeModifier.Operation.MULTIPLY_SCALAR_1 : AttributeModifier.Operation.ADD_NUMBER;
                break;
            case ONYX_III:
                attribute = Attribute.GENERIC_LUCK;
                amount = weapon ? 0.03 : 3;
                op = weapon ? AttributeModifier.Operation.MULTIPLY_SCALAR_1 : AttributeModifier.Operation.ADD_NUMBER;
                break;

            default:
                return;
        }
        AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "gem_" + name(), amount, op, slot);
        meta.addAttributeModifier(attribute, modifier);
    }

    public void removeAttributes(ItemMeta meta) {
        if (meta == null) return;
        Multimap<Attribute, AttributeModifier> modifiers = meta.getAttributeModifiers();
        if (modifiers == null) return;
        for (Attribute attribute : new HashSet<>(modifiers.keySet())) {
            for (AttributeModifier modifier : new ArrayList<>(modifiers.get(attribute))) {
                if (modifier.getName().equals("gem_" + name())) {
                    meta.removeAttributeModifier(attribute, modifier);
                }
            }
        }
    }

    public static GemType fromItem(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return null;
        String name = ChatColor.stripColor(meta.getDisplayName());
        for (GemType type : values()) {
            if (ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', type.display)).equals(name)) {
                return type;
            }
        }
        return null;
    }
}
