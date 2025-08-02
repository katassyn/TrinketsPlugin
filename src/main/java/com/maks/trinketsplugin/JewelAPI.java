package com.maks.trinketsplugin;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Klasa API udostępniająca funkcje dla innych pluginów
 */
public class JewelAPI {

    private static final int debuggingFlag = 0;
    private static TrinketsPlugin plugin;

    public static void initialize(TrinketsPlugin mainPlugin) {
        plugin = mainPlugin;
        if (debuggingFlag == 1) {
            plugin.getLogger().info("[JewelAPI] Initialized JewelAPI for other plugins");
        }
    }

    /**
     * Sprawdza, czy gracz ma założony jewel Steam Sale i zwraca bonus do sprzedaży
     * 
     * @param player Gracz do sprawdzenia
     * @return Mnożnik dla sprzedaży (1.0 oznacza brak modyfikacji)
     */
    public static double getSellBonus(Player player) {
        if (plugin == null) return 1.0;

        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getDatabaseManager().getPlayerData(uuid);

        if (data == null) return 1.0;

        ItemStack jewel = data.getJewel(JewelType.STEAM_SALE);
        if (jewel == null) return 1.0;

        int tier = plugin.getJewelManager().getJewelTier(jewel);
        return tier == 1 ? 1.2 : tier == 2 ? 1.3 : 1.5; // +20%/+30%/+50%
    }

    /**
     * Sprawdza, czy gracz ma założony jewel Steam Sale i zwraca zniżkę do craftingu
     * 
     * @param player Gracz do sprawdzenia
     * @return Mnożnik zniżki dla craftingu (1.0 oznacza brak modyfikacji)
     */
    public static double getCraftingDiscount(Player player) {
        if (plugin == null) return 1.0;

        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getDatabaseManager().getPlayerData(uuid);

        if (data == null) return 1.0;

        ItemStack jewel = data.getJewel(JewelType.STEAM_SALE);
        if (jewel == null) return 1.0;

        int tier = plugin.getJewelManager().getJewelTier(jewel);
        return tier == 1 ? 0.9 : tier == 2 ? 0.8 : 0.7; // -10%/-20%/-30%
    }
}
