package com.maks.trinketsplugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        TrinketsPlugin.getInstance().getDatabaseManager().loadPlayerData(player.getUniqueId(), data -> {
            // Remove all attribute modifiers
            data.removeAllAttributes(player);

            // Save player data
            TrinketsPlugin.getInstance().getDatabaseManager().savePlayerData(player.getUniqueId(), data);

            // Remove player data from memory
            TrinketsPlugin.getInstance().getDatabaseManager().removePlayerData(player.getUniqueId());
        });
    }
}
