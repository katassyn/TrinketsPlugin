package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Vhar'Zhul's Ripper Soul (Floor IV - Ancient Slayer)
 *
 * Effects:
 * - Rend: Each attack applies a Bleed stack (max 5). At 5 stacks - burst damage (800 dmg) + reset (cd 10s on burst)
 * - Ancient Fury: When enemy is below 25% HP, deal +100% damage (execute mechanic)
 */
public class VharzhulSoulEffect implements Listener {

    private final TrinketsPlugin plugin;

    // Track bleed stacks per target (attacker UUID -> target UUID -> stacks)
    private final Map<UUID, Map<UUID, Integer>> bleedStacks = new HashMap<>();
    private final Map<UUID, Long> bleedBurstCooldown = new HashMap<>();

    public VharzhulSoulEffect(TrinketsPlugin plugin) {
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
        return name.contains("vhar") && name.contains("ripper soul");
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!hasSoulEquipped(damager)) return;

        Entity target = event.getEntity();
        if (!(target instanceof LivingEntity livingTarget)) return;

        UUID attackerId = damager.getUniqueId();
        UUID targetId = target.getUniqueId();
        long now = System.currentTimeMillis();

        // Ancient Fury - Execute mechanic (+100% damage when target below 25% HP)
        double maxHp = livingTarget.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null
            ? livingTarget.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()
            : 20.0;
        double currentHp = livingTarget.getHealth();
        double hpPercent = (currentHp / maxHp) * 100.0;

        if (hpPercent < 25.0) {
            // Double damage (execute)
            event.setDamage(event.getDamage() * 2.0);

            // Visual effect for execute
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.1);
            target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1.5, 0), 5, 0.2, 0.3, 0.2, 0.1);
        }

        // Rend - Apply bleed stacks
        Map<UUID, Integer> targetStacks = bleedStacks.computeIfAbsent(attackerId, k -> new HashMap<>());
        int currentStacks = targetStacks.getOrDefault(targetId, 0);
        currentStacks++;

        // Visual bleed effect
        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 3, 0.2, 0.3, 0.2, 0.01);

        if (currentStacks >= 5) {
            // Check cooldown for burst
            long cdEnd = bleedBurstCooldown.getOrDefault(attackerId, 0L);

            if (now >= cdEnd) {
                // Trigger bleed burst!
                bleedBurstCooldown.put(attackerId, now + 10_000);
                targetStacks.put(targetId, 0); // Reset stacks

                // Deal burst damage
                livingTarget.damage(800, damager);

                // Visual and sound effects for burst
                target.getWorld().spawnParticle(Particle.BLOCK_CRACK, target.getLocation().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.1,
                    org.bukkit.Material.REDSTONE_BLOCK.createBlockData());
                target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.2);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.5f);
                target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);

                damager.sendMessage(ChatColor.DARK_GRAY + "⚔ " + ChatColor.RED + "Rend explodes for massive damage!");

            } else {
                // On cooldown, just keep stacks at 5
                targetStacks.put(targetId, 5);
            }
        } else {
            targetStacks.put(targetId, currentStacks);

            // Notify at 3 and 4 stacks
            if (currentStacks == 3) {
                damager.sendMessage(ChatColor.GRAY + "⚔ Rend: " + ChatColor.RED + "███" + ChatColor.DARK_GRAY + "░░");
            } else if (currentStacks == 4) {
                damager.sendMessage(ChatColor.GRAY + "⚔ Rend: " + ChatColor.RED + "████" + ChatColor.DARK_GRAY + "░ " + ChatColor.YELLOW + "Almost there!");
            }
        }
    }
}
