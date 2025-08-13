package com.maks.trinketsplugin;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GemActionsListener implements Listener {

    private void normalizeRarityLine(List<String> lore) {
        boolean found = false;
        for (int i = 0; i < lore.size();) {
            String stripped = ChatColor.stripColor(lore.get(i));
            if (stripped.startsWith("Rarity:")) {
                if (!found) {
                    String line = lore.get(i);
                    int resetIndex = line.indexOf(ChatColor.RESET.toString());
                    String suffix = resetIndex >= 0 ? line.substring(resetIndex + ChatColor.RESET.toString().length()) : line.substring(line.indexOf(":") + 1);
                    lore.set(i, ChatColor.WHITE.toString() + ChatColor.BOLD + "Rarity:" + ChatColor.RESET + suffix);
                    found = true;
                    i++;
                } else {
                    lore.remove(i);
                }
            } else {
                i++;
            }
        }
    }

    private void giveItem(Player player, ItemStack item) {
        if (item == null) return;
        Map<Integer, ItemStack> left = player.getInventory().addItem(item);
        for (ItemStack leftover : left.values()) {
            player.getWorld().dropItem(player.getLocation(), leftover);
        }
    }

    private boolean isWeapon(Material type) {
        String name = type.name();
        return name.endsWith("_SWORD") || name.endsWith("_AXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE");
    }

    private boolean isArmor(Material type) {
        String name = type.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }

    private EquipmentSlot getArmorSlot(Material type) {
        String name = type.name();
        if (name.endsWith("_HELMET")) return EquipmentSlot.HEAD;
        if (name.endsWith("_CHESTPLATE")) return EquipmentSlot.CHEST;
        if (name.endsWith("_LEGGINGS")) return EquipmentSlot.LEGS;
        if (name.endsWith("_BOOTS")) return EquipmentSlot.FEET;
        return EquipmentSlot.CHEST;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory top = event.getView().getTopInventory();
        String title = event.getView().getTitle();

        if (title.equals(GemActionsGUI.TITLE_MAIN)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null) return;
            if (clicked.getType() == Material.EMERALD) {
                GemActionsGUI.openInsertMenu(player);
            } else if (clicked.getType() == Material.REDSTONE) {
                GemActionsGUI.openExtractMenu(player);
            }
        } else if (title.equals(GemActionsGUI.TITLE_INSERT)) {
            if (event.getClickedInventory() == top) {
                int slot = event.getSlot();
                if (slot == 11 || slot == 15) {
                    event.setCancelled(false);
                } else if (slot == 22) {
                    event.setCancelled(true);
                    handleInsertConfirm(player, top);
                } else {
                    event.setCancelled(true);
                }
            }
        } else if (title.equals(GemActionsGUI.TITLE_EXTRACT)) {
            if (event.getClickedInventory() == top) {
                int slot = event.getSlot();
                if (slot == 13) {
                    event.setCancelled(false);
                } else if (slot == 22) {
                    event.setCancelled(true);
                    handleExtractConfirm(player, top);
                } else {
                    event.setCancelled(true);
                }
            }
        }
    }

    private void handleInsertConfirm(Player player, Inventory inv) {
        ItemStack item = inv.getItem(11);
        ItemStack gemItem = inv.getItem(15);
        if (item == null || gemItem == null) {
            player.sendMessage(ChatColor.RED + "Place an item and a gem.");
            return;
        }
        if (gemItem.getAmount() > 1) {
            ItemStack leftover = gemItem.clone();
            leftover.setAmount(gemItem.getAmount() - 1);
            giveItem(player, leftover);
            gemItem.setAmount(1);
        }
        GemType gem = GemType.fromItem(gemItem);
        if (gem == null) {
            player.sendMessage(ChatColor.RED + "Invalid gem.");
            return;
        }
        boolean weapon = isWeapon(item.getType());
        boolean armor = isArmor(item.getType());
        if (!weapon && !armor) {
            player.sendMessage(ChatColor.RED + "Item must be a weapon or armor.");
            return;
        }
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta != null && meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        for (String line : lore) {
            if (ChatColor.stripColor(line).contains("Socketed")) {
                player.sendMessage(ChatColor.RED + "Item already has a gem.");
                return;
            }
        }
        String gemLore = gem.buildSocketLore(weapon);
        int insertIndex = lore.size();
        for (int i = 0; i < lore.size(); i++) {
            if (ChatColor.stripColor(lore.get(i)).contains("Rarity:")) {
                insertIndex = i;
                break;
            }
        }
        lore.add(insertIndex, gemLore);
        normalizeRarityLine(lore);
        if (meta != null) {
            meta.setLore(lore);
            EquipmentSlot slot = weapon ? EquipmentSlot.HAND : getArmorSlot(item.getType());
            gem.applyAttributes(meta, weapon, slot);
            item.setItemMeta(meta);
        }
        inv.setItem(11, null);
        inv.setItem(15, null);
        giveItem(player, item);
        player.closeInventory();
        player.sendMessage(ChatColor.GREEN + "Gem inserted!");
    }

    private void handleExtractConfirm(Player player, Inventory inv) {
        ItemStack item = inv.getItem(13);
        if (item == null) {
            player.sendMessage(ChatColor.RED + "Place an item with a gem.");
            return;
        }
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta != null && meta.hasLore() ? new ArrayList<>(meta.getLore()) : null;
        if (lore == null) {
            player.sendMessage(ChatColor.RED + "This item has no gem.");
            return;
        }
        GemType found = null;
        String removeLine = null;
        for (String line : lore) {
            String stripped = ChatColor.stripColor(line);
            if (!stripped.contains("Socketed")) continue;
            for (GemType type : GemType.values()) {
                if (stripped.startsWith(ChatColor.stripColor(type.getDisplay()))) {
                    found = type;
                    removeLine = line;
                    break;
                }
            }
            if (found != null) break;
        }
        if (found == null) {
            player.sendMessage(ChatColor.RED + "This item has no gem.");
            return;
        }
        Economy econ = TrinketsPlugin.getEconomy();
        double cost = 50_000_000d;
        if (econ.getBalance(player) < cost) {
            player.sendMessage(ChatColor.RED + "You need $50,000,000 to extract a gem.");
            return;
        }
        econ.withdrawPlayer(player, cost);
        lore.remove(removeLine);
        normalizeRarityLine(lore);
        if (meta != null) {
            meta.setLore(lore);
            found.removeAttributes(meta);
            item.setItemMeta(meta);
        }
        inv.setItem(13, null);
        giveItem(player, item);
        giveItem(player, found.createItem());
        player.closeInventory();
        player.sendMessage(ChatColor.GREEN + "Gem extracted!");
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        Inventory inv = event.getInventory();
        String title = event.getView().getTitle();
        if (title.equals(GemActionsGUI.TITLE_INSERT)) {
            giveItem(player, inv.getItem(11));
            giveItem(player, inv.getItem(15));
        } else if (title.equals(GemActionsGUI.TITLE_EXTRACT)) {
            giveItem(player, inv.getItem(13));
        }
    }
}
