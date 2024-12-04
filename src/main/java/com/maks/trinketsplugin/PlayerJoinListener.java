package com.maks.trinketsplugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Load player data synchronously
        TrinketsPlugin.getInstance().getDatabaseManager().loadPlayerDataSync(player.getUniqueId());

        // Get the data and apply attributes
        PlayerData data = TrinketsPlugin.getInstance().getDatabaseManager().getPlayerData(player.getUniqueId());
        data.removeAllAttributes(player);
        data.applyAllAttributes(player);
    }
}
