package com.maks.trinketsplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayerAttribStatsGUI {

    public static void open(Player player) {
        // Larger, structured layout
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.DARK_AQUA + "Your Stats");

        // Fetch player data for custom stats
        PlayerData data = TrinketsPlugin.getInstance().getDatabaseManager().getPlayerData(player.getUniqueId());

        // Section: Core Attributes (header + items)
        gui.setItem(9, createHeader(Material.LIGHT_BLUE_STAINED_GLASS_PANE, ChatColor.AQUA + "Core Attributes"));
        addAttributeItem(gui, 10, Material.APPLE, ChatColor.RED + "Max Health",
                player, Attribute.GENERIC_MAX_HEALTH, false);
        addAttributeItem(gui, 11, Material.IRON_SWORD, ChatColor.GOLD + "Attack Damage",
                player, Attribute.GENERIC_ATTACK_DAMAGE, false);
        addAttributeItem(gui, 12, Material.CLOCK, ChatColor.YELLOW + "Attack Speed",
                player, Attribute.GENERIC_ATTACK_SPEED, false);
        addAttributeItem(gui, 13, Material.FEATHER, ChatColor.AQUA + "Movement Speed",
                player, Attribute.GENERIC_MOVEMENT_SPEED, true);
        addAttributeItem(gui, 14, Material.IRON_CHESTPLATE, ChatColor.BLUE + "Armor",
                player, Attribute.GENERIC_ARMOR, false);
        addAttributeItem(gui, 15, Material.NETHERITE_CHESTPLATE, ChatColor.DARK_BLUE + "Armor Toughness",
                player, Attribute.GENERIC_ARMOR_TOUGHNESS, false);
        addAttributeItem(gui, 16, Material.EMERALD, ChatColor.GREEN + "Luck",
                player, Attribute.GENERIC_LUCK, false);
        addAttributeItem(gui, 17, Material.ANVIL, ChatColor.DARK_GRAY + "Knockback Resistance",
                player, Attribute.GENERIC_KNOCKBACK_RESISTANCE, true);

        // Section: Defense & Block
        gui.setItem(18, createHeader(Material.YELLOW_STAINED_GLASS_PANE, ChatColor.GOLD + "Defense & Block"));
        int blockChance = 0;
        int blockStrength = 0;
        if (data != null) {
            blockChance = data.getBlockChance();
            blockStrength = data.getBlockStrength();
        }
        // Also scan player's equipped armor for block stats
        for (ItemStack armorPiece : player.getInventory().getArmorContents()) {
            if (armorPiece != null) {
                int[] armorBlockStats = PlayerData.parseBlockStatsFromItem(armorPiece);
                blockChance += armorBlockStats[0];
                blockStrength += armorBlockStats[1];
            }
        }
        int setBlockBonus = TrinketsPlugin.getInstance().getSetBonusManager().getBlockChanceBonus(player);
        int totalBlockChance = Math.min(blockChance + setBlockBonus, 100); // Capped at 100%

        List<String> bcLore = new ArrayList<>();
        bcLore.add(bullet("Base: 0%"));
        bcLore.add(bullet("+Bonus: " + totalBlockChance + "%"));
        bcLore.add(bullet("Total: " + totalBlockChance + "%"));
        gui.setItem(19, createItem(Material.SHIELD, ChatColor.GOLD + "Block Chance", bcLore));

        int baseBlockStrength = 35; // same as info from DatabaseManager
        int totalBlockStrength = baseBlockStrength + blockStrength;
        List<String> bsLore = new ArrayList<>();
        bsLore.add(bullet("Base: " + baseBlockStrength + "%"));
        bsLore.add(bullet("+Bonus: " + blockStrength + "%"));
        bsLore.add(bullet("Total: " + totalBlockStrength + "%"));
        gui.setItem(20, createItem(Material.IRON_BLOCK, ChatColor.GOLD + "Block Strength", bsLore));

        ProtectionReductionCalculator.ProtectionStats protectionStats = ProtectionReductionCalculator.calculate(player);
        List<String> protectionLore = new ArrayList<>();
        protectionLore.add(bullet("Sum Levels: " + protectionStats.totalProtection()));
        if (protectionStats.cappedTotalProtection() != protectionStats.totalProtection()) {
            protectionLore.add(bullet("Capped Levels: " + protectionStats.cappedTotalProtection()));
        }
        protectionLore.add(bullet("Base Reduction: " + formatPercent(protectionStats.baseReductionApplied())));
        protectionLore.add(bullet("Extra Reduction: " + formatPercent(protectionStats.extraReduction())));
        protectionLore.add(bullet("Total Reduction: " + formatPercent(protectionStats.totalReduction())));
        protectionLore.add(bullet("Damage Taken: " + formatPercent(protectionStats.finalDamageMultiplier())));
        gui.setItem(21, createItem(Material.ENCHANTED_BOOK, ChatColor.BLUE + "Protection Reduction", protectionLore));

        // Section: Trinkets Stats (custom stats from equipment)
        gui.setItem(22, createHeader(Material.CYAN_STAINED_GLASS_PANE, ChatColor.DARK_AQUA + "Trinkets Stats"));
        addTrinketsStats(gui, player, 23);

        // Section: Class & Skills
        gui.setItem(27, createHeader(Material.PURPLE_STAINED_GLASS_PANE, ChatColor.LIGHT_PURPLE + "Class & Skills"));
        addMyExperienceStats(gui, player, 28);

        // Section: Pets
        gui.setItem(36, createHeader(Material.ORANGE_STAINED_GLASS_PANE, ChatColor.GOLD + "Pet Bonuses"));
        addPetStats(gui, player, 37);

        // Section: Biologist
        gui.setItem(45, createHeader(Material.GREEN_STAINED_GLASS_PANE, ChatColor.DARK_GREEN + "Biologist Buffs"));
        addBiologistStats(gui, player, 46);

        // Close button
        gui.setItem(53, createItem(Material.ARROW, ChatColor.RED + "Close", List.of(ChatColor.GRAY + "Close this menu")));

        // Fill remaining with neutral filler for clean look
        fillEmpty(gui);

        player.openInventory(gui);
    }

    private static void addTrinketsStats(Inventory gui, Player player, int startSlot) {
        // Evasion Chance (CAPPED at 25%)
        double rawEvasion = TrinketsStatsAPI.getRawEvasionChance(player);
        double cappedEvasion = TrinketsStatsAPI.getEvasionChance(player);
        boolean evasionCapped = rawEvasion > TrinketsStatsAPI.EVASION_CAP;

        List<String> evasionLore = new ArrayList<>();
        if (evasionCapped) {
            evasionLore.add(bullet("Raw: " + String.format("%.1f%%", rawEvasion)));
            evasionLore.add(ChatColor.RED + "  » CAPPED!");
            evasionLore.add(bullet("Effective: " + String.format("%.1f%%", cappedEvasion)));
        } else {
            evasionLore.add(bullet("Value: " + String.format("%.1f%%", cappedEvasion)));
        }
        evasionLore.add("");
        evasionLore.add(ChatColor.GRAY + "Cap: " + ChatColor.YELLOW + String.format("%.0f%%", TrinketsStatsAPI.EVASION_CAP));
        gui.setItem(startSlot, createItem(Material.PHANTOM_MEMBRANE, ChatColor.AQUA + "Evasion Chance 💨", evasionLore));

        // Armor Penetration (CAPPED at 35%)
        double rawArmorPen = TrinketsStatsAPI.getRawArmorPenetration(player);
        double cappedArmorPen = TrinketsStatsAPI.getArmorPenetration(player);
        boolean armorPenCapped = rawArmorPen > TrinketsStatsAPI.ARMOR_PENETRATION_CAP;

        List<String> armorPenLore = new ArrayList<>();
        if (armorPenCapped) {
            armorPenLore.add(bullet("Raw: " + String.format("%.1f%%", rawArmorPen)));
            armorPenLore.add(ChatColor.RED + "  » CAPPED!");
            armorPenLore.add(bullet("Effective: " + String.format("%.1f%%", cappedArmorPen)));
        } else {
            armorPenLore.add(bullet("Value: " + String.format("%.1f%%", cappedArmorPen)));
        }
        armorPenLore.add("");
        armorPenLore.add(ChatColor.GRAY + "Cap: " + ChatColor.YELLOW + String.format("%.0f%%", TrinketsStatsAPI.ARMOR_PENETRATION_CAP));
        gui.setItem(startSlot + 1, createItem(Material.IRON_SWORD, ChatColor.DARK_RED + "Armor Penetration 🗡", armorPenLore));

        // Critical Chance (CAPPED at 50%)
        double rawCritChance = TrinketsStatsAPI.getRawCritChance(player);
        double cappedCritChance = TrinketsStatsAPI.getCritChance(player);
        boolean critChanceCapped = rawCritChance > TrinketsStatsAPI.CRITICAL_CHANCE_CAP;

        List<String> critChanceLore = new ArrayList<>();
        if (critChanceCapped) {
            critChanceLore.add(bullet("Raw: " + String.format("%.1f%%", rawCritChance)));
            critChanceLore.add(ChatColor.RED + "  » CAPPED!");
            critChanceLore.add(bullet("Effective: " + String.format("%.1f%%", cappedCritChance)));
        } else {
            critChanceLore.add(bullet("Value: " + String.format("%.1f%%", cappedCritChance)));
        }
        critChanceLore.add("");
        critChanceLore.add(ChatColor.GRAY + "Cap: " + ChatColor.YELLOW + String.format("%.0f%%", TrinketsStatsAPI.CRITICAL_CHANCE_CAP));
        critChanceLore.add(ChatColor.DARK_GRAY + "(Bonus from trinkets/armor)");
        gui.setItem(startSlot + 2, createItem(Material.TARGET, ChatColor.RED + "Critical Chance 🎯", critChanceLore));

        // Critical Damage (NO CAP)
        double critDamage = TrinketsStatsAPI.getCritDamage(player);
        List<String> critDamageLore = new ArrayList<>();
        critDamageLore.add(bullet("Bonus: +" + String.format("%.1f%%", critDamage)));
        critDamageLore.add("");
        critDamageLore.add(ChatColor.DARK_GRAY + "(Adds to base 200% crit)");
        gui.setItem(startSlot + 3, createItem(Material.TNT, ChatColor.RED + "Critical Damage 💥", critDamageLore));
    }

    private static void addMyExperienceStats(Inventory gui, Player player, int slot) {
        try {
            org.bukkit.plugin.Plugin plug = Bukkit.getPluginManager().getPlugin("MyExperiencePlugin");
            if (plug == null) return;

            Class<?> myExpClazz = Class.forName("com.maks.myexperienceplugin.MyExperiencePlugin");
            if (!myExpClazz.isInstance(plug)) return;

            // Class/Ascendancy
            String playerClass = "Unknown";
            String ascendancy = "None";
            try {
                Object classMgr = myExpClazz.getMethod("getClassManager").invoke(plug);
                if (classMgr != null) {
                    playerClass = String.valueOf(classMgr.getClass().getMethod("getPlayerClass", java.util.UUID.class)
                            .invoke(classMgr, player.getUniqueId()));
                    Object asc = classMgr.getClass().getMethod("getPlayerAscendancy", java.util.UUID.class)
                            .invoke(classMgr, player.getUniqueId());
                    ascendancy = String.valueOf(asc);
                    if (ascendancy == null || ascendancy.isEmpty()) ascendancy = "None";
                }
            } catch (Exception ignored) { }

            // Skill stats
            double critChance = 0.0;
            double critMult = 2.0;
            double dmgMult = 1.0;
            double evade = 0.0;
            double shieldBlock = 0.0;
            try {
                Object seHandler = myExpClazz.getMethod("getSkillEffectsHandler").invoke(plug);
                if (seHandler != null) {
                    Object stats = seHandler.getClass().getMethod("getPlayerStats", Player.class).invoke(seHandler, player);
                    if (stats != null) {
                        // Damage multiplier
                        try { dmgMult = ((Number) stats.getClass().getMethod("getDamageMultiplier").invoke(stats)).doubleValue(); } catch (Exception ignored) {}
                        // Evade, Shield Block
                        try { evade = ((Number) stats.getClass().getMethod("getEvadeChance").invoke(stats)).doubleValue(); } catch (Exception ignored) {}
                        try { shieldBlock = ((Number) stats.getClass().getMethod("getShieldBlockChance").invoke(stats)).doubleValue(); } catch (Exception ignored) {}
                        // Crit chance (from stats if available)
                        try { critChance = ((Number) stats.getClass().getMethod("getCriticalChance").invoke(stats)).doubleValue(); } catch (Exception ignored) {}
                    }
                }

                // Critical system multiplier and chance override
                Object critSys = myExpClazz.getMethod("getCriticalStrikeSystem").invoke(plug);
                if (critSys != null) {
                    try { critMult = ((Number) critSys.getClass().getMethod("getCriticalDamageMultiplier", Player.class).invoke(critSys, player)).doubleValue(); } catch (Exception ignored) {}
                    try { critChance = ((Number) critSys.getClass().getMethod("getCriticalChance", Player.class).invoke(critSys, player)).doubleValue(); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) { }

            // Get abyssal bonuses
            double abyssalCritChance = getAbyssalStat(player, "CRIT_CHANCE_PCT");
            double abyssalCritDmg = getAbyssalStat(player, "CRIT_DMG_PCT");

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Class: " + ChatColor.WHITE + playerClass);
            lore.add(ChatColor.GRAY + "Ascendancy: " + ChatColor.WHITE + ascendancy);

            // Crit Chance with abyssal
            String critLine = ChatColor.GRAY + "Crit Chance: " + ChatColor.WHITE + String.format("%.1f%%", critChance);
            if (abyssalCritChance > 0) {
                critLine += ChatColor.DARK_PURPLE + " (+" + String.format("%.1f%%", abyssalCritChance) + " Abyssal)";
            }
            lore.add(critLine);

            // Crit Damage with abyssal
            String critDmgLine = ChatColor.GRAY + "Crit Damage: " + ChatColor.WHITE + String.format("%.2fx", critMult);
            if (abyssalCritDmg > 0) {
                double addedMult = abyssalCritDmg / 100.0;
                critDmgLine += ChatColor.DARK_PURPLE + " (+" + String.format("%.2fx", addedMult) + " Abyssal)";
            }
            lore.add(critDmgLine);

            lore.add(ChatColor.GRAY + "Damage Mult.: " + ChatColor.WHITE + String.format("%.2fx", dmgMult));
            lore.add(ChatColor.GRAY + "Evade: " + ChatColor.WHITE + String.format("%.1f%%", evade));
            lore.add(ChatColor.GRAY + "Shield Block: " + ChatColor.WHITE + String.format("%.1f%%", shieldBlock));

            // Effective single-hit damage helpers based on attribute damage
            AttributeInstance atkAttr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (atkAttr != null) {
                double atk = atkAttr.getValue();
                double dmgM = Math.max(0.0, dmgMult);
                double critC = Math.max(0.0, critChance) / 100.0;
                double critM = Math.max(0.0, critMult);

                double effective = atk * dmgM;
                double expectedCrit = atk * (1.0 + critC * (critM - 1.0));
                double expectedCritWithMult = effective * (1.0 + critC * (critM - 1.0));

                lore.add("");
                lore.add(ChatColor.GRAY + "Effective Damage: " + ChatColor.WHITE + String.format("%.2f", effective));
                lore.add(ChatColor.GRAY + "Expected Crit: " + ChatColor.WHITE + String.format("%.2f", expectedCrit));
                lore.add(ChatColor.GRAY + "Expected Crit (x Mult): " + ChatColor.WHITE + String.format("%.2f", expectedCritWithMult));
            }

            gui.setItem(slot, createItem(Material.ENCHANTED_BOOK, ChatColor.LIGHT_PURPLE + "Class & Skills", lore));
        } catch (Throwable ignored) {
        }
    }

    private static void addPetStats(Inventory gui, Player player, int slot) {
        try {
            org.bukkit.plugin.Plugin plug = Bukkit.getPluginManager().getPlugin("PetPlugin");
            if (plug == null) return;

            Class<?> petClazz = Class.forName("pl.yourserver.PetPlugin");
            if (!petClazz.isInstance(plug)) return;

            Object integrations = petClazz.getMethod("getIntegrationManager").invoke(plug);
            if (integrations == null) return;

            // Ensure placeholders are up to date for this player
            try { integrations.getClass().getMethod("updatePlayerPlaceholders", Player.class).invoke(integrations, player); } catch (Exception ignored) {}

            // Read internal map via reflection (playerPlaceholders)
            java.lang.reflect.Field f = integrations.getClass().getDeclaredField("playerPlaceholders");
            f.setAccessible(true);
            Object map = f.get(integrations);
            if (!(map instanceof java.util.Map)) return;
            @SuppressWarnings("unchecked")
            java.util.Map<java.util.UUID, java.util.Map<String, Double>> pp = (java.util.Map<java.util.UUID, java.util.Map<String, Double>>) map;
            java.util.Map<String, Double> vals = pp.get(player.getUniqueId());
            if (vals == null || vals.isEmpty()) return;

            java.util.Map<String, String> labels = new java.util.HashMap<>();
            // Generic
            labels.put("pet_slot_bonus", "Extra Pet Slots");
            labels.put("donkey_extra_storage", "Donkey Extra Storage");
            labels.put("boss_damage_bonus", "Boss Damage");
            labels.put("mythicmobs_boss_damage", "Boss Damage");
            labels.put("dungeon_free_tp_chance", "Free Teleport Chance");
            labels.put("fishing_chest_bonus", "Fishing Chest Bonus");
            labels.put("farm_growth_speed", "Farm Growth Speed");
            labels.put("irongolem_damage_boost", "Damage Boost");
            labels.put("turtle_damage_reduction", "Damage Reduction");
            labels.put("llama_mob_damage", "Mob Damage Bonus");
            labels.put("alchemy_potion_duration_bonus", "Potion Duration Bonus");
            labels.put("mine_mobsphere_chance", "Mob Sphere Chance");
            labels.put("beekeeper_honey_speed", "Honey Speed");
            labels.put("beekeeper_honey_quality", "Honey Quality");
            labels.put("beekeeper_rare_honey_chance", "Rare Honey Chance");
            labels.put("crafting_cost_reduction", "Crafting Cost Reduction");
            labels.put("crafting_refund_chance", "Crafting Refund Chance");
            labels.put("farm_yield_bonus", "Farm Yield Bonus");
            labels.put("farm_double_harvest_chance", "Double Harvest Chance");
            labels.put("mine_rare_ore_chance", "Rare Ore Chance");
            labels.put("mine_rare_sphere_chance", "Rare Sphere Chance");
            labels.put("mine_mob_spawn_chance", "Mob Spawn Chance");
            labels.put("fishing_ocean_treasure_chance", "Ocean Treasure Chance");
            labels.put("fishing_rune_chance", "Fishing Rune Chance");
            labels.put("fishing_map_chance", "Fishing Map Chance");
            labels.put("fishing_all_bonuses", "Fishing Bonuses");
            labels.put("mine_all_bonuses", "Mining Bonuses");
            labels.put("wolf_pvp_damage", "PvP Damage");
            labels.put("wolf_pvp_damage_reduction", "PvP Damage Reduction");
            labels.put("wolf_pvp_lifesteal", "PvP Lifesteal");
            labels.put("pig_money_chance", "Money Chance on Kill");
            labels.put("mythicmobs_item_chance", "Mythic Item Chance");

            java.util.Set<String> booleanKeys = new java.util.HashSet<>(java.util.Arrays.asList(
                    "fishing_water_walking", "mine_xray_vision", "beekeeper_auto_collect",
                    "farm_guaranteed_rare", "effect_duplicate", "mythicmobs_no_normal_drops",
                    "mythicmobs_boss_wither"
            ));
            java.util.Set<String> countKeys = new java.util.HashSet<>(java.util.Arrays.asList(
                    "pet_slot_bonus", "donkey_extra_storage"
            ));

            // Build human-friendly lines
            java.util.List<String> lines = new java.util.ArrayList<>();

            // Dynamic dungeon qX damage
            for (int i = 1; i <= 10; i++) {
                String key = "dungeon_q" + i + "_damage";
                if (vals.containsKey(key)) {
                    double v = vals.getOrDefault(key, 0.0);
                    if (v > 0) {
                        lines.add(bullet("Dungeon Q" + i + " Damage: " + formatPercentGuess(v)));
                    }
                }
            }

            // Regular keys
            java.util.List<String> keys = new java.util.ArrayList<>(vals.keySet());
            java.util.Collections.sort(keys);
            for (String key : keys) {
                double v = vals.getOrDefault(key, 0.0);
                if (v <= 0) continue;
                // dungeon_q handled above
                if (key.startsWith("dungeon_q") && key.endsWith("_damage")) continue;
                String label = labels.getOrDefault(key, toTitle(key));

                String formatted;
                if (booleanKeys.contains(key)) {
                    formatted = "Enabled";
                } else if (countKeys.contains(key)) {
                    formatted = String.format("+%.0f", v);
                } else if ("wolf_pvp_lifesteal".equals(key)) {
                    formatted = formatPercentGuess(v);
                } else {
                    formatted = formatPercentGuess(v);
                }
                lines.add(bullet(label + ": " + formatted));
            }

            if (lines.isEmpty()) {
                lines.add(ChatColor.DARK_GRAY + "No active pet bonuses detected");
            }

            gui.setItem(slot, createItem(Material.NAME_TAG, ChatColor.GOLD + "Pet Bonuses", lines));
        } catch (Throwable ignored) {
        }
    }

    private static String toTitle(String key) {
        String k = key.replace('_', ' ').trim();
        if (k.isEmpty()) return key;
        String[] parts = k.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    private static String formatPercentGuess(double v) {
        // Heuristic: treat <=1.0 as fraction (x100), else assume already percent
        double pct = v <= 1.0 ? v * 100.0 : v;
        return String.format("+%.1f%%", pct);
    }

    private static void addBiologistStats(Inventory gui, Player player, int slot) {
        try {
            org.bukkit.plugin.Plugin plug = Bukkit.getPluginManager().getPlugin("biologPlugin");
            if (plug == null) return;
            Class<?> bioClazz = Class.forName("org.maks.biologPlugin.BiologPlugin");
            if (!bioClazz.isInstance(plug)) return;

            // Access private field buffManager
            java.lang.reflect.Field bf = bioClazz.getDeclaredField("buffManager");
            bf.setAccessible(true);
            Object buffMgr = bf.get(plug);
            if (buffMgr == null) return;

            // Call package-private loadPlayerBuffs(UUID)
            java.lang.reflect.Method load = buffMgr.getClass().getDeclaredMethod("loadPlayerBuffs", java.util.UUID.class);
            load.setAccessible(true);
            Object buffs = load.invoke(buffMgr, player.getUniqueId());
            if (buffs == null) return;

            double flatDmg = ((Number) buffs.getClass().getMethod("getFlatDamage").invoke(buffs)).doubleValue();
            double flatHp = ((Number) buffs.getClass().getMethod("getFlatHp").invoke(buffs)).doubleValue();
            double multiDmg = ((Number) buffs.getClass().getMethod("getMultiDamage").invoke(buffs)).doubleValue();
            double multiHp = ((Number) buffs.getClass().getMethod("getMultiHp").invoke(buffs)).doubleValue();

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Flat Damage: " + ChatColor.WHITE + String.format("+%.0f", flatDmg));
            lore.add(ChatColor.GRAY + "Flat Health: " + ChatColor.WHITE + String.format("+%.0f", flatHp));
            lore.add(ChatColor.GRAY + "% Damage: " + ChatColor.WHITE + String.format("+%.1f%%", multiDmg * 100.0));
            lore.add(ChatColor.GRAY + "% Health: " + ChatColor.WHITE + String.format("+%.1f%%", multiHp * 100.0));

            gui.setItem(slot, createItem(Material.TOTEM_OF_UNDYING, ChatColor.DARK_GREEN + "Biologist Buffs", lore));
        } catch (Throwable ignored) {
        }
    }

    private static void addAttributeItem(Inventory gui, int slot, Material material, String name,
                                         Player player, Attribute attribute, boolean percentFormat) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;

        double base = instance.getBaseValue();
        double value = instance.getValue();
        double bonus = value - base;

        List<String> lore = new ArrayList<>();

        // Special handling for Luck with soft-cap
        if (attribute == Attribute.GENERIC_LUCK) {
            TrinketsStatsAPI.LuckBreakdown breakdown = TrinketsStatsAPI.getLuckBreakdown(player);

            lore.add(bullet("Base: " + formatNumber(breakdown.baseValue)));
            lore.add(bullet("Flat Bonus: " + formatNumber(breakdown.rawFlatBonus)));

            // Show soft-cap reduction if applicable
            if (breakdown.rawFlatBonus > 30.0) {
                double reduction = breakdown.rawFlatBonus - breakdown.effectiveFlatBonus;
                lore.add(ChatColor.YELLOW + "  » Soft-cap (30+): -" + formatNumber(reduction));
                lore.add(bullet("Effective Flat: " + formatNumber(breakdown.effectiveFlatBonus)));
            }

            // Show scalar multiplier if present
            if (breakdown.scalarMultiplier > 0) {
                lore.add(bullet("Scalar Bonus: +" + formatPercent(breakdown.scalarMultiplier)));
            }

            lore.add(ChatColor.GREEN + "  ━━━━━━━━━━━━━━");
            lore.add(ChatColor.AQUA + "  ⚡ Effective: " + ChatColor.WHITE + formatNumber(breakdown.effectiveLuck));

            // Add soft-cap explanation
            lore.add("");
            lore.add(ChatColor.GRAY + "Soft-cap: Luck above 30");
            lore.add(ChatColor.GRAY + "counts as 10% (0.1)");

            // Add abyssal bonuses for Luck
            double abyssalLuck = getAbyssalStat(player, "LUCK_ADD");
            if (abyssalLuck > 0) {
                lore.add("");
                lore.add(ChatColor.DARK_PURPLE + "  +Abyssal: " + formatNumber(abyssalLuck));
            }
        } else {
            // Standard attribute display
            if (percentFormat) {
                lore.add(bullet("Base: " + formatPercent(base)));
                lore.add(bullet("+Bonus: " + formatPercent(bonus)));
                lore.add(bullet("Total: " + formatPercent(value)));
            } else {
                lore.add(bullet("Base: " + formatNumber(base)));
                lore.add(bullet("+Bonus: " + formatNumber(bonus)));
                lore.add(bullet("Total: " + formatNumber(value)));
            }
        }

        gui.setItem(slot, createItem(material, name, lore));
    }

    private static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createHeader(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(ChatColor.DARK_GRAY + "────────────"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String formatNumber(double value) {
        return String.format("%.2f", value);
    }

    private static String formatPercent(double value) {
        return String.format("%.2f%%", value * 100.0);
    }

    private static String bullet(String text) {
        return ChatColor.GRAY + "• " + ChatColor.WHITE + text;
    }

    private static double getAbyssalStat(Player player, String statType) {
        try {
            org.bukkit.plugin.Plugin plug = Bukkit.getPluginManager().getPlugin("ScientistPlugin");
            if (plug == null) return 0.0;

            double total = 0.0;

            // Check all equipment slots
            org.bukkit.inventory.ItemStack[] items = new org.bukkit.inventory.ItemStack[]{
                    player.getInventory().getItemInMainHand(),
                    player.getInventory().getHelmet(),
                    player.getInventory().getChestplate(),
                    player.getInventory().getLeggings(),
                    player.getInventory().getBoots()
            };

            for (org.bukkit.inventory.ItemStack item : items) {
                total += getAbyssalStatFromItem(item, statType);
            }

            // Check trinkets
            PlayerData data = TrinketsPlugin.getInstance().getDatabaseManager().getPlayerData(player.getUniqueId());
            if (data != null) {
                // Try to get trinket slots via reflection
                try {
                    java.lang.reflect.Field slotsField = data.getClass().getDeclaredField("slots");
                    slotsField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, org.bukkit.inventory.ItemStack> slots =
                        (java.util.Map<String, org.bukkit.inventory.ItemStack>) slotsField.get(data);

                    if (slots != null) {
                        for (org.bukkit.inventory.ItemStack trinket : slots.values()) {
                            total += getAbyssalStatFromItem(trinket, statType);
                        }
                    }
                } catch (Exception ignored) {}
            }

            return total;
        } catch (Throwable ignored) {
            return 0.0;
        }
    }

    private static double getAbyssalStatFromItem(org.bukkit.inventory.ItemStack item, String statType) {
        if (item == null || !item.hasItemMeta()) return 0.0;
        try {
            org.bukkit.persistence.PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey("scientistplugin", "abyssal");
            if (!pdc.has(key, org.bukkit.persistence.PersistentDataType.STRING)) return 0.0;

            String json = pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING);
            if (json == null || json.isEmpty()) return 0.0;

            com.google.gson.JsonObject obj = new com.google.gson.JsonParser().parse(json).getAsJsonObject();

            // Check if this mod matches the stat type we're looking for
            String type = obj.has("type") ? obj.get("type").getAsString() : null;
            if (type == null || !type.equals(statType)) return 0.0;

            double value = obj.has("value") ? obj.get("value").getAsDouble() : 0.0;
            return value;
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private static void fillEmpty(Inventory gui) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            filler.setItemMeta(meta);
        }
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
    }
}
