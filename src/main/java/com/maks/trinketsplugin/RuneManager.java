package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class RuneManager {

    private final TrinketsPlugin plugin;
    private final List<String> runeNames = Arrays.asList(
            "Uruz", "Algiz", "Shield", "Thurisaz", "Wunjo",
            "Laguz", "Gebo", "Ehwaz", "Berkano"
    );

    public RuneManager(TrinketsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isRune(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        return runeNames.stream().anyMatch(name::contains);
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
