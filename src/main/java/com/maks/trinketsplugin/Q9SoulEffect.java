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
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Q9SoulEffect implements Listener {

    private final TrinketsPlugin plugin;

    private final Map<UUID, Long> levitationCD = new HashMap<>();
    private final Map<UUID, Long> stoneCD = new HashMap<>();
    private final Map<UUID, Integer> hitsOnMob = new HashMap<>();

    // do freeze
    private final Map<UUID, Double> originalSpeed = new HashMap<>();
    private final Map<UUID, Double> originalAttack = new HashMap<>();

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
        String name = ChatColor.stripColor(soul.getItemMeta().getDisplayName()).toLowerCase(Locale.ROOT);
        return name.contains("medara") && name.contains("petrifying soul");
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!hasQ9SoulEquipped(damager)) return;

        Entity target = event.getEntity();

        // 1) Gracz -> levitation 2s, cd 20s (jeśli chcesz to zostawić)
        if (target instanceof Player pTarget) {
            long now = System.currentTimeMillis();
            long next = levitationCD.getOrDefault(damager.getUniqueId(), 0L);
            if (now >= next) {
                pTarget.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 40, 1));
                // Możemy np. zamiast tego obniżyć prędkość do -100% (podniesienie w powietrze trudniej zasymulować).
                // Lub zostawić tak, jak jest w oryginale. Jeśli wolisz – zostawiamy oryginał:

                pTarget.setVelocity(pTarget.getVelocity().setY(1.0));
                // Prosta symulacja podbicia do góry. Nie jest to idealny "levitation", ale gracze nie wyłączą.

                // Krótki dźwięk, particle
                pTarget.getWorld().playSound(pTarget.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1f, 1.2f);
                pTarget.getWorld().spawnParticle(Particle.CLOUD, pTarget.getLocation().add(0,1,0), 15, 0.3, 0.3, 0.3, 0.01);

                levitationCD.put(damager.getUniqueId(), now + 20_000);
            }
        }
        // 2) Mob -> po 3 hitach → kamień 1s, cd 10s
        else if (target instanceof Monster || (target instanceof LivingEntity && !(target instanceof Player))) {
            int count = hitsOnMob.getOrDefault(damager.getUniqueId(), 0) + 1;
            hitsOnMob.put(damager.getUniqueId(), count);

            if (count >= 3) {
                long now = System.currentTimeMillis();
                long next = stoneCD.getOrDefault(damager.getUniqueId(), 0L);
                if (now >= next) {
                    freezeAsStone((LivingEntity) target, 20); // 1s
                    stoneCD.put(damager.getUniqueId(), now + 10_000);
                }
                hitsOnMob.put(damager.getUniqueId(), 0);
            }
        }
    }

    private void freezeAsStone(LivingEntity entity, int ticks) {
        if (entity instanceof Player p) {
            // Gracz – root
            AttributeInstance speed = p.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            AttributeInstance attack = p.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
            if (speed == null || attack == null) return;

            originalSpeed.putIfAbsent(p.getUniqueId(), speed.getBaseValue());
            originalAttack.putIfAbsent(p.getUniqueId(), attack.getBaseValue());

            speed.setBaseValue(0.0);
            attack.setBaseValue(0.0);

            // Efekt wizualny
            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_STONE_PLACE, 1f, 0.8f);
            p.getWorld().spawnParticle(Particle.BLOCK_CRACK, p.getLocation().add(0,1,0), 30, 0.3, 0.5, 0.3, 0.1,
                    org.bukkit.Material.STONE.createBlockData());

            new BukkitRunnable() {
                @Override
                public void run() {
                    Double oldS = originalSpeed.remove(p.getUniqueId());
                    Double oldA = originalAttack.remove(p.getUniqueId());
                    if (oldS != null) speed.setBaseValue(oldS);
                    if (oldA != null) attack.setBaseValue(oldA);
                }
            }.runTaskLater(plugin, ticks);
        } else {
            // Moby
            AttributeInstance speed = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            AttributeInstance attack = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (speed == null || attack == null) return;

            double oldS = speed.getBaseValue();
            double oldA = attack.getBaseValue();

            speed.setBaseValue(0.0);
            attack.setBaseValue(0.0);

            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_STONE_PLACE, 1f, 0.8f);
            entity.getWorld().spawnParticle(Particle.BLOCK_CRACK, entity.getLocation().add(0,1,0), 30,
                    0.3, 0.5, 0.3, 0.1,
                    org.bukkit.Material.STONE.createBlockData());

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (entity.isDead()) return;
                    speed.setBaseValue(oldS);
                    attack.setBaseValue(oldA);
                }
            }.runTaskLater(plugin, ticks);
        }
    }
}
