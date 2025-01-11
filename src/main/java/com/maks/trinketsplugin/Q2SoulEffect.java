package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class Q2SoulEffect implements Listener {

    private final TrinketsPlugin plugin;

    public Q2SoulEffect(TrinketsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean hasQ2SoulEquipped(Player player) {
        PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (data == null) return false;

        ItemStack soul = data.getAccessory(AccessoryType.BOSS_SOUL);
        if (soul == null || !soul.hasItemMeta() || soul.getItemMeta().getDisplayName() == null) {
            return false;
        }
        String name = ChatColor.stripColor(soul.getItemMeta().getDisplayName());
        return name.contains("Arachna’s Venomous Soul");
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!hasQ2SoulEquipped(player)) return;

        ItemStack stack = event.getItem().getItemStack();
        if (!stack.hasItemMeta() || !stack.getItemMeta().hasLore()) {
            return;
        }

        boolean foundRare = false;
        for (String line : stack.getItemMeta().getLore()) {
            String cleaned = ChatColor.stripColor(line).toLowerCase();
            if (cleaned.contains("unique") || cleaned.contains("mythic")) {
                foundRare = true;
                break;
            }
        }

        if (foundRare) {
            Random r = new Random();
            if (r.nextInt(100) < 10) { // 10% chance
                // Duplikacja
                player.getInventory().addItem(stack.clone());
                // Drobny efekt wizualny:
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f);
                player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(0,1,0), 10, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Monster)) return;

        Player killer = entity.getKiller();
        if (killer == null) return;
        if (!hasQ2SoulEquipped(killer)) return;

        TrinketsPlugin.getEconomy().depositPlayer(killer, 10000);
        // Możesz tu zostawić krótką notkę, albo usunąć całkowicie:
        // killer.sendMessage(ChatColor.GREEN + "[Q2] +$10,000");
    }
}
