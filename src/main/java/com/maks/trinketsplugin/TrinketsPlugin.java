package com.maks.trinketsplugin;

import net.milkbowl.vault.economy.Economy;
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

        // Initialize configuration from blokady.yml
        blokadyFile = new File(getDataFolder(), "blokady.yml");
        if (!blokadyFile.exists()) {
            saveResource("blokady.yml", false);
        }
        blokadyConfig = YamlConfiguration.loadConfiguration(blokadyFile);

        // Initialize DatabaseManager
        databaseManager = new DatabaseManager();

        // Load configuration
        databaseManager.loadConfig();

        // Open database connection
        databaseManager.openConnection();

        // Register commands
        getCommand("trinkets").setExecutor(new TrinketsCommand());
        getCommand("resetattributes").setExecutor(new ResetAttributesCommand());

        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerDamageListener(), this); // Register the new listener
        getServer().getPluginManager().registerEvents(new Q1SoulEffect(this), this);
        getServer().getPluginManager().registerEvents(new Q2SoulEffect(this), this);
        getServer().getPluginManager().registerEvents(new Q3SoulEffect(this), this);
        getServer().getPluginManager().registerEvents(new Q4SoulEffect(this), this);
        getServer().getPluginManager().registerEvents(new Q5SoulEffect(this), this);
        getServer().getPluginManager().registerEvents(new Q6SoulEffect(this), this);
        getServer().getPluginManager().registerEvents(new Q7SoulEffect(this), this);
        getServer().getPluginManager().registerEvents(new Q8SoulEffect(this), this);
        getServer().getPluginManager().registerEvents(new Q9SoulEffect(this), this);
        getServer().getPluginManager().registerEvents(new Q10SoulEffect(this), this);
        // Load data for already logged-in players
        for (Player player : Bukkit.getOnlinePlayers()) {
            getDatabaseManager().loadPlayerData(player.getUniqueId(), data -> {
                data.removeAllAttributes(player);
                data.applyAllAttributes(player);
            });
        }
        getCommand("soul").setExecutor(new SoulCommand());

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
    private static Economy econ;

    public static Economy getEconomy() {
        return econ;
    }
}
