package com.maks.trinketsplugin;

import com.google.common.collect.Multimap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class OffhandListener implements Listener {
    private final TrinketsPlugin plugin;

    public OffhandListener(TrinketsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> updateOffhand(event.getPlayer()));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getSlotType() != InventoryType.SlotType.QUICKBAR
                || event.getSlot() != 40) return;

        Player player = (Player) event.getWhoClicked();
        Bukkit.getScheduler().runTask(plugin, () -> updateOffhand(player));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> updateOffhand(event.getPlayer()));
    }

    public void updateOffhand(Player player) {
        removeOffhandModifiers(player);

        ItemStack item = player.getInventory().getItemInOffHand();
        if (item == null || item.getType() == Material.AIR) return;
        if (!isAccessoryItem(item)) return;

        applyNegativeAttributes(player, item);
    }

    private boolean isAccessoryItem(ItemStack item) {
        for (RestrictedAccessory accessory : plugin.getDatabaseManager().getRestrictedAccessories()) {
            if (accessory.matches(item)) {
                return true;
            }
        }
        for (AccessoryType type : AccessoryType.values()) {
            if (type.getMaterial() == item.getType()) {
                return true;
            }
        }
        return false;
    }

    private void applyNegativeAttributes(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        Multimap<Attribute, AttributeModifier> modifiers = meta.getAttributeModifiers();
        if (modifiers == null) return;
        for (Attribute attribute : modifiers.keySet()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;
            
            Collection<AttributeModifier> attributeModifiers = modifiers.get(attribute);
            if (attributeModifiers == null || attributeModifiers.isEmpty()) continue;
            
            for (AttributeModifier modifier : attributeModifiers) {
                AttributeModifier negative = new AttributeModifier(
                        UUID.randomUUID(),
                        "trinket.offhand." + modifier.getName(),
                        -modifier.getAmount(),
                        modifier.getOperation(),
                        modifier.getSlot());
                instance.addModifier(negative);
            }
        }
    }

    private void removeOffhandModifiers(Player player) {
        for (Attribute attribute : Attribute.values()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;
            for (AttributeModifier modifier : new ArrayList<>(instance.getModifiers())) {
                if (modifier.getName().startsWith("trinket.offhand.")) {
                    instance.removeModifier(modifier);
                }
            }
        }
    }
}
