package com.maks.trinketsplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class JewelEvents implements Listener {

    private static final int debuggingFlag = 0;
    private final TrinketsPlugin plugin;
    private final JewelManager jewelManager;
    private final Random random = new Random();

    // Flag to control visual effects
    private static final boolean SHOW_VISUAL_EFFECTS = false; // Set to true if you want particles

    // Keywords for items affected by the Ingredient Jewel
    private static final Set<String> INGREDIENT_KEYWORDS = new HashSet<>(Arrays.asList(
            "Broken Armor Piece",
            "Tousled Priest Robe",
            "Black Fur",
            "Dragon Scale",
            "Chain Fragment",
            "Satyr`s Horn",
            "Gorgon`s Poison",
            "Dragon`s Gold",
            "Protector`s Heart",
            "Dead Bush",
            "Demon Blood",
            "Sticky Mucus",
            "Soul of an Acient Spartan",
            "Shadow Rose",
            "Throphy of the Long Forgotten Bone Dragon",
            "Monster Soul Fragment",
            "Monster Heart Fragment",
            "Grimmage Burned Cape",
            "Arachna Poisonous Skeleton",
            "Heredur's Glacial Armor",
            "Bearach Honey Hide",
            "Khalys Magic Robe",
            "Herald's Dragon Skin",
            "Sigrismarr's Eternal Ice",
            "Medusa Stone Scales",
            "Gorga's Broken Tooth",
            "Mortis Sacrificial Bones",
            "Ore",
            "Cursed Blood",
            "Shattered Bone",
            "Leaf",
            "Algal",
            "Shiny Pearl",
            "Heart of the Ocean",
            "Hematite",
            "Black Spinel",
            "Black Diamond",
            "Magnetite",
            "Silver",
            "Osmium",
            "Azurite",
            "Tanzanite",
            "Blue Sapphire",
            "Carnelian",
            "Red Spinel",
            "Pigeon Blood Ruby",
            "Pyrite",
            "Yellow Topaz",
            "Yellow Sapphire",
            "Malachite",
            "Peridot",
            "Tropiche Emerald",
            "Danburite",
            "Goshenite",
            "Cerussite"
    ));

    // Helper method to send action bar messages with cooldown
    private final Map<UUID, Long> lastActionBarTime = new HashMap<>();
    private final long ACTION_BAR_COOLDOWN = 2000; // 2 seconds between messages

    private void sendActionBar(Player player, String message) {
        UUID playerUUID = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastTime = lastActionBarTime.getOrDefault(playerUUID, 0L);

        // Only send message if enough time has passed
        if (now - lastTime >= ACTION_BAR_COOLDOWN) {
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
            lastActionBarTime.put(playerUUID, now);
        }
    }

    // Maps to track cooldowns and effects
    private final Map<UUID, Long> lastHealTime = new HashMap<>();
    private final Map<UUID, Double> healingAccumulated = new HashMap<>();

    // Map to track cooldowns
    private final Map<UUID, Map<JewelType, Long>> cooldowns = new HashMap<>();

    // Map to track active effects
    private final Map<UUID, Map<JewelType, Boolean>> activeEffects = new HashMap<>();

    public JewelEvents(TrinketsPlugin plugin, JewelManager jewelManager) {
        this.plugin = plugin;
        this.jewelManager = jewelManager;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (debuggingFlag == 1) {
            Bukkit.getLogger().info("[JewelEvents] EntityDamageByEntityEvent triggered");
        }

        // Handle jewels related to dealing damage
        if (event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            handleAttackerJewels(damager, event);
        }

        // Handle jewels related to taking damage
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            handleVictimJewels(victim, event);
        }
    }

    private void handleAttackerJewels(Player damager, EntityDamageByEntityEvent event) {
        UUID uuid = damager.getUniqueId();
        PlayerData data = plugin.getDatabaseManager().getPlayerData(uuid);

        if (data == null) {
            if (debuggingFlag == 1) {
                Bukkit.getLogger().warning("[JewelEvents] Player data is null for: " + damager.getName());
            }
            return;
        }

        long now = System.currentTimeMillis();

        // Handle AMPLIFIED_HEALING effect
        ItemStack ampHealingJewel = data.getJewel(JewelType.AMPLIFIED_HEALING);
        if (ampHealingJewel != null) {
            boolean active = getActiveEffect(uuid, JewelType.AMPLIFIED_HEALING);
            if (active) {
                int tier = jewelManager.getJewelTier(ampHealingJewel);
                double damageBonus = tier == 1 ? 0.25 : tier == 2 ? 0.5 : 1.0;

                // Increase damage
                event.setDamage(event.getDamage() * (1 + damageBonus));

                if (debuggingFlag == 1) {
                    Bukkit.getLogger().info("[JewelEvents] Applied Amplified Healing damage bonus: " + 
                            damageBonus + " for player: " + damager.getName());
                }
            }
        }

        // Handle JEWEL_OF_RAGE jewel
        ItemStack rageJewel = data.getJewel(JewelType.JEWEL_OF_RAGE);
        if (rageJewel != null) {
            int tier = jewelManager.getJewelTier(rageJewel);
            int procChance = tier == 1 ? 10 : tier == 2 ? 15 : 20;

            // Check if Fury effect is already active
            boolean active = getActiveEffect(uuid, JewelType.JEWEL_OF_RAGE);

            if (!active && random.nextInt(100) < procChance) {
                // Check cooldown
                long cooldownEnd = getCooldownEnd(uuid, JewelType.JEWEL_OF_RAGE);

                if (now >= cooldownEnd) {
                    // Activate Fury effect
                    setActiveEffect(uuid, JewelType.JEWEL_OF_RAGE, true);

                    // Set cooldown (60s)
                    setCooldown(uuid, JewelType.JEWEL_OF_RAGE, 60 * 1000);

                    // Schedule deactivation after 5s (unless player kills an enemy)
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            boolean stillActive = getActiveEffect(uuid, JewelType.JEWEL_OF_RAGE);
                            if (stillActive) {
                                setActiveEffect(uuid, JewelType.JEWEL_OF_RAGE, false);
                                sendActionBar(damager, ChatColor.RED + "Your Fury has ended!");
                            }
                        }
                    }.runTaskLater(plugin, 5 * 20); // 5s

                    sendActionBar(damager, ChatColor.RED + "Your Fury has been activated!");

                    if (debuggingFlag == 1) {
                        Bukkit.getLogger().info("[JewelEvents] Rage effect activated for player: " + 
                                damager.getName());
                    }
                }
            }

            // If Fury is active, add critical hit chance
            if (active) {
                int critChance = tier == 1 ? 15 : tier == 2 ? 20 : 30;

                if (random.nextInt(100) < critChance) {
                    // Critical hit - 200% damage
                    event.setDamage(event.getDamage() * 2);
                    // No action bar message for critical hits

                    if (debuggingFlag == 1) {
                        Bukkit.getLogger().info("[JewelEvents] Critical hit for player: " + 
                                damager.getName() + ", damage: " + event.getDamage());
                    }
                }
            }
        }

        // Handle COLLECTOR jewel
        ItemStack collectorJewel = data.getJewel(JewelType.COLLECTOR);
        if (collectorJewel != null && event.getEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) event.getEntity();
            int tier = jewelManager.getJewelTier(collectorJewel);
            double executeThreshold = tier == 1 ? 0.02 : tier == 2 ? 0.03 : 0.05; // 2/3/5%

            // Check if target health AFTER this hit would be below threshold but not zero
            double maxHealth = target.getMaxHealth();
            double currentHealth = target.getHealth();
            double damageAmount = event.getFinalDamage();
            double healthAfterDamage = Math.max(0, currentHealth - damageAmount);
            double healthPercentAfterDamage = healthAfterDamage / maxHealth;

            if (debuggingFlag == 1) {
                Bukkit.getLogger().info("[JewelEvents] Collector jewel check - Target: " + target.getType() + 
                        " - Current health: " + currentHealth + 
                        " - Damage: " + damageAmount +
                        " - Health after damage: " + healthAfterDamage +
                        " - Max health: " + maxHealth +
                        " - Health % after damage: " + (healthPercentAfterDamage * 100) + "%" +
                        " - Threshold: " + (executeThreshold * 100) + "%");
            }

            if (healthPercentAfterDamage <= executeThreshold && healthPercentAfterDamage > 0) {
                // Only execute if they wouldn't die from the hit anyway
                if (debuggingFlag == 1) {
                    Bukkit.getLogger().info("[JewelEvents] Collector jewel execution triggered for player: " + 
                            damager.getName());
                }

                // Instant kill - use a large damage value instead of setHealth(0)
                // This ensures proper death processing and drops
                event.setDamage(target.getHealth() * 2); // Ensure it kills the entity
                // No action bar message for Collector Jewel execution
            }
        }
    }

    private void handleVictimJewels(Player victim, EntityDamageByEntityEvent event) {
        UUID uuid = victim.getUniqueId();
        PlayerData data = plugin.getDatabaseManager().getPlayerData(uuid);

        if (data == null) {
            if (debuggingFlag == 1) {
                Bukkit.getLogger().warning("[JewelEvents] Player data is null for: " + victim.getName());
            }
            return;
        }

        long now = System.currentTimeMillis();

        // Handle PHOENIX jewel
        ItemStack phoenixJewel = data.getJewel(JewelType.PHOENIX);
        if (phoenixJewel != null) {
            double damage = event.getDamage();
            double currentHealth = victim.getHealth();

            // Check if player would die from this damage
            if (damage >= currentHealth) {
                int tier = jewelManager.getJewelTier(phoenixJewel);
                long cooldownTime = tier == 1 ? 60 : tier == 2 ? 45 : 30; // Minutes

                // Check cooldown
                long cooldownEnd = getCooldownEnd(uuid, JewelType.PHOENIX);

                if (now >= cooldownEnd) {
                    // Activate Phoenix effect
                    event.setCancelled(true);
                    victim.setHealth(victim.getMaxHealth()); // Full health

                    // Add immunity for 3s
                    victim.addPotionEffect(new PotionEffect(
                            PotionEffectType.DAMAGE_RESISTANCE, 60, 4, false, false));

                    // Set cooldown
                    setCooldown(uuid, JewelType.PHOENIX, cooldownTime * 60 * 1000); // Minutes -> milliseconds

                    // Visual effects
                    victim.getWorld().strikeLightningEffect(victim.getLocation());
                    victim.playSound(victim.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 1.0f);

                    // No action bar message for Phoenix Jewel

                    if (debuggingFlag == 1) {
                        Bukkit.getLogger().info("[JewelEvents] Phoenix jewel activated for player: " + 
                                victim.getName());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Monster)) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        UUID uuid = killer.getUniqueId();
        PlayerData data = plugin.getDatabaseManager().getPlayerData(uuid);

        if (data == null) return;

        long now = System.currentTimeMillis();

        // Handle LASTING_HEALING jewel
        ItemStack lastingHealingJewel = data.getJewel(JewelType.LASTING_HEALING);
        if (lastingHealingJewel != null) {
            // Check cooldown
            long cooldownEnd = getCooldownEnd(uuid, JewelType.LASTING_HEALING);

            if (now >= cooldownEnd) {
                int tier = jewelManager.getJewelTier(lastingHealingJewel);
                int healAmount = tier == 1 ? 5 : tier == 2 ? 10 : 15;

                // Set cooldown (30s)
                setCooldown(uuid, JewelType.LASTING_HEALING, 30 * 1000);

                // Activate effect
                setActiveEffect(uuid, JewelType.LASTING_HEALING, true);

                // Start task to heal player every 1s for 5s
                new BukkitRunnable() {
                    int counter = 0;

                    @Override
                    public void run() {
                        if (counter >= 5 || !killer.isOnline()) {
                            setActiveEffect(uuid, JewelType.LASTING_HEALING, false);
                            this.cancel();
                            return;
                        }

                        // Heal player
                        double maxHealth = killer.getMaxHealth();
                        double currentHealth = killer.getHealth();

                        // Don't exceed max health
                        if (currentHealth < maxHealth) {
                            double newHealth = Math.min(currentHealth + healAmount, maxHealth);
                            killer.setHealth(newHealth);

                            // Update healing counter (for AMPLIFIED_HEALING)
                            updateHealingCounter(killer, newHealth - currentHealth);

                            if (counter == 0) {
                                sendActionBar(killer, ChatColor.GREEN + "Your Lasting Healing has been activated!");
                            }
                        }

                        counter++;
                    }
                }.runTaskTimer(plugin, 20, 20); // Every 1s, for 5s

                if (debuggingFlag == 1) {
                    Bukkit.getLogger().info("[JewelEvents] Lasting Healing activated for player: " + 
                            killer.getName());
                }
            }
        }

        // Handle JEWEL_OF_RAGE jewel - extend Fury duration
        boolean rageActive = getActiveEffect(uuid, JewelType.JEWEL_OF_RAGE);
        if (rageActive) {
            // Extend Fury effect - reset timer
            new BukkitRunnable() {
                @Override
                public void run() {
                    boolean stillActive = getActiveEffect(uuid, JewelType.JEWEL_OF_RAGE);
                    if (stillActive) {
                        setActiveEffect(uuid, JewelType.JEWEL_OF_RAGE, false);
                        sendActionBar(killer, ChatColor.RED + "Your Fury has ended!");
                    }
                }
            }.runTaskLater(plugin, 5 * 20); // 5s

            // No action bar message for Fury extension

            if (debuggingFlag == 1) {
                Bukkit.getLogger().info("[JewelEvents] Rage effect extended for player: " + 
                        killer.getName());
            }
        }
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getDatabaseManager().getPlayerData(uuid);

        if (data == null) return;

        ItemStack item = event.getItem().getItemStack();
        String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() 
                ? ChatColor.stripColor(item.getItemMeta().getDisplayName()) 
                : null;

        // Handle ANDERMANT jewel
        ItemStack andermantJewel = data.getJewel(JewelType.ANDERMANT);
        if (andermantJewel != null && itemName != null && itemName.contains("Andermant")) {
            int tier = jewelManager.getJewelTier(andermantJewel);
            int dupeChance = tier == 1 ? 10 : tier == 2 ? 20 : 30;

            if (random.nextInt(100) < dupeChance) {
                // Duplicate Andermant
                ItemStack duplicate = item.clone();
                player.getInventory().addItem(duplicate);
                // No action bar message for Andermant Jewel duplication

                if (debuggingFlag == 1) {
                    Bukkit.getLogger().info("[JewelEvents] Andermant duplicated for player: " + 
                            player.getName());
                }
            }
        }

        // Handle CLOVER jewel (Sunspire Amber)
        ItemStack cloverJewel = data.getJewel(JewelType.CLOVER);
        if (cloverJewel != null && itemName != null && itemName.contains("Glided Sunflower")) {
            int tier = jewelManager.getJewelTier(cloverJewel);
            int extraAmount = tier == 1 ? 1 : tier == 2 ? 2 : 3;

            // Add extra Glided Sunflowers
            ItemStack extraItem = item.clone();
            extraItem.setAmount(extraAmount);
            player.getInventory().addItem(extraItem);
            // No action bar message for Sunspire Amber

            if (debuggingFlag == 1) {
                Bukkit.getLogger().info("[JewelEvents] Extra Glided Sunflowers given to player: " + 
                        player.getName() + ", amount: " + extraAmount);
            }
        }

        // Handle DRAKENMELON jewel
        ItemStack drakenmelonJewel = data.getJewel(JewelType.DRAKENMELON);
        if (drakenmelonJewel != null && itemName != null && itemName.contains("DrakenMelon")) {
            int tier = jewelManager.getJewelTier(drakenmelonJewel);
            int extraAmount = tier == 1 ? 1 : tier == 2 ? 2 : 3;

            // Add extra DrakenMelons
            ItemStack extraItem = item.clone();
            extraItem.setAmount(extraAmount);
            player.getInventory().addItem(extraItem);
            // No action bar message for DrakenMelon Jewel

            if (debuggingFlag == 1) {
                Bukkit.getLogger().info("[JewelEvents] Extra drakenmelons given to player: " + 
                        player.getName() + ", amount: " + extraAmount);
            }
        }

        // Handle LOCKPICK jewel
        ItemStack lockpickJewel = data.getJewel(JewelType.LOCKPICK);
        if (lockpickJewel != null && itemName != null && itemName.contains("Lockpick")) {
            int tier = jewelManager.getJewelTier(lockpickJewel);
            int extraAmount = tier == 1 ? 3 : tier == 2 ? 4 : 5;

            // Add extra Lockpicks
            ItemStack extraItem = item.clone();
            extraItem.setAmount(extraAmount);
            player.getInventory().addItem(extraItem);
            // No action bar message for Lockpick Jewel

            if (debuggingFlag == 1) {
                Bukkit.getLogger().info("[JewelEvents] Extra lockpicks given to player: " +
                        player.getName() + ", amount: " + extraAmount);
            }
        }

        // Handle INGREDIENT jewel
        ItemStack ingredientJewel = data.getJewel(JewelType.INGREDIENT);
        if (ingredientJewel != null && itemName != null &&
                INGREDIENT_KEYWORDS.stream().anyMatch(itemName::contains)) {
            int tier = jewelManager.getJewelTier(ingredientJewel);
            int extraAmount = tier == 1 ? 1 : tier == 2 ? 2 : 3;

            ItemStack extraItem = item.clone();
            extraItem.setAmount(extraAmount);
            player.getInventory().addItem(extraItem);

            if (debuggingFlag == 1) {
                Bukkit.getLogger().info("[JewelEvents] Extra ingredients given to player: " +
                        player.getName() + ", amount: " + extraAmount);
            }
        }
    }

    // Method to track healing for AMPLIFIED_HEALING jewel
    private void updateHealingCounter(Player player, double healAmount) {
        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getDatabaseManager().getPlayerData(uuid);

        if (data == null) return;

        ItemStack ampHealingJewel = data.getJewel(JewelType.AMPLIFIED_HEALING);
        if (ampHealingJewel == null) return;

        long now = System.currentTimeMillis();
        long lastHeal = lastHealTime.getOrDefault(uuid, 0L);
        double accumulated = healingAccumulated.getOrDefault(uuid, 0.0);

        // If more than 2.5s since last heal, reset counter
        if (now - lastHeal > 2500) {
            accumulated = 0;
        }

        // Add current healing to counter
        accumulated += healAmount;

        // Save time and amount of healing
        lastHealTime.put(uuid, now);
        healingAccumulated.put(uuid, accumulated);

        // Check if threshold of 20 HP is exceeded
        if (accumulated >= 20) {
            // Check cooldown
            long cooldownEnd = getCooldownEnd(uuid, JewelType.AMPLIFIED_HEALING);

            if (now >= cooldownEnd) {
                int tier = jewelManager.getJewelTier(ampHealingJewel);
                int damageAmpPercent = tier == 1 ? 25 : tier == 2 ? 50 : 100;

                // Activate effect
                setActiveEffect(uuid, JewelType.AMPLIFIED_HEALING, true);

                // Set cooldown (60s)
                setCooldown(uuid, JewelType.AMPLIFIED_HEALING, 60 * 1000);

                // Set timer to deactivate effect after 5s
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        setActiveEffect(uuid, JewelType.AMPLIFIED_HEALING, false);
                        sendActionBar(player, ChatColor.GREEN + "Your damage amplification has worn off!");
                    }
                }.runTaskLater(plugin, 5 * 20); // 5s

                sendActionBar(player, ChatColor.GREEN + "Your healing has amplified your damage by " + 
                        damageAmpPercent + "%!");

                if (debuggingFlag == 1) {
                    Bukkit.getLogger().info("[JewelEvents] Amplified Healing activated for player: " + 
                            player.getName() + ", damage bonus: " + damageAmpPercent + "%");
                }
            }

            // Reset counter
            healingAccumulated.put(uuid, 0.0);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (debuggingFlag == 1) {
            // Log baseline values before applying
            double currentHealth = player.getHealth();
            double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            double moveSpeed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue();

            Bukkit.getLogger().info("[JewelEvents] Player join (before jewels) - " + player.getName() + 
                    " - Health: " + currentHealth + "/" + maxHealth +
                    " - Speed: " + moveSpeed);
        }

        // Safety check - remove any leftover attributes first
        jewelManager.removeAllJewelAttributes(player);

        // Load player data and apply jewel attributes
        plugin.getDatabaseManager().loadPlayerData(uuid, data -> {
            if (data != null) {
                jewelManager.applyJewelAttributes(player, data);

                if (debuggingFlag == 1) {
                    // Log values after applying
                    double newMaxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                    double newMoveSpeed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue();

                    Bukkit.getLogger().info("[JewelEvents] Applied jewel attributes on join for player: " + 
                            player.getName() + " - New max health: " + newMaxHealth + 
                            " - New move speed: " + newMoveSpeed);
                }
            } else {
                if (debuggingFlag == 1) {
                    Bukkit.getLogger().warning("[JewelEvents] Failed to load player data for: " + 
                            player.getName());
                }
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Explicitly remove jewel attributes
        jewelManager.removeAllJewelAttributes(player);

        // Clear temporary data
        lastHealTime.remove(uuid);
        healingAccumulated.remove(uuid);
        cooldowns.remove(uuid);
        activeEffects.remove(uuid);
        lastActionBarTime.remove(uuid); // Clear the action bar cooldown map too

        if (debuggingFlag == 1) {
            // Check current health and speed after removal
            double currentHealth = player.getHealth();
            double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            double moveSpeed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue();

            Bukkit.getLogger().info("[JewelEvents] Player quit - " + player.getName() + 
                    " - Health: " + currentHealth + "/" + maxHealth +
                    " - Speed: " + moveSpeed);
            Bukkit.getLogger().info("[JewelEvents] Cleared temporary data on quit for player: " + 
                    player.getName());
        }
    }

    // Helper methods for managing cooldowns
    private void setCooldown(UUID uuid, JewelType type, long duration) {
        Map<JewelType, Long> playerCooldowns = cooldowns.computeIfAbsent(uuid, k -> new HashMap<>());
        long now = System.currentTimeMillis();

        // Apply cooldown reduction from COOLDOWN jewels
        PlayerData data = plugin.getDatabaseManager().getPlayerData(uuid);
        if (data != null) {
            double reduction = 0.0;

            // Check all 3 slots for JEWEL_OF_FOCUS
            ItemStack cdJewel1 = data.getJewel(JewelType.JEWEL_OF_FOCUS);
            ItemStack cdJewel2 = data.getJewel(JewelType.JEWEL_OF_FOCUS_2);
            ItemStack cdJewel3 = data.getJewel(JewelType.JEWEL_OF_FOCUS_3);

            if (cdJewel1 != null) {
                int tier = jewelManager.getJewelTier(cdJewel1);
                reduction += (tier == 1 ? 0.02 : tier == 2 ? 0.03 : 0.05);
            }

            if (cdJewel2 != null) {
                int tier = jewelManager.getJewelTier(cdJewel2);
                reduction += (tier == 1 ? 0.02 : tier == 2 ? 0.03 : 0.05);
            }

            if (cdJewel3 != null) {
                int tier = jewelManager.getJewelTier(cdJewel3);
                reduction += (tier == 1 ? 0.02 : tier == 2 ? 0.03 : 0.05);
            }

            // Limit reduction to 15%
            reduction = Math.min(reduction, 0.15);

            // Apply reduction
            duration = (long) (duration * (1.0 - reduction));

            // Debugging information
            if (debuggingFlag == 1 && reduction > 0) {
                Bukkit.getLogger().info("[JewelEvents] Applied cooldown reduction: " + 
                        (reduction * 100) + "% for jewel: " + type.name() + 
                        " - Active CD jewels: " + 
                        (cdJewel1 != null ? "Slot1 (Tier " + jewelManager.getJewelTier(cdJewel1) + ") " : "") +
                        (cdJewel2 != null ? "Slot2 (Tier " + jewelManager.getJewelTier(cdJewel2) + ") " : "") +
                        (cdJewel3 != null ? "Slot3 (Tier " + jewelManager.getJewelTier(cdJewel3) + ") " : ""));
            }
        }

        playerCooldowns.put(type, now + duration);
    }

    private long getCooldownEnd(UUID uuid, JewelType type) {
        Map<JewelType, Long> playerCooldowns = cooldowns.getOrDefault(uuid, new HashMap<>());
        return playerCooldowns.getOrDefault(type, 0L);
    }

    private boolean hasCooldown(UUID uuid, JewelType type) {
        long cooldownEnd = getCooldownEnd(uuid, type);
        return System.currentTimeMillis() < cooldownEnd;
    }

    private String formatRemainingCooldown(UUID uuid, JewelType type) {
        long cooldownEnd = getCooldownEnd(uuid, type);
        long remaining = cooldownEnd - System.currentTimeMillis();

        if (remaining <= 0) {
            return "ready";
        }

        // Convert milliseconds to more readable format
        long seconds = (remaining / 1000) % 60;
        long minutes = (remaining / (1000 * 60)) % 60;
        long hours = (remaining / (1000 * 60 * 60));

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    // Helper methods for managing active effects
    private void setActiveEffect(UUID uuid, JewelType type, boolean active) {
        Map<JewelType, Boolean> playerEffects = activeEffects.computeIfAbsent(uuid, k -> new HashMap<>());
        playerEffects.put(type, active);
    }

    private boolean getActiveEffect(UUID uuid, JewelType type) {
        Map<JewelType, Boolean> playerEffects = activeEffects.getOrDefault(uuid, new HashMap<>());
        return playerEffects.getOrDefault(type, false);
    }

    // Command to check jewel status (optional)
    public boolean onJewelCommand(Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getDatabaseManager().getPlayerData(uuid);

        if (data == null) {
            player.sendMessage(ChatColor.RED + "Your data could not be loaded.");
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "=== Jewel Status ===");

        boolean hasJewels = false;

        for (JewelType type : JewelType.values()) {
            ItemStack jewel = data.getJewel(type);
            if (jewel != null) {
                hasJewels = true;
                int tier = jewelManager.getJewelTier(jewel);
                boolean active = getActiveEffect(uuid, type);
                String status = active ? ChatColor.GREEN + "ACTIVE" : "";

                // Show cooldown if exists
                if (hasCooldown(uuid, type)) {
                    status = ChatColor.RED + "CD: " + formatRemainingCooldown(uuid, type);
                }

                player.sendMessage(ChatColor.YELLOW + type.getDisplayName() + 
                        ChatColor.GRAY + " (Tier " + tier + ") " + status);
            }
        }

        if (!hasJewels) {
            player.sendMessage(ChatColor.GRAY + "You don't have any jewels equipped.");
        }

        return true;
    }
}
