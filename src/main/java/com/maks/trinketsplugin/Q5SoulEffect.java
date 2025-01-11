package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Q5SoulEffect implements Listener {

    private final TrinketsPlugin plugin;

    // Evade cd
    private final Map<UUID, Long> evadeCooldown = new HashMap<>();
    // Flaga na +300% dmg
    private final Map<UUID, Boolean> nextCritFlag = new HashMap<>();

    public Q5SoulEffect(TrinketsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean hasQ5SoulEquipped(Player player) {
        PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (data == null) return false;

        ItemStack soul = data.getAccessory(AccessoryType.BOSS_SOUL);
        if (soul == null || !soul.hasItemMeta() || soul.getItemMeta().getDisplayName() == null) {
            return false;
        }
        String name = ChatColor.stripColor(soul.getItemMeta().getDisplayName());
        return name.contains("Khalys’s Shadowbound Soul");
    }

    // Ofiara = Evade
    @EventHandler
    public void onVictimDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!hasQ5SoulEquipped(victim)) return;

        long now = System.currentTimeMillis();
        long nextUse = evadeCooldown.getOrDefault(victim.getUniqueId(), 0L);

        if (now >= nextUse) {
            // Evade
            event.setDamage(0);
            evadeCooldown.put(victim.getUniqueId(), now + 30_000);

            // Następny atak +300%
            nextCritFlag.put(victim.getUniqueId(), true);

            // Efekty wizualne
            victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1.2f);
            victim.getWorld().spawnParticle(Particle.CRIT_MAGIC, victim.getLocation().add(0,1,0), 30, 0.5, 1, 0.5, 0.2);
        }
    }

    // Atakujący = +300%
    @EventHandler
    public void onAttackerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!hasQ5SoulEquipped(damager)) return;

        if (nextCritFlag.getOrDefault(damager.getUniqueId(), false)) {
            event.setDamage(event.getDamage() * 4.0); // +300% => *4
            nextCritFlag.put(damager.getUniqueId(), false);

            // Efekty
            damager.getWorld().playSound(damager.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.8f);
            damager.getWorld().spawnParticle(Particle.CRIT, damager.getLocation().add(0,1,0), 20, 0.3, 0.5, 0.3, 0.1);
        }
    }
}
