package com.maks.trinketsplugin;

import com.maks.trinketsplugin.TrinketsPlugin;
import com.maks.trinketsplugin.AccessoryType;
import com.maks.trinketsplugin.PlayerData;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class Q6SoulEffect implements Listener {

    private final TrinketsPlugin plugin;

    // "Hit => 50% weakness (5s), cd 15s"
    private final Map<UUID, Long> weaknessCooldown = new HashMap<>();

    // "Killing a mob => +2% stack (max 20 stacks), taking damage => lose 5 stacks"
    private final Map<UUID, Integer> killStacks = new HashMap<>();

    public Q6SoulEffect(TrinketsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean hasQ6SoulEquipped(Player player) {
        PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (data == null) return false;

        ItemStack soul = data.getAccessory(AccessoryType.BOSS_SOUL);
        if (soul == null || !soul.hasItemMeta() || soul.getItemMeta().getDisplayName() == null) {
            return false;
        }
        String name = ChatColor.stripColor(soul.getItemMeta().getDisplayName());
        return name.contains("Mortis’s Unchained Soul");
    }

    // 1) On hit => apply weakness 50% (5s), cd 15s
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player damager = (Player) event.getDamager();
        if (!hasQ6SoulEquipped(damager)) return;

        long now = System.currentTimeMillis();
        long nextUse = weaknessCooldown.getOrDefault(damager.getUniqueId(), 0L);
        if (now < nextUse) {
            // cooldown
        } else {
            // Apply weakness
            if (event.getEntity() instanceof Player) {
                Player pTarget = (Player) event.getEntity();
                pTarget.sendMessage(ChatColor.DARK_GRAY + "[Q6] You have been weakened by Mortis’s curse!");
                pTarget.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 4));
                // 5s, amplifier 4 => dość mocna weakness (nie zawsze 50% w prosty sposób).
            }
            // Możesz też obsłużyć moby, jeśli chcesz.
            damager.sendMessage(ChatColor.GRAY + "[Q6] You applied 50% weakness for 5s!");
            weaknessCooldown.put(damager.getUniqueId(), now + 15_000);
        }

        // Zwiększ dmg o stacks
        int stacks = killStacks.getOrDefault(damager.getUniqueId(), 0);
        if (stacks > 0) {
            double dmg = event.getDamage();
            double multiplier = 1.0 + (stacks * 0.02); // each stack = +2%
            event.setDamage(dmg * multiplier);
        }
    }

    // 2) Kill a mob => +1 stack (max 20)
    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Monster)) return;
        Monster mob = (Monster) event.getEntity();
        Player killer = mob.getKiller();
        if (killer == null) return;

        if (!hasQ6SoulEquipped(killer)) return;

        int current = killStacks.getOrDefault(killer.getUniqueId(), 0);
        if (current < 20) {
            current++;
        }
        killStacks.put(killer.getUniqueId(), current);
        killer.sendMessage(ChatColor.DARK_RED + "[Q6] Mortis’s power grows! Stacks: " + current);
    }

    // 3) If victim has Q6 => losing 5 stacks on damage
    @EventHandler
    public void onVictimDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        if (!hasQ6SoulEquipped(victim)) return;

        // lose 5 stacks
        int current = killStacks.getOrDefault(victim.getUniqueId(), 0);
        current -= 5;
        if (current < 0) current = 0;
        killStacks.put(victim.getUniqueId(), current);
        victim.sendMessage(ChatColor.RED + "[Q6] You lost 5 stacks! Now: " + current);
    }
}
