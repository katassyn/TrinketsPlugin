package com.maks.trinketsplugin;

import com.maks.trinketsplugin.TrinketsPlugin;
import com.maks.trinketsplugin.AccessoryType;
import com.maks.trinketsplugin.PlayerData;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Q5SoulEffect implements Listener {

    private final TrinketsPlugin plugin;

    // Kiedy można ponownie zrobić "evade"? <victimUUID, nextUsableTime>
    private final Map<UUID, Long> evadeCooldown = new HashMap<>();
    // Flaga na "następny atak = +300% dmg"
    // Wystarczy np. <playerUUID, Boolean> lub <playerUUID, Long> z czasem.
    private final Map<UUID, Boolean> nextCritFlag = new HashMap<>();

    public Q5SoulEffect(TrinketsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean hasQ5SoulEquipped(Player player) {
        PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (data == null) return false;

        ItemStack soul = data.getAccessory(AccessoryType.BOSS_SOUL);
        if (soul == null || !soul.hasItemMeta() || soul.getItemMeta().getDisplayName() == null) {
            return false;
        }
        String name = ChatColor.stripColor(soul.getItemMeta().getDisplayName());
        return name.contains("Khalys’s Shadowbound Soul");
    }

    @EventHandler
    public void onEntityDamageVictim(EntityDamageByEntityEvent event) {
        // Evade => victim side
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        if (!hasQ5SoulEquipped(victim)) return;

        long now = System.currentTimeMillis();
        long nextUse = evadeCooldown.getOrDefault(victim.getUniqueId(), 0L);
        if (now >= nextUse) {
            // Evade
            event.setDamage(0);
            victim.sendMessage(ChatColor.DARK_PURPLE + "[Q5] You have evaded all damage! Your next attack deals +300%.");

            // Ustaw cooldown 30s
            evadeCooldown.put(victim.getUniqueId(), now + 30_000);

            // Ustaw flagę nextCrit
            nextCritFlag.put(victim.getUniqueId(), true);
        }
    }

    @EventHandler
    public void onEntityDamageAttacker(EntityDamageByEntityEvent event) {
        // +300% => attacker side
        if (!(event.getDamager() instanceof Player)) return;
        Player damager = (Player) event.getDamager();
        if (!hasQ5SoulEquipped(damager)) return;

        if (nextCritFlag.getOrDefault(damager.getUniqueId(), false)) {
            double dmg = event.getDamage();
            event.setDamage(dmg * 4.0); // total x4 => +300%
            damager.sendMessage(ChatColor.DARK_PURPLE + "[Q5] Your attack dealt +300% damage!");
            nextCritFlag.put(damager.getUniqueId(), false);
        }
    }
}
