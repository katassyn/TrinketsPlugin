package com.maks.trinketsplugin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.text.DecimalFormat;
import java.util.*;

public class AugmenterListener implements Listener {

    private final DecimalFormat df = new DecimalFormat("0.0");

    private void normalizeRarityLine(List<String> lore) {
        boolean found = false;
        for (int i = 0; i < lore.size(); ) {
            String stripped = ChatColor.stripColor(lore.get(i)).trim().toLowerCase();
            if (stripped.startsWith("rarity:")) {
                if (!found) {
                    String line = lore.get(i);
                    int idx = line.toLowerCase().indexOf("rarity:");
                    String suffix = line.substring(idx + "rarity:".length());
                    if (suffix.startsWith(ChatColor.RESET.toString())) {
                        suffix = suffix.substring(ChatColor.RESET.toString().length());
                    }
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

    private boolean isAccessory(ItemStack item) {
        if (item == null) return false;
        for (AccessoryType type : AccessoryType.values()) {
            if (type == AccessoryType.BOSS_SOUL) continue;
            if (type.getMaterial() == item.getType()) return true;
        }
        return false;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory top = event.getView().getTopInventory();
        String title = event.getView().getTitle();

        if (title.equals(AugmenterGUI.TITLE_MAIN)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null) return;
            if (clicked.getType() == Material.HONEY_BOTTLE) {
                AugmenterGUI.openApplyMenu(player);
            } else if (clicked.getType() == Material.MILK_BUCKET) {
                AugmenterGUI.openRemoveMenu(player);
            }
        } else if (title.equals(AugmenterGUI.TITLE_APPLY)) {
            if (event.getClickedInventory() == top) {
                int slot = event.getSlot();
                if (slot == 11 || slot == 15) {
                    event.setCancelled(false);
                } else if (slot == 22) {
                    event.setCancelled(true);
                    handleApplyConfirm(player, top);
                } else {
                    event.setCancelled(true);
                }
            }
        } else if (title.equals(AugmenterGUI.TITLE_REMOVE)) {
            if (event.getClickedInventory() == top) {
                int slot = event.getSlot();
                if (slot == 13) {
                    event.setCancelled(false);
                } else if (slot == 22) {
                    event.setCancelled(true);
                    handleRemoveConfirm(player, top);
                } else {
                    event.setCancelled(true);
                }
            }
        }
    }

    private void handleApplyConfirm(Player player, Inventory inv) {
        ItemStack item = inv.getItem(11);
        ItemStack honey = inv.getItem(15);
        if (item == null || honey == null) {
            player.sendMessage(ChatColor.RED + "Place an accessory and a honey bottle.");
            return;
        }
        if (!isAccessory(item)) {
            player.sendMessage(ChatColor.RED + "Invalid accessory.");
            return;
        }
        QualityHoney q = QualityHoney.fromItem(honey);
        if (q == null) {
            player.sendMessage(ChatColor.RED + "Invalid honey bottle.");
            return;
        }
        if (honey.getAmount() > 1) {
            ItemStack leftover = honey.clone();
            leftover.setAmount(honey.getAmount() - 1);
            giveItem(player, leftover);
            honey.setAmount(1);
        }
        double percent = q.roll();
        applyQuality(item, percent);
        inv.setItem(11, null);
        inv.setItem(15, null);
        giveItem(player, item);
        player.closeInventory();
        String formatted = df.format(percent);
        player.sendMessage(ChatColor.GREEN + "Applied quality: " + formatted + "%");
    }

    private void handleRemoveConfirm(Player player, Inventory inv) {
        ItemStack item = inv.getItem(13);
        if (item == null) {
            player.sendMessage(ChatColor.RED + "Place an item.");
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey percentKey = new NamespacedKey(TrinketsPlugin.getInstance(), "quality_percent");
        if (!pdc.has(percentKey, PersistentDataType.DOUBLE)) {
            player.sendMessage(ChatColor.RED + "This item has no quality.");
            return;
        }
        Economy econ = TrinketsPlugin.getEconomy();
        double cost = 25_000_000d;
        if (econ.getBalance(player) < cost) {
            player.sendMessage(ChatColor.RED + "You need $25,000,000 to remove quality.");
            return;
        }
        econ.withdrawPlayer(player, cost);
        removeQuality(item);
        inv.setItem(13, null);
        giveItem(player, item);
        player.closeInventory();
        player.sendMessage(ChatColor.GREEN + "Quality removed.");
    }

    private void applyQuality(ItemStack item, double percent) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey percentKey = new NamespacedKey(TrinketsPlugin.getInstance(), "quality_percent");
        pdc.set(percentKey, PersistentDataType.DOUBLE, percent);

        // Handle attributes
        Multimap<Attribute, AttributeModifier> existing = meta.getAttributeModifiers();
        Multimap<Attribute, AttributeModifier> adjusted = HashMultimap.create();
        if (existing != null) {
            for (Attribute attr : existing.keySet()) {
                for (AttributeModifier mod : existing.get(attr)) {
                    NamespacedKey baseKey = new NamespacedKey(TrinketsPlugin.getInstance(), "quality_base_" + attr.name());
                    double base = pdc.has(baseKey, PersistentDataType.DOUBLE) ?
                            pdc.get(baseKey, PersistentDataType.DOUBLE) : mod.getAmount();
                    pdc.set(baseKey, PersistentDataType.DOUBLE, base);
                    double newAmount = base * (1 + percent / 100.0);
                    AttributeModifier newMod = new AttributeModifier(mod.getUniqueId(), mod.getName(), newAmount, mod.getOperation(), mod.getSlot());
                    adjusted.put(attr, newMod);
                }
            }
        }
        meta.setAttributeModifiers(adjusted);

        // Handle lore
        List<String> baseLore;
        NamespacedKey loreKey = new NamespacedKey(TrinketsPlugin.getInstance(), "quality_base_lore");
        if (pdc.has(loreKey, PersistentDataType.STRING)) {
            baseLore = new ArrayList<>(Arrays.asList(pdc.get(loreKey, PersistentDataType.STRING).split("\\n")));
        } else {
            baseLore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            pdc.set(loreKey, PersistentDataType.STRING, String.join("\n", baseLore));
        }
        if (!baseLore.isEmpty() && ChatColor.stripColor(baseLore.get(0)).trim().isEmpty()) {
            baseLore.remove(0);
        }

        List<String> newLore = new ArrayList<>();
        ChatColor color = percent >= 0 ? ChatColor.GREEN : ChatColor.RED;
        newLore.add(ChatColor.GRAY + "Quality: " + color + df.format(percent) + "%");
        newLore.add("");
        for (String line : baseLore) {
            String stripped = ChatColor.stripColor(line);
            double number = 0.0;
            try {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(-?\\d+(?:\\.\\d+)?)").matcher(stripped);
                if (m.find()) {
                    number = Double.parseDouble(m.group(1));
                    double extra = number * percent / 100.0;
                    if (Math.abs(extra) > 0.0001) {
                        ChatColor bonusColor = extra >= 0 ? ChatColor.GREEN : ChatColor.RED;
                        line = line + " " + ChatColor.GOLD + "(" + bonusColor + (extra >= 0 ? "+" : "") + df.format(extra) + ChatColor.GOLD + ")";
                    }
                }
            } catch (Exception ignored) {
            }
            newLore.add(line);
        }
        normalizeRarityLine(newLore);
        meta.setLore(newLore);
        item.setItemMeta(meta);
    }

    private void removeQuality(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey percentKey = new NamespacedKey(TrinketsPlugin.getInstance(), "quality_percent");
        NamespacedKey loreKey = new NamespacedKey(TrinketsPlugin.getInstance(), "quality_base_lore");

        // Restore attributes
        Multimap<Attribute, AttributeModifier> restored = HashMultimap.create();
        for (Attribute attr : Attribute.values()) {
            NamespacedKey baseKey = new NamespacedKey(TrinketsPlugin.getInstance(), "quality_base_" + attr.name());
            if (pdc.has(baseKey, PersistentDataType.DOUBLE)) {
                double base = pdc.get(baseKey, PersistentDataType.DOUBLE);
                Multimap<Attribute, AttributeModifier> current = meta.getAttributeModifiers();
                if (current != null) {
                    for (AttributeModifier mod : current.get(attr)) {
                        if (mod.getName() != null) {
                            AttributeModifier newMod = new AttributeModifier(mod.getUniqueId(), mod.getName(), base, mod.getOperation(), mod.getSlot());
                            restored.put(attr, newMod);
                        }
                    }
                }
                pdc.remove(baseKey);
            }
        }
        if (!restored.isEmpty()) meta.setAttributeModifiers(restored);

        // Restore lore
        if (pdc.has(loreKey, PersistentDataType.STRING)) {
            String stored = pdc.get(loreKey, PersistentDataType.STRING);
            List<String> baseLore = new ArrayList<>(Arrays.asList(stored.split("\\n")));
            normalizeRarityLine(baseLore);
            meta.setLore(baseLore);
            pdc.remove(loreKey);
        } else {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            if (!lore.isEmpty()) {
                lore.remove(0);
                if (!lore.isEmpty() && lore.get(0).isEmpty()) lore.remove(0);
            }
            normalizeRarityLine(lore);
            meta.setLore(lore);
        }
        pdc.remove(percentKey);
        item.setItemMeta(meta);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        Inventory inv = event.getInventory();
        String title = event.getView().getTitle();
        if (title.equals(AugmenterGUI.TITLE_APPLY)) {
            giveItem(player, inv.getItem(11));
            giveItem(player, inv.getItem(15));
        } else if (title.equals(AugmenterGUI.TITLE_REMOVE)) {
            giveItem(player, inv.getItem(13));
        }
    }
}
