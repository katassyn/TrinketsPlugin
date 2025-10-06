package com.maks.trinketsplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class TrinketsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args.length > 0) {
                String subCommand = args[0].toLowerCase();

                switch (subCommand) {
                    case "accessories":
                        TrinketsGUI.openAccessoriesMenu(player);
                        break;
                    case "jewels":
                        JewelsGUI.openJewelsMenu(player);
                        break;
                    case "unique":
                        UniqueTrinketsGUI.openUniqueTrinketsMenu(player);
                        break;
                    default:
                        TrinketsGUI.openMainMenu(player);
                        break;
                }
            } else {
                TrinketsGUI.openMainMenu(player);
            }
        } else {
            sender.sendMessage("This command can only be used by players.");
        }

        return true;
    }
}
