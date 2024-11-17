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
import java.util.function.Consumer;

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
              //          TrinketsPlugin.getInstance().getLogger().warning("Invalid accessory configuration for: " + key);
                    }
                }
            }
        } else {
           // TrinketsPlugin.getInstance().getLogger().warning("No items section found in blokady.yml!");
        }
    }


    public void loadPlayerData(UUID playerUUID, Consumer<PlayerData> callback) {
        if (playerDataMap.containsKey(playerUUID)) {
            // Dane są już załadowane, wywołaj callback natychmiast
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

                // Umieść załadowane dane w mapie
                playerDataMap.put(playerUUID, data);

            } catch (SQLException e) {
                e.printStackTrace();
            }

            // Zastosuj atrybuty na wątku głównym i wywołaj callback
            Bukkit.getScheduler().runTask(TrinketsPlugin.getInstance(), () -> {
                callback.accept(data);
            });
        });
    }


    public void equipAccessory(Player player, ItemStack item) {
        UUID uuid = player.getUniqueId();
      //  Bukkit.getLogger().info("equipAccessory called for player: " + player.getName());

        loadPlayerData(uuid, data -> {
       //     Bukkit.getLogger().info("Player data loaded for: " + player.getName());

            // Znajdź typ akcesorium
            AccessoryType type = getAccessoryType(item);
            if (type == null) {
         //       player.sendMessage("Cannot determine accessory type.");
            //    Bukkit.getLogger().info("Accessory type is null for item: " + item.getType());
                return;
            }
         //   Bukkit.getLogger().info("Accessory type: " + type.name());

            if (data.getAccessory(type) != null) {
                player.sendMessage("You have already equipped an accessory in this slot!");
              //  Bukkit.getLogger().info("Player already has accessory equipped in slot: " + type.name());
                return;
            }

            // Sprawdź, czy przedmiot jest w restrictedAccessories
            RestrictedAccessory restrictedAccessory = null;
            for (RestrictedAccessory accessory : restrictedAccessories) {
                if (accessory.matches(item)) {
                    restrictedAccessory = accessory;
                //    Bukkit.getLogger().info("Restricted accessory matched: " + accessory.getDisplayName());
                    break;
                }
            }

            // Jeśli przedmiot jest ograniczony, sprawdź poziom gracza
            if (restrictedAccessory != null) {
                int playerLevel = player.getLevel();
                int requiredLevel = restrictedAccessory.getRequiredLevel();
            //    Bukkit.getLogger().info("Player level: " + playerLevel + ", Required level: " + requiredLevel);

                if (playerLevel < requiredLevel) {
                    String message = levelMessage.replace("%level%", String.valueOf(requiredLevel));
                    player.sendMessage(message);
                //    Bukkit.getLogger().info("Player does not meet level requirement.");
                    return;
                }
            } else {
        //        Bukkit.getLogger().info("Accessory is not restricted.");
            }

            data.setAccessory(type, item);
            savePlayerData(uuid, data);

            // Zastosuj atrybuty z przedmiotu
            data.applyAttributes(player, item, type);

            // Usuń przedmiot z ręki
            player.getInventory().removeItem(item);

            // Wyślij wiadomość do gracza
            String accessoryName = (restrictedAccessory != null) ? restrictedAccessory.getDisplayName() : type.getDisplayName();
            player.sendMessage("You have equipped the " + accessoryName + "!");

         //   Bukkit.getLogger().info("Accessory equipped successfully for player: " + player.getName());
        });
    }


    public List<RestrictedAccessory> getRestrictedAccessories() {
        return restrictedAccessories;
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
            savePlayerData(uuid, data);

            // Usuń atrybuty
            data.removeAttributes(player, type);

            // Dodaj przedmiot z powrotem do ekwipunku gracza
            player.getInventory().addItem(item);

            // Wyślij wiadomość do gracza
            player.sendMessage("You have unequipped the " + type.getDisplayName() + ".");
        });
    }




    private AccessoryType getAccessoryType(ItemStack item) {
   //     Bukkit.getLogger().info("Determining AccessoryType for item: " + item.getType());
        for (AccessoryType type : AccessoryType.values()) {
            if (type.getMaterial() == item.getType()) {
      //          Bukkit.getLogger().info("AccessoryType matched: " + type.name());
                return type;
            }
        }
      //  Bukkit.getLogger().info("No matching AccessoryType found for item: " + item.getType());
        return null;
    }




}
