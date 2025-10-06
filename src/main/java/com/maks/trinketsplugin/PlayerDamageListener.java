package com.maks.trinketsplugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.entity.Player;

import java.util.Random;

public class PlayerDamageListener implements Listener {

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        // Check if the entity damaged is a player
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        // Get the player's data synchronously
        PlayerData data = TrinketsPlugin.getInstance().getDatabaseManager().getPlayerData(player.getUniqueId());
        if (data != null) {
            int blockChance = data.getBlockChance();
            int blockStrength = data.getBlockStrength() + 35; // Base block strength is 35%

            // Add set bonus block chance from Aegis Protection - ZAKTUALIZOWANA SEKCJA
            SetBonusManager setBonusManager = TrinketsPlugin.getInstance().getSetBonusManager();
            if (setBonusManager != null) {
                // Użyj metody getBlockChanceBonus zamiast ręcznego sprawdzania
                blockChance += setBonusManager.getBlockChanceBonus(player);
            }

            // Cap block chance at 100%
            blockChance = Math.min(blockChance, 100);

            if (blockChance > 0) {
                // Implement the chance logic
                Random random = new Random();
                int rand = random.nextInt(100) + 1; // Generates a number between 1 and 100
                if (rand <= blockChance) {
                    // Reduce damage by blockStrength percentage
                    double originalDamage = event.getDamage();
                    double reducedDamage = originalDamage * (1 - (blockStrength / 100.0));
                    event.setDamage(reducedDamage);

                    // Optionally, send a message or play a sound
                    //  player.sendMessage("You blocked the attack, reducing damage by " + blockStrength + "%!");
                }
            }
        }
    }
}