package com.maks.trinketsplugin;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseManager {

    private Connection connection;
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
                if (connection != null && !connection.isClosed()) {
                    return;
                }

                Class.forName("com.mysql.jdbc.Driver");
                connection = DriverManager.getConnection(
                        "jdbc:mysql://" + host + ":" + port + "/" + database, username, password);

                // Create table if it doesn't exist
                Statement statement = connection.createStatement();
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS trinkets_data (" +
                        "uuid VARCHAR(36) PRIMARY KEY," +
                        "data LONGTEXT" +
                        ");");
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed())
                connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void savePlayerData(UUID playerUUID, PlayerData data) {
        playerDataMap.put(playerUUID, data);

        Bukkit.getScheduler().runTaskAsynchronously(TrinketsPlugin.getInstance(), () -> {
            try {
                String serializedData = data.serialize();
                PreparedStatement ps = connection.prepareStatement(
                        "REPLACE INTO trinkets_data (uuid, data) VALUES (?, ?)");
                ps.setString(1, playerUUID.toString());
                ps.setString(2, serializedData);
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
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
                    } else {
                        // Log invalid configuration
                    }
                }
            }
        } else {
            // Log missing items section
        }
    }

    public void loadPlayerData(UUID playerUUID, Consumer<PlayerData> callback) {
        if (playerDataMap.containsKey(playerUUID)) {
            // Data is already loaded, invoke callback immediately
            Bukkit.getScheduler().runTask(TrinketsPlugin.getInstance(), () -> {
                callback.accept(playerDataMap.get(playerUUID));
            });
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(TrinketsPlugin.getInstance(), () -> {
            PlayerData data = new PlayerData();

            try {
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT data FROM trinkets_data WHERE uuid = ?");
                ps.setString(1, playerUUID.toString());
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    String serializedData = rs.getString("data");
                    data.deserialize(serializedData);
                }

                rs.close();
                ps.close();

                // Put loaded data into the map
                playerDataMap.put(playerUUID, data);

            } catch (SQLException e) {
                e.printStackTrace();
            }

            // Apply attributes on the main thread and invoke callback
            Bukkit.getScheduler().runTask(TrinketsPlugin.getInstance(), () -> {
                callback.accept(data);
            });
        });
    }

    public void loadPlayerDataSync(UUID playerUUID) {
        if (playerDataMap.containsKey(playerUUID)) {
            // Data is already loaded
            return;
        }

        PlayerData data = new PlayerData();

        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT data FROM trinkets_data WHERE uuid = ?");
            ps.setString(1, playerUUID.toString());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String serializedData = rs.getString("data");
                data.deserialize(serializedData);
            }

            rs.close();
            ps.close();

            // Put loaded data into the map
            playerDataMap.put(playerUUID, data);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Metoda odpowiedzialna za założenie akcesorium przez gracza.
     * Poniżej znajduje się dodany fragment blokujący wszystkie sloty poza RING_1 i RING_2
     * dopóki gracz nie osiągnie minimalnego poziomu 50.
     */
    public void equipAccessory(Player player, ItemStack item) {
        UUID uuid = player.getUniqueId();

        loadPlayerData(uuid, data -> {

            // Znajdź typ akcesorium
            AccessoryType type = getAccessoryType(item);
            if (type == null) {
                player.sendMessage("Cannot determine accessory type.");
                return;
            }

            // -----------------------------
            // DODANE SPRAWDZENIE POZIOMU:
            // Wymagany poziom dla pozostałych slotów
            int minLevelForOtherSlots = 50;
            int playerLevel = player.getLevel();

            // Jeśli slot NIE jest RING_1 i NIE jest RING_2, to blokujemy do lv 50
            if (type != AccessoryType.RING_1 && type != AccessoryType.RING_2) {
                if (playerLevel < minLevelForOtherSlots) {
                    player.sendMessage(ChatColor.RED + "You must be at least level "
                            + minLevelForOtherSlots
                            + " to use this accessory slot!");
                    return;
                }
            }
            // -----------------------------

            if (data.getAccessory(type) != null) {
                player.sendMessage("You have already equipped an accessory in this slot!");
                return;
            }

            // Sprawdź, czy item jest na liście restrictedAccessories
            RestrictedAccessory restrictedAccessory = null;
            for (RestrictedAccessory accessory : restrictedAccessories) {
                if (accessory.matches(item)) {
                    restrictedAccessory = accessory;
                    break;
                }
            }

            // Jeśli item jest restricted, sprawdź poziom gracza
            if (restrictedAccessory != null) {
                int requiredLevel = restrictedAccessory.getRequiredLevel();
                if (playerLevel < requiredLevel) {
                    String message = levelMessage.replace("%level%", String.valueOf(requiredLevel));
                    player.sendMessage(message);
                    return;
                }
            }

            // Ustaw akcesorium w PlayerData
            data.setAccessory(type, item);

            // Zastosuj atrybuty
            data.applyAttributes(player, item, type);

            // Zapisz PlayerData w bazie
            savePlayerData(uuid, data);

            // Usuwamy przedmiot z ręki gracza
            player.getInventory().removeItem(item);

            // Wyświetl informację o założeniu
            String accessoryName = (restrictedAccessory != null) ? restrictedAccessory.getDisplayName() : type.getDisplayName();
            player.sendMessage("You have equipped the " + accessoryName + "!");

            // Pokaż info o block stats, jeśli istnieją
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

            // Usuń atrybuty
            data.removeAttributes(player, type);

            savePlayerData(uuid, data);

            // Zwróć przedmiot graczowi
            player.getInventory().addItem(item);

            // Wiadomość
            player.sendMessage("You have unequipped the " + type.getDisplayName() + ".");

            // Pokaż info o block stats
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

    // Metoda do pobrania PlayerData synchronicznie
    public PlayerData getPlayerData(UUID playerUUID) {
        return playerDataMap.get(playerUUID);
    }

    // Metoda do usunięcia PlayerData po wyjściu gracza
    public void removePlayerData(UUID playerUUID) {
        playerDataMap.remove(playerUUID);
    }

    // Metoda wyświetlająca info o block stats w zależności od akcji
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
            int totalBlockStrength = data.getBlockStrength() + 35; // Base block strength is 35%
            player.sendMessage(ChatColor.GREEN + "Total Block Strength: " + totalBlockStrength + "%");
        }
    }

    // Parsowanie Block Chance
    private int parseBlockChance(ItemStack item) {
        int blockChance = 0;
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            if (lore != null) {
                // Compile a case-insensitive pattern to match "Block Chance: X%"
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

    // Parsowanie Block Strength
    private int parseBlockStrength(ItemStack item) {
        int blockStrength = 0;
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            if (lore != null) {
                // Zaktualizowane wyrażenie regularne, aby pasowało do "Block Strength: +X%"
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
