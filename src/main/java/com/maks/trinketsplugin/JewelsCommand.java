package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class JewelsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        sender.sendMessage(ChatColor.GREEN + "Check out all jewels at: " + ChatColor.YELLOW + "https://dsocraft.pl/jewels.php");
        return true;
    }
}