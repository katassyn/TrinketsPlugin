package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SetBonusManager {
    private final Map<SetType, List<SetBonus>> setBonuses;
    private final TrinketsPlugin plugin;
    private final Map<UUID, Integer> playerBlockChanceBonuses = new HashMap<>();
    private final Map<UUID, Integer> playerCritChanceBonuses = new HashMap<>();
    // Track previous active bonuses to detect changes
    private final Map<UUID, Map<SetType, Set<Integer>>> previousActiveBonuses = new HashMap<>();

    public SetBonusManager(TrinketsPlugin plugin) {
        this.plugin = plugin;
        this.setBonuses = new HashMap<>();
        initializeSetBonuses();
    }

    private void initializeSetBonuses() {
        // Hermes Divine Speed Set (2 pieces)
        SetBonus hermesBonus = new SetBonus(SetType.HERMES_DIVINE_SPEED, 2);
        hermesBonus.addPercentageBonus(Attribute.GENERIC_MOVEMENT_SPEED, 5);
        hermesBonus.addFlatBonus(Attribute.GENERIC_MAX_HEALTH, 10);
        setBonuses.computeIfAbsent(SetType.HERMES_DIVINE_SPEED, k -> new ArrayList<>()).add(hermesBonus);

        // Olympian Trinity Set (3 pieces)
        SetBonus olympianBonus = new SetBonus(SetType.OLYMPIAN_TRINITY, 3);
        olympianBonus.addFlatBonus(Attribute.GENERIC_ATTACK_DAMAGE, 50);
        olympianBonus.addFlatBonus(Attribute.GENERIC_MAX_HEALTH, 50);
        olympianBonus.addFlatBonus(Attribute.GENERIC_LUCK, 5);
        setBonuses.computeIfAbsent(SetType.OLYMPIAN_TRINITY, k -> new ArrayList<>()).add(olympianBonus);

        // Divine Olympus Set (2 and 4 pieces)
        SetBonus divineOlympus2 = new SetBonus(SetType.DIVINE_OLYMPUS, 2);
        divineOlympus2.addPercentageBonus(Attribute.GENERIC_MAX_HEALTH, 3);
        setBonuses.computeIfAbsent(SetType.DIVINE_OLYMPUS, k -> new ArrayList<>()).add(divineOlympus2);

        SetBonus divineOlympus4 = new SetBonus(SetType.DIVINE_OLYMPUS, 4);
        divineOlympus4.addFlatBonus(Attribute.GENERIC_ATTACK_DAMAGE, 100);
        divineOlympus4.addPercentageBonus(Attribute.GENERIC_ATTACK_DAMAGE, 1);
        setBonuses.computeIfAbsent(SetType.DIVINE_OLYMPUS, k -> new ArrayList<>()).add(divineOlympus4);

        // Aegis Protection Set (1 piece)
        SetBonus aegisBonus = new SetBonus(SetType.AEGIS_PROTECTION, 1);
        // Block chance is handled separately as it's not a standard attribute
        aegisBonus.addFlatBonus(Attribute.GENERIC_ATTACK_DAMAGE, 50);
        setBonuses.computeIfAbsent(SetType.AEGIS_PROTECTION, k -> new ArrayList<>()).add(aegisBonus);

        // Titan Supremacy Set (2, 4, and 6 pieces)
        SetBonus titanSupremacy2 = new SetBonus(SetType.TITAN_SUPREMACY, 2);
        titanSupremacy2.addPercentageBonus(Attribute.GENERIC_MAX_HEALTH, 3);
        titanSupremacy2.addPercentageBonus(Attribute.GENERIC_ATTACK_DAMAGE, 3);
        titanSupremacy2.addPercentageBonus(Attribute.GENERIC_LUCK, 3);
        setBonuses.computeIfAbsent(SetType.TITAN_SUPREMACY, k -> new ArrayList<>()).add(titanSupremacy2);

        SetBonus titanSupremacy4 = new SetBonus(SetType.TITAN_SUPREMACY, 4);
        titanSupremacy4.addPercentageBonus(Attribute.GENERIC_MAX_HEALTH, 3);
        titanSupremacy4.addPercentageBonus(Attribute.GENERIC_ATTACK_DAMAGE, 3);
        titanSupremacy4.addPercentageBonus(Attribute.GENERIC_LUCK, 3);
        setBonuses.computeIfAbsent(SetType.TITAN_SUPREMACY, k -> new ArrayList<>()).add(titanSupremacy4);

        SetBonus titanSupremacy6 = new SetBonus(SetType.TITAN_SUPREMACY, 6);
        titanSupremacy6.addPercentageBonus(Attribute.GENERIC_MAX_HEALTH, 3);
        titanSupremacy6.addPercentageBonus(Attribute.GENERIC_ATTACK_DAMAGE, 3);
        titanSupremacy6.addPercentageBonus(Attribute.GENERIC_LUCK, 3);
        setBonuses.computeIfAbsent(SetType.TITAN_SUPREMACY, k -> new ArrayList<>()).add(titanSupremacy6);

        // White Frost T5 Set (2 and 4 pieces)
        SetBonus whiteFrostT5_2 = new SetBonus(SetType.WHITE_FROST_T5, 2);
        whiteFrostT5_2.addPercentageBonus(Attribute.GENERIC_MAX_HEALTH, 10); // +10% Health
        whiteFrostT5_2.addFlatBonus(Attribute.GENERIC_ARMOR, 10); // +10 Armor
        setBonuses.computeIfAbsent(SetType.WHITE_FROST_T5, k -> new ArrayList<>()).add(whiteFrostT5_2);

        SetBonus whiteFrostT5_4 = new SetBonus(SetType.WHITE_FROST_T5, 4);
        // [4] piece has no additional stat bonuses - only passive effect
        setBonuses.computeIfAbsent(SetType.WHITE_FROST_T5, k -> new ArrayList<>()).add(whiteFrostT5_4);

        // Black Frost T5 Set (2 and 4 pieces)
        SetBonus blackFrostT5_2 = new SetBonus(SetType.BLACK_FROST_T5, 2);
        blackFrostT5_2.addPercentageBonus(Attribute.GENERIC_ATTACK_DAMAGE, 10); // +10% Damage
        blackFrostT5_2.addPercentageBonus(Attribute.GENERIC_ATTACK_SPEED, 10); // +10% Attack Speed
        setBonuses.computeIfAbsent(SetType.BLACK_FROST_T5, k -> new ArrayList<>()).add(blackFrostT5_2);

        SetBonus blackFrostT5_4 = new SetBonus(SetType.BLACK_FROST_T5, 4);
        // [4] piece has no additional stat bonuses - only passive effect
        setBonuses.computeIfAbsent(SetType.BLACK_FROST_T5, k -> new ArrayList<>()).add(blackFrostT5_4);

        // White Frost T3 Set (2 and 4 pieces)
        SetBonus whiteFrostT3_2 = new SetBonus(SetType.WHITE_FROST_T3, 2);
        whiteFrostT3_2.addFlatBonus(Attribute.GENERIC_MAX_HEALTH, 50); // +50 Health
        whiteFrostT3_2.addPercentageBonus(Attribute.GENERIC_KNOCKBACK_RESISTANCE, 10); // +10% Knockback Resist
        setBonuses.computeIfAbsent(SetType.WHITE_FROST_T3, k -> new ArrayList<>()).add(whiteFrostT3_2);

        SetBonus whiteFrostT3_4 = new SetBonus(SetType.WHITE_FROST_T3, 4);
        // [4] piece has no additional stat bonuses - only passive effect
        setBonuses.computeIfAbsent(SetType.WHITE_FROST_T3, k -> new ArrayList<>()).add(whiteFrostT3_4);

        // Black Frost T3 Set (2 and 4 pieces)
        SetBonus blackFrostT3_2 = new SetBonus(SetType.BLACK_FROST_T3, 2);
        blackFrostT3_2.addFlatBonus(Attribute.GENERIC_ATTACK_DAMAGE, 40); // +40 Damage
        setBonuses.computeIfAbsent(SetType.BLACK_FROST_T3, k -> new ArrayList<>()).add(blackFrostT3_2);

        SetBonus blackFrostT3_4 = new SetBonus(SetType.BLACK_FROST_T3, 4);
        // [4] piece has no additional stat bonuses - only passive effect
        // NOTE: +5% Crit Chance will be tracked separately
        setBonuses.computeIfAbsent(SetType.BLACK_FROST_T3, k -> new ArrayList<>()).add(blackFrostT3_4);
    }

    public void updatePlayerSetBonuses(Player player, PlayerData playerData) {
        // Remove all existing set bonuses
        removeAllSetBonuses(player);

        // Count equipped set pieces (accessories + armor)
        Map<SetType, Integer> setPieceCounts = countAllSetPieces(player, playerData);

        // Debug output
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Set pieces for " + player.getName() + ":");
            for (Map.Entry<SetType, Integer> entry : setPieceCounts.entrySet()) {
                plugin.getLogger().info("  " + entry.getKey().getDisplayName() + ": " + entry.getValue());
            }
        }

        // Track current active bonuses
        Map<SetType, Set<Integer>> currentActiveBonuses = new HashMap<>();
        UUID playerId = player.getUniqueId();

        // Apply eligible set bonuses
        for (Map.Entry<SetType, Integer> entry : setPieceCounts.entrySet()) {
            SetType setType = entry.getKey();
            int equippedPieces = entry.getValue();

            List<SetBonus> bonusesForSet = setBonuses.get(setType);
            if (bonusesForSet != null) {
                for (SetBonus bonus : bonusesForSet) {
                    if (equippedPieces >= bonus.getRequiredPieces()) {
                        applySetBonus(player, bonus);

                        // Track this active bonus
                        currentActiveBonuses.computeIfAbsent(setType, k -> new HashSet<>())
                            .add(bonus.getRequiredPieces());
                    }
                }
            }

            // Handle special bonuses for Aegis Protection (Block Chance)
            if (setType == SetType.AEGIS_PROTECTION && equippedPieces >= 1) {
                playerBlockChanceBonuses.put(player.getUniqueId(), 10);
            }

            // Handle Black Frost T3 Crit Chance bonus
            if (setType == SetType.BLACK_FROST_T3 && equippedPieces >= 2) {
                playerCritChanceBonuses.put(player.getUniqueId(), 5); // +5% Crit
            }
        }

        // Compare with previous state and notify only on meaningful changes
        notifyBonusChanges(player, currentActiveBonuses);

        // Store current state for next comparison
        previousActiveBonuses.put(playerId, currentActiveBonuses);
    }

    private Map<SetType, Integer> countAllSetPieces(Player player, PlayerData playerData) {
        Map<SetType, Integer> counts = new HashMap<>();

        // Check accessories from PlayerData
        for (AccessoryType type : AccessoryType.values()) {
            ItemStack item = playerData.getAccessory(type);
            if (item != null) {
                SetType setType = getSetTypeFromItem(item);
                if (setType != null) {
                    counts.put(setType, counts.getOrDefault(setType, 0) + 1);
                }
            }
        }

        // Check armor slots (helmet, chestplate, leggings, boots)
        ItemStack[] armorContents = player.getInventory().getArmorContents();
        for (ItemStack armorPiece : armorContents) {
            if (armorPiece != null) {
                SetType setType = getSetTypeFromItem(armorPiece);
                if (setType != null) {
                    counts.put(setType, counts.getOrDefault(setType, 0) + 1);
                }
            }
        }

        return counts;
    }

    private SetType getSetTypeFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;

        List<String> lore = meta.getLore();
        if (lore == null) return null;

        // Pattern to find set line - looking for "Set: SetName"
        for (String line : lore) {
            // First strip color codes to check for "Set:" keyword
            String strippedLine = ChatColor.stripColor(line);

            if (strippedLine != null && strippedLine.contains("Set:")) {
                // Extract set name after "Set:"
                String[] parts = strippedLine.split("Set:", 2);
                if (parts.length > 1) {
                    String setName = parts[1].trim();

                    // Try to match with our set types
                    for (SetType setType : SetType.values()) {
                        if (setType.getDisplayName().equalsIgnoreCase(setName)) {
                            return setType;
                        }
                    }
                }
            }
        }

        return null;
    }

    private void applySetBonus(Player player, SetBonus setBonus) {
        for (SetBonus.BonusModifier bonusModifier : setBonus.getBonuses()) {
            Attribute attribute = bonusModifier.getAttribute();
            AttributeModifier modifier = bonusModifier.getModifier();

            AttributeInstance attributeInstance = player.getAttribute(attribute);
            if (attributeInstance != null) {
                // Create a unique modifier with UUID to avoid conflicts
                AttributeModifier uniqueModifier = new AttributeModifier(
                        UUID.randomUUID(),
                        modifier.getName(),
                        modifier.getAmount(),
                        modifier.getOperation()
                );
                attributeInstance.addModifier(uniqueModifier);
            }
        }
    }

    private void removeAllSetBonuses(Player player) {
        for (Attribute attribute : Attribute.values()) {
            AttributeInstance attributeInstance = player.getAttribute(attribute);
            if (attributeInstance != null) {
                for (AttributeModifier modifier : new ArrayList<>(attributeInstance.getModifiers())) {
                    if (modifier.getName().startsWith("set_bonus.")) {
                        attributeInstance.removeModifier(modifier);
                    }
                }
            }
        }
        // Clear block chance bonus
        playerBlockChanceBonuses.remove(player.getUniqueId());
        // Clear crit chance bonus
        playerCritChanceBonuses.remove(player.getUniqueId());
    }

    public int getSetPieceCount(Player player, SetType setType) {
        PlayerData playerData = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (playerData == null) return 0;

        Map<SetType, Integer> counts = countAllSetPieces(player, playerData);
        return counts.getOrDefault(setType, 0);
    }

    public boolean hasSetBonus(Player player, SetType setType, int requiredPieces) {
        return getSetPieceCount(player, setType) >= requiredPieces;
    }

    public int getBlockChanceBonus(Player player) {
        return playerBlockChanceBonuses.getOrDefault(player.getUniqueId(), 0);
    }

    public int getCritChanceBonus(Player player) {
        return playerCritChanceBonuses.getOrDefault(player.getUniqueId(), 0);
    }

    private void notifyBonusChanges(Player player, Map<SetType, Set<Integer>> currentBonuses) {
        UUID playerId = player.getUniqueId();
        Map<SetType, Set<Integer>> previousBonuses = previousActiveBonuses.get(playerId);

        // If no previous state, don't notify (first time equipping)
        if (previousBonuses == null) {
            return;
        }

        // Check for new bonuses or removed bonuses
        for (Map.Entry<SetType, Set<Integer>> entry : currentBonuses.entrySet()) {
            SetType setType = entry.getKey();
            Set<Integer> currentPieces = entry.getValue();
            Set<Integer> previousPieces = previousBonuses.getOrDefault(setType, new HashSet<>());

            // Find newly activated bonuses
            for (Integer pieces : currentPieces) {
                if (!previousPieces.contains(pieces)) {
                    player.sendMessage(ChatColor.GREEN + "✓ Set Bonus Activated: " + ChatColor.YELLOW +
                            setType.getDisplayName() + " [" + pieces + " pieces]");
                }
            }
        }

        // Check for deactivated bonuses
        for (Map.Entry<SetType, Set<Integer>> entry : previousBonuses.entrySet()) {
            SetType setType = entry.getKey();
            Set<Integer> previousPieces = entry.getValue();
            Set<Integer> currentPieces = currentBonuses.getOrDefault(setType, new HashSet<>());

            // Find deactivated bonuses
            for (Integer pieces : previousPieces) {
                if (!currentPieces.contains(pieces)) {
                    player.sendMessage(ChatColor.RED + "✗ Set Bonus Deactivated: " + ChatColor.GRAY +
                            setType.getDisplayName() + " [" + pieces + " pieces]");
                }
            }
        }
    }
}