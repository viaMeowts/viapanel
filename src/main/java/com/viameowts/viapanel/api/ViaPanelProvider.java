package com.viameowts.viapanel.api;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.List;

public interface ViaPanelProvider {
    String modId();

    Component modDisplayName();

    Component panelTitle();

    boolean hasPermission(CommandSourceStack source);

    Class<?> configClass();

    Object configInstance();

    List<ViaPanelSection> sections();

    default Component fieldDisplayName(String fieldName) {
        return Component.literal(fieldName);
    }

    default Component fieldDescription(String fieldName) {
        return Component.literal(fieldName);
    }

    default Component toggleHintText() {
        return Component.literal("Click to toggle");
    }

    default Component editHintText() {
        return Component.literal("Click to edit");
    }

    default Component savedSuffixText() {
        return Component.literal(" (saved)");
    }

    default Component fieldNotBooleanText() {
        return Component.literal("This field is not a boolean.");
    }

    default Component unknownFieldText() {
        return Component.literal("Unknown config field.");
    }

    default Component invalidNumberText() {
        return Component.literal("Invalid number.");
    }

    default void reload(CommandSourceStack source) {
    }

    default Component reloadDoneText() {
        return Component.literal("Config reloaded successfully.");
    }

    default void onFieldUpdated(String fieldName, CommandSourceStack source) {
    }

    default void applyGlobalLanguage(String languageCode, CommandSourceStack source) {
    }
}
