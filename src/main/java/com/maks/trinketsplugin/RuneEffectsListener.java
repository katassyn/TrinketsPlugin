package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class RuneEffectsListener implements Listener {

    private final TrinketsPlugin plugin;
    private final Map<UUID, Long> laguzCooldown = new HashMap<>();
    private final Map<UUID, Long> algizCooldown = new HashMap<>();
    private final Map<UUID, Long> algizActiveUntil = new HashMap<>();
    private final Map<UUID, Integer> algizReduction = new HashMap<>();
    private final Map<UUID, Long> shieldCooldown = new HashMap<>();
    private final Map<UUID, Long> geboCooldown = new HashMap<>();
    private final Map<UUID, Long> ehwazCooldown = new HashMap<>();
    private final Map<UUID, Long> berkanoCooldown = new HashMap<>();
    private final Map<UUID, Long> ehwazActiveUntil = new HashMap<>();
    private final Map<UUID, Integer> ehwazReduction = new HashMap<>();

    public RuneEffectsListener(TrinketsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Outgoing damage effects
        if (event.getDamager() instanceof Player damager) {
            PlayerData data = plugin.getDatabaseManager().getPlayerData(damager.getUniqueId());
            if (data != null) {
                int uruzBonus = getRuneEffect(data, "Uruz", 1, 2, 3);
                if (uruzBonus > 0) {
                    event.setDamage(event.getDamage() * (1 + uruzBonus / 100.0));
                }

                double laguzHeal = getRuneEffectDouble(data, "Laguz", 0.003, 0.006, 0.01);
                if (laguzHeal > 0) {
                    long now = System.currentTimeMillis();
                    long last = laguzCooldown.getOrDefault(damager.getUniqueId(), 0L);
                    if (now - last >= 500) {
                        double healAmt = event.getFinalDamage() * laguzHeal;
                        double max = damager.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                        damager.setHealth(Math.min(damager.getHealth() + healAmt, max));
                        laguzCooldown.put(damager.getUniqueId(), now);
                    }
                }
            }
        }

        // Incoming damage effects
        if (event.getEntity() instanceof Player victim) {
            PlayerData data = plugin.getDatabaseManager().getPlayerData(victim.getUniqueId());
            if (data != null) {
                int thurisaz = getRuneEffect(data, "Thurisaz", 2, 3, 4);
                if (thurisaz > 0 && event.getDamager() instanceof LivingEntity attacker) {
                    double reflect = event.getDamage() * thurisaz / 100.0;
                    attacker.damage(reflect, victim);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Apply Algiz reduction if active
        if (now < algizActiveUntil.getOrDefault(id, 0L)) {
            int reduction = algizReduction.getOrDefault(id, 0);
            event.setDamage(Math.max(0, event.getDamage() - reduction));
        }

        // Trigger Algiz buff if off cooldown
        long next = algizCooldown.getOrDefault(id, 0L);
        if (now >= next) {
            int value = getRuneEffect(data, "Algiz", 8, 12, 16);
            if (value > 0) {
                algizReduction.put(id, value);
                algizActiveUntil.put(id, now + 3000); // 3s duration
                algizCooldown.put(id, now + 10000); // 10s cooldown
            }
        }

        // Ehwaz damage reduction if active
        if (now < ehwazActiveUntil.getOrDefault(id, 0L)) {
            int red = ehwazReduction.getOrDefault(id, 0);
            event.setDamage(Math.max(0, event.getDamage() - red));
            ehwazActiveUntil.remove(id);
            ehwazReduction.remove(id);
        }

        double max = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double healthAfter = player.getHealth() - event.getFinalDamage();

        // Shield rune
        int shieldValue = getRuneEffect(data, "Shield", 200, 280, 360);
        if (shieldValue > 0 && healthAfter <= max * 0.3 && now >= shieldCooldown.getOrDefault(id, 0L)) {
            player.setAbsorptionAmount(player.getAbsorptionAmount() + shieldValue);
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    player.setAbsorptionAmount(Math.max(0, player.getAbsorptionAmount() - shieldValue)), 100L);
            shieldCooldown.put(id, now + 45000);
        }

        // Berkano rune
        int berkanoValue = getRuneEffect(data, "Berkano", 1, 1, 1);
        if (berkanoValue > 0 && healthAfter <= max * 0.3 && now >= berkanoCooldown.getOrDefault(id, 0L)) {
            for (PotionEffect effect : player.getActivePotionEffects()) {
                if (effect.getType().isBad()) {
                    player.removePotionEffect(effect.getType());
                    break;
                }
            }
            player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 60, 0, false, false));
            berkanoCooldown.put(id, now + 25000);
        }
    }

    @EventHandler
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        double max = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double newHealth = player.getHealth() + event.getAmount();
        if (newHealth > max) {
            long now = System.currentTimeMillis();
            int barrier = getRuneEffect(data, "Gebo", 30, 60, 100);
            if (barrier > 0 && now >= geboCooldown.getOrDefault(player.getUniqueId(), 0L)) {
                player.setAbsorptionAmount(player.getAbsorptionAmount() + barrier);
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        player.setAbsorptionAmount(Math.max(0, player.getAbsorptionAmount() - barrier)), 100L);
                geboCooldown.put(player.getUniqueId(), now + 15000);
            }
            event.setAmount(max - player.getHealth());
        }
    }

    private final Map<UUID, Long> sprintStart = new HashMap<>();

    @EventHandler
    public void onToggleSprint(PlayerToggleSprintEvent event) {
        if (event.isSprinting()) {
            sprintStart.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        } else {
            sprintStart.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (!player.isSprinting()) return;
        long start = sprintStart.getOrDefault(id, 0L);
        if (start == 0L) return;

        PlayerData data = plugin.getDatabaseManager().getPlayerData(id);
        if (data == null) return;

        long now = System.currentTimeMillis();
        if (now - start >= 2000 && now >= ehwazCooldown.getOrDefault(id, 0L)) {
            int reduction = getRuneEffect(data, "Ehwaz", 20, 35, 50);
            double kb = getRuneEffectDouble(data, "Ehwaz", 0.20, 0.30, 0.40);
            if (reduction > 0) {
                ehwazReduction.put(id, reduction);
                ehwazActiveUntil.put(id, now + 3000);
                ehwazCooldown.put(id, now + 8000);
                sprintStart.remove(id);
                AttributeInstance attr = player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
                if (attr != null && kb > 0) {
                    AttributeModifier mod = new AttributeModifier(UUID.randomUUID(),
                            "trinket.rune.ehwaz", kb, AttributeModifier.Operation.ADD_SCALAR);
                    attr.addModifier(mod);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> attr.removeModifier(mod), 60L);
                }
            }
        }
    }

    public void updateLuck(Player player) {
        PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_LUCK);
        if (attr == null) return;

        for (AttributeModifier mod : new ArrayList<>(attr.getModifiers())) {
            if (mod.getName().startsWith("trinket.rune.luck")) {
                attr.removeModifier(mod);
            }
        }

        int bonus = getRuneEffect(data, "Wunjo", 1, 2, 3);
        if (bonus > 0) {
            AttributeModifier mod = new AttributeModifier(UUID.randomUUID(),
                    "trinket.rune.luck", bonus, AttributeModifier.Operation.ADD_NUMBER);
            attr.addModifier(mod);
        }
    }

    private int getRuneEffect(PlayerData data, String runeName, int valI, int valII, int valIII) {
        int total = 0;
        for (ItemStack rune : data.getRunes()) {
            if (rune == null || !rune.hasItemMeta()) continue;
            String name = ChatColor.stripColor(rune.getItemMeta().getDisplayName());
            if (name == null || !name.contains(runeName)) continue;
            if (name.contains("[ III ]")) {
                total += valIII;
            } else if (name.contains("[ II ]")) {
                total += valII;
            } else {
                total += valI;
            }
        }
        return total;
    }

    private double getRuneEffectDouble(PlayerData data, String runeName, double valI, double valII, double valIII) {
        double total = 0;
        for (ItemStack rune : data.getRunes()) {
            if (rune == null || !rune.hasItemMeta()) continue;
            String name = ChatColor.stripColor(rune.getItemMeta().getDisplayName());
            if (name == null || !name.contains(runeName)) continue;
            if (name.contains("[ III ]")) {
                total += valIII;
            } else if (name.contains("[ II ]")) {
                total += valII;
            } else {
                total += valI;
            }
        }
        return total;
    }
}

