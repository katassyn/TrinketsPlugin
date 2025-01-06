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

public class Q8SoulEffect implements Listener {

    private final TrinketsPlugin plugin;

    // countHits <attackerUUID, numberOfHits>
    private final Map<UUID, Integer> countHits = new HashMap<>();
    // freezeCooldown <attackerUUID, nextUsableTime>
    private final Map<UUID, Long> freezeCooldown = new HashMap<>();

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
        String name = ChatColor.stripColor(soul.getItemMeta().getDisplayName());
        return name.contains("Sigrismar’s Blizzard Soul");
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player damager = (Player) event.getDamager();
        if (!hasQ8SoulEquipped(damager)) return;

        // 1) Every attack slows target by 30%
        // W Vanilla Spigot SLOW(0) to ~15%. SLOW(1) to ~60%.
        // Możesz testować. Dam amplifier 1, bo 0 to za mało.
        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity) {
            ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1));
            // 3s slow
        }

        // 2) After 5 hits => freeze 1s + 300 dmg, cd 15s
        int hits = countHits.getOrDefault(damager.getUniqueId(), 0) + 1;
        countHits.put(damager.getUniqueId(), hits);

        if (hits >= 5) {
            long now = System.currentTimeMillis();
            long nextUse = freezeCooldown.getOrDefault(damager.getUniqueId(), 0L);
            if (now >= nextUse) {
                // apply freeze => SLOW(255) for 1s, and +300 dmg
                if (entity instanceof LivingEntity) {
                    LivingEntity liv = (LivingEntity) entity;
                    liv.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 255));
                    // 1s root
                    event.setDamage(event.getDamage() + 300);
                    damager.sendMessage(ChatColor.BLUE + "[Q8] You unleashed a blizzard freeze (300 bonus dmg)!");
                }
                freezeCooldown.put(damager.getUniqueId(), now + 15_000);
            }
            // reset hits
            countHits.put(damager.getUniqueId(), 0);
        }
    }
}
