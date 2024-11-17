package com.maks.trinketsplugin;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class ResetAttributesCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Sprawdź, czy nadawca ma uprawnienia
        if (!sender.hasPermission("trinkets.resetattributes")) {
            sender.sendMessage(ChatColor.RED + "Nie masz uprawnień do użycia tej komendy.");
            return true;
        }

        // Pobierz gracza docelowego
        Player targetPlayer;
        if (args.length > 0) {
            targetPlayer = Bukkit.getPlayer(args[0]);
            if (targetPlayer == null) {
                sender.sendMessage(ChatColor.RED + "Gracz " + args[0] + " nie jest online.");
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Musisz podać nazwę gracza, jeśli używasz tej komendy z konsoli.");
                return true;
            }
            targetPlayer = (Player) sender;
        }

        // Usuń wszystkie modyfikatory atrybutów
        for (Attribute attribute : Attribute.values()) {
            AttributeInstance attributeInstance = targetPlayer.getAttribute(attribute);
            if (attributeInstance != null) {
                for (AttributeModifier modifier : new ArrayList<>(attributeInstance.getModifiers())) {
                    attributeInstance.removeModifier(modifier);
                }
                // Opcjonalnie, ustaw bazową wartość atrybutu na domyślną
                switch (attribute) {
                    case GENERIC_MAX_HEALTH:
                        attributeInstance.setBaseValue(20.0);
                        break;
                    case GENERIC_ATTACK_DAMAGE:
                        attributeInstance.setBaseValue(1.0);
                        break;
                    // Dodaj inne atrybuty, jeśli chcesz zresetować bazowe wartości
                    default:
                        // Nie zmieniaj bazowej wartości
                        break;
                }
            }
        }

        // Ustaw aktualne zdrowie gracza na maksymalne
        targetPlayer.setHealth(targetPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());

        sender.sendMessage(ChatColor.GREEN + "Wszystkie atrybuty zostały usunięte z gracza " + targetPlayer.getName() + ".");

        return true;
    }
}
