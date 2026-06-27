package com.viameowts.viapanel;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ViaPanelConfig {
    public String globalLanguagePermission = "viapanel.command.lang";
    public int globalLanguageOpLevel = 3;

    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("viaPanel");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("viaPanel.toml");

    public static ViaPanelConfig load() {
        ViaPanelConfig config = new ViaPanelConfig();
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException ignored) {
        }

        if (!Files.exists(CONFIG_PATH)) {
            config.save();
            return config;
        }

        try {
            List<String> lines = Files.readAllLines(CONFIG_PATH);
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }

                String[] parts = line.split("=", 2);
                String key = parts[0].trim();
                String value = parts[1].trim();

                if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                    value = value.substring(1, value.length() - 1);
                }

                switch (key) {
                    case "global_language_permission" -> config.globalLanguagePermission = value;
                    case "global_language_op_level" -> {
                        try {
                            config.globalLanguageOpLevel = Math.max(0, Math.min(4, Integer.parseInt(value)));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    default -> {
                    }
                }
            }
        } catch (IOException e) {
            ViaPanelMod.LOGGER.warn("[viaPanel] Failed to read config: {}", e.getMessage());
        }

        config.save();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_DIR);
            List<String> out = new ArrayList<>();
            out.add("# viaPanel configuration");
            out.add("# Permission node checked for /viapanel lang <ru|en> (when LuckPerms is present)");
            out.add("global_language_permission = \"" + escape(globalLanguagePermission) + "\"");
            out.add("");
            out.add("# Vanilla OP fallback level for /viapanel lang (0..4)");
            out.add("global_language_op_level = " + Math.max(0, Math.min(4, globalLanguageOpLevel)));
            Files.write(CONFIG_PATH, out);
        } catch (IOException e) {
            ViaPanelMod.LOGGER.warn("[viaPanel] Failed to write config: {}", e.getMessage());
        }
    }

    private static String escape(String in) {
        if (in == null) return "";
        return in.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
