package com.maks.trinketsplugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Wczytaj dane gracza i zastosuj atrybuty
        TrinketsPlugin.getInstance().getDatabaseManager().loadPlayerData(player.getUniqueId(), data -> {
            // Usuń istniejące atrybuty, aby zapobiec kumulacji
            data.removeAllAttributes(player);

            // Zastosuj atrybuty z założonych akcesoriów
            data.applyAllAttributes(player);
        });
    }
}
