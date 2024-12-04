package com.maks.trinketsplugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlayerInteractListener implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
      //  Bukkit.getLogger().info("PlayerInteractEvent triggered by " + event.getPlayer().getName());
        Action action = event.getAction();
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if (isAccessoryItem(item)) {
                // Equip the accessory
                TrinketsPlugin.getInstance().getDatabaseManager().equipAccessory(player, item);
                event.setCancelled(true);
            }
        }
    }

    private boolean isAccessoryItem(ItemStack item) {
        // Check if the item matches any restricted accessories
        for (RestrictedAccessory accessory : TrinketsPlugin.getInstance().getDatabaseManager().getRestrictedAccessories()) {
            if (accessory.matches(item)) {
                return true;
            }
        }
        // Check if the material matches any AccessoryType
        Material itemMaterial = item.getType();
        for (AccessoryType type : AccessoryType.values()) {
            if (type.getMaterial() == itemMaterial) {
                return true;
            }
        }
        return false;
    }




}
