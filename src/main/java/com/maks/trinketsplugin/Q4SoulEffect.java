package com.maks.trinketsplugin;

import com.maks.trinketsplugin.TrinketsPlugin;
import com.maks.trinketsplugin.AccessoryType;
import com.maks.trinketsplugin.PlayerData;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Q4SoulEffect implements Listener {

    private final TrinketsPlugin plugin;

    // We have 2 separate cooldowns: rootCD (15s) and nauseaCD (30s)
    private final Map<UUID, Long> rootCooldown = new HashMap<>();
    private final Map<UUID, Long> nauseaCooldown = new HashMap<>();

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
        if (!(event.getDamager() instanceof Player)) return;
        Player damager = (Player) event.getDamager();

        if (!hasQ4SoulEquipped(damager)) return;

        Entity target = event.getEntity();
        long now = System.currentTimeMillis();

        // 1) Root (15s cd)
        Long nextRoot = rootCooldown.getOrDefault(damager.getUniqueId(), 0L);
        if (now >= nextRoot) {
            // We can root the target for 3s
            if (target instanceof LivingEntity) {
                ((LivingEntity) target).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 255));
                damager.sendMessage(ChatColor.GREEN + "[Q4] You rooted your target for 3s!");
            }
            rootCooldown.put(damager.getUniqueId(), now + 15_000);
        }

        // 2) Nausea on player (30s cd)
        if (target instanceof Player) {
            Long nextNausea = nauseaCooldown.getOrDefault(damager.getUniqueId(), 0L);
            if (now >= nextNausea) {
                Player pTarget = (Player) target;
                pTarget.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 100, 4)); // 5s
                damager.sendMessage(ChatColor.DARK_GREEN + "[Q4] You inflicted nausea for 5s!");
                pTarget.sendMessage(ChatColor.DARK_GREEN + "[Q4] You have been hit by Wildheart Soul!");
                nauseaCooldown.put(damager.getUniqueId(), now + 30_000);
            }
        }
    }
}
