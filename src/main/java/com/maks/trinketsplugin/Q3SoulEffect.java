package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class Q3SoulEffect implements Listener {

    private final TrinketsPlugin plugin;

    // Przechowujemy oryginalną prędkość, by móc ją potem przywrócić
    // Key: UUID gracza, Value: poprzednia prędkość
    private final Map<UUID, Double> originalSpeeds = new HashMap<>();

    public Q3SoulEffect(TrinketsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean hasQ3SoulEquipped(Player player) {
        PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (data == null) return false;

        ItemStack soul = data.getAccessory(AccessoryType.BOSS_SOUL);
        if (soul == null || !soul.hasItemMeta() || soul.getItemMeta().getDisplayName() == null) {
            return false;
        }
        String name = ChatColor.stripColor(soul.getItemMeta().getDisplayName());
        return name.contains("King Heredur’s Frostbound Soul");
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!hasQ3SoulEquipped(victim)) return;

        // 10% szansy na zablokowanie 50% dmg
        if (new Random().nextInt(100) < 10) {
            event.setDamage(event.getDamage() * 0.5);

            // AoE slow w promieniu 10 bloków
            for (Entity near : victim.getNearbyEntities(10, 10, 10)) {
                if (near instanceof Player nearPlayer) {
                    applyTemporarySlow(nearPlayer, 0.25, 60); // 80% speed, na 60 ticków (3s)
                }
            }

            // Efekt wizualny
            victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_SNOW_BREAK, 1f, 1f);
            victim.getWorld().spawnParticle(Particle.SNOWFLAKE, victim.getLocation().add(0,1,0), 30, 1, 1, 1, 0.1);
        }
    }

    private void applyTemporarySlow(Player player, double multiplier, int durationTicks) {
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (attr == null) return;

        // Jeśli nie mamy zapisanego oryginalnego speeda, zapisz
        originalSpeeds.putIfAbsent(player.getUniqueId(), attr.getBaseValue());

        // Ustaw nową (zmniejszoną) wartość
        double newSpeed = originalSpeeds.get(player.getUniqueId()) * multiplier;
        attr.setBaseValue(newSpeed);

        // Po upływie 'durationTicks' przywróć oryginalną prędkość
        new BukkitRunnable() {
            @Override
            public void run() {
                Double oldSpeed = originalSpeeds.remove(player.getUniqueId());
                if (oldSpeed != null) {
                    AttributeInstance a = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
                    if (a != null) {
                        a.setBaseValue(oldSpeed);
                    }
                }
            }
        }.runTaskLater(plugin, durationTicks);
    }
}
