package com.maks.trinketsplugin;

import com.maks.trinketsplugin.TrinketsPlugin;
import com.maks.trinketsplugin.AccessoryType;
import com.maks.trinketsplugin.PlayerData;

import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class Q2SoulEffect implements Listener {

    private final TrinketsPlugin plugin;

    public Q2SoulEffect(TrinketsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean hasQ2SoulEquipped(Player player) {
        PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        if (data == null) return false;

        ItemStack soul = data.getAccessory(AccessoryType.BOSS_SOUL);
        if (soul == null || !soul.hasItemMeta() || soul.getItemMeta().getDisplayName() == null) {
            return false;
        }
        String name = ChatColor.stripColor(soul.getItemMeta().getDisplayName());
        return name.contains("Arachna’s Venomous Soul");
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!hasQ2SoulEquipped(player)) return;

        ItemStack stack = event.getItem().getItemStack();
        // Sprawdzamy lore (o ile istnieje)
        if (!stack.hasItemMeta() || !stack.getItemMeta().hasLore()) {
            return;  // brak lore => nic nie robimy
        }

        boolean foundRare = false;
        // Pobierz lore i przeleć każdą linię
        for (String line : stack.getItemMeta().getLore()) {
            // Usuwamy kody kolorów i zmieniamy na małe litery
            String cleaned = ChatColor.stripColor(line).toLowerCase();
            if (cleaned.contains("unique") || cleaned.contains("mythic")) {
                foundRare = true;
                break;
            }
        }

        // Jeśli w lore znaleźliśmy "unique" lub "mythic" => 10% szansy na duplikację
        if (foundRare) {
            Random r = new Random();
            if (r.nextInt(100) < 10) { // 10% chance
                player.sendMessage(ChatColor.DARK_GREEN + "[Q2] You duplicated a unique/mythic item!");
                player.getInventory().addItem(stack.clone());
            }
        }
    }


    /**
     * For each mob kill -> +10k $
     */
    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        // event.getEntity() is a LivingEntity
        LivingEntity e = event.getEntity();
        // Check if it's a Monster
        if (!(e instanceof Monster)) return;

        Player killer = e.getKiller();  // <-- poprawne wywołanie (LivingEntity ma getKiller())
        if (killer == null) return;
        if (!hasQ2SoulEquipped(killer)) return;

        // +10k$
        TrinketsPlugin.getEconomy().depositPlayer(killer, 10000);
        killer.sendMessage(ChatColor.GREEN + "[Q2] You received $10,000 for killing a mob!");
    }
}
