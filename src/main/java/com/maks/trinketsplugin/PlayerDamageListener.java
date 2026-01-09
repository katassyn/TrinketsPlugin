package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.concurrent.ThreadLocalRandom;

public class PlayerDamageListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        // Check if the entity damaged is a player
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        // Get the player's data synchronously for all trinket stats
        PlayerData data = TrinketsPlugin.getInstance().getDatabaseManager().getPlayerData(player.getUniqueId());

        // Check for Evasion Chance first - 100% dodge if successful (CAPPED via API)
        double evasionChance = TrinketsStatsAPI.getEvasionChance(player);
        if (evasionChance > 0) {
            int roll = ThreadLocalRandom.current().nextInt(100) + 1;
            if (roll <= evasionChance) {
                // Evaded! Take no damage
                event.setDamage(0);
                player.sendMessage(ChatColor.AQUA + "💨 Evaded!");
                return;
            }
        }

        // Check for attacker's Armor Penetration (CAPPED via API)
        double attackerArmorPen = 0.0;
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageByEntity = (EntityDamageByEntityEvent) event;
            Player attacker = getPlayerAttacker(damageByEntity);
            if (attacker != null) {
                attackerArmorPen = TrinketsStatsAPI.getArmorPenetration(attacker);
            }
        }

        // Apply the custom protection curve (with armor penetration from attacker)
        double damage = event.getDamage();
        double protectedDamage = ProtectionReductionCalculator.applyToDamage(player, damage, attackerArmorPen);

        int blockChance = 0;
        int blockStrengthBonus = 0;
        if (data != null) {
            blockChance = data.getBlockChance();
            blockStrengthBonus = data.getBlockStrength();
        }

        // Also scan player's equipped armor for block stats
        for (ItemStack armorPiece : player.getInventory().getArmorContents()) {
            if (armorPiece != null) {
                int[] armorBlockStats = PlayerData.parseBlockStatsFromItem(armorPiece);
                blockChance += armorBlockStats[0];
                blockStrengthBonus += armorBlockStats[1];
            }
        }

        SetBonusManager setBonusManager = TrinketsPlugin.getInstance().getSetBonusManager();
        if (setBonusManager != null) {
            blockChance += setBonusManager.getBlockChanceBonus(player);
        }

        blockChance = Math.min(blockChance, 100);

        double blockMultiplier = 1.0;
        if (blockChance > 0) {
            int roll = ThreadLocalRandom.current().nextInt(100) + 1;
            if (roll <= blockChance) {
                int totalBlockStrength = 35 + blockStrengthBonus; // Base block strength is 35%
                blockMultiplier -= totalBlockStrength / 100.0;
                if (blockMultiplier < 0.0) {
                    blockMultiplier = 0.0;
                }
            }
        }

        double finalDamage = protectedDamage * blockMultiplier;

        // Check for Gluttonous Bear Soul - Thick Layer of Fat effect
        if (data != null) {
            ItemStack bossSoul = data.getAccessory(AccessoryType.BOSS_SOUL);
            if (bossSoul != null && hasDisplayName(bossSoul, "Gluttonous Bear's Soul")) {
                // Calculate max damage allowed (50% of max HP including absorption)
                double maxHealth = player.getMaxHealth();
                double absorptionHealth = 0.0;

                // Get absorption hearts
                PotionEffect absorptionEffect = player.getPotionEffect(PotionEffectType.ABSORPTION);
                if (absorptionEffect != null) {
                    absorptionHealth = (absorptionEffect.getAmplifier() + 1) * 4.0;
                }

                double totalMaxHealth = maxHealth + absorptionHealth;
                double maxDamageAllowed = totalMaxHealth * 0.5;

                // Cap damage to 50% of total max HP
                if (finalDamage > maxDamageAllowed) {
                    finalDamage = maxDamageAllowed;
                }
            }
        }

        event.setDamage(finalDamage);
    }

    private boolean hasDisplayName(ItemStack item, String name) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        String displayName = ChatColor.stripColor(meta.getDisplayName());
        return displayName.contains(name);
    }

    /**
     * Get the player attacker from a damage event (handles direct hits and projectiles).
     */
    private Player getPlayerAttacker(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();

        // Direct player attack
        if (damager instanceof Player) {
            return (Player) damager;
        }

        // Projectile attack (arrow, trident, etc.)
        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player) {
                return (Player) shooter;
            }
        }

        return null;
    }
}
