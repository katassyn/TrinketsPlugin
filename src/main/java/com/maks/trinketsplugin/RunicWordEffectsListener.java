package com.maks.trinketsplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Handles combat effects for weapons imbued with runic words.
 */
public class RunicWordEffectsListener implements Listener {

    private final TrinketsPlugin plugin;

    public RunicWordEffectsListener(TrinketsPlugin plugin) {
        this.plugin = plugin;
    }

    private final Map<String, Integer> tetherHits = new HashMap<>();
    private final Map<UUID, Long> tetherCooldown = new HashMap<>();

    private final Map<UUID, Long> surgicalSever = new HashMap<>();

    private final Map<UUID, Long> blessingCooldown = new HashMap<>();
    private static final Set<PotionEffectType> POSITIVE_EFFECTS = Set.of(
            PotionEffectType.ABSORPTION, PotionEffectType.DAMAGE_RESISTANCE, PotionEffectType.FAST_DIGGING,
            PotionEffectType.FIRE_RESISTANCE, PotionEffectType.GLOWING, PotionEffectType.HEALTH_BOOST,
            PotionEffectType.INCREASE_DAMAGE, PotionEffectType.INVISIBILITY, PotionEffectType.JUMP,
            PotionEffectType.NIGHT_VISION, PotionEffectType.REGENERATION, PotionEffectType.SPEED
    );

    private final Map<String, Integer> huntersHits = new HashMap<>();
    private final Map<String, Long> huntersFirstHit = new HashMap<>();
    private final Map<UUID, Long> huntersCooldown = new HashMap<>();

    private final Map<String, Integer> displacementHits = new HashMap<>();
    private final Map<UUID, Long> displacementCooldown = new HashMap<>();

    private final Map<String, Integer> rattleHits = new HashMap<>();
    private final Map<String, Long> rattleFirstHit = new HashMap<>();
    private final Map<UUID, Long> rattleCooldown = new HashMap<>();

    private final Map<UUID, Long> whiplashCooldown = new HashMap<>();

    private final Map<UUID, Long> mischiefCooldown = new HashMap<>();

    private static class OverWindow {
        UUID attacker;
        long expire;
    }
    private final Map<UUID, OverWindow> overextensionWindow = new HashMap<>();
    private final Map<UUID, Long> overextensionCooldown = new HashMap<>();

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        ItemStack weapon = damager.getInventory().getItemInMainHand();
        RunicWord word = RunicWordManager.getRunicWord(weapon);

        // Check level requirement
        if (word != null && damager.getLevel() < 80) {
            event.setCancelled(true);
            damager.sendMessage(ChatColor.RED + "You must be level 80 to use runic words.");
            return;
        }

        // Check for overextension counter when victim strikes back
        OverWindow win = overextensionWindow.get(damager.getUniqueId());
        if (win != null && win.attacker.equals(victim.getUniqueId()) && System.currentTimeMillis() <= win.expire) {
            event.setCancelled(true);
            overextensionWindow.remove(damager.getUniqueId());
            Vector kb = damager.getLocation().toVector().subtract(victim.getLocation().toVector()).normalize().multiply(1.5);
            damager.setVelocity(kb);
            return;
        }

        if (word == null) return; // no runic word

