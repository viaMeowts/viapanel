package com.viameowts.viapanel;

import com.viameowts.viapanel.api.ViaPanelApi;
import com.viameowts.viapanel.command.ViaPanelCommand;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class ViaPanelMod implements DedicatedServerModInitializer {
    public static final String MOD_ID = "viapanel";
    public static final Logger LOGGER = LoggerFactory.getLogger("viaPanel");
    public static ViaPanelConfig CONFIG;

    @Override
    public void onInitializeServer() {
        CONFIG = ViaPanelConfig.load();
        ViaPanelApi.setServerIdentity(resolveServerId(CONFIG.serverId), CONFIG.serverDisplayName);
        ViaPanelPermissionHelper.init();
        CommandRegistrationCallback.EVENT.register(ViaPanelCommand::register);
        MeridianaAudit.check("panels", "Панели: какие моды подключили свои настройки", () -> {
            int n = ViaPanelApi.getProviders().size();
            return java.util.List.of(n == 0 ? "INFO: ни один мод пока не подключил панель настроек"
                    : "Подключено панелей настроек: " + n);
        });
        LOGGER.info("Initialized viaPanel server module (server id: {}).", ViaPanelApi.getServerId());
    }

    /** Configured id, or the name of the server folder ("lobby", "arrakis", ...) when empty. */
    static String resolveServerId(String configured) {
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        Path dir = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize().getFileName();
        return dir == null ? "server" : dir.toString();
    }
}
