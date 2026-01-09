package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SetBonusEffectsListener implements Listener {

    private final TrinketsPlugin plugin;
    private final Random random = new Random();

    // Cooldown tracking
    private final Map<UUID, Long> whiteFrostT3Cooldowns = new HashMap<>();

    // Freeze tracking (from Q8SoulEffect pattern)
    private final Map<UUID, Double> originalSpeed = new HashMap<>();
    private final Map<UUID, Double> originalAttackSpeed = new HashMap<>();

    public SetBonusEffectsListener(TrinketsPlugin plugin) {
        this.plugin = plugin;
        startWhiteFrostT5AuraTask();
    }

    // ========== WHITE FROST T5: Absolute Zero Aura ==========
    // Runs every 5 seconds, freezes enemies in 5 block radius, heals +50 HP per enemy

    private void startWhiteFrostT5AuraTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (hasSetBonus(player, SetType.WHITE_FROST_T5, 4)) {
                        processAbsoluteZeroAura(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 100L); // Every 5 seconds (100 ticks)
    }

    private void processAbsoluteZeroAura(Player player) {
        Location loc = player.getLocation();
        int enemiesAffected = 0;

        // Get nearby entities within 5 blocks
        for (Entity entity : player.getWorld().getNearbyEntities(loc, 5.0, 5.0, 5.0)) {
            if (entity instanceof LivingEntity target && !entity.equals(player)) {
                // Only freeze mobs, NOT players (to avoid issues in towns/cities)
                if (!(target instanceof Player)) {
                    freezeTarget(target, 20); // 1 second freeze
                    enemiesAffected++;
                }
            }
        }

        if (enemiesAffected > 0) {
            // Heal +50 HP per enemy
            double healAmount = enemiesAffected * 50.0;
            double currentHealth = player.getHealth();
            double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            player.setHealth(Math.min(maxHealth, currentHealth + healAmount));

            // Visual feedback (particles and sound only, no chat message)
            player.getWorld().spawnParticle(Particle.BLOCK_CRACK,
                loc.clone().add(0, 1, 0), 50, 2.0, 2.0, 2.0, 0.1,
                org.bukkit.Material.ICE.createBlockData());
            player.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);
        }
    }

    // ========== BLACK FROST T5: Abyssal Execution ==========
    // +100% Damage to enemies <50% HP, heals +50 HP per hit

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlackFrostT5Attack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (!hasSetBonus(attacker, SetType.BLACK_FROST_T5, 4)) return;

        // Check if victim is below 50% HP
        double currentHealth = victim.getHealth();
        AttributeInstance maxHealthAttr = victim.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr == null) return;

        double maxHealth = maxHealthAttr.getValue();
        double healthPercent = currentHealth / maxHealth;

        if (healthPercent < 0.5) {
            // Double damage
            event.setDamage(event.getDamage() * 2.0);

            // Heal attacker +50 HP
            double attackerHealth = attacker.getHealth();
            AttributeInstance attackerMaxHealthAttr = attacker.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attackerMaxHealthAttr != null) {
                double attackerMaxHealth = attackerMaxHealthAttr.getValue();
                attacker.setHealth(Math.min(attackerMaxHealth, attackerHealth + 50.0));
            }

            // Visual: Red particles on victim (no chat message to avoid spam)
            Particle.DustOptions redDust = new Particle.DustOptions(Color.RED, 1.5f);
            victim.getWorld().spawnParticle(Particle.REDSTONE,
                victim.getLocation().clone().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1, redDust);
        }
    }

    // ========== WHITE FROST T3: Cryogenic Blast ==========
    // When hit: Explode frost in 8 block radius, knockback + freeze enemies, 10s cooldown

    @EventHandler(priority = EventPriority.HIGH)
    public void onWhiteFrostT3Damaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!hasSetBonus(victim, SetType.WHITE_FROST_T3, 4)) return;

        // Check cooldown (10 seconds)
        UUID victimId = victim.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastUse = whiteFrostT3Cooldowns.getOrDefault(victimId, 0L);

        if (currentTime - lastUse < 10000) {
            return; // On cooldown
        }

        // Trigger Cryogenic Blast
        whiteFrostT3Cooldowns.put(victimId, currentTime);

        Location loc = victim.getLocation();
        int enemiesHit = 0;

        // Get nearby entities within 8 blocks
        for (Entity entity : victim.getWorld().getNearbyEntities(loc, 8.0, 8.0, 8.0)) {
            if (entity instanceof LivingEntity target && !entity.equals(victim)) {
                // Knockback
                org.bukkit.util.Vector direction = target.getLocation().toVector()
                    .subtract(loc.toVector()).normalize();
                target.setVelocity(direction.multiply(1.5).setY(0.5)); // Strong knockback

                // Freeze for 2 seconds
                freezeTarget(target, 40);
                enemiesHit++;
            }
        }

        if (enemiesHit > 0) {
            // Visual: Ice explosion (no chat message to avoid spam)
            victim.getWorld().spawnParticle(Particle.BLOCK_CRACK,
                loc.clone().add(0, 1, 0), 100, 4.0, 2.0, 4.0, 0.2,
                org.bukkit.Material.ICE.createBlockData());
            victim.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.6f);
        }
    }

    // ========== BLACK FROST T3: Frost Reaper ==========
    // 25% chance to deal 200 True Damage (ignores armor)

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlackFrostT3Attack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (!hasSetBonus(attacker, SetType.BLACK_FROST_T3, 4)) return;

        // 25% chance
        if (random.nextInt(100) < 25) {
            // Deal 200 True Damage (add to existing damage - approximates bypassing armor)
            event.setDamage(event.getDamage() + 200.0);

            // Visual: Red/flame particles (no chat message to avoid spam)
            Particle.DustOptions darkRedDust = new Particle.DustOptions(Color.fromRGB(139, 0, 0), 2.0f);
            victim.getWorld().spawnParticle(Particle.REDSTONE,
                victim.getLocation().clone().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.1, darkRedDust);
            victim.getWorld().spawnParticle(Particle.FLAME,
                victim.getLocation().clone().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.05);
        }
    }

    // ========== HELPER METHODS ==========

    private boolean hasSetBonus(Player player, SetType setType, int requiredPieces) {
        SetBonusManager manager = plugin.getSetBonusManager();
        return manager != null && manager.hasSetBonus(player, setType, requiredPieces);
    }

    // Freeze implementation (borrowed from Q8SoulEffect pattern)
    private void freezeTarget(LivingEntity target, int ticks) {
        if (target instanceof Player p) {
            AttributeInstance speedAttr = p.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            AttributeInstance attackAttr = p.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
            if (speedAttr == null || attackAttr == null) return;

            originalSpeed.putIfAbsent(p.getUniqueId(), speedAttr.getBaseValue());
            originalAttackSpeed.putIfAbsent(p.getUniqueId(), attackAttr.getBaseValue());

            speedAttr.setBaseValue(0.0);
            attackAttr.setBaseValue(0.0);

            new BukkitRunnable() {
                @Override
                public void run() {
                    Double oldS = originalSpeed.remove(p.getUniqueId());
                    Double oldA = originalAttackSpeed.remove(p.getUniqueId());
                    if (oldS != null) {
                        AttributeInstance a = p.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
                        if (a != null) a.setBaseValue(oldS);
                    }
                    if (oldA != null) {
                        AttributeInstance a2 = p.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
                        if (a2 != null) a2.setBaseValue(oldA);
                    }
                }
            }.runTaskLater(plugin, ticks);
        } else {
            // Freeze mob (simplified)
            AttributeInstance speedAttr = target.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speedAttr == null) return;

            double oldSpeed = speedAttr.getBaseValue();
            speedAttr.setBaseValue(0.0);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!target.isDead()) {
                        AttributeInstance attr = target.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
                        if (attr != null) attr.setBaseValue(oldSpeed);
                    }
                }
            }.runTaskLater(plugin, ticks);
        }
    }
}
