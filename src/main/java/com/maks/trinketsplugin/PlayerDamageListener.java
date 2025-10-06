package com.maks.trinketsplugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

public class PlayerDamageListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        // Check if the entity damaged is a player
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        // Apply the custom protection curve first
        double damage = event.getDamage();
        double protectedDamage = ProtectionReductionCalculator.applyToDamage(player, damage);

        // Get the player's data synchronously for block statistics
        PlayerData data = TrinketsPlugin.getInstance().getDatabaseManager().getPlayerData(player.getUniqueId());

        int blockChance = 0;
        int blockStrengthBonus = 0;
        if (data != null) {
            blockChance = data.getBlockChance();
            blockStrengthBonus = data.getBlockStrength();
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
        event.setDamage(finalDamage);
    }
}
