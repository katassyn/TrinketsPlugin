package com.maks.trinketsplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.attribute.Attribute;

import java.util.*;

public class UniqueTrinketEffectsListener implements Listener {
    private final TrinketsPlugin plugin;
    private final Map<UUID, Long> divineShieldCooldowns = new HashMap<>();
    private final Map<UUID, Long> absorptionCooldowns = new HashMap<>();
    private final Set<UUID> playersWithDivineShield = new HashSet<>();
    private final Random random = new Random();

    public UniqueTrinketEffectsListener(TrinketsPlugin plugin) {
        this.plugin = plugin;
        startEffectsTask();
    }

    private void startEffectsTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    processPlayerEffects(player);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Run every second
    }

    private void processPlayerEffects(Player player) {
        PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (data == null) return;
        
        // Process Team Relic effects (only if player is party leader)
        ItemStack teamRelic = data.getUniqueTrinket(UniqueTrinketType.TEAM_RELIC);
        if (teamRelic != null && PartyAPIIntegration.isPartyLeader(player)) {
            processTeamRelicEffects(player, teamRelic);
        }
        
        // Process Boss Heart effects
        ItemStack bossHeart = data.getUniqueTrinket(UniqueTrinketType.BOSS_HEART);
        if (bossHeart != null) {
            processBossHeartEffects(player, bossHeart);
        }
    }

    private void processTeamRelicEffects(Player leader, ItemStack teamRelic) {
        String itemName = getItemName(teamRelic);
        if (itemName == null) return;
        
        switch (itemName.toLowerCase()) {
            case "team relic of hermes":
                processDivineShield(leader, 15.0);
                break;
            case "team relic of apollo":
                processHPRegen(leader, 10.0, 100);
                break;
            case "team relic of athena":
                processDamageBoost(leader, 15.0, 5);
                break;
            case "team relic of ares":
                processSpeedBoost(leader, 20.0, 3);
                break;
            case "team relic of zeus":
                processLuckBoost(leader, 20.0, 20);
                break;
        }
    }

    private void processBossHeartEffects(Player player, ItemStack bossHeart) {
        String itemName = getItemName(bossHeart);
        if (itemName == null) return;
        
        switch (itemName.toLowerCase()) {
            case "heart of olympus":
                // Deflect handled in damage event
                break;
            case "heart of kronos":
                processMovementSpeedDamage(player);
                break;
            case "heart of hades":
                processArmorAndAbsorption(player);
                break;
            case "heart of poseidon":
                processLuckEffects(player);
                break;
            case "heart of zeus":
                processHealthBoost(player);
                break;
        }
    }

    private void processDivineShield(Player leader, double range) {
        long currentTime = System.currentTimeMillis();
        UUID leaderId = leader.getUniqueId();
        
        // Check cooldown (60 seconds)
        if (divineShieldCooldowns.containsKey(leaderId)) {
            long lastUse = divineShieldCooldowns.get(leaderId);
            if (currentTime - lastUse < 60000) {
                return;
            }
        }
        
        // Apply divine shield to party members in range
        List<Player> members = PartyAPIIntegration.getPartyMembersInRange(leader, range);
        for (Player member : members) {
            playersWithDivineShield.add(member.getUniqueId());
            member.sendMessage(ChatColor.GOLD + "Divine Shield activated! Next damage will be blocked.");
        }
        
        divineShieldCooldowns.put(leaderId, currentTime);
    }

    private void processHPRegen(Player leader, double range, int regenAmount) {
        List<Player> members = PartyAPIIntegration.getPartyMembersInRange(leader, range);
        for (Player member : members) {
            double currentHealth = member.getHealth();
            double maxHealth = member.getMaxHealth();
            double newHealth = Math.min(maxHealth, currentHealth + regenAmount);
            member.setHealth(newHealth);
        }
    }

    private void processDamageBoost(Player leader, double range, int damagePercent) {
        List<Player> members = PartyAPIIntegration.getPartyMembersInRange(leader, range);
        for (Player member : members) {
            // Apply damage boost potion effect
            member.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 40, 0, false, false));
        }
    }

    private void processSpeedBoost(Player leader, double range, int speedPercent) {
        List<Player> members = PartyAPIIntegration.getPartyMembersInRange(leader, range);
        for (Player member : members) {
            // Apply speed boost potion effect
            member.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, false, false));
        }
    }

    private void processLuckBoost(Player leader, double range, int luckAmount) {
        List<Player> members = PartyAPIIntegration.getPartyMembersInRange(leader, range);
        for (Player member : members) {
            // Apply luck boost potion effect
            member.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 40, luckAmount - 1, false, false));
        }
    }

    private void processMovementSpeedDamage(Player player) {
        // Calculate damage based on movement speed
        float walkSpeed = player.getWalkSpeed();
        double damage = walkSpeed * 100;
        // This would be applied during combat - need to track for damage events
    }

    private void processArmorAndAbsorption(Player player) {
        // Add armor points (handled through attributes)
        // Add absorption every 10 seconds
        long currentTime = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        
        if (!absorptionCooldowns.containsKey(playerId) || 
            currentTime - absorptionCooldowns.get(playerId) >= 10000) {
            
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 9, false, false));
            absorptionCooldowns.put(playerId, currentTime);
        }
    }

    private void processLuckEffects(Player player) {
        // Apply luck effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 40, 9, false, false));
    }

    private void processHealthBoost(Player player) {
        // Apply health boost
        player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 40, 4, false, false));
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        
        // Check for divine shield
        if (playersWithDivineShield.contains(player.getUniqueId())) {
            event.setCancelled(true);
            playersWithDivineShield.remove(player.getUniqueId());
            player.sendMessage(ChatColor.GOLD + "Divine Shield blocked the damage!");
            return;
        }
        
        // Check for Heart of Olympus deflect
        PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (data == null) return;
        ItemStack bossHeart = data.getUniqueTrinket(UniqueTrinketType.BOSS_HEART);
        
        if (bossHeart != null && getItemName(bossHeart) != null && 
            getItemName(bossHeart).toLowerCase().contains("heart of olympus")) {
            
            if (random.nextDouble() < 0.30) { // 30% chance
                event.setCancelled(true);
                player.sendMessage(ChatColor.GOLD + "Damage deflected!");
                return;
            }
        }
        
        // Check for Heart of Kronos movement speed damage
        if (bossHeart != null && getItemName(bossHeart) != null && 
            getItemName(bossHeart).toLowerCase().contains("heart of kronos")) {
            
            float walkSpeed = player.getWalkSpeed();
            double bonusDamage = walkSpeed * 100;
            // Apply bonus damage (this would need to be handled in combat system)
        }
    }

    private String getItemName(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return null;
        return ChatColor.stripColor(meta.getDisplayName()).toLowerCase();
    }
}