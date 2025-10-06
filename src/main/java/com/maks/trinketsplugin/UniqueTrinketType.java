package com.maks.trinketsplugin;

public enum UniqueTrinketType {
    TEAM_RELIC(0, "Team Relic"),
    BOSS_HEART(1, "Boss Heart");

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