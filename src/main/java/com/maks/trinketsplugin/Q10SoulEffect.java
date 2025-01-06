package com.maks.trinketsplugin;

import com.maks.trinketsplugin.TrinketsPlugin;
import com.maks.trinketsplugin.AccessoryType;
import com.maks.trinketsplugin.PlayerData;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
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

public class Q10SoulEffect implements Listener {

    private final TrinketsPlugin plugin;

    // short cd for blindness
    private final Map<UUID, Long> blindCD = new HashMap<>();
    // big cd for emergency heal
    private final Map<UUID, Long> healCD = new HashMap<>();
    // flag for +30% dmg
    private final Map<UUID, Long> dmgBuffActive = new HashMap<>();

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
        String name = ChatColor.stripColor(soul.getItemMeta().getDisplayName());
        return name.contains("Gorga’s Abyssal Soul");
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // 1) Blindness if attacker has Q10
        if (event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            if (hasQ10SoulEquipped(damager)) {
                long now = System.currentTimeMillis();
                long nextUse = blindCD.getOrDefault(damager.getUniqueId(), 0L);
                if (now >= nextUse && event.getEntity() instanceof Player) {
                    Player pTarget = (Player) event.getEntity();
                    pTarget.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0)); // 3s
                    damager.sendMessage(ChatColor.DARK_BLUE + "[Q10] You inflicted blindness for 3s!");
                    blindCD.put(damager.getUniqueId(), now + 5_000); // 5s cd
                }

                // check if we have 30% dmg buff active
                long buffExpire = dmgBuffActive.getOrDefault(damager.getUniqueId(), 0L);
                if (System.currentTimeMillis() < buffExpire) {
                    // +30%
                    double newDmg = event.getDamage() * 1.3;
                    event.setDamage(newDmg);
                }
            }
        }

        // 2) Emergency heal if victim has Q10
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            if (hasQ10SoulEquipped(victim)) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    // check HP after damage is applied
                    double maxHp = victim.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                    double currentHp = victim.getHealth();
                    double percent = (currentHp / maxHp) * 100.0;

                    if (percent < 30.0) {
                        // check big cd
                        long now = System.currentTimeMillis();
                        long nextUse = healCD.getOrDefault(victim.getUniqueId(), 0L);
                        if (now >= nextUse) {
                            victim.setHealth(maxHp);
                            victim.sendMessage(ChatColor.AQUA + "[Q10] Emergency heal triggered! +30% dmg for 5s!");
                            healCD.put(victim.getUniqueId(), now + 60_000);

                            // set 30% dmg buff for 5s
                            dmgBuffActive.put(victim.getUniqueId(), now + 5_000);
                        }
                    }
                }, 1L);
            }
        }
    }
}
