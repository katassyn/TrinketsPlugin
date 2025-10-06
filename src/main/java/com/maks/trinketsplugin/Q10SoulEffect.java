package com.maks.trinketsplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
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

public class Q10SoulEffect implements Listener {

    private final TrinketsPlugin plugin;

    // cd for blindness
    private final Map<UUID, Long> blindCD = new HashMap<>();
    // cd for emergency heal
    private final Map<UUID, Long> healCD = new HashMap<>();
    // buff end time
    private final Map<UUID, Long> dmgBuffUntil = new HashMap<>();

    public Q10SoulEffect(TrinketsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean hasQ10SoulEquipped(Player player) {
        PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (data == null) return false;

        ItemStack soul = data.getAccessory(AccessoryType.BOSS_SOUL);
        if (soul == null || !soul.hasItemMeta() || soul.getItemMeta().getDisplayName() == null) {
            return false;
        }
        String name = ChatColor.stripColor(soul.getItemMeta().getDisplayName()).toLowerCase(Locale.ROOT);
        return name.contains("gorgra") && name.contains("abyssal soul");
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // 1) Attacker side
        if (event.getDamager() instanceof Player damager) {
            if (hasQ10SoulEquipped(damager)) {
                long now = System.currentTimeMillis();
                long nextBlind = blindCD.getOrDefault(damager.getUniqueId(), 0L);
                if (now >= nextBlind && event.getEntity() instanceof Player pTarget) {
                    // Blindness (3s), cd 5s
                    pTarget.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.BLINDNESS, 60, 0
                    ));
                    blindCD.put(damager.getUniqueId(), now + 5_000);

                    // Efekty
                    pTarget.getWorld().playSound(pTarget.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1f, 0.5f);
                    pTarget.getWorld().spawnParticle(Particle.SMOKE_LARGE, pTarget.getLocation().add(0,1,0),
                            10, 0.4, 0.4, 0.4, 0.01);
                }

                // Sprawdź, czy mamy +30% dmg buff
                long buffEnd = dmgBuffUntil.getOrDefault(damager.getUniqueId(), 0L);
                if (System.currentTimeMillis() < buffEnd) {
                    event.setDamage(event.getDamage() * 1.3);
                    // Możesz dodać krótką animację
                }
            }
        }

        // 2) Victim side
        if (event.getEntity() instanceof Player victim) {
            if (hasQ10SoulEquipped(victim)) {
                // Po ticku sprawdź HP
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    double maxHp = victim.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                    double currentHp = victim.getHealth();
                    double percent = (currentHp / maxHp) * 100.0;

                    if (percent < 30.0) {
                        long now = System.currentTimeMillis();
                        long nextUse = healCD.getOrDefault(victim.getUniqueId(), 0L);
                        if (now >= nextUse) {
                            // Heal do full
                            victim.setHealth(maxHp);
                            // +30% dmg na 5s
                            dmgBuffUntil.put(victim.getUniqueId(), now + 5_000);
                            healCD.put(victim.getUniqueId(), now + 60_000);

                            // Efekty
                            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
                            victim.getWorld().spawnParticle(Particle.PORTAL, victim.getLocation().add(0,1,0), 40,
                                    0.5, 0.5, 0.5, 1);
                        }
                    }
                }, 1L);
            }
        }
    }
}
