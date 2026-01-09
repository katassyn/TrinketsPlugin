package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class KrampusSoulEffect implements Listener {

    private final TrinketsPlugin plugin;
    private final Set<UUID> processingDamage = new HashSet<>();

    public KrampusSoulEffect(TrinketsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean hasKrampusSoulEquipped(Player player) {
        PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (data == null) return false;

        ItemStack soul = data.getAccessory(AccessoryType.BOSS_SOUL);
        if (soul == null || !soul.hasItemMeta() || soul.getItemMeta().getDisplayName() == null) {
            return false;
        }
        String name = ChatColor.stripColor(soul.getItemMeta().getDisplayName()).toLowerCase(Locale.ROOT);
        return name.contains("krampus") && name.contains("spirit of christmas");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;

        // Prevent recursive calls - if we're already processing damage for this player, skip
        if (processingDamage.contains(damager.getUniqueId())) return;

        if (!hasKrampusSoulEquipped(damager)) return;

        Entity primaryTarget = event.getEntity();
        if (!(primaryTarget instanceof LivingEntity)) return;

        double damage = event.getFinalDamage();

        // Mark as processing to prevent recursion
        processingDamage.add(damager.getUniqueId());
        try {
            // Chains of Misery - spread damage to nearby entities within 5 blocks
            primaryTarget.getWorld().getNearbyEntities(primaryTarget.getLocation(), 5.0, 5.0, 5.0).stream()
                .filter(entity -> entity instanceof LivingEntity)
                .filter(entity -> !entity.equals(primaryTarget))
                .filter(entity -> !entity.equals(damager))
                .filter(entity -> entity instanceof Player || !(entity instanceof Player))
                .forEach(entity -> {
                    LivingEntity target = (LivingEntity) entity;

                    // Apply the same damage
                    target.damage(damage, damager);

                    // Visual effects
                    target.getWorld().spawnParticle(Particle.SOUL, target.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.02);
                    target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_BREAK, 0.5f, 0.8f);
                });

            // Visual effect on primary target
            if (primaryTarget.getWorld().getNearbyEntities(primaryTarget.getLocation(), 5.0, 5.0, 5.0).stream()
                    .filter(entity -> entity instanceof LivingEntity)
                    .filter(entity -> !entity.equals(primaryTarget))
                    .filter(entity -> !entity.equals(damager))
                    .count() > 0) {

                primaryTarget.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, primaryTarget.getLocation().add(0, 1, 0), 15, 0.5, 1, 0.5, 0.05);
            }
        } finally {
            processingDamage.remove(damager.getUniqueId());
        }
    }
}
