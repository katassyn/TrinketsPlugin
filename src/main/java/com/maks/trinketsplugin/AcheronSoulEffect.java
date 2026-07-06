package com.maks.trinketsplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Acheron's Primordial Soul (Floor IV Final Boss - THE ULTIMATE SOUL)
 *
 * Effects:
 * - Primordial Darkness: Immune to Blindness, Wither, Slowness, Poison
 * - Death's Embrace: When you would die, survive with 1 HP + 3s invulnerability + AoE fear on all enemies in 10 blocks (cd 120s)
 * - Shadow Reaper: Each kill heals you for 10% of max HP
 */
public class AcheronSoulEffect implements Listener {

    private final TrinketsPlugin plugin;

    private final Map<UUID, Long> deathEmbraceCooldown = new HashMap<>();
    private final Set<UUID> invulnerable = new HashSet<>();

    // Debuffs to cleanse
    private static final List<PotionEffectType> CLEANSED_EFFECTS = Arrays.asList(
        PotionEffectType.BLINDNESS,
        PotionEffectType.WITHER,
        PotionEffectType.SLOW,
        PotionEffectType.POISON
    );

    public AcheronSoulEffect(TrinketsPlugin plugin) {
        this.plugin = plugin;

        // Start task to cleanse debuffs from players with this soul
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (hasSoulEquipped(player)) {
                        cleansePrimordialDarkness(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 20, 20); // Check every second
    }

    private boolean hasSoulEquipped(Player player) {
        PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (data == null) return false;

        ItemStack soul = data.getAccessory(AccessoryType.BOSS_SOUL);
        if (soul == null || !soul.hasItemMeta() || soul.getItemMeta().getDisplayName() == null) {
            return false;
        }
        String name = ChatColor.stripColor(soul.getItemMeta().getDisplayName()).toLowerCase(Locale.ROOT);
        return name.contains("acheron") && name.contains("primordial soul");
    }

    /**
     * Primordial Darkness - Cleanse forbidden debuffs
     */
    private void cleansePrimordialDarkness(Player player) {
        boolean cleansed = false;
        for (PotionEffectType effect : CLEANSED_EFFECTS) {
            if (player.hasPotionEffect(effect)) {
                player.removePotionEffect(effect);
                cleansed = true;
            }
        }

        if (cleansed) {
            // Subtle visual feedback
            player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.02);
        }
    }

    /**
     * Death's Embrace - Prevent death
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!hasSoulEquipped(player)) return;

        // Check if player is invulnerable from Death's Embrace
        if (invulnerable.contains(player.getUniqueId())) {
            event.setCancelled(true);
            player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.02);
            return;
        }

        // Check if this damage would kill the player
        double damageAmount = event.getFinalDamage();
        double currentHealth = player.getHealth();

        if (damageAmount >= currentHealth) {
            long now = System.currentTimeMillis();
            long cdEnd = deathEmbraceCooldown.getOrDefault(player.getUniqueId(), 0L);

            if (now >= cdEnd) {
                // Activate Death's Embrace!
                event.setCancelled(true);
                deathEmbraceCooldown.put(player.getUniqueId(), now + 120_000); // 2 min cooldown

                // Set HP to 1
                player.setHealth(1.0);

                // Grant invulnerability for 3s
                invulnerable.add(player.getUniqueId());

                // Visual effects
                player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 1, 0), 50, 1, 1.5, 1, 0.1);
                player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 0.5, 0), 30, 1, 0.5, 1, 0.05);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 0.5f);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.3f);

                player.sendMessage(ChatColor.DARK_PURPLE + "💀 " + ChatColor.LIGHT_PURPLE + "Death's Embrace saves you from the abyss!");

                // Apply fear to all enemies in 10 blocks
                applyAoEFear(player);

                // Remove invulnerability after 3s
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        invulnerable.remove(player.getUniqueId());
                        player.sendMessage(ChatColor.GRAY + "💀 Your invulnerability fades...");
                    }
                }.runTaskLater(plugin, 60); // 3s
            }
        }
    }

    private void applyAoEFear(Player player) {
        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 10.0, 10.0, 10.0)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity.equals(player)) continue;

            // Visual fear effect
            living.getWorld().spawnParticle(Particle.SQUID_INK, living.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.02);
            living.getWorld().playSound(living.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.3f);

            if (living instanceof Player pTarget) {
                // Apply fear debuffs to players
                pTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 2)); // 3s, level 3
                pTarget.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0)); // 2s
                pTarget.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1)); // 3s
                pTarget.sendMessage(ChatColor.DARK_PURPLE + "☠ " + ChatColor.GRAY + "Primordial terror grips your soul!");
            } else if (living instanceof Mob mob) {
                // Make mobs flee
                mob.setTarget(null);
                org.bukkit.util.Vector direction = mob.getLocation().toVector()
                    .subtract(player.getLocation().toVector()).normalize().multiply(2.0);
                direction.setY(0.3);
                mob.setVelocity(direction);
            }
        }
    }

    /**
     * Shadow Reaper - Heal on kill
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (!hasSoulEquipped(killer)) return;

        // Heal 10% of max HP
        double maxHp = killer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double healAmount = maxHp * 0.10;
        double newHp = Math.min(killer.getHealth() + healAmount, maxHp);
        killer.setHealth(newHp);

        // Visual effect
        killer.getWorld().spawnParticle(Particle.SOUL, killer.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.05);
        killer.getWorld().spawnParticle(Particle.HEART, killer.getLocation().add(0, 1.5, 0), 3, 0.3, 0.3, 0.3, 0.01);
        killer.getWorld().playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.3f, 1.5f);
    }
}
