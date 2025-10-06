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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Q1SoulEffect implements Listener {

    private final TrinketsPlugin plugin;
    private final Map<UUID, Long> cooldownMob = new HashMap<>();
    private final Map<UUID, Long> cooldownPlayer = new HashMap<>();

    public Q1SoulEffect(TrinketsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean hasQ1SoulEquipped(Player player) {
        PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (data == null) return false;

        ItemStack soul = data.getAccessory(AccessoryType.BOSS_SOUL);
        if (soul == null || !soul.hasItemMeta() || soul.getItemMeta().getDisplayName() == null) {
            return false;
        }
        String name = ChatColor.stripColor(soul.getItemMeta().getDisplayName()).toLowerCase(Locale.ROOT);
        return name.contains("grimmor") && name.contains("burning soul");
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!hasQ1SoulEquipped(damager)) return;

        Entity target = event.getEntity();
        long now = System.currentTimeMillis();

        // Mob
        if (target instanceof Monster || (target instanceof LivingEntity && !(target instanceof Player))) {
            Long nextUse = cooldownMob.getOrDefault(damager.getUniqueId(), 0L);
            if (now < nextUse) {
                return; // cooldown wciąż trwa
            }
            cooldownMob.put(damager.getUniqueId(), now + 15_000);

            // Efekt wizualny uderzenia ogniem:
            target.getWorld().playSound(target.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1.2f);
            target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0,1,0), 20, 0.5, 1, 0.5, 0.01);

            LivingEntity mob = (LivingEntity) target;
            new BukkitRunnable() {
                int count = 0;
                @Override
                public void run() {
                    if (mob.isDead() || count >= 5) {
                        this.cancel();
                        return;
                    }
                    mob.damage(1000, damager);
                    count++;
                }
            }.runTaskTimer(plugin, 20, 20);
        }
        // Player
        else if (target instanceof Player pTarget) {
            Long nextUse = cooldownPlayer.getOrDefault(damager.getUniqueId(), 0L);
            if (now < nextUse) {
                return;
            }
            cooldownPlayer.put(damager.getUniqueId(), now + 15_000);

            // Efekt wizualny uderzenia ogniem:
            pTarget.getWorld().playSound(pTarget.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1f);
            pTarget.getWorld().spawnParticle(Particle.FLAME, pTarget.getLocation().add(0,1,0), 20, 0.5, 1, 0.5, 0.01);

            new BukkitRunnable() {
                int count = 0;
                @Override
                public void run() {
                    if (!pTarget.isOnline() || pTarget.isDead() || count >= 3) {
                        this.cancel();
                        return;
                    }
                    pTarget.damage(50, damager);
                    count++;
                }
            }.runTaskTimer(plugin, 20, 20);
        }
    }
}
