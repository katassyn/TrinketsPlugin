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

import java.util.*;

public class Q4SoulEffect implements Listener {

    private final TrinketsPlugin plugin;

    // rootCD – 15s
    private final Map<UUID, Long> rootCD = new HashMap<>();
    private final Random random = new Random();

    // Przechowujemy stare wartości atrybutów dla *każdego* LivingEntity
    private final Map<UUID, Double> oldSpeed = new HashMap<>();
    private final Map<UUID, Double> oldAttack = new HashMap<>();

    public Q4SoulEffect(TrinketsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean hasQ4SoulEquipped(Player player) {
        PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (data == null) return false;

        ItemStack soul = data.getAccessory(AccessoryType.BOSS_SOUL);
        if (soul == null || !soul.hasItemMeta() || soul.getItemMeta().getDisplayName() == null) {
            return false;
        }
        String name = ChatColor.stripColor(soul.getItemMeta().getDisplayName());
        return name.contains("Bearach’s Wildheart Soul");
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!hasQ4SoulEquipped(damager)) return;

        Entity target = event.getEntity();
        long now = System.currentTimeMillis();

        // 1) Root na 3s, cd 15s
        long nextRoot = rootCD.getOrDefault(damager.getUniqueId(), 0L);
        if (now >= nextRoot) {
            rootTarget(target, 3 * 20); // 3s = 60 ticków
            rootCD.put(damager.getUniqueId(), now + 15_000);

            // Efekt wizualny / dźwięk (root)
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 0.5f);
            target.getWorld().spawnParticle(
                    Particle.BLOCK_CRACK,
                    target.getLocation().add(0,1,0),
                    15,
                    0.3, 1, 0.3, 0.1,
                    org.bukkit.Material.COBBLESTONE.createBlockData()
            );
        }

        // 2) 15% szansy na obrócenie graczem o 90° (lub mobem, jeśli chcesz)
        if (random.nextInt(100) < 15) {
            if (target instanceof Player pTarget) {
                float oldYaw = pTarget.getLocation().getYaw();
                float pitch = pTarget.getLocation().getPitch();
                float newYaw = oldYaw + 90f;
                pTarget.setRotation(newYaw, pitch);

                // Efekt wizualny/dźwięk
                pTarget.getWorld().playSound(pTarget.getLocation(), Sound.ENTITY_PLAYER_BREATH, 1f, 0.5f);
                pTarget.getWorld().spawnParticle(Particle.SMOKE_LARGE, pTarget.getLocation().add(0,1,0), 10, 0.3, 0.3, 0.3, 0.01);

            }
        }
    }

    private void rootTarget(Entity entity, int ticks) {
        if (!(entity instanceof LivingEntity le)) return;

        // Dla gracza: speed=0, attackSpeed=0
        // Dla mobów: speed=0, attackDamage=0 (żeby nie uderzały)
        AttributeInstance speedAttr = le.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr == null) return;

        // Dla gracza => GENERIC_ATTACK_SPEED
        // Dla moba => GENERIC_ATTACK_DAMAGE (jeśli istnieje)
        AttributeInstance attackAttr = null;
        if (le instanceof Player) {
            attackAttr = le.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
        } else {
            attackAttr = le.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        }

        if (attackAttr == null) return;

        UUID uuid = le.getUniqueId();
        oldSpeed.putIfAbsent(uuid, speedAttr.getBaseValue());
        oldAttack.putIfAbsent(uuid, attackAttr.getBaseValue());

        speedAttr.setBaseValue(0.0);
        attackAttr.setBaseValue(0.0);

        new BukkitRunnable() {
            @Override
            public void run() {
                restoreRoot(uuid, le);
            }
        }.runTaskLater(plugin, ticks);
    }

    private void restoreRoot(UUID uuid, LivingEntity le) {
        // Przywracamy prędkości
        Double oldS = oldSpeed.remove(uuid);
        Double oldA = oldAttack.remove(uuid);
        if (oldS == null || oldA == null) return;

        if (le.isDead()) return;
        AttributeInstance speedAttr = le.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        AttributeInstance attackAttr = null;

        if (le instanceof Player) {
            attackAttr = le.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
        } else {
            attackAttr = le.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        }

        if (speedAttr != null) speedAttr.setBaseValue(oldS);
        if (attackAttr != null) attackAttr.setBaseValue(oldA);
    }
}
