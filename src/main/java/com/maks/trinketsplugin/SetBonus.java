package com.maks.trinketsplugin;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SetBonus {
    private final SetType setType;
    private final int requiredPieces;
    // Zmiana z Map na List aby móc mieć wiele modyfikatorów dla tego samego atrybutu
    private final List<BonusModifier> bonuses;

    public SetBonus(SetType setType, int requiredPieces) {
        this.setType = setType;
        this.requiredPieces = requiredPieces;
        this.bonuses = new ArrayList<>();
    }

    public SetType getSetType() {
        return setType;
    }

    public int getRequiredPieces() {
        return requiredPieces;
    }

    public List<BonusModifier> getBonuses() {
        return bonuses;
    }

    public void addBonus(Attribute attribute, double value, AttributeModifier.Operation operation) {
        String name = "set_bonus." + setType.name().toLowerCase() + "." + requiredPieces + "_pieces." +
                attribute.name().toLowerCase() + "." + bonuses.size();
        AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), name, value, operation);
        bonuses.add(new BonusModifier(attribute, modifier));
    }

    public void addFlatBonus(Attribute attribute, double value) {
        addBonus(attribute, value, AttributeModifier.Operation.ADD_NUMBER);
    }

    public void addPercentageBonus(Attribute attribute, double percentage) {
        addBonus(attribute, percentage / 100.0, AttributeModifier.Operation.ADD_SCALAR);
    }

    // Wewnętrzna klasa do przechowywania pary atrybut-modyfikator
    public static class BonusModifier {
        private final Attribute attribute;
        private final AttributeModifier modifier;

        public BonusModifier(Attribute attribute, AttributeModifier modifier) {
            this.attribute = attribute;
            this.modifier = modifier;
        }

        public Attribute getAttribute() {
            return attribute;
        }

        public AttributeModifier getModifier() {
            return modifier;
        }
    }
}