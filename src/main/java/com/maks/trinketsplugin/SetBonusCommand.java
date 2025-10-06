package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetBonusCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        SetBonusManager setBonusManager = TrinketsPlugin.getInstance().getSetBonusManager();
        
        if (setBonusManager == null) {
            player.sendMessage(ChatColor.RED + "Set bonus system is not available!");
            return true;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== SET BONUS STATUS ===");
        
        // Check each set type
        for (SetType setType : SetType.values()) {
            int count = setBonusManager.getSetPieceCount(player, setType);
            player.sendMessage(ChatColor.YELLOW + setType.getDisplayName() + ": " + 
                             ChatColor.WHITE + count + " pieces equipped");
            
            // Show active bonuses
            switch (setType) {
                case HERMES_DIVINE_SPEED:
                    if (count >= 2) {
                        player.sendMessage(ChatColor.GREEN + "  ✓ +5% Movement Speed, +10 Health");
                    }
                    break;
                case OLYMPIAN_TRINITY:
                    if (count >= 3) {
                        player.sendMessage(ChatColor.GREEN + "  ✓ +50 Damage, +50 Health, +5 Luck");
                    }
                    break;
                case DIVINE_OLYMPUS:
                    if (count >= 2) {
                        player.sendMessage(ChatColor.GREEN + "  ✓ +3% Health");
                    }
                    if (count >= 4) {
                        player.sendMessage(ChatColor.GREEN + "  ✓ +100 Damage, +1% Damage");
                    }
                    break;
                case AEGIS_PROTECTION:
                    if (count >= 1) {
                        player.sendMessage(ChatColor.GREEN + "  ✓ +10% Block Chance, +50 Damage");
                    }
                    break;
                case TITAN_SUPREMACY:
                    if (count >= 2) {
                        player.sendMessage(ChatColor.GREEN + "  ✓ +3% Health, +3% Damage, +3% Luck (2 pieces)");
                    }
                    if (count >= 4) {
                        player.sendMessage(ChatColor.GREEN + "  ✓ +3% Health, +3% Damage, +3% Luck (4 pieces)");
                    }
                    if (count >= 6) {
                        player.sendMessage(ChatColor.GREEN + "  ✓ +3% Health, +3% Damage, +3% Luck (6 pieces)");
                    }
                    break;
            }
        }
        
        return true;
    }
}