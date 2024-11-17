package com.maks.trinketsplugin;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class TrinketsPlugin extends JavaPlugin {
    private static TrinketsPlugin instance;
    private DatabaseManager databaseManager;
    private File blokadyFile;
    private FileConfiguration blokadyConfig;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        // Inicjalizacja konfiguracji z pliku blokady.yml
        blokadyFile = new File(getDataFolder(), "blokady.yml");
        if (!blokadyFile.exists()) {
            saveResource("blokady.yml", false);
        }
        blokadyConfig = YamlConfiguration.loadConfiguration(blokadyFile);

        // Inicjalizacja DatabaseManager
        databaseManager = new DatabaseManager();

        // Wczytaj konfigurację
        databaseManager.loadConfig();

        // Otwórz połączenie z bazą danych
        databaseManager.openConnection();

        // Register commands
        getCommand("trinkets").setExecutor(new TrinketsCommand());
        getCommand("resetattributes").setExecutor(new ResetAttributesCommand());

        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);

        // Wczytaj dane dla już zalogowanych graczy
        for (Player player : Bukkit.getOnlinePlayers()) {
            getDatabaseManager().loadPlayerData(player.getUniqueId(), data -> {
                data.removeAllAttributes(player);
                data.applyAllAttributes(player);
            });
        }
    }




    @Override
    public void onDisable() {
        // Close database connection if it exists
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
    }

    public FileConfiguration getBlokadyConfig() {
        return blokadyConfig;
    }

    public static TrinketsPlugin getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
