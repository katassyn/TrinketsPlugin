package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class Q7SoulEffect implements Listener {

    private final TrinketsPlugin plugin;

    public Q7SoulEffect(TrinketsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean hasQ7SoulEquipped(Player player) {
        PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (data == null) return false;

        ItemStack soul = data.getAccessory(AccessoryType.BOSS_SOUL);
        if (soul == null || !soul.hasItemMeta() || soul.getItemMeta().getDisplayName() == null) {
            return false;
        }
        String name = ChatColor.stripColor(soul.getItemMeta().getDisplayName());
        return name.contains("Herald’s Molten Soul");
    }

    @EventHandler
    public void onVictimDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!hasQ7SoulEquipped(victim)) return;

        double originalDmg = event.getDamage();
        // Zmniejszamy o 20%
        double reduced = originalDmg * 0.8;
        event.setDamage(reduced);

        // Reflect 20% do atakującego
        double reflect = originalDmg * 0.2;
        if (event.getDamager() instanceof LivingEntity attacker) {
            attacker.damage(reflect, victim);

            // Usuwamy dźwięk, zostawiamy particle
            // victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 0.7f);
            victim.getWorld().spawnParticle(Particle.LAVA,
                    victim.getLocation().add(0,1,0),
                    5, 0.5, 0.5, 0.5, 0.01);
        }
    }
}
