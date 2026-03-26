package com.viameowts.viapanel.api;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ViaPanelApi {
    private static final Map<String, ViaPanelProvider> PROVIDERS = new ConcurrentHashMap<>();
    private static volatile String globalLanguage = "en";

    private ViaPanelApi() {
    }

    public static void register(ViaPanelProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider cannot be null");
        }
        PROVIDERS.put(provider.modId(), provider);
    }

    public static void unregister(String modId) {
        if (modId != null) {
            PROVIDERS.remove(modId);
        }
    }

    public static ViaPanelProvider getProvider(String modId) {
        if (modId == null) {
            return null;
        }
        return PROVIDERS.get(modId);
    }

    public static List<ViaPanelProvider> getProviders() {
        return PROVIDERS.values().stream()
                .sorted(Comparator.comparing(ViaPanelProvider::modId))
                .toList();
    }

    public static String getGlobalLanguage() {
        return globalLanguage;
    }

    public static void setGlobalLanguage(String languageCode) {
        if (languageCode == null) {
            return;
        }
        String normalized = languageCode.trim().toLowerCase();
        if (!normalized.equals("ru") && !normalized.equals("en")) {
            return;
        }
        globalLanguage = normalized;
    }

    public static void applyGlobalLanguageToAll(String languageCode, net.minecraft.server.command.ServerCommandSource source) {
        setGlobalLanguage(languageCode);
        for (ViaPanelProvider provider : getProviders()) {
            provider.applyGlobalLanguage(globalLanguage, source);
        }
    }
}
