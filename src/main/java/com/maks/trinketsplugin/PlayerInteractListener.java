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
                TrinketsPlugin.getInstance().getOffhandListener().updateOffhand(player);
                event.setCancelled(true);
            } else if (isJewelItem(item)) {
                // Equip the jewel
                TrinketsPlugin.getInstance().getJewelManager().equipJewel(player, item);
                event.setCancelled(true);
            } else if (isRuneItem(item)) {
                TrinketsPlugin.getInstance().getRuneManager().equipRune(player, item);
                event.setCancelled(true);
            } else if (isUniqueTrinketItem(item)) {
                equipUniqueTrinket(player, item);
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

    private boolean isJewelItem(ItemStack item) {
        // Use JewelManager to check if the item is a jewel
        return TrinketsPlugin.getInstance().getJewelManager().isJewel(item);
    }

    private boolean isRuneItem(ItemStack item) {
        return TrinketsPlugin.getInstance().getRuneManager().isRune(item);
    }

    private boolean isUniqueTrinketItem(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }
        
        String displayName = org.bukkit.ChatColor.stripColor(item.getItemMeta().getDisplayName()).toLowerCase();
        
        // Check for Team Relics
        if (displayName.contains("team relic of hermes") || 
            displayName.contains("team relic of apollo") || 
            displayName.contains("team relic of athena") || 
            displayName.contains("team relic of ares") || 
            displayName.contains("team relic of zeus")) {
            return true;
        }
        
        // Check for Boss Hearts
        if (displayName.contains("heart of olympus") ||
            displayName.contains("heart of kronos") ||
            displayName.contains("heart of hades") ||
            displayName.contains("heart of poseidon") ||
            displayName.contains("heart of zeus")) {
            return true;
        }
        
        return false;
    }

    private void equipUniqueTrinket(Player player, ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }
        
        String displayName = org.bukkit.ChatColor.stripColor(item.getItemMeta().getDisplayName()).toLowerCase();
        PlayerData data = TrinketsPlugin.getInstance().getDatabaseManager().getPlayerData(player.getUniqueId());
        
        UniqueTrinketType trinketType = null;
        
        // Determine trinket type
        if (displayName.contains("team relic")) {
            trinketType = UniqueTrinketType.TEAM_RELIC;
        } else if (displayName.contains("heart of")) {
            trinketType = UniqueTrinketType.BOSS_HEART;
        }
        
        if (trinketType == null) {
            player.sendMessage("§cThis item cannot be equipped!");
            return;
        }
        
        // Check for existing trinket and return it to player
        ItemStack existing = data.getUniqueTrinket(trinketType);
        if (existing != null) {
            player.getInventory().addItem(existing);
        }
        
        // Equip new trinket
        data.setUniqueTrinket(trinketType, item.clone());
        
        // Apply attributes immediately
        data.applyUniqueTrinketAttribute(player, item.clone(), trinketType);
        
        // Remove item from hand
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getAmount() > 1) {
            itemInHand.setAmount(itemInHand.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
        
        // For team relics, check if player is in party and is leader
        if (trinketType == UniqueTrinketType.TEAM_RELIC) {
            if (!PartyAPIIntegration.isPartyLeader(player)) {
                player.sendMessage("§eTeam Relic equipped, but you must be a party leader for it to work!");
            } else {
                player.sendMessage("§aTeam Relic equipped and active!");
            }
        } else {
            player.sendMessage("§aUnique trinket equipped!");
        }
        
        // Save data
        TrinketsPlugin.getInstance().getDatabaseManager().savePlayerData(player.getUniqueId(), data);
    }
}
