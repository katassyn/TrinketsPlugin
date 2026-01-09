package com.maks.trinketsplugin;

public enum UniqueTrinketType {
    TEAM_RELIC(0, "Team Relic"),
    BOSS_HEART(1, "Boss Heart"),
    FULL_MOON_SHARD(2, "Full Moon Trinket"),
    NEW_MOON_SHARD(3, "New Moon Trinket"),
    WINTER_TRINKET(4, "Winter Trinket Slot");

    private final int slot;
    private final String displayName;

    UniqueTrinketType(int slot, String displayName) {
        this.slot = slot;
        this.displayName = displayName;
    }

    public int getSlot() {
        return slot;
    }

    public String getDisplayName() {
        return displayName;
    }
}