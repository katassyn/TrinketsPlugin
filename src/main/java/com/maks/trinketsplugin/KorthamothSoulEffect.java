package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Korthamoth's Abyss Soul (Floor III - Void Guardian)
 *
 * Effects:
 * - Void Rift: Every 25s, attack creates a Void Zone - enemies within 4 blocks are pulled to center and take 300 dmg/s for 3s
 * - Abyssal Shield: When HP drops below 40%, teleport 5 blocks backward + 2s invisibility (cd 60s)
 */
public class KorthamothSoulEffect implements Listener {

    private final TrinketsPlugin plugin;

    private final Map<UUID, Long> voidRiftCooldown = new HashMap<>();
    private final Map<UUID, Long> abyssalShieldCooldown = new HashMap<>();

    public KorthamothSoulEffect(TrinketsPlugin plugin) {
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
        return name.contains("korthamoth") && name.contains("abyss soul");
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!hasSoulEquipped(damager)) return;

        Entity target = event.getEntity();
        if (!(target instanceof LivingEntity)) return;

        long now = System.currentTimeMillis();

        // Void Rift - cd 25s
        long nextVoidRift = voidRiftCooldown.getOrDefault(damager.getUniqueId(), 0L);
        if (now >= nextVoidRift) {
            voidRiftCooldown.put(damager.getUniqueId(), now + 25_000);
            applyVoidRift(damager, target.getLocation());
        }
    }

    @EventHandler
    public void onDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!hasSoulEquipped(victim)) return;

        // Check HP after damage is applied (next tick)
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (victim.isDead()) return;

            double maxHp = victim.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            double currentHp = victim.getHealth();
            double percent = (currentHp / maxHp) * 100.0;

            if (percent < 40.0) {
                long now = System.currentTimeMillis();
                long cdEnd = abyssalShieldCooldown.getOrDefault(victim.getUniqueId(), 0L);

                if (now >= cdEnd) {
                    abyssalShieldCooldown.put(victim.getUniqueId(), now + 60_000);
                    applyAbyssalShield(victim);
                }
            }
        }, 1L);
    }

    private void applyVoidRift(Player damager, Location center) {
        // Visual effects for void rift creation
        center.getWorld().playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 2f, 0.3f);
        center.getWorld().playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1f, 0.5f);

        // Create void rift visual effect and damage over 3 seconds
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 3) {
                    cancel();
                    return;
                }

                // Spawn portal particles in a circle
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                    double x = Math.cos(angle) * 4;
                    double z = Math.sin(angle) * 4;
                    center.getWorld().spawnParticle(Particle.PORTAL, center.clone().add(x, 0.5, z), 5, 0.1, 0.5, 0.1, 0.01);
                }

                // Center vortex particles
                center.getWorld().spawnParticle(Particle.REVERSE_PORTAL, center.clone().add(0, 1, 0), 30, 1.5, 1, 1.5, 0.05);
                center.getWorld().spawnParticle(Particle.SQUID_INK, center.clone().add(0, 0.5, 0), 15, 2, 0.5, 2, 0.01);

                // Pull and damage enemies within 4 blocks
                for (Entity entity : center.getWorld().getNearbyEntities(center, 4.0, 4.0, 4.0)) {
                    if (!(entity instanceof LivingEntity living)) continue;
                    if (entity.equals(damager)) continue;

                    // Pull toward center
                    Vector pull = center.toVector().subtract(living.getLocation().toVector()).normalize().multiply(0.5);
                    pull.setY(0.1); // Slight upward
                    living.setVelocity(pull);

                    // Deal 300 damage
                    living.damage(300, damager);

                    // Visual effect on victim
                    living.getWorld().spawnParticle(Particle.PORTAL, living.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.5);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 20); // Every second for 3 seconds

        damager.sendMessage(ChatColor.DARK_BLUE + "🌀 " + ChatColor.GRAY + "Void Rift tears through reality!");
    }

    private void applyAbyssalShield(Player victim) {
        // Calculate position 5 blocks behind player
        Vector direction = victim.getLocation().getDirection().normalize().multiply(-5);
        Location teleportLoc = victim.getLocation().add(direction);

        // Make sure it's safe (check for air)
        teleportLoc.setY(victim.getWorld().getHighestBlockYAt(teleportLoc) + 1);

        // Visual effect at original location
        victim.getWorld().spawnParticle(Particle.REVERSE_PORTAL, victim.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.1);
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);

        // Teleport
        victim.teleport(teleportLoc);

        // Visual effect at new location
        victim.getWorld().spawnParticle(Particle.PORTAL, victim.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.5);

        // Apply invisibility for 2s
        victim.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0)); // 2s

        victim.sendMessage(ChatColor.DARK_BLUE + "🌀 " + ChatColor.AQUA + "The Abyss shields you from harm!");
    }
}
