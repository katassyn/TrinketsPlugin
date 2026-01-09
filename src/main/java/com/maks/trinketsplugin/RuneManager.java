package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class RuneManager {

    private final TrinketsPlugin plugin;
    private final List<String> runeNames = Arrays.asList(
            "Uruz", "Algiz", "Shield", "Thurisaz", "Wunjo",
            "Laguz", "Gebo", "Ehwaz", "Berkano", "Winter Unity"
    );

    public RuneManager(TrinketsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isRune(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        for (String runeName : runeNames) {
            if (!name.contains(runeName)) {
                continue;
            }
            if ("Shield".equals(runeName) && item.getType() != Material.HEART_POTTERY_SHERD) {
                continue; // Shield rune must be crafted from the pottery sherd material
            }
            if ("Winter Unity".equals(runeName) && item.getType() != Material.MINER_POTTERY_SHERD) {
                continue; // Winter Unity rune must be MINER_POTTERY_SHERD
            }
            return true;
        }
        return false;
    }

    /**
     * Counts how many Winter Unity runes the player currently has equipped.
     * @param data Player's rune data
     * @return Count of Winter Unity runes (0-9)
     */
    private int countWinterUnityRunes(PlayerData data) {
        int count = 0;
        for (ItemStack rune : data.getRunes()) {
            if (rune == null || !rune.hasItemMeta()) continue;
            String name = ChatColor.stripColor(rune.getItemMeta().getDisplayName());
            if (name != null && name.contains("Winter Unity")) {
                count++;
            }
        }
        return count;
    }

    public void equipRune(Player player, ItemStack item) {
        if (!isRune(item)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getDatabaseManager().getPlayerData(uuid);
        if (data == null) {
            return;
        }

        // Validate Winter Unity limit (max 3)
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String itemName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            if (itemName != null && itemName.contains("Winter Unity")) {
                int currentCount = countWinterUnityRunes(data);
                if (currentCount >= 3) {
                    player.sendMessage(ChatColor.RED + "You can only equip up to 3 Winter Unity runes.");
                    return;
                }
            }
        }

        int unlocked = getUnlockedSlots(player.getLevel());
        if (data.getRunes().size() >= unlocked) {
            player.sendMessage(ChatColor.RED + "No available rune slots.");
            return;
        }

        ItemStack rune = item.clone();
        rune.setAmount(1);
        data.addRune(rune);
        plugin.getDatabaseManager().savePlayerData(uuid, data);
        plugin.getRuneEffectsListener().updateLuck(player);

        if (player.getInventory().getItemInMainHand().equals(item)) {
            ItemStack hand = player.getInventory().getItemInMainHand().clone();
            hand.setAmount(hand.getAmount() - 1);
            player.getInventory().setItemInMainHand(hand);
        } else if (player.getInventory().getItemInOffHand().equals(item)) {
            ItemStack off = player.getInventory().getItemInOffHand().clone();
            off.setAmount(off.getAmount() - 1);
            player.getInventory().setItemInOffHand(off);
        } else {
            item.setAmount(item.getAmount() - 1);
        }

        player.sendMessage(ChatColor.GREEN + "Rune equipped!");
    }

    private int getUnlockedSlots(int level) {
        if (level < 50) {
            return 0;
        }
        int slots = 1 + (level - 50) / 5;
        return Math.min(slots, 9);
    }
}
