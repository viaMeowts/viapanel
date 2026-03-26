package com.viameowts.viapanel.api;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.List;

public interface ViaPanelProvider {
    String modId();

    Text modDisplayName();

    Text panelTitle();

    boolean hasPermission(ServerCommandSource source);

    Class<?> configClass();

    Object configInstance();

    List<ViaPanelSection> sections();

    default Text fieldDisplayName(String fieldName) {
        return Text.literal(fieldName);
    }

    default Text fieldDescription(String fieldName) {
        return Text.literal(fieldName);
    }

    default Text toggleHintText() {
        return Text.literal("Click to toggle");
    }

    default Text editHintText() {
        return Text.literal("Click to edit");
    }

    default Text savedSuffixText() {
        return Text.literal(" (saved)");
    }

    default Text fieldNotBooleanText() {
        return Text.literal("This field is not a boolean.");
    }

    default Text unknownFieldText() {
        return Text.literal("Unknown config field.");
    }

    default Text invalidNumberText() {
        return Text.literal("Invalid number.");
    }

    default void reload(ServerCommandSource source) {
    }

    default Text reloadDoneText() {
        return Text.literal("Config reloaded successfully.");
    }

    default void onFieldUpdated(String fieldName, ServerCommandSource source) {
    }

    default void applyGlobalLanguage(String languageCode, ServerCommandSource source) {
    }
}
