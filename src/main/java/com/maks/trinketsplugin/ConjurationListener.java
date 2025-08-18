package com.maks.trinketsplugin;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ConjurationListener implements Listener {

    private boolean isWeapon(Material type) {
        String name = type.name();
        return name.endsWith("_SWORD") || name.endsWith("_AXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE");
    }

    private void giveItem(Player player, ItemStack item) {
        if (item == null) return;
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack left : leftover.values()) {
            player.getWorld().dropItem(player.getLocation(), left);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory top = event.getView().getTopInventory();
        String title = event.getView().getTitle();

        if (title.equals(ConjurationGUI.TITLE_MAIN)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null) return;
            if (clicked.getType() == Material.DIAMOND_SWORD) {
                ConjurationGUI.openItemsMenu(player);
            } else if (clicked.getType() == Material.PAPER) {
                ConjurationGUI.openDispelMenu(player);
            }
        } else if (title.equals(ConjurationGUI.TITLE_ITEMS)) {
            if (event.getClickedInventory() == top) {
                int slot = event.getSlot();
                if (slot == 11 || slot == 15) {
                    event.setCancelled(false);
                } else if (slot == 22) {
                    event.setCancelled(true);
                    handleConjure(player, top);
                } else {
                    event.setCancelled(true);
                }
            }
        } else if (title.equals(ConjurationGUI.TITLE_DISPEL)) {
            if (event.getClickedInventory() == top) {
                int slot = event.getSlot();
                if (slot == 13) {
                    event.setCancelled(false);
                } else if (slot == 22) {
                    event.setCancelled(true);
                    handleDispel(player, top);
                } else {
                    event.setCancelled(true);
                }
            }
        }
    }

    private void handleConjure(Player player, Inventory inv) {
        ItemStack weapon = inv.getItem(11);
        ItemStack wordItem = inv.getItem(15);
        if (weapon == null || wordItem == null) {
            player.sendMessage(ChatColor.RED + "Place a weapon and a runic word.");
            return;
        }
        if (!isWeapon(weapon.getType())) {
            player.sendMessage(ChatColor.RED + "Item must be a weapon.");
            return;
        }
        RunicWord word = RunicWord.fromItem(wordItem);
        if (word == null) {
            player.sendMessage(ChatColor.RED + "Invalid runic word.");
            return;
        }
        if (RunicWordManager.getRunicWord(weapon) != null) {
            player.sendMessage(ChatColor.RED + "Weapon already has a runic word.");
            return;
        }

        Economy econ = TrinketsPlugin.getEconomy();
        double cost = 100_000_000d;
        if (econ.getBalance(player) < cost) {
            player.sendMessage(ChatColor.RED + "You need $100,000,000 to conjure." );
            return;
        }
        econ.withdrawPlayer(player, cost);
        RunicWordManager.applyRunicWord(weapon, word);
        inv.setItem(11, null);
        inv.setItem(15, null);
        giveItem(player, weapon);
        player.closeInventory();
        player.sendMessage(ChatColor.GREEN + "Weapon enchanted with " + word.getDisplayName() + "!");
    }

    private void handleDispel(Player player, Inventory inv) {
        ItemStack weapon = inv.getItem(13);
        if (weapon == null) {
            player.sendMessage(ChatColor.RED + "Place a weapon.");
            return;
        }
        RunicWord word = RunicWordManager.getRunicWord(weapon);
        if (word == null) {
            player.sendMessage(ChatColor.RED + "This weapon has no runic word.");
            return;
        }
        Economy econ = TrinketsPlugin.getEconomy();
        double cost = 250_000_000d;
        if (econ.getBalance(player) < cost) {
            player.sendMessage(ChatColor.RED + "You need $250,000,000 to dispel." );
            return;
        }
        econ.withdrawPlayer(player, cost);
        RunicWordManager.removeRunicWord(weapon);
        inv.setItem(13, null);
        giveItem(player, weapon);
        player.closeInventory();
        player.sendMessage(ChatColor.GREEN + "Runic word removed.");
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        Inventory inv = event.getInventory();
        String title = event.getView().getTitle();
        if (title.equals(ConjurationGUI.TITLE_ITEMS)) {
            giveItem(player, inv.getItem(11));
            giveItem(player, inv.getItem(15));
        } else if (title.equals(ConjurationGUI.TITLE_DISPEL)) {
            giveItem(player, inv.getItem(13));
        }
    }
}
