package com.maks.trinketsplugin;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.EventExecutor;

import java.io.File;

public class TrinketsPlugin extends JavaPlugin {
    private static TrinketsPlugin instance;
    private DatabaseManager databaseManager;
    private File blokadyFile;
    private FileConfiguration blokadyConfig;
    private JewelManager jewelManager;
    private RuneManager runeManager;
    private RuneEffectsListener runeEffectsListener;
    private UniqueTrinketEffectsListener uniqueTrinketEffectsListener;
    private SetBonusManager setBonusManager;

    private OffhandListener offhandListener;
    private static final int debuggingFlag = 1;


    private static Economy econ = null;
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

        // Create jewels.yml if it doesn't exist
        File jewelsFile = new File(getDataFolder(), "jewels.yml");
        if (!jewelsFile.exists()) {
            saveResource("jewels.yml", false);
        }

        // Create runes.yml if it doesn't exist
        File runesFile = new File(getDataFolder(), "runes.yml");
        if (!runesFile.exists()) {
            saveResource("runes.yml", false);
        }

        // Create unique_trinkets.yml if it doesn't exist
        File uniqueTrinketsFile = new File(getDataFolder(), "unique_trinkets.yml");
        if (!uniqueTrinketsFile.exists()) {
            saveResource("unique_trinkets.yml", false);
        }

        // Create set_items.yml if it doesn't exist
        File setItemsFile = new File(getDataFolder(), "set_items.yml");
        if (!setItemsFile.exists()) {
            saveResource("set_items.yml", false);
        }

        // Initialize DatabaseManager
        databaseManager = new DatabaseManager();

        // Load configuration
        databaseManager.loadConfig();

        // Open database connection
        databaseManager.openConnection();

        // Initialize JewelManager
        jewelManager = new JewelManager(this);
        runeManager = new RuneManager(this);
        runeEffectsListener = new RuneEffectsListener(this);
        uniqueTrinketEffectsListener = new UniqueTrinketEffectsListener(this);
        setBonusManager = new SetBonusManager(this);


        // Initialize JewelAPI
        JewelAPI.initialize(this);

        // Register commands
        getCommand("trinkets").setExecutor(new TrinketsCommand());
        getCommand("resetattributes").setExecutor(new ResetAttributesCommand());
        if (!setupEconomy()) {
            getLogger().severe("Vault dependency not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("Vault hooked successfully!");
        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerDamageListener(), this); // Register the new listener
        getServer().getPluginManager().registerEvents(runeEffectsListener, this);
        offhandListener = new OffhandListener(this);
        getServer().getPluginManager().registerEvents(offhandListener, this);
        JewelEvents jewelEvents = new JewelEvents(this, jewelManager);
        getServer().getPluginManager().registerEvents(jewelEvents, this);
        try {
            Class<? extends Event> fishEvent = (Class<? extends Event>) Class.forName("org.maks.fishingPlugin.api.FishRewardEvent");
            getServer().getPluginManager().registerEvent(
                    fishEvent,
                    jewelEvents,
                    EventPriority.NORMAL,
                    (EventExecutor) (listener, event) -> ((JewelEvents) listener).handleFishReward(event),
                    this,
                    true
            );
        } catch (ClassNotFoundException ignored) {
        }
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
        getServer().getPluginManager().registerEvents(new GemActionsListener(), this);
        getServer().getPluginManager().registerEvents(new ConjurationListener(), this);
        getServer().getPluginManager().registerEvents(new RunicWordEffectsListener(this), this);
        getServer().getPluginManager().registerEvents(new AugmenterListener(), this);
        getServer().getPluginManager().registerEvents(uniqueTrinketEffectsListener, this);
        getServer().getPluginManager().registerEvents(new ArmorChangeListener(this), this);

        // Load data for already logged-in players
        for (Player player : Bukkit.getOnlinePlayers()) {
            getDatabaseManager().loadPlayerData(player.getUniqueId(), data -> {
                data.removeAllAttributes(player);
                data.applyAllAttributes(player);

                // Apply jewel attributes
                jewelManager.applyJewelAttributes(player, data);
                runeEffectsListener.updateLuck(player);

                // Apply set bonuses - NOWA LINIA
                setBonusManager.updatePlayerSetBonuses(player, data);

                // Ensure accessories in hands don't grant attributes
                offhandListener.updateOffhand(player);
                offhandListener.updateMainHand(player);
            });
        }

        if (debuggingFlag == 1) {
            getLogger().info("TrinketsPlugin has been enabled with Jewel system and debugging enabled!");
        } else {
            getLogger().info("TrinketsPlugin has been enabled with Jewel system!");
        }
        getCommand("soul").setExecutor(new SoulCommand());
        getCommand("jewels").setExecutor(new JewelsCommand());
        getCommand("gem_actions").setExecutor(new GemActionsCommand());
        getCommand("conjuration_menu").setExecutor(new ConjurationCommand());
        getCommand("augmenter").setExecutor(new AugmenterCommand());
        getCommand("setbonus").setExecutor(new SetBonusCommand());
        getCommand("player_attrib_stats").setExecutor(new PlayerAttribStatsCommand());

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

    public JewelManager getJewelManager() {
        return jewelManager;
    }

    public RuneManager getRuneManager() {
        return runeManager;
    }

    public RuneEffectsListener getRuneEffectsListener() {
        return runeEffectsListener;
    }


    public OffhandListener getOffhandListener() {
        return offhandListener;
    }

    public SetBonusManager getSetBonusManager() {
        return setBonusManager;
    }
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy getEconomy() {
        return econ;
    }
}
