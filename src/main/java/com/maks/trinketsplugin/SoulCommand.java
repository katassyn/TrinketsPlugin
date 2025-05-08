package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SoulCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "q1":
                sender.sendMessage(ChatColor.GOLD + "=== Q1: Grimmag’s Burning Soul ===");
                sender.sendMessage(ChatColor.YELLOW + " - On mob hit: Ignite for 5s, dealing 1000 dmg/s (cd 15s)");
                sender.sendMessage(ChatColor.YELLOW + " - On player hit: Ignite for 3s, dealing 50 dmg/s (cd 15s)");
                break;
            case "q2":
                sender.sendMessage(ChatColor.GOLD + "=== Q2: Arachna’s Venomous Soul ===");
                sender.sendMessage(ChatColor.YELLOW + " - 10% chance to duplicate unique/mythic item on pickup");
                sender.sendMessage(ChatColor.YELLOW + " - Gain $10,000 for each mob kill");
                break;
            case "q3":
                sender.sendMessage(ChatColor.GOLD + "=== Q3: King Heredur’s Frostbound Soul ===");
                sender.sendMessage(ChatColor.YELLOW + " - 10% chance to block 50% incoming dmg");
                sender.sendMessage(ChatColor.YELLOW + " - After block, all entities in 10 blocks get 75% slow");
                break;
            case "q4":
                sender.sendMessage(ChatColor.GOLD + "=== Q4: Bearach’s Wildheart Soul ===");
                sender.sendMessage(ChatColor.YELLOW + " - Once every 15s, your attack roots target for 3s");
                sender.sendMessage(ChatColor.YELLOW + " - Every attack on a player has 15% chance to rotate him 180 degree`s");
                break;
            case "q5":
                sender.sendMessage(ChatColor.GOLD + "=== Q5: Khalys’s Shadowbound Soul ===");
                sender.sendMessage(ChatColor.YELLOW + " - Every 30s, you fully evade next damage (dmg=0)");
                sender.sendMessage(ChatColor.YELLOW + " - After evade, your next attack deals +300% dmg");
                break;
            case "q6":
                sender.sendMessage(ChatColor.GOLD + "=== Q6: Mortis’s Unchained Soul ===");
                sender.sendMessage(ChatColor.YELLOW + " - On hit: apply 50% weakness for 5s (cd 15s)");
                sender.sendMessage(ChatColor.YELLOW + " - Kill a mob = +2% dmg stack (max 20). Taking dmg = lose 5 stacks");
                break;
            case "q7":
                sender.sendMessage(ChatColor.GOLD + "=== Q7: Herald’s Molten Soul ===");
                sender.sendMessage(ChatColor.YELLOW + " - You receive 20% less dmg");
                sender.sendMessage(ChatColor.YELLOW + " - When taking dmg, reflect 20% back to attacker");
                break;
            case "q8":
                sender.sendMessage(ChatColor.GOLD + "=== Q8: Sigrismar’s Blizzard Soul ===");
                sender.sendMessage(ChatColor.YELLOW + " - Every attack slows target by 30%");
                sender.sendMessage(ChatColor.YELLOW + " - After 5 hits, freeze target (1s) and deal +300 dmg (cd 15s)");
                break;
            case "q9":
                sender.sendMessage(ChatColor.GOLD + "=== Q9: Medusa’s Petrifying Soul ===");
                sender.sendMessage(ChatColor.YELLOW + " - On player hit: Levitation for 2s (cd 20s)");
                sender.sendMessage(ChatColor.YELLOW + " - After 3 hits on a mob: turn it to stone (1s), cd 10s");
                break;
            case "q10":
                sender.sendMessage(ChatColor.GOLD + "=== Q10: Gorga’s Abyssal Soul ===");
                sender.sendMessage(ChatColor.YELLOW + " - On player hit: Blindness for 3s (cd 5s)");
                sender.sendMessage(ChatColor.YELLOW + " - When HP < 30%, heal to full +30% dmg for 5s (cd 60s)");
                break;
            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "Usage: /soul <q1|q2|...|q10>");
        sender.sendMessage(ChatColor.GRAY + "Example: /soul q3");
        sender.sendMessage(ChatColor.GREEN + "Check out all boss souls at: " + ChatColor.YELLOW + "https://dsocraft.pl/bossouls.php");
    }
}
