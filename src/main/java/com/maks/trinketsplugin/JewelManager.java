package com.maks.trinketsplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class JewelManager {

    private static final int debuggingFlag = 1;
    private final TrinketsPlugin plugin;
    private final Map<UUID, Map<String, AttributeModifier>> appliedModifiers = new HashMap<>();

    // Helper method to send action bar messages
    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
    }

    public JewelManager(TrinketsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isJewel(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }

        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        // Check if it has a tier marker
        boolean hasTierMarker = name.contains("[ I ]") || name.contains("[ II ]") || name.contains("[ III ]");
        if (!hasTierMarker) {
            return false;
        }

        // Check if it matches any jewel type
        for (JewelType type : JewelType.values()) {
            if (name.contains(type.getDisplayName())) {
                return true;
            }
        }

        return false;
    }

    public JewelType getJewelType(ItemStack item) {
        if (!isJewel(item)) {
            return null;
        }

        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        // Special handling for "Jewel of Focus"
        if (name.contains("Jewel of Focus")) {
            // Return the base type, equipJewel will decide which slot to use
            return JewelType.JEWEL_OF_FOCUS;
        }

        // For other jewel types, use standard logic
        for (JewelType type : JewelType.values()) {
            if (type != JewelType.JEWEL_OF_FOCUS_2 && type != JewelType.JEWEL_OF_FOCUS_3 && 
                name.contains(type.getDisplayName())) {
                return type;
            }
        }

        return null;
    }

    public int getJewelTier(ItemStack item) {
        if (!isJewel(item)) {
            return 0;
        }

        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        if (name.contains("[ I ]")) return 1;
        if (name.contains("[ II ]")) return 2;
        if (name.contains("[ III ]")) return 3;

        return 1;  // Default to tier 1
    }

    /**
     * Checks if two jewels are the same (same type and tier)
     * @param jewel1 First jewel to compare
     * @param jewel2 Second jewel to compare
     * @return true if the jewels are the same, false otherwise
     */
    public boolean isSameJewel(ItemStack jewel1, ItemStack jewel2) {
        if (!isJewel(jewel1) || !isJewel(jewel2)) {
            return false;
        }

        JewelType type1 = getJewelType(jewel1);
        JewelType type2 = getJewelType(jewel2);

        if (type1 != type2) {
            return false;
        }

        int tier1 = getJewelTier(jewel1);
        int tier2 = getJewelTier(jewel2);

        return tier1 == tier2;
    }

    public void equipJewel(Player player, ItemStack item) {
        UUID uuid = player.getUniqueId();

        if (debuggingFlag == 1) {
            Bukkit.getLogger().info("[JewelManager] Equipping jewel for player: " + player.getName());
        }

        // Check player level
        if (player.getLevel() < 50) {
            sendActionBar(player, ChatColor.RED + "You must be at least level 50 to use jewels!");
            return;
        }

        JewelType type = getJewelType(item);
        if (type == null) {
            sendActionBar(player, ChatColor.RED + "This item is not a valid jewel.");
            return;
        }

        plugin.getDatabaseManager().loadPlayerData(uuid, data -> {
            // Special handling for CD Jewels (JEWEL_OF_FOCUS)
            if (type == JewelType.JEWEL_OF_FOCUS) {
                // Check if player has already reached the maximum of 3 Focus Jewels
                boolean slot1Occupied = data.getJewel(JewelType.JEWEL_OF_FOCUS) != null;
                boolean slot2Occupied = data.getJewel(JewelType.JEWEL_OF_FOCUS_2) != null;
                boolean slot3Occupied = data.getJewel(JewelType.JEWEL_OF_FOCUS_3) != null;

                if (debuggingFlag == 1) {
                    Bukkit.getLogger().info("[JewelManager] CD Jewel slots: " + 
                            "Slot1: " + (slot1Occupied ? "Occupied" : "Empty") + ", " +
                            "Slot2: " + (slot2Occupied ? "Occupied" : "Empty") + ", " +
                            "Slot3: " + (slot3Occupied ? "Occupied" : "Empty"));
                }

                // Assign jewel to the first available slot
                if (!slot1Occupied) {
                    equipJewelToSpecificType(player, item, data, JewelType.JEWEL_OF_FOCUS);
                    return;
                } else if (!slot2Occupied) {
                    equipJewelToSpecificType(player, item, data, JewelType.JEWEL_OF_FOCUS_2);
                    return;
                } else if (!slot3Occupied) {
                    equipJewelToSpecificType(player, item, data, JewelType.JEWEL_OF_FOCUS_3);
                    return;
                } else {
                    sendActionBar(player, ChatColor.RED + "You have reached the maximum of 3 Focus Jewels!");
                    return;
                }
            } else {
                // For all other jewel types, check if player already has one equipped
                ItemStack existingJewel = data.getJewel(type);
                if (existingJewel != null) {
                    // Check if it's the same jewel (to prevent duplication)
                    if (isSameJewel(existingJewel, item)) {
                        sendActionBar(player, ChatColor.RED + "You already have this jewel equipped!");
                        return;
                    } else {
                        // If it's a different jewel of the same type, inform the player
                        sendActionBar(player, ChatColor.RED + "You already have a " + type.getDisplayName() + " equipped. Unequip it first.");
                        return;
                    }
                }
            }

            // Equip the new jewel (for non-JEWEL_OF_FOCUS types)
            data.setJewel(type, item.clone());

            // Remove the jewel from the player's inventory immediately
            if (player.getInventory().getItemInMainHand().equals(item)) {
                ItemStack handItem = player.getInventory().getItemInMainHand().clone();
                handItem.setAmount(handItem.getAmount() - 1);
                player.getInventory().setItemInMainHand(handItem);
            } else if (player.getInventory().getItemInOffHand().equals(item)) {
                ItemStack offHandItem = player.getInventory().getItemInOffHand().clone();
                offHandItem.setAmount(offHandItem.getAmount() - 1);
                player.getInventory().setItemInOffHand(offHandItem);
            } else {
                // If not in hands, just decrease the amount
                item.setAmount(item.getAmount() - 1);
            }

            // Save the data
            plugin.getDatabaseManager().savePlayerData(uuid, data);

            // Apply jewel attributes
            applyJewelAttributes(player, data);

            // Inform the player with action bar message
            String message = ChatColor.GREEN + "You have equipped the " + 
                    ChatColor.stripColor(item.getItemMeta().getDisplayName()) + "!";
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));

            if (debuggingFlag == 1) {
                Bukkit.getLogger().info("[JewelManager] Jewel equipped successfully for player: " + 
                        player.getName() + ", type: " + type.name());
            }
        });
    }

    // Helper method to equip a jewel to a specific type slot
    private void equipJewelToSpecificType(Player player, ItemStack item, PlayerData data, JewelType targetType) {
        data.setJewel(targetType, item.clone());

        // Remove the jewel from the player's inventory
        if (player.getInventory().getItemInMainHand().equals(item)) {
            ItemStack handItem = player.getInventory().getItemInMainHand().clone();
            handItem.setAmount(handItem.getAmount() - 1);
            player.getInventory().setItemInMainHand(handItem);
        } else if (player.getInventory().getItemInOffHand().equals(item)) {
            ItemStack offHandItem = player.getInventory().getItemInOffHand().clone();
            offHandItem.setAmount(offHandItem.getAmount() - 1);
            player.getInventory().setItemInOffHand(offHandItem);
        } else {
            // If not in hands, just decrease the amount
            item.setAmount(item.getAmount() - 1);
        }

        // Save player data
        plugin.getDatabaseManager().savePlayerData(player.getUniqueId(), data);

        // Apply jewel attributes
        applyJewelAttributes(player, data);

        // Inform the player
        String message = ChatColor.GREEN + "You have equipped a Jewel of Focus in slot " + 
                (targetType == JewelType.JEWEL_OF_FOCUS ? "1" : 
                 targetType == JewelType.JEWEL_OF_FOCUS_2 ? "2" : "3") + "!";
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));

        if (debuggingFlag == 1) {
            Bukkit.getLogger().info("[JewelManager] Equipped Focus Jewel to slot " + 
                    (targetType == JewelType.JEWEL_OF_FOCUS ? "1" : 
                     targetType == JewelType.JEWEL_OF_FOCUS_2 ? "2" : "3") + 
                    " for player: " + player.getName());
        }
    }

    public void unequipJewel(Player player, JewelType type) {
        UUID uuid = player.getUniqueId();

        if (debuggingFlag == 1) {
            Bukkit.getLogger().info("[JewelManager] Unequipping jewel for player: " + 
                    player.getName() + ", type: " + type.name());
        }

        plugin.getDatabaseManager().loadPlayerData(uuid, data -> {
            ItemStack jewel = data.getJewel(type);

            if (jewel == null) {
                sendActionBar(player, ChatColor.RED + "You don't have a " + type.getDisplayName() + " equipped.");
                return;
            }

            // Remove the jewel
            data.removeJewel(type);

            // Remove jewel attributes
            removeJewelAttributes(player, type);

            // Ensure we only return one jewel, not the entire stack
            ItemStack jewelToReturn = jewel.clone();
            jewelToReturn.setAmount(1);

            // Return the jewel to the player
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(jewelToReturn);

            // If the jewel doesn't fit in the inventory, drop it on the ground
            if (!leftover.isEmpty()) {
                for (ItemStack item : leftover.values()) {
                    player.getWorld().dropItem(player.getLocation(), item);
                }
                sendActionBar(player, ChatColor.YELLOW + "Your inventory is full, some items were dropped on the ground.");
            }

            // Save the data
            plugin.getDatabaseManager().savePlayerData(uuid, data);

            // Inform the player with action bar message
            String message = ChatColor.GREEN + "You have unequipped the " + type.getDisplayName() + ".";
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));

            if (debuggingFlag == 1) {
                Bukkit.getLogger().info("[JewelManager] Jewel unequipped successfully for player: " + 
                        player.getName() + ", type: " + type.name());
            }
        });
    }

    public void applyJewelAttributes(Player player, PlayerData data) {
        UUID uuid = player.getUniqueId();

        if (debuggingFlag == 1) {
            Bukkit.getLogger().info("[JewelManager] Applying jewel attributes for player: " + player.getName());
        }

        // Remove all previous modifiers
        removeAllJewelAttributes(player);

        // Map for new modifiers
        Map<String, AttributeModifier> newModifiers = new HashMap<>();

        // Check each jewel type
        for (JewelType type : JewelType.values()) {
            ItemStack jewel = data.getJewel(type);
            if (jewel == null) continue;

            int tier = getJewelTier(jewel);

            // Add modifiers based on jewel type
            switch (type) {
                case DAMAGE:
                    double damageBonus = tier == 1 ? 0.02 : tier == 2 ? 0.04 : 0.08;
                    AttributeInstance attackDamage = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
                    if (attackDamage != null) {
                        AttributeModifier modifier = new AttributeModifier(
                                UUID.randomUUID(),
                                "jewel.damage." + tier,
                                damageBonus,
                                AttributeModifier.Operation.MULTIPLY_SCALAR_1
                        );
                        attackDamage.addModifier(modifier);
                        newModifiers.put("jewel.damage." + tier, modifier);
                    }
                    break;

                case MOVE_SPEED:
                    double moveSpeedBonus = tier == 1 ? 0.02 : tier == 2 ? 0.04 : 0.08;
                    AttributeInstance moveSpeed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
                    if (moveSpeed != null) {
                        AttributeModifier modifier = new AttributeModifier(
                                UUID.randomUUID(),
                                "jewel.movespeed." + tier,
                                moveSpeedBonus,
                                AttributeModifier.Operation.MULTIPLY_SCALAR_1
                        );
                        moveSpeed.addModifier(modifier);
                        newModifiers.put("jewel.movespeed." + tier, modifier);
                    }
                    break;

                case ATTACK_SPEED:
                    double attackSpeedBonus = tier == 1 ? 0.02 : tier == 2 ? 0.04 : 0.08;
                    AttributeInstance attackSpeed = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
                    if (attackSpeed != null) {
                        AttributeModifier modifier = new AttributeModifier(
                                UUID.randomUUID(),
                                "jewel.attackspeed." + tier,
                                attackSpeedBonus,
                                AttributeModifier.Operation.MULTIPLY_SCALAR_1
                        );
                        attackSpeed.addModifier(modifier);
                        newModifiers.put("jewel.attackspeed." + tier, modifier);
                    }
                    break;

                case HEALTH:
                    double healthBonus = tier == 1 ? 10 : tier == 2 ? 20 : 30;
                    AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    if (maxHealth != null) {
                        AttributeModifier modifier = new AttributeModifier(
                                UUID.randomUUID(),
                                "jewel.health." + tier,
                                healthBonus,
                                AttributeModifier.Operation.ADD_NUMBER
                        );
                        maxHealth.addModifier(modifier);
                        newModifiers.put("jewel.health." + tier, modifier);
                    }
                    break;

                case ARMOR_TOUGHNESS:
                    double toughnessBonus = tier == 1 ? 1 : tier == 2 ? 2 : 4;
                    AttributeInstance armorToughness = player.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS);
                    if (armorToughness != null) {
                        AttributeModifier modifier = new AttributeModifier(
                                UUID.randomUUID(),
                                "jewel.toughness." + tier,
                                toughnessBonus,
                                AttributeModifier.Operation.ADD_NUMBER
                        );
                        armorToughness.addModifier(modifier);
                        newModifiers.put("jewel.toughness." + tier, modifier);
                    }
                    break;

                // Other jewel types are handled by JewelEvents
            }
        }

        // Save new modifiers
        appliedModifiers.put(uuid, newModifiers);

        if (debuggingFlag == 1) {
            Bukkit.getLogger().info("[JewelManager] Applied " + newModifiers.size() + 
                    " attribute modifiers for player: " + player.getName());
        }
    }

    public void removeJewelAttributes(Player player, JewelType type) {
        UUID uuid = player.getUniqueId();

        if (debuggingFlag == 1) {
            Bukkit.getLogger().info("[JewelManager] Removing jewel attributes for player: " + 
                    player.getName() + ", type: " + type.name());
        }

        Map<String, AttributeModifier> playerModifiers = appliedModifiers.getOrDefault(uuid, new HashMap<>());

        // Remove modifiers for the given type
        List<String> keysToRemove = new ArrayList<>();
        for (Map.Entry<String, AttributeModifier> entry : playerModifiers.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("jewel." + type.name().toLowerCase())) {
                // Remove the modifier from the player
                removeAttributeModifier(player, entry.getValue());
                keysToRemove.add(key);
            }
        }

        // Remove keys from the map
        for (String key : keysToRemove) {
            playerModifiers.remove(key);
        }

        if (debuggingFlag == 1) {
            Bukkit.getLogger().info("[JewelManager] Removed " + keysToRemove.size() + 
                    " attribute modifiers for player: " + player.getName());
        }
    }

    private void removeAttributeModifier(Player player, AttributeModifier modifier) {
        for (Attribute attribute : Attribute.values()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(modifier);
            }
        }
    }

    public void removeAllJewelAttributes(Player player) {
        UUID uuid = player.getUniqueId();

        if (debuggingFlag == 1) {
            Bukkit.getLogger().info("[JewelManager] Removing all jewel attributes for player: " + 
                    player.getName());
        }

        Map<String, AttributeModifier> playerModifiers = appliedModifiers.getOrDefault(uuid, new HashMap<>());

        // Remove all modifiers
        for (AttributeModifier modifier : playerModifiers.values()) {
            removeAttributeModifier(player, modifier);
        }

        // Clear the map
        playerModifiers.clear();
        appliedModifiers.put(uuid, playerModifiers);

        // Additional safety check for attributes
        for (Attribute attribute : Attribute.values()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                // Remove any modifiers with our jewel prefix
                for (AttributeModifier modifier : new ArrayList<>(instance.getModifiers())) {
                    if (modifier.getName().startsWith("jewel.")) {
                        instance.removeModifier(modifier);
                        if (debuggingFlag == 1) {
                            Bukkit.getLogger().info("[JewelManager] Removed lingering jewel modifier: " + 
                                    modifier.getName() + " for player: " + player.getName());
                        }
                    }
                }
            }
        }

        if (debuggingFlag == 1) {
            Bukkit.getLogger().info("[JewelManager] Removed all jewel attributes for player: " + 
                    player.getName());
        }
    }

    // Method to create a jewel instance based on type and tier
    public ItemStack createJewel(JewelType type, int tier) {
        if (tier < 1 || tier > 3) {
            if (debuggingFlag == 1) {
                Bukkit.getLogger().warning("[JewelManager] Invalid tier: " + tier + " for jewel type: " + type.name());
            }
            return null;
        }

        Material material = type.getMaterial();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return null;
        }

        String tierMark = tier == 1 ? "&9[ I ]" : tier == 2 ? "&5[ II ]" : "&6[ III ]";
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', tierMark + " &c" + type.getDisplayName()));

        List<String> lore = new ArrayList<>();
        lore.add("");

        // Add jewel effect based on type and tier
        switch (type) {
            case DAMAGE:
                double damageBonus = tier == 1 ? 2 : tier == 2 ? 4 : 8;
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6Grants " + damageBonus + "% Damage"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7The essence of a fire elemental, forged into"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7a jewel that enhances your strength."));
                break;
            case MOVE_SPEED:
                double speedBonus = tier == 1 ? 2 : tier == 2 ? 4 : 8;
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6Grants " + speedBonus + "% Movement Speed"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7A fragment of pure wind energy that"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7allows you to move more swiftly."));
                break;
            case ATTACK_SPEED:
                double attackSpeedBonus = tier == 1 ? 2 : tier == 2 ? 4 : 8;
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6Grants " + attackSpeedBonus + "% Attack Speed"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7This jewel seems to vibrate with energy,"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7allowing your attacks to flow faster."));
                break;
            case HEALTH:
                int healthBonus = tier == 1 ? 10 : tier == 2 ? 20 : 30;
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6Grants +" + healthBonus + " Health"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7A pulsing red jewel that enhances"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7your life force and vitality."));
                break;
            case ARMOR_TOUGHNESS:
                int toughnessBonus = tier == 1 ? 1 : tier == 2 ? 2 : 4;
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6Grants +" + toughnessBonus + " Armor Toughness"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7A gem that hardens your defenses,"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7making you resistant to powerful blows."));
                break;
            case LASTING_HEALING:
                int healAmount = tier == 1 ? 5 : tier == 2 ? 10 : 15;
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6Heals " + healAmount + " HP for 5s after killing an enemy"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7A verdant jewel that mends wounds"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7with the life energy of fallen foes."));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&8Cooldown: 30s"));
                break;
            case AMPLIFIED_HEALING:
                int damageAmpPercent = tier == 1 ? 25 : tier == 2 ? 50 : 100;
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6When healed for 20+ HP in 2.5s"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6Increases damage by " + damageAmpPercent + "% for 5s"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7This gem channels healing energy"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7into offensive power."));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&8Cooldown: 60s"));
                break;
            case JEWEL_OF_FOCUS:
                int cooldownReduction = tier == 1 ? 2 : tier == 2 ? 3 : 5;
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6Reduces other jewel cooldowns by " + cooldownReduction + "%"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7A strange jewel that seems to bend"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7the flow of time around it."));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&cLimit: 3"));
                break;
            case JEWEL_OF_RAGE:
                int procChance = tier == 1 ? 10 : tier == 2 ? 15 : 20;
                int critChance = tier == 1 ? 15 : tier == 2 ? 20 : 30;
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6" + procChance + "% chance on attack to gain Fury"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6Fury grants " + critChance + "% crit chance for 5s"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6Fury extends while killing enemies"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7A jewel filled with ancient rage that"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7infuses your attacks with deadly precision."));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&8Cooldown: 60s"));
                break;
            case STEAM_SALE:
                int sellBonus = tier == 1 ? 20 : tier == 2 ? 30 : 50;
                int craftDiscount = tier == 1 ? 10 : tier == 2 ? 20 : 30;
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6+" + sellBonus + "% money from Quick Sell"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6-" + craftDiscount + "% cost on all crafting"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7This jewel bears the mark of a famous"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7merchant guild, bringing good fortune in trade."));
                break;
            case PHOENIX:
                int cooldownMinutes = tier == 1 ? 60 : tier == 2 ? 45 : 30;
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6When fatal damage would be taken:"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6Heal to full HP and gain 3s immunity"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7A feather from the immortal firebird,"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7crystallized into a gem of rebirth."));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&8Cooldown: " + cooldownMinutes + " minutes"));
                break;
            case ANDERMANT:
                int dupeChance = tier == 1 ? 10 : tier == 2 ? 20 : 30;
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6" + dupeChance + "% chance to duplicate Andermant"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6when picked up"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7This jewel resonates with the energy"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7of Andermant, creating echoes of it."));
                break;
            case CLOVER:
                int extraClovers = tier == 1 ? 1 : tier == 2 ? 2 : 3;
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6Get +" + extraClovers + " additional Clovers"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6when picked up"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7A jewel with the essence of fortune,"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7attracting more clovers to your possession."));
                break;
            case DRAKENMELON:
                int extraMelons = tier == 1 ? 1 : tier == 2 ? 2 : 3;
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6Get +" + extraMelons + " additional DrakenMelons"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6when picked up"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7This jewel pulses with sweet energy,"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7drawing more DrakenMelons to you."));
                break;
            case LOCKPICK:
                int extraLockpicks = tier == 1 ? 3 : tier == 2 ? 4 : 5;
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6Get +" + extraLockpicks + " additional Lockpicks"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6when picked up"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7A dark jewel that whispers secrets"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7of locks and teaches nimble fingers."));
                break;
            case COLLECTOR:
                double executeThreshold = tier == 1 ? 2 : tier == 2 ? 3 : 5;
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6Enemies below " + executeThreshold + "% HP"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6die instantly when attacked"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7A crystallized tear that hungers for"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7the souls of the nearly dead."));
                break;
        }

        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8-------------------"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&aRequired Level: &6 50"));

        meta.setLore(lore);
        meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 10, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE);
        meta.setUnbreakable(true);

        item.setItemMeta(meta);

        return item;
    }
}
