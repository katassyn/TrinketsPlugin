package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Valgroth's Monarch Soul (Floor I - Dark Monarch)
 *
 * Effects:
 * - Shadow Dominion: Every 20s, attack inflicts Fear (mob flees 3s, player gets Slowness III + Blindness 2s)
 * - Monarch's Presence: 15% chance that nearby mobs (8 blocks) stop attacking you for 5s
 */
public class ValgrothSoulEffect implements Listener {

    private final TrinketsPlugin plugin;
    private final Map<UUID, Long> fearCooldown = new HashMap<>();
    private final Random random = new Random();

    // Store original speed values for mobs affected by Monarch's Presence
    private final Map<UUID, Double> oldMobSpeed = new HashMap<>();

    public ValgrothSoulEffect(TrinketsPlugin plugin) {
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
        return name.contains("valgroth") && name.contains("monarch soul");
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!hasSoulEquipped(damager)) return;

        Entity target = event.getEntity();
        if (!(target instanceof LivingEntity livingTarget)) return;

        long now = System.currentTimeMillis();

        // Shadow Dominion - Fear effect (cd 20s)
        long nextFear = fearCooldown.getOrDefault(damager.getUniqueId(), 0L);
        if (now >= nextFear) {
            applyFear(damager, livingTarget);
            fearCooldown.put(damager.getUniqueId(), now + 20_000);
        }

        // Monarch's Presence - 15% chance to pacify nearby mobs
        if (random.nextInt(100) < 15) {
            applyMonarchsPresence(damager, target);
        }
    }

    private void applyFear(Player damager, LivingEntity target) {
        // Visual effects
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1f, 0.5f);
        target.getWorld().spawnParticle(Particle.SMOKE_LARGE, target.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.05);
        target.getWorld().spawnParticle(Particle.SQUID_INK, target.getLocation().add(0, 1, 0), 20, 0.5, 1, 0.5, 0.02);

        if (target instanceof Player pTarget) {
            // Player: Slowness III + Blindness for 2s
            pTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 2)); // 2s, level 3
            pTarget.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0)); // 2s
            pTarget.sendMessage(ChatColor.DARK_PURPLE + "☠ " + ChatColor.GRAY + "You feel overwhelming fear from the Monarch's shadow!");
        } else if (target instanceof Mob mob) {
            // Mob: "flee" by setting speed to max and making them run away for 3s
            AttributeInstance speedAttr = mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speedAttr != null) {
                double originalSpeed = speedAttr.getBaseValue();
                oldMobSpeed.put(mob.getUniqueId(), originalSpeed);

                // Make mob run fast (simulating flee)
                speedAttr.setBaseValue(originalSpeed * 2.0);
                mob.setTarget(null); // Stop targeting player

                // Push mob away from player
                org.bukkit.util.Vector direction = mob.getLocation().toVector()
                    .subtract(damager.getLocation().toVector()).normalize().multiply(1.5);
                mob.setVelocity(direction);

                // Restore after 3s
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!mob.isDead()) {
                            Double oldSpeed = oldMobSpeed.remove(mob.getUniqueId());
                            if (oldSpeed != null && speedAttr != null) {
                                speedAttr.setBaseValue(oldSpeed);
                            }
                        }
                    }
                }.runTaskLater(plugin, 60); // 3s
            }
        }
    }

    private void applyMonarchsPresence(Player damager, Entity primaryTarget) {
        // Get nearby mobs within 8 blocks
        List<Mob> nearbyMobs = new ArrayList<>();
        for (Entity entity : primaryTarget.getWorld().getNearbyEntities(primaryTarget.getLocation(), 8.0, 8.0, 8.0)) {
            if (entity instanceof Mob mob && !entity.equals(primaryTarget)) {
                nearbyMobs.add(mob);
            }
        }

        if (nearbyMobs.isEmpty()) return;

        // Visual effect on damager
        damager.getWorld().spawnParticle(Particle.SMOKE_NORMAL, damager.getLocation().add(0, 2, 0), 20, 1, 0.5, 1, 0.01);
        damager.getWorld().playSound(damager.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.5f, 0.3f);

        for (Mob mob : nearbyMobs) {
            // Stop mob from targeting player for 5s
            if (mob.getTarget() != null && mob.getTarget().equals(damager)) {
                mob.setTarget(null);
            }

            // Freeze their movement temporarily
            AttributeInstance speedAttr = mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speedAttr != null) {
                UUID mobId = mob.getUniqueId();
                oldMobSpeed.putIfAbsent(mobId, speedAttr.getBaseValue());
                speedAttr.setBaseValue(0.0);

                // Visual effect on affected mobs
                mob.getWorld().spawnParticle(Particle.SQUID_INK, mob.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.01);

                // Restore after 5s
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!mob.isDead()) {
                            Double oldSpeed = oldMobSpeed.remove(mobId);
                            if (oldSpeed != null) {
                                AttributeInstance attr = mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
                                if (attr != null) {
                                    attr.setBaseValue(oldSpeed);
                                }
                            }
                        }
                    }
                }.runTaskLater(plugin, 100); // 5s
            }
        }
    }
}
