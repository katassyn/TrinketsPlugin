package com.maks.trinketsplugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.ArrayList;

public class PartyAPIIntegration {
    private static boolean partyAPIAvailable = false;
    private static Plugin myExperiencePlugin = null;
    private static Class<?> partyAPIClass = null;
    private static Method isInPartyMethod = null;
    private static Method isPartyLeaderMethod = null;
    private static Method getPartySizeMethod = null;
    private static Method getPartyMembersMethod = null;
    
    static {
        try {
            myExperiencePlugin = Bukkit.getPluginManager().getPlugin("MyExperiencePlugin");
            if (myExperiencePlugin != null && myExperiencePlugin.isEnabled()) {
                partyAPIClass = Class.forName("com.maks.myexperienceplugin.party.PartyAPI");
                isInPartyMethod = partyAPIClass.getMethod("isInParty", Player.class);
                isPartyLeaderMethod = partyAPIClass.getMethod("isPartyLeader", Player.class);
                getPartySizeMethod = partyAPIClass.getMethod("getPartySize", Player.class);
                getPartyMembersMethod = partyAPIClass.getMethod("getPartyMembers", Player.class);
                partyAPIAvailable = true;
                TrinketsPlugin.getInstance().getLogger().info("PartyAPI integration successful!");
            }
        } catch (Exception e) {
            TrinketsPlugin.getInstance().getLogger().warning("PartyAPI not available: " + e.getMessage());
            partyAPIAvailable = false;
        }
    }

    public static boolean isPartyAPIAvailable() {
        return partyAPIAvailable;
    }

    public static boolean isInParty(Player player) {
        if (!partyAPIAvailable) return false;
        try {
            return (Boolean) isInPartyMethod.invoke(null, player);
        } catch (Exception e) {
            TrinketsPlugin.getInstance().getLogger().warning("Error checking party status: " + e.getMessage());
            return false;
        }
    }

    public static boolean isPartyLeader(Player player) {
        if (!partyAPIAvailable) return false;
        try {
            return (Boolean) isPartyLeaderMethod.invoke(null, player);
        } catch (Exception e) {
            TrinketsPlugin.getInstance().getLogger().warning("Error checking party leader status: " + e.getMessage());
            return false;
        }
    }

    public static int getPartySize(Player player) {
        if (!partyAPIAvailable) return 1;
        try {
            return (Integer) getPartySizeMethod.invoke(null, player);
        } catch (Exception e) {
            TrinketsPlugin.getInstance().getLogger().warning("Error getting party size: " + e.getMessage());
            return 1;
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Player> getPartyMembers(Player player) {
        if (!partyAPIAvailable) {
            List<Player> members = new ArrayList<>();
            members.add(player);
            return members;
        }
        try {
            return (List<Player>) getPartyMembersMethod.invoke(null, player);
        } catch (Exception e) {
            TrinketsPlugin.getInstance().getLogger().warning("Error getting party members: " + e.getMessage());
            List<Player> members = new ArrayList<>();
            members.add(player);
            return members;
        }
    }

    public static List<Player> getPartyMembersInRange(Player player, double range) {
        List<Player> allMembers = getPartyMembers(player);
        List<Player> membersInRange = new ArrayList<>();
        
        for (Player member : allMembers) {
            if (member.getWorld().equals(player.getWorld()) && 
                member.getLocation().distance(player.getLocation()) <= range) {
                membersInRange.add(member);
            }
        }
        
        return membersInRange;
    }
}