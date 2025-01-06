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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Q9SoulEffect implements Listener {

    private final TrinketsPlugin plugin;

    // cooldowns for levitation and turning mob to stone
    private final Map<UUID, Long> levitationCD = new HashMap<>();
    private final Map<UUID, Long> stoneCD = new HashMap<>();

    // track consecutive hits on mobs
    private final Map<UUID, Integer> hitsOnMob = new HashMap<>();

    public Q9SoulEffect(TrinketsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean hasQ9SoulEquipped(Player player) {
        PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (data == null) return false;

        ItemStack soul = data.getAccessory(AccessoryType.BOSS_SOUL);
        if (soul == null || !soul.hasItemMeta() || soul.getItemMeta().getDisplayName() == null) {
            return false;
        }
        String name = ChatColor.stripColor(soul.getItemMeta().getDisplayName());
        return name.contains("Medusa’s Petrifying Soul");
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player damager = (Player) event.getDamager();

        if (!hasQ9SoulEquipped(damager)) return;

        Entity target = event.getEntity();

        // 1) On player hit -> Levitation 2s, cd 20s
        if (target instanceof Player) {
            long now = System.currentTimeMillis();
            long nextUse = levitationCD.getOrDefault(damager.getUniqueId(), 0L);
            if (now >= nextUse) {
                Player pTarget = (Player) target;
                pTarget.sendMessage(ChatColor.GRAY + "[Q9] You have been levitated by Medusa’s power!");
                pTarget.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 40, 1)); // 2s
                damager.sendMessage(ChatColor.DARK_PURPLE + "[Q9] You inflicted levitation for 2s!");
                levitationCD.put(damager.getUniqueId(), now + 20_000);
            }
        }
        // 2) On mob hit -> after 3 hits, turn to stone for 1s, cd 10s
        else if (target instanceof Monster || (target instanceof LivingEntity && !(target instanceof Player))) {
            // increment hits
            int count = hitsOnMob.getOrDefault(damager.getUniqueId(), 0) + 1;
            hitsOnMob.put(damager.getUniqueId(), count);

            if (count >= 3) {
                long now = System.currentTimeMillis();
                long nextUse = stoneCD.getOrDefault(damager.getUniqueId(), 0L);
                if (now >= nextUse) {
                    LivingEntity mob = (LivingEntity) target;
                    mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 255)); // 1s root
                    damager.sendMessage(ChatColor.GREEN + "[Q9] You turned the mob to stone for 1s!");
                    stoneCD.put(damager.getUniqueId(), now + 10_000);
                }
                // reset hits
                hitsOnMob.put(damager.getUniqueId(), 0);
            }
        }
    }
}
