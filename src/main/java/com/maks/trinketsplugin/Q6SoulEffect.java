package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Q6SoulEffect implements Listener {

    private final TrinketsPlugin plugin;

    // Co 15s możemy nałożyć Weakness
    private final Map<UUID, Long> weaknessCooldown = new HashMap<>();

    // Ilość stacków (1–20)
    private final Map<UUID, Integer> killStacks = new HashMap<>();

    // Przechowujemy oryginalną wartość ataku dla Weakness
    private final Map<UUID, Double> originalAttack = new HashMap<>();

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
        String name = ChatColor.stripColor(soul.getItemMeta().getDisplayName()).toLowerCase(Locale.ROOT);
        return name.contains("mortrix") && name.contains("unchained soul");
    }

    /**
     * Gracz (z Q6) atakuje innego gracza lub moba => nakładamy Weakness (50%) jeśli minął CD,
     * a także dodajemy bonusowe obrażenia w zależności od stacków.
     */
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!hasQ6SoulEquipped(damager)) return;

        long now = System.currentTimeMillis();
        long nextUse = weaknessCooldown.getOrDefault(damager.getUniqueId(), 0L);
        if (now >= nextUse) {
            // Tylko teraz nakładamy Weakness (co 15s)
            Entity victim = event.getEntity();
            if (victim instanceof Player victimPlayer) {
                applyWeakness(victimPlayer, 0.5, 5 * 20); // 5s
            }
            // (Jeśli chcesz także osłabiać moby, tu możesz dodać analogiczną obsługę.)

            weaknessCooldown.put(damager.getUniqueId(), now + 15_000);
        }

        // Bonus DMG za stacki
        int stacks = killStacks.getOrDefault(damager.getUniqueId(), 0);
        if (stacks > 0) {
            double multiplier = 1.0 + (stacks * 0.02); // +2% DMG per stack
            double newDmg = event.getDamage() * multiplier;
            event.setDamage(newDmg);
        }
    }

    /**
     * Zabicie moba => +1 stack do max 20. Wyświetlamy komunikat do czasu aż osiągniemy 20.
     */
    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Monster)) return;

        Player killer = entity.getKiller();
        if (killer == null) return;
        if (!hasQ6SoulEquipped(killer)) return;

        int current = killStacks.getOrDefault(killer.getUniqueId(), 0);
        if (current < 20) {
            current++;
            killStacks.put(killer.getUniqueId(), current);

            if (current < 20) {
                // Pokazujemy staki do 19 włącznie
                killer.sendMessage(ChatColor.DARK_RED + "[Q6] Stacks: " + current + " / 20");
               // killer.getWorld().playSound(killer.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1f, 1.2f);
            } else {
                // Dotarliśmy do 20
                killer.sendMessage(ChatColor.RED + "[Q6] You have reached MAX stacks (20)!");
                killer.getWorld().playSound(killer.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1f, 0.5f);
            }
        }
    }

    /**
     * Jeśli ofiara ma Q6 i otrzymuje obrażenia => traci 5 stacków. Wyświetlamy info o aktualnej liczbie.
     */
    @EventHandler
    public void onVictimDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!hasQ6SoulEquipped(victim)) return;

        int current = killStacks.getOrDefault(victim.getUniqueId(), 0);
        if (current > 0) {
            int oldStacks = current;
            current -= 5;
            if (current < 0) current = 0;
            killStacks.put(victim.getUniqueId(), current);

            // Komunikat, że gracz stracił stacki
            // (Pokazujemy tylko, jeśli faktycznie coś stracił)
            if (oldStacks != current) {
                victim.sendMessage(ChatColor.DARK_RED + "[Q6] You lost 5 stacks! Current: "
                        + current + " / 20");
            }
        }
    }

    /**
     * Nakładamy Weakness – obniżamy AttackDamage ofiary do 50% bazowej wartości
     * na 5s. Po tym czasie przywracamy starą wartość.
     */
    private void applyWeakness(Player target, double multiplier, int ticks) {
        AttributeInstance attr = target.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (attr == null) return;

        // Zachowaj oryginalną wartość, jeśli nie ma jej w mapie
        originalAttack.putIfAbsent(target.getUniqueId(), attr.getBaseValue());

        double newVal = attr.getBaseValue() * multiplier;
        attr.setBaseValue(newVal);

        // Efekty
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 0.8f);
        target.getWorld().spawnParticle(Particle.SQUID_INK,
                target.getLocation().add(0,1,0),
                15, 0.4, 0.4, 0.4, 0.01);

        // Po upływie 5s przywróć oryginalny atak
        new BukkitRunnable() {
            @Override
            public void run() {
                Double oldVal = originalAttack.remove(target.getUniqueId());
                if (oldVal != null) {
                    AttributeInstance a = target.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
                    if (a != null) {
                        a.setBaseValue(oldVal);
                    }
                }
            }
        }.runTaskLater(plugin, ticks);
    }
}
