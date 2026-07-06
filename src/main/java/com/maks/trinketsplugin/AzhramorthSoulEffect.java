package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Azhra'morth's Legion Soul (Floor II - Infernal Commander)
 *
 * Effects:
 * - Infernal Command: When killing an enemy, all enemies within 6 blocks get ignited for 4s (200 dmg/tick)
 * - Legion's Wrath: After killing 5 enemies within 30s, next attack deals AoE explosion (500 dmg, 5 block radius, cd 45s)
 */
public class AzhramorthSoulEffect implements Listener {

    private final TrinketsPlugin plugin;

    // Track kills for Legion's Wrath
    private final Map<UUID, List<Long>> recentKills = new HashMap<>();
    private final Map<UUID, Long> legionWrathCooldown = new HashMap<>();
    private final Map<UUID, Boolean> legionWrathReady = new HashMap<>();

    // Prevent recursive damage from AoE
    private final Set<UUID> processingDamage = new HashSet<>();

    public AzhramorthSoulEffect(TrinketsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean hasSoulEquipped(Player player) {
        PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (data == null) return false;

        ItemStack soul = data.getAccessory(AccessoryType.BOSS_SOUL);
        if (soul == null || !soul.hasItemMeta() || soul.getItemMeta().getDisplayName() == null) {
            return false;
        }
        String name = ChatColor.stripColor(soul.getItemMeta().getDisplayName()).toLowerCase(Locale.ROOT);
        return name.contains("azhra") && name.contains("legion soul");
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (!hasSoulEquipped(killer)) return;

        Entity deadEntity = event.getEntity();
        long now = System.currentTimeMillis();

        // Infernal Command - ignite nearby enemies
        applyInfernalCommand(killer, deadEntity);

        // Track kill for Legion's Wrath
        trackKill(killer.getUniqueId(), now);

        // Check if Legion's Wrath is ready
        checkLegionWrathReady(killer);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!hasSoulEquipped(damager)) return;
        if (processingDamage.contains(damager.getUniqueId())) return;

        // Check if Legion's Wrath is ready and should trigger
        if (legionWrathReady.getOrDefault(damager.getUniqueId(), false)) {
            long now = System.currentTimeMillis();
            long cdEnd = legionWrathCooldown.getOrDefault(damager.getUniqueId(), 0L);

            if (now >= cdEnd) {
                legionWrathReady.put(damager.getUniqueId(), false);
                legionWrathCooldown.put(damager.getUniqueId(), now + 45_000);

                // Trigger Legion's Wrath AoE explosion
                applyLegionWrath(damager, event.getEntity());
            }
        }
    }

    private void applyInfernalCommand(Player killer, Entity deadEntity) {
        // Visual effect on kill location
        deadEntity.getWorld().spawnParticle(Particle.FLAME, deadEntity.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.05);
        deadEntity.getWorld().spawnParticle(Particle.LAVA, deadEntity.getLocation().add(0, 0.5, 0), 10, 0.5, 0.5, 0.5, 0.01);
        deadEntity.getWorld().playSound(deadEntity.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.8f);

        // Get nearby enemies within 6 blocks and ignite them
        for (Entity entity : deadEntity.getWorld().getNearbyEntities(deadEntity.getLocation(), 6.0, 6.0, 6.0)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity.equals(killer)) continue;
            if (entity.equals(deadEntity)) continue;

            // Apply fire damage over time (4s, 200 dmg/tick = 200 dmg per second)
            living.setFireTicks(80); // 4s of visual fire

            // Spawn fire particles
            living.getWorld().spawnParticle(Particle.FLAME, living.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.02);

            // Deal fire damage over time
            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (living.isDead() || ticks >= 4) {
                        cancel();
                        return;
                    }
                    living.damage(200, killer);
                    living.getWorld().spawnParticle(Particle.FLAME, living.getLocation().add(0, 1, 0), 5, 0.2, 0.3, 0.2, 0.01);
                    ticks++;
                }
            }.runTaskTimer(plugin, 20, 20); // Every second for 4 seconds
        }
    }

    private void trackKill(UUID playerId, long timestamp) {
        List<Long> kills = recentKills.computeIfAbsent(playerId, k -> new ArrayList<>());

        // Remove kills older than 30s
        kills.removeIf(time -> timestamp - time > 30_000);

        // Add this kill
        kills.add(timestamp);
    }

    private void checkLegionWrathReady(Player player) {
        List<Long> kills = recentKills.get(player.getUniqueId());
        if (kills == null) return;

        // If 5+ kills in last 30s, Legion's Wrath is ready
        if (kills.size() >= 5) {
            if (!legionWrathReady.getOrDefault(player.getUniqueId(), false)) {
                legionWrathReady.put(player.getUniqueId(), true);
                player.sendMessage(ChatColor.DARK_RED + "🔥 " + ChatColor.GOLD + "Legion's Wrath is ready! Your next attack will explode!");
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1f, 0.5f);
            }
        }
    }

    private void applyLegionWrath(Player damager, Entity target) {
        processingDamage.add(damager.getUniqueId());

        try {
            // Visual explosion effect
            target.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, target.getLocation().add(0, 1, 0), 3, 0.5, 0.5, 0.5, 0.01);
            target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 100, 3, 2, 3, 0.1);
            target.getWorld().spawnParticle(Particle.LAVA, target.getLocation().add(0, 1, 0), 50, 2, 1, 2, 0.05);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.7f);

            damager.sendMessage(ChatColor.DARK_RED + "💥 " + ChatColor.RED + "Legion's Wrath unleashed!");

            // Deal 500 damage to all enemies within 5 blocks
            for (Entity entity : target.getWorld().getNearbyEntities(target.getLocation(), 5.0, 5.0, 5.0)) {
                if (!(entity instanceof LivingEntity living)) continue;
                if (entity.equals(damager)) continue;

                living.damage(500, damager);
                living.setFireTicks(40); // 2s fire

                // Knockback
                org.bukkit.util.Vector knockback = living.getLocation().toVector()
                    .subtract(target.getLocation().toVector()).normalize().multiply(1.0);
                knockback.setY(0.5);
                living.setVelocity(knockback);
            }

            // Clear kill counter
            recentKills.remove(damager.getUniqueId());

        } finally {
            processingDamage.remove(damager.getUniqueId());
        }
    }
}
