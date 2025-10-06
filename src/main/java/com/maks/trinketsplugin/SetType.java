package com.maks.trinketsplugin;

public enum SetType {
    HERMES_DIVINE_SPEED("Hermes Divine Speed"),
    OLYMPIAN_TRINITY("Olympian Trinity"),
    DIVINE_OLYMPUS("Divine Olympus"),
    AEGIS_PROTECTION("Aegis Protection"),
    TITAN_SUPREMACY("Titan Supremacy");
    
    private final String displayName;
    
    SetType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static SetType fromDisplayName(String displayName) {
        for (SetType type : values()) {
            if (type.displayName.equals(displayName)) {
                return type;
            }
        }
        return null;
    }
}