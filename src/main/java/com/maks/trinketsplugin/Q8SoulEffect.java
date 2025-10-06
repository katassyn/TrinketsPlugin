package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
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

public class Q8SoulEffect implements Listener {

    private final TrinketsPlugin plugin;

    // countHits <attackerUUID, numberOfHits>
    private final Map<UUID, Integer> countHits = new HashMap<>();
    // freezeCooldown <attackerUUID, nextUsableTime>
    private final Map<UUID, Long> freezeCooldown = new HashMap<>();

    // do przechowania oryginalnej prędkości spowalnianego celu
    private final Map<UUID, Double> slowedTargets = new HashMap<>();

    public Q8SoulEffect(TrinketsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean hasQ8SoulEquipped(Player player) {
        PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (data == null) return false;

        ItemStack soul = data.getAccessory(AccessoryType.BOSS_SOUL);
        if (soul == null || !soul.hasItemMeta() || soul.getItemMeta().getDisplayName() == null) {
            return false;
        }
        String name = ChatColor.stripColor(soul.getItemMeta().getDisplayName()).toLowerCase(Locale.ROOT);
        return name.contains("sigrosmar") && name.contains("blizzard soul");
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!hasQ8SoulEquipped(damager)) return;

        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity target)) return;

        // 1) Spowalniamy target o 30% na 3s
        slowTarget(target, 0.70, 3 * 20);

        // 2) Liczymy ciosy
        int hits = countHits.getOrDefault(damager.getUniqueId(), 0) + 1;
        countHits.put(damager.getUniqueId(), hits);

        if (hits >= 5) {
            long now = System.currentTimeMillis();
            long nextUse = freezeCooldown.getOrDefault(damager.getUniqueId(), 0L);
            if (now >= nextUse) {
                // freeze: 1s + +300 dmg
                freezeTarget(target, 20); // 1s
                event.setDamage(event.getDamage() + 300);

                damager.getWorld().playSound(damager.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.8f);
                damager.getWorld().spawnParticle(Particle.BLOCK_CRACK, target.getLocation().add(0,1,0),
                        30, 0.3, 0.3, 0.3, 0.1, org.bukkit.Material.ICE.createBlockData());

                freezeCooldown.put(damager.getUniqueId(), now + 15_000);
            }
            // reset ciosów
            countHits.put(damager.getUniqueId(), 0);
        }
    }

    private void slowTarget(LivingEntity target, double multiplier, int ticks) {
        if (target instanceof Player p) {
            // Gracz
            AttributeInstance attr = p.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (attr == null) return;

            slowedTargets.putIfAbsent(p.getUniqueId(), attr.getBaseValue());
            double newVal = attr.getBaseValue() * multiplier;
            attr.setBaseValue(newVal);

            new BukkitRunnable() {
                @Override
                public void run() {
                    Double oldVal = slowedTargets.remove(p.getUniqueId());
                    if (oldVal != null) {
                        AttributeInstance a = p.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
                        if (a != null) a.setBaseValue(oldVal);
                    }
                }
            }.runTaskLater(plugin, ticks);
        } else {
            // Moby – analogicznie, jeśli mob ma atrybut prędkości
            AttributeInstance attr = target.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (attr == null) return;

            // Nieco trudniej z mobami, bo znikną i nie przywrócisz wartości.
            // Demo: nadpisujemy prędkość, potem ewentualnie przywracamy.
            double oldVal = attr.getBaseValue();
            attr.setBaseValue(oldVal * multiplier);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (target.isDead()) return;
                    AttributeInstance a = target.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
                    if (a != null) a.setBaseValue(oldVal);
                }
            }.runTaskLater(plugin, ticks);
        }
    }

    private final Map<UUID, Double> originalSpeed = new HashMap<>();
    private final Map<UUID, Double> originalAttackSpeed = new HashMap<>();

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
            // Freeze mob analogicznie
            AttributeInstance speedAttr = target.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            AttributeInstance attackAttr = target.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (speedAttr == null || attackAttr == null) return;

            double oldS = speedAttr.getBaseValue();
            double oldA = attackAttr.getBaseValue();

            speedAttr.setBaseValue(0.0);
            attackAttr.setBaseValue(0.0);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (target.isDead()) return;
                    speedAttr.setBaseValue(oldS);
                    attackAttr.setBaseValue(oldA);
                }
            }.runTaskLater(plugin, ticks);
        }
    }
}
