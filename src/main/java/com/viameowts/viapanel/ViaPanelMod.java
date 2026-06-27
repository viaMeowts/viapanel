package com.viameowts.viapanel;

import com.viameowts.viapanel.command.ViaPanelCommand;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViaPanelMod implements DedicatedServerModInitializer {
    public static final String MOD_ID = "viapanel";
    public static final Logger LOGGER = LoggerFactory.getLogger("viaPanel");
    public static ViaPanelConfig CONFIG;

    @Override
    public void onInitializeServer() {
        CONFIG = ViaPanelConfig.load();
        ViaPanelPermissionHelper.init();
        CommandRegistrationCallback.EVENT.register(ViaPanelCommand::register);
        LOGGER.info("Initialized viaPanel server module.");
    }
}
