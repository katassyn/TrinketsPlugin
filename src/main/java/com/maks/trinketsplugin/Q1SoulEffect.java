package com.maks.trinketsplugin;

import com.maks.trinketsplugin.TrinketsPlugin;
import com.maks.trinketsplugin.AccessoryType;
import com.maks.trinketsplugin.PlayerData;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Q1SoulEffect implements Listener {

    private final TrinketsPlugin plugin;

    // Separate cooldowns for hitting mob vs hitting player
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
        String name = ChatColor.stripColor(soul.getItemMeta().getDisplayName());
        return name.contains("Grimmag’s Burning Soul");
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player damager = (Player) event.getDamager();

        if (!hasQ1SoulEquipped(damager)) return;

        Entity target = event.getEntity();
        long now = System.currentTimeMillis();

        // Mob
        if (target instanceof Monster || (target instanceof LivingEntity && !(target instanceof Player))) {
            Long nextUse = cooldownMob.getOrDefault(damager.getUniqueId(), 0L);
            if (now < nextUse) {
                return; // still on cooldown
            }
            cooldownMob.put(damager.getUniqueId(), now + 15_000); // 15s
            damager.sendMessage(ChatColor.RED + "[Q1] You ignited the mob for 5 seconds (1000 dmg/s)!");

            LivingEntity mob = (LivingEntity) target;
            // 5-second schedule
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
            }.runTaskTimer(plugin, 20, 20); // Start after 1s, repeat every 1s
        }

        // Player
        else if (target instanceof Player) {
            Long nextUse = cooldownPlayer.getOrDefault(damager.getUniqueId(), 0L);
            if (now < nextUse) {
                return;
            }
            cooldownPlayer.put(damager.getUniqueId(), now + 15_000);
            damager.sendMessage(ChatColor.RED + "[Q1] You ignited the player for 3 seconds (50 dmg/s)!");

            Player pTarget = (Player) target;
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
