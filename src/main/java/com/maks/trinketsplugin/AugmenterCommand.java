package com.maks.trinketsplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AugmenterCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("mycraftingplugin.use")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        if (player.getLevel() < 75) {
            player.sendMessage(ChatColor.RED + "You must be at least level 75 to use the augmenter.");
            return true;
        }
        AugmenterGUI.openMainMenu(player);
        return true;
    }
}
