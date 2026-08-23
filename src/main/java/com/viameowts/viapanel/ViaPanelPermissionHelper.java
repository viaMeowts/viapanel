package com.viameowts.viapanel;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.lang.reflect.Method;
import java.util.UUID;

public final class ViaPanelPermissionHelper {
    private static boolean luckPermsAvailable = false;

    private static Method getApiMethod;
    private static Method getUserManagerMethod;
    private static Method getUserMethod;
    private static Method getCachedDataMethod;
    private static Method getPermissionDataMethod;
    private static Method checkPermissionMethod;
    private static Method tristateAsBooleanMethod;

    private ViaPanelPermissionHelper() {
    }

    public static void init() {
        if (!FabricLoader.getInstance().isModLoaded("luckperms")) {
            ViaPanelMod.LOGGER.info("[viaPanel] LuckPerms not found; /viapanel lang uses OP fallback only.");
            return;
        }

        try {
            Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            getApiMethod = providerClass.getMethod("get");

            Class<?> apiClass = Class.forName("net.luckperms.api.LuckPerms");
            getUserManagerMethod = apiClass.getMethod("getUserManager");

            Class<?> userManagerClass = Class.forName("net.luckperms.api.model.user.UserManager");
            getUserMethod = userManagerClass.getMethod("getUser", UUID.class);

            Class<?> permissionHolderClass = Class.forName("net.luckperms.api.model.PermissionHolder");
            getCachedDataMethod = permissionHolderClass.getMethod("getCachedData");

            Class<?> cachedDataManagerClass = Class.forName("net.luckperms.api.cacheddata.CachedDataManager");
            getPermissionDataMethod = cachedDataManagerClass.getMethod("getPermissionData");

            Class<?> cachedPermissionDataClass = Class.forName("net.luckperms.api.cacheddata.CachedPermissionData");
            checkPermissionMethod = cachedPermissionDataClass.getMethod("checkPermission", String.class);

            Class<?> tristateClass = Class.forName("net.luckperms.api.util.Tristate");
            tristateAsBooleanMethod = tristateClass.getMethod("asBoolean");

            luckPermsAvailable = true;
            ViaPanelMod.LOGGER.info("[viaPanel] LuckPerms detected; global language node checks are enabled.");
        } catch (Throwable t) {
            ViaPanelMod.LOGGER.warn("[viaPanel] LuckPerms reflection init failed: {}", t.getMessage());
            luckPermsAvailable = false;
        }
    }

    public static boolean checkPermission(CommandSourceStack source, String permissionNode, int opFallbackLevel) {
        if (source == null) {
            return false;
        }

        String node = permissionNode == null ? "" : permissionNode.trim();
        if (node.isEmpty()) {
            return hasOpLevel(source, opFallbackLevel);
        }

        if (source.getEntity() instanceof ServerPlayer player) {
            if (hasLuckPermsPermission(player.getUUID(), node)) {
                return true;
            }
        }

        return hasOpLevel(source, opFallbackLevel);
    }

    public static boolean hasOpLevel(CommandSourceStack source, int opLevel) {
        return source != null && opLevel >= 0
                && source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(opLevel)));
    }

    private static boolean hasLuckPermsPermission(UUID uuid, String permissionNode) {
        if (!luckPermsAvailable) {
            return false;
        }

        try {
            Object api = getApiMethod.invoke(null);
            Object userManager = getUserManagerMethod.invoke(api);
            Object user = getUserMethod.invoke(userManager, uuid);
            if (user == null) {
                return false;
            }

            Object cachedData = getCachedDataMethod.invoke(user);
            Object permissionData = getPermissionDataMethod.invoke(cachedData);
            Object tristate = checkPermissionMethod.invoke(permissionData, permissionNode);
            Object result = tristateAsBooleanMethod.invoke(tristate);
            return result instanceof Boolean b && b;
        } catch (Throwable t) {
            ViaPanelMod.LOGGER.debug("[viaPanel] LP permission check failed: {}", t.getMessage());
            return false;
        }
    }
}
