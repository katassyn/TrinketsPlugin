package com.maks.trinketsplugin;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseManager {
    private HikariDataSource dataSource;
    private String host, database, username, password;
    private String port;
    private List<RestrictedAccessory> restrictedAccessories = new ArrayList<>();
    private String levelMessage;
    private HashMap<UUID, PlayerData> playerDataMap = new HashMap<>();

    public void openConnection() {
        String host = TrinketsPlugin.getInstance().getConfig().getString("database.host");
        String port = TrinketsPlugin.getInstance().getConfig().getString("database.port");
        String database = TrinketsPlugin.getInstance().getConfig().getString("database.name");
        String username = TrinketsPlugin.getInstance().getConfig().getString("database.user");
        String password = TrinketsPlugin.getInstance().getConfig().getString("database.password");

        try {
            synchronized (this) {
                if (dataSource != null) {
                    return;
                }

                HikariConfig config = new HikariConfig();
                config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
                config.setUsername(username);
                config.setPassword(password);

                // HikariCP settings
                config.setMaximumPoolSize(10);
                config.setMinimumIdle(5);
                config.setIdleTimeout(300000); // 5 minutes
                config.setConnectionTimeout(10000); // 10 seconds
                config.setMaxLifetime(600000); // 10 minutes
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                config.addDataSourceProperty("useServerPrepStmts", "true");

                dataSource = new HikariDataSource(config);

                // Create table if it doesn't exist
                try (Connection conn = dataSource.getConnection();
                     Statement statement = conn.createStatement()) {
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS trinkets_data (" +
                            "uuid VARCHAR(36) PRIMARY KEY," +
                            "data LONGTEXT" +
                            ");");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void loadConfig() {
        restrictedAccessories.clear();
        levelMessage = TrinketsPlugin.getInstance().getBlokadyConfig().getString("message", "You must be at least level %level% to use this item!");

        ConfigurationSection itemsSection = TrinketsPlugin.getInstance().getBlokadyConfig().getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null) {
                    String id = itemSection.getString("id");
                    String displayName = itemSection.getString("display_name");
                    int level = itemSection.getInt("lv");
                    if (id != null && displayName != null) {
                        RestrictedAccessory accessory = new RestrictedAccessory(id, displayName, level);
                        restrictedAccessories.add(accessory);
                    }
                }
            }
        }
    }

    public void savePlayerData(UUID playerUUID, PlayerData data) {
        playerDataMap.put(playerUUID, data);

        Bukkit.getScheduler().runTaskAsynchronously(TrinketsPlugin.getInstance(), () -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "REPLACE INTO trinkets_data (uuid, data) VALUES (?, ?)")) {
                String serializedData = data.serialize();
                ps.setString(1, playerUUID.toString());
                ps.setString(2, serializedData);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void loadPlayerData(UUID playerUUID, Consumer<PlayerData> callback) {
        if (playerDataMap.containsKey(playerUUID)) {
            Bukkit.getScheduler().runTask(TrinketsPlugin.getInstance(), () -> {
                callback.accept(playerDataMap.get(playerUUID));
            });
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(TrinketsPlugin.getInstance(), () -> {
            PlayerData data = new PlayerData();

            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT data FROM trinkets_data WHERE uuid = ?")) {
                ps.setString(1, playerUUID.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String serializedData = rs.getString("data");
                        data.deserialize(serializedData);
                    }
                }

                playerDataMap.put(playerUUID, data);

            } catch (SQLException e) {
                e.printStackTrace();
            }

            Bukkit.getScheduler().runTask(TrinketsPlugin.getInstance(), () -> {
                callback.accept(data);
            });
        });
    }

    public void loadPlayerDataSync(UUID playerUUID) {
        if (playerDataMap.containsKey(playerUUID)) {
            return;
        }

        PlayerData data = new PlayerData();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT data FROM trinkets_data WHERE uuid = ?")) {
            ps.setString(1, playerUUID.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String serializedData = rs.getString("data");
                    data.deserialize(serializedData);
                }
            }

            playerDataMap.put(playerUUID, data);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void equipAccessory(Player player, ItemStack item) {
        UUID uuid = player.getUniqueId();

        loadPlayerData(uuid, data -> {
            AccessoryType type = getAccessoryType(item);
            if (type == null) {
                player.sendMessage("Cannot determine accessory type.");
                return;
            }

            if (item.getAmount() > 1) {
                player.sendMessage(ChatColor.RED + "Accessories must be unstacked before equipping.");
                return;
            }

            int minLevelForOtherSlots = 50;
            int playerLevel = player.getLevel();

            if (type != AccessoryType.RING_1 && type != AccessoryType.RING_2) {
                if (playerLevel < minLevelForOtherSlots) {
                    player.sendMessage(ChatColor.RED + "You must be at least level "
                            + minLevelForOtherSlots
                            + " to use this accessory slot!");
                    return;
                }
            }

            if (data.getAccessory(type) != null) {
                player.sendMessage("You have already equipped an accessory in this slot!");
                return;
            }

            RestrictedAccessory restrictedAccessory = null;
            for (RestrictedAccessory accessory : restrictedAccessories) {
                if (accessory.matches(item)) {
                    restrictedAccessory = accessory;
                    break;
                }
            }

            if (restrictedAccessory != null) {
                int requiredLevel = restrictedAccessory.getRequiredLevel();
                if (playerLevel < requiredLevel) {
                    String message = levelMessage.replace("%level%", String.valueOf(requiredLevel));
                    player.sendMessage(message);
                    return;
                }
            }

            // Clone the item and set amount to 1 to store in player data
            ItemStack accessoryToEquip = item.clone();
            accessoryToEquip.setAmount(1);

            data.setAccessory(type, accessoryToEquip);
            data.applyAttributes(player, accessoryToEquip, type);
            savePlayerData(uuid, data);

            // Remove only one item from the player's inventory
            ItemStack itemToRemove = item.clone();
            itemToRemove.setAmount(1);
            player.getInventory().removeItem(itemToRemove);

            // Refresh main hand and offhand to clear negative modifiers
            TrinketsPlugin.getInstance().getOffhandListener().updateMainHand(player);
            TrinketsPlugin.getInstance().getOffhandListener().updateOffhand(player);

            String accessoryName = (restrictedAccessory != null) ? restrictedAccessory.getDisplayName() : type.getDisplayName();
            player.sendMessage("You have equipped the " + accessoryName + "!");

            sendBlockStatsInfo(player, data, item, true);
        });
    }

    public void unequipAccessory(Player player, ItemStack item) {
        UUID uuid = player.getUniqueId();

        loadPlayerData(uuid, data -> {
            AccessoryType type = getAccessoryType(item);

            if (type == null) {
                player.sendMessage("This item cannot be unequipped as an accessory.");
                return;
            }

            data.removeAccessory(type);
            data.removeAttributes(player, type);
            savePlayerData(uuid, data);

            // Ensure we only return one accessory, not the entire stack
            ItemStack accessoryToReturn = item.clone();
            accessoryToReturn.setAmount(1);
            player.getInventory().addItem(accessoryToReturn);
            TrinketsPlugin.getInstance().getOffhandListener().updateMainHand(player);
            TrinketsPlugin.getInstance().getOffhandListener().updateOffhand(player);

            player.sendMessage("You have unequipped the " + type.getDisplayName() + ".");
            sendBlockStatsInfo(player, data, item, false);
        });
    }

    private AccessoryType getAccessoryType(ItemStack item) {
        for (AccessoryType type : AccessoryType.values()) {
            if (type.getMaterial() == item.getType()) {
                return type;
            }
        }
        return null;
    }

    public List<RestrictedAccessory> getRestrictedAccessories() {
        return restrictedAccessories;
    }

    public PlayerData getPlayerData(UUID playerUUID) {
        return playerDataMap.get(playerUUID);
    }

    public void removePlayerData(UUID playerUUID) {
        playerDataMap.remove(playerUUID);
    }

    private void sendBlockStatsInfo(Player player, PlayerData data, ItemStack item, boolean isEquip) {
        int itemBlockChance = parseBlockChance(item);
        int itemBlockStrength = parseBlockStrength(item);

        if (itemBlockChance > 0 || itemBlockStrength > 0) {
            String action = isEquip ? "Equipped" : "Unequipped";
            player.sendMessage(ChatColor.GREEN + action + " accessory with:");
            if (itemBlockChance > 0) {
                player.sendMessage(ChatColor.YELLOW + " - Block Chance: " + itemBlockChance + "%");
            }
            if (itemBlockStrength > 0) {
                player.sendMessage(ChatColor.YELLOW + " - Block Strength: +" + itemBlockStrength + "%");
            }
            player.sendMessage(ChatColor.GREEN + "Total Block Chance: " + data.getBlockChance() + "%");
            int totalBlockStrength = data.getBlockStrength() + 35;
            player.sendMessage(ChatColor.GREEN + "Total Block Strength: " + totalBlockStrength + "%");
        }
    }

    private int parseBlockChance(ItemStack item) {
        int blockChance = 0;
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            if (lore != null) {
                Pattern pattern = Pattern.compile("(?i)block chance:\\s*(\\d+)%?");
                for (String line : lore) {
                    String strippedLine = ChatColor.stripColor(line);
                    if (strippedLine != null) {
                        Matcher matcher = pattern.matcher(strippedLine);
                        if (matcher.find()) {
                            String percentageString = matcher.group(1);
                            try {
                                int chance = Integer.parseInt(percentageString);
                                chance = Math.max(0, Math.min(100, chance));
                                blockChance += chance;
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        return blockChance;
    }

    private int parseBlockStrength(ItemStack item) {
        int blockStrength = 0;
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            if (lore != null) {
                Pattern pattern = Pattern.compile("(?i)block strength:\\s*\\+\\s*(\\d+)%?");
                for (String line : lore) {
                    String strippedLine = ChatColor.stripColor(line);
                    if (strippedLine != null) {
                        Matcher matcher = pattern.matcher(strippedLine);
                        if (matcher.find()) {
                            String percentageString = matcher.group(1);
                            try {
                                int strength = Integer.parseInt(percentageString);
                                strength = Math.max(0, Math.min(100, strength));
                                blockStrength += strength;
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        return blockStrength;
    }
}
