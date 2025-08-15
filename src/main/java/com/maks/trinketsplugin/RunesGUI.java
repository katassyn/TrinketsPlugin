package com.maks.trinketsplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class RunesGUI {

    public static void openRunesMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, "Runes");

        PlayerData data = TrinketsPlugin.getInstance().getDatabaseManager().getPlayerData(player.getUniqueId());
        List<ItemStack> runes = data.getRunes();
        int unlocked = getUnlockedSlots(player.getLevel());

        for (int i = 0; i < 9; i++) {
            if (i < unlocked) {
                if (i < runes.size() && runes.get(i) != null) {
                    ItemStack display = runes.get(i).clone();
                    display.setAmount(1);
                    gui.setItem(i, display);
                } else {
                    gui.setItem(i, createEmptySlot());
                }
            } else {
                int reqLevel = 50 + i * 5;
                gui.setItem(i, createLockedSlot(reqLevel));
            }
        }

        player.openInventory(gui);
    }

    private static int getUnlockedSlots(int level) {
        if (level < 50) {
            return 0;
        }
        int slots = 1 + (level - 50) / 5;
        return Math.min(slots, 9);
    }

    private static ItemStack createEmptySlot() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + "Empty Slot");
        meta.setLore(Arrays.asList(ChatColor.YELLOW + "Right-click a rune item", ChatColor.YELLOW + "to equip it."));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createLockedSlot(int level) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Locked Slot");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Unlock at level " + level));
        item.setItemMeta(meta);
        return item;
    }
}
