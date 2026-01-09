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

        Material material = item.getType();
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

        // Check for Full Moon Trinket (PRISMARINE_SHARD + curse shard name)
        if (material == Material.PRISMARINE_SHARD && displayName.contains("curse shard")) {
            return true;
        }

        // Check for New Moon Trinket - Black Lord (BROWN_DYE + specific name)
        if (material == Material.BROWN_DYE &&
            (displayName.contains("heart of the black lord") ||
             (displayName.contains("black lord") && displayName.contains("malachai")))) {
            return true;
        }

        // Check for New Moon Trinket - White Lord (WHITE_DYE + specific name)
        if (material == Material.WHITE_DYE &&
            (displayName.contains("heart of the white lord") ||
             (displayName.contains("white lord") && displayName.contains("silvanus")))) {
            return true;
        }

        // Check for Winter Trinket - Sack of Gifts (BONE_MEAL + specific name)
        if (material == Material.BONE_MEAL && displayName.contains("sack of gifts")) {
            return true;
        }

        return false;
    }

    private void equipUniqueTrinket(Player player, ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }

        Material material = item.getType();
        String displayName = org.bukkit.ChatColor.stripColor(item.getItemMeta().getDisplayName()).toLowerCase();
        PlayerData data = TrinketsPlugin.getInstance().getDatabaseManager().getPlayerData(player.getUniqueId());

        UniqueTrinketType trinketType = null;

        // Determine trinket type
        if (displayName.contains("team relic")) {
            trinketType = UniqueTrinketType.TEAM_RELIC;
        } else if (displayName.contains("heart of")) {
            trinketType = UniqueTrinketType.BOSS_HEART;
        } else if (material == Material.PRISMARINE_SHARD && displayName.contains("curse shard")) {
            trinketType = UniqueTrinketType.FULL_MOON_SHARD;
        } else if (material == Material.BROWN_DYE &&
                   (displayName.contains("heart of the black lord") ||
                    (displayName.contains("black lord") && displayName.contains("malachai")))) {
            trinketType = UniqueTrinketType.NEW_MOON_SHARD;
        } else if (material == Material.WHITE_DYE &&
                   (displayName.contains("heart of the white lord") ||
                    (displayName.contains("white lord") && displayName.contains("silvanus")))) {
            trinketType = UniqueTrinketType.NEW_MOON_SHARD;
        } else if (material == Material.BONE_MEAL && displayName.contains("sack of gifts")) {
            trinketType = UniqueTrinketType.WINTER_TRINKET;
        }

        if (trinketType == null) {
            player.sendMessage("§cThis item cannot be equipped!");
            return;
        }

        // Prevent equipping stacked unique trinkets to avoid duplication/stacking issues
        if (item.getAmount() > 1) {
            player.sendMessage("§cUnique trinkets must be unstacked (amount = 1) before equipping.");
            return;
        }

        // Check if slot is already occupied
        ItemStack existing = data.getUniqueTrinket(trinketType);
        if (existing != null) {
            player.sendMessage("§cThis slot is already occupied! Unequip the current trinket first in the Unique Trinkets menu.");
            return;
        }
        
        // Equip new trinket
        data.setUniqueTrinket(trinketType, item.clone());
        
        // Apply attributes immediately (after removing old ones above)
        data.applyUniqueTrinketAttribute(player, item.clone(), trinketType);
        
        // Remove item from hand (amount is guaranteed to be 1 at this point)
        player.getInventory().setItemInMainHand(null);
        
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
