package com.maks.trinketsplugin;

import com.maks.trinketsplugin.TrinketsPlugin;
import com.maks.trinketsplugin.AccessoryType;
import com.maks.trinketsplugin.PlayerData;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class Q3SoulEffect implements Listener {

    private final TrinketsPlugin plugin;

    public Q3SoulEffect(TrinketsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean hasQ3SoulEquipped(Player player) {
        PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (data == null) return false;

        ItemStack soul = data.getAccessory(AccessoryType.BOSS_SOUL);
        if (soul == null || !soul.hasItemMeta() || soul.getItemMeta().getDisplayName() == null) {
            return false;
        }
        String name = ChatColor.stripColor(soul.getItemMeta().getDisplayName());
        return name.contains("King Heredur’s Frostbound Soul");
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Check if victim is a player
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();

        if (!hasQ3SoulEquipped(victim)) return;

        Random r = new Random();
        if (r.nextInt(100) < 10) { // 10% chance
            double originalDamage = event.getDamage();
            double newDamage = originalDamage * 0.5; // block 50%
            event.setDamage(newDamage);

            victim.sendMessage(ChatColor.AQUA + "[Q3] You have blocked 50% of the incoming damage!");

            // Now slow all entities in 10-block radius by ~20%
            for (Entity near : victim.getNearbyEntities(10, 10, 10)) {
                if (near instanceof Player) {
                    Player p = (Player) near;
                    p.sendMessage(ChatColor.BLUE + "[Q3] You've been slowed by Heredur’s frost!");
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 0));
                    // Amplifier 0 ~ 15% slow, if you want exactly 20%, you might need amplifier 1 or test.
                }
            }
        }
    }
}