        switch (word) {
            case RUNIC_TETHER -> handleTether(damager, victim);
            case SURGICAL_SEVER -> handleSurgicalSever(damager, victim);
            case BLESSING_THEFT -> handleBlessingTheft(damager, victim);
            case HUNTERS_MARK -> handleHuntersMark(damager, victim);
            case RHYTHMIC_DISPLACEMENT -> handleDisplacement(damager, victim);
            case CROSSHAIR_RATTLE -> handleRattle(damager, victim);
            case WHIPLASH_SPRINT -> handleWhiplash(damager, victim);
            case MISCHIEF -> handleMischief(damager, victim);
            case OVEREXTENSION -> handleOverextension(damager, victim);
        }
    }

    @EventHandler
    public void onRegain(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        long until = surgicalSever.getOrDefault(player.getUniqueId(), 0L);
        if (until > System.currentTimeMillis()) {
            event.setAmount(event.getAmount() * 0.5);
        }
    }

    private String key(Player a, Player b) {
        return a.getUniqueId() + ":" + b.getUniqueId();
    }

    private void handleTether(Player damager, Player victim) {
        String key = key(damager, victim);
        int hits = tetherHits.getOrDefault(key, 0) + 1;
        tetherHits.put(key, hits);
        long now = System.currentTimeMillis();
        if (hits >= 2) {
            tetherHits.remove(key);
            if (now < tetherCooldown.getOrDefault(damager.getUniqueId(), 0L)) return;
            tetherCooldown.put(damager.getUniqueId(), now + 20000L);
            Location anchor = damager.getLocation().clone();
            new BukkitRunnable() {
                final long end = System.currentTimeMillis() + 3000L;
                @Override
                public void run() {
                    if (System.currentTimeMillis() >= end || victim.isDead()) { cancel(); return; }
                    if (victim.getLocation().distanceSquared(anchor) > 49) {
                        victim.teleport(anchor);
                    }
                }
            }.runTaskTimer(plugin, 0L, 5L);
        }
    }

    private void handleSurgicalSever(Player damager, Player victim) {
        surgicalSever.put(victim.getUniqueId(), System.currentTimeMillis() + 3000L);
    }

    private void handleBlessingTheft(Player damager, Player victim) {
        long now = System.currentTimeMillis();
        if (now < blessingCooldown.getOrDefault(damager.getUniqueId(), 0L)) return;
        List<PotionEffect> effects = new ArrayList<>();
        for (PotionEffect eff : victim.getActivePotionEffects()) {
            if (POSITIVE_EFFECTS.contains(eff.getType())) effects.add(eff);
        }
        if (effects.isEmpty()) return;
        PotionEffect chosen = effects.get(new Random().nextInt(effects.size()));
        victim.removePotionEffect(chosen.getType());
        damager.addPotionEffect(new PotionEffect(chosen.getType(), 100, chosen.getAmplifier()));
        blessingCooldown.put(damager.getUniqueId(), now + 22000L);
    }

    private void handleHuntersMark(Player damager, Player victim) {
        long now = System.currentTimeMillis();
        if (now < huntersCooldown.getOrDefault(damager.getUniqueId(), 0L)) return;
        String key = key(damager, victim);
        long first = huntersFirstHit.getOrDefault(key, 0L);
        int hits = huntersHits.getOrDefault(key, 0);
        if (now - first > 4000L) {
            first = now;
            hits = 1;
        } else {
            hits++;
        }
        huntersFirstHit.put(key, first);
        huntersHits.put(key, hits);
        if (hits >= 2) {
            huntersHits.remove(key);
            huntersFirstHit.remove(key);
            huntersCooldown.put(damager.getUniqueId(), now + 20000L);
            victim.removePotionEffect(PotionEffectType.INVISIBILITY);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0));
        }
    }

    private void handleDisplacement(Player damager, Player victim) {
        long now = System.currentTimeMillis();
        if (now < displacementCooldown.getOrDefault(damager.getUniqueId(), 0L)) return;
        String key = key(damager, victim);
        int hits = displacementHits.getOrDefault(key, 0) + 1;
        displacementHits.put(key, hits);
        if (hits >= 4) {
            displacementHits.remove(key);
            displacementCooldown.put(damager.getUniqueId(), now + 28000L);
            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                if (victim.getInventory().getItem(i) != null) slots.add(i);
            }
            if (slots.isEmpty()) return;
            int slot = slots.get(new Random().nextInt(slots.size()));
            ItemStack original = victim.getInventory().getItem(slot);
            ItemStack barrier = new ItemStack(Material.BARRIER);
            ItemMeta meta = barrier.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.RED + "Locked");
                barrier.setItemMeta(meta);
            }
            victim.getInventory().setItem(slot, barrier);
            Bukkit.getScheduler().runTaskLater(plugin, () -> victim.getInventory().setItem(slot, original), 60L);
        }
    }

    private void handleRattle(Player damager, Player victim) {
        long now = System.currentTimeMillis();
        if (now < rattleCooldown.getOrDefault(damager.getUniqueId(), 0L)) return;
        String key = key(damager, victim);
        long first = rattleFirstHit.getOrDefault(key, 0L);
        int hits = rattleHits.getOrDefault(key, 0);
        if (now - first > 3000L) {
            first = now;
            hits = 1;
        } else {
            hits++;
        }
        rattleFirstHit.put(key, first);
        rattleHits.put(key, hits);
        if (hits >= 2) {
            rattleHits.remove(key);
            rattleFirstHit.remove(key);
            rattleCooldown.put(damager.getUniqueId(), now + 12000L);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20, 0));
        }
    }

    private void handleWhiplash(Player damager, Player victim) {
        long now = System.currentTimeMillis();
        if (!victim.isSprinting()) return;
        if (now < whiplashCooldown.getOrDefault(damager.getUniqueId(), 0L)) return;
        whiplashCooldown.put(damager.getUniqueId(), now + 14000L);
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 6));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 60, 200));
    }

    private void handleMischief(Player damager, Player victim) {
        long now = System.currentTimeMillis();
        if (now < mischiefCooldown.getOrDefault(damager.getUniqueId(), 0L)) return;
        int held = victim.getInventory().getHeldItemSlot();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (i != held && victim.getInventory().getItem(i) != null) slots.add(i);
        }
        if (slots.isEmpty()) return;
        int other = slots.get(new Random().nextInt(slots.size()));
        ItemStack heldItem = victim.getInventory().getItem(held);
        ItemStack otherItem = victim.getInventory().getItem(other);
        victim.getInventory().setItem(held, otherItem);
        victim.getInventory().setItem(other, heldItem);
        mischiefCooldown.put(damager.getUniqueId(), now + 16000L);
    }

    private void handleOverextension(Player damager, Player victim) {
        long now = System.currentTimeMillis();
        if (now < overextensionCooldown.getOrDefault(damager.getUniqueId(), 0L)) return;
        OverWindow win = new OverWindow();
        win.attacker = damager.getUniqueId();
        win.expire = now + 300L;
        overextensionWindow.put(victim.getUniqueId(), win);
        overextensionCooldown.put(damager.getUniqueId(), now + 16000L);
    }
}
