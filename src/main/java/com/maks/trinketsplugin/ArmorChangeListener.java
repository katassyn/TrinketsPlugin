package com.maks.trinketsplugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.Bukkit;

public class ArmorChangeListener implements Listener {

    private final TrinketsPlugin plugin;

    public ArmorChangeListener(TrinketsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        // Check if the click involves armor slots
        boolean isArmorSlot = false;

        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            isArmorSlot = true;
        }

        // Check if shift-clicking equipment
        if (event.isShiftClick() && event.getCurrentItem() != null) {
            ItemStack item = event.getCurrentItem();
            String typeName = item.getType().name();
            if (typeName.endsWith("_HELMET") || typeName.endsWith("_CHESTPLATE") ||
                    typeName.endsWith("_LEGGINGS") || typeName.endsWith("_BOOTS")) {
                isArmorSlot = true;
            }
        }

        // Check if directly clicking on armor slots (slots 36-39 in player inventory)
        if (event.getClickedInventory() instanceof PlayerInventory &&
                event.getSlot() >= 36 && event.getSlot() <= 39) {
            isArmorSlot = true;
        }

        if (isArmorSlot) {
            // Schedule update for next tick to ensure the armor has changed
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                updateSetBonuses(player);
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        // Check if any of the drag slots are armor slots
        boolean hasArmorSlot = false;
        for (Integer slot : event.getRawSlots()) {
            if (slot >= 5 && slot <= 8) { // Armor slots in the player inventory view
                hasArmorSlot = true;
                break;
            }
        }

        if (hasArmorSlot) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                updateSetBonuses(player);
            }, 1L);
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getItem() == null) return;

        String typeName = event.getItem().getType().name();
        if (typeName.endsWith("_HELMET") || typeName.endsWith("_CHESTPLATE") ||
                typeName.endsWith("_LEGGINGS") || typeName.endsWith("_BOOTS")) {

            Player player = event.getPlayer();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                updateSetBonuses(player);
            }, 1L);
        }
    }

    private void updateSetBonuses(Player player) {
        PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (data != null) {
            plugin.getSetBonusManager().updatePlayerSetBonuses(player, data);
        }
    }
}