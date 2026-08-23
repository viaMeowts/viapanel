package com.viameowts.viapanel.api;

import com.viameowts.viapanel.ViaPanelMod;
import com.viameowts.viapanel.ViaPanelPermissionHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Factory for reflection-free {@link ViaPanelProvider} implementations driven by
 * {@link ViaPanelField} annotations on the config class.
 *
 * <pre>{@code
 * ViaPanelApi.register(ViaPanelProviders
 *         .builder("viastress", "viaStress", ViaStress.CONFIG)
 *         .permission(src -> Permissions.check(src, "viastress.panel", 2))
 *         .build());
 * }</pre>
 *
 * Legacy hand-written providers keep working unchanged.
 */
public final class ViaPanelProviders {

    private ViaPanelProviders() {
    }

    public static Builder builder(String modId, String displayName, Object configInstance) {
        return new Builder(modId, Component.literal(displayName), configInstance);
    }

    public static Builder builder(String modId, Component displayName, Object configInstance) {
        return new Builder(modId, displayName, configInstance);
    }

    public static final class Builder {
        private final String modId;
        private final Component displayName;
        private final Object configInstance;
        private Component panelTitle;
        private Predicate<CommandSourceStack> permission =
                src -> src != null && ViaPanelPermissionHelper.hasOpLevel(src, 2);
        private BiConsumer<String, CommandSourceStack> onFieldUpdated;
        private Consumer<String> languageHook;
        private Runnable onReload;

        private Builder(String modId, Component displayName, Object configInstance) {
            this.modId = modId;
            this.displayName = displayName;
            this.configInstance = configInstance;
        }

        /** Title of the panel screen. Default: "{displayName} Settings". */
        public Builder panelTitle(Component title) {
            this.panelTitle = title;
            return this;
        }

        /** Access check for the whole panel. Default: OP level 2. */
        public Builder permission(Predicate<CommandSourceStack> permission) {
            this.permission = permission;
            return this;
        }

        /** Extra callback after a field was written and saved. */
        public Builder onFieldUpdated(BiConsumer<String, CommandSourceStack> hook) {
            this.onFieldUpdated = hook;
            return this;
        }

        /** Called by /viapanel lang for every registered provider (may be null-safe no-op). */
        public Builder languageHook(Consumer<String> hook) {
            this.languageHook = hook;
            return this;
        }

        /** Extra callback after reload finished. */
        public Builder onReload(Runnable hook) {
            this.onReload = hook;
            return this;
        }

        /** Validates metadata and builds the provider. Does not register it. */
        public ViaPanelProvider build() {
            if (modId == null || modId.isBlank()) {
                throw new IllegalStateException("modId must not be blank");
            }
            if (configInstance == null) {
                throw new IllegalStateException("configInstance must not be null (mod: " + modId + ")");
            }
            if (!ViaPanelIntrospector.isAnnotated(configInstance.getClass())) {
                throw new IllegalStateException(
                        "Config class " + configInstance.getClass().getName()
                                + " has no @" + ViaPanelField.class.getSimpleName()
                                + " fields. Implement ViaPanelProvider directly for legacy panels.");
            }
            return new AnnotatedProvider(this);
        }
    }

    private static final class AnnotatedProvider implements ViaPanelProvider {
        private final String modId;
        private final Component displayName;
        private final Component panelTitle;
        private final Object config;
        private final Predicate<CommandSourceStack> permission;
        private final BiConsumer<String, CommandSourceStack> onFieldUpdated;
        private final Consumer<String> languageHook;
        private final Runnable onReload;

        private AnnotatedProvider(Builder b) {
            this.modId = b.modId;
            this.displayName = b.displayName;
            this.panelTitle = b.panelTitle != null ? b.panelTitle
                    : Component.literal(b.displayName.getString() + " Settings");
            this.config = b.configInstance;
            this.permission = b.permission;
            this.onFieldUpdated = b.onFieldUpdated;
            this.languageHook = b.languageHook;
            this.onReload = b.onReload;
        }

        @Override
        public String modId() {
            return modId;
        }

        @Override
        public Component modDisplayName() {
            return displayName.copy();
        }

        @Override
        public Component panelTitle() {
            return panelTitle.copy();
        }

        @Override
        public boolean hasPermission(CommandSourceStack source) {
            return permission.test(source);
        }

        @Override
        public Class<?> configClass() {
            return config.getClass();
        }

        @Override
        public Object configInstance() {
            return config;
        }

        @Override
        public List<ViaPanelSection> sections() {
            return ViaPanelIntrospector.sections(config.getClass()).stream()
                    .map(s -> new ViaPanelSection(s.id(), s.title(),
                            s.fields().stream().map(ViaPanelIntrospector.FieldMeta::key).toList()))
                    .toList();
        }

        @Override
        public Component fieldDisplayName(String fieldName) {
            ViaPanelIntrospector.FieldMeta meta = ViaPanelIntrospector.field(configClass(), fieldName);
            return meta != null
                    ? meta.nameFor(ViaPanelApi.getGlobalLanguage()).copy()
                    : Component.literal(fieldName);
        }

        @Override
        public Component fieldDescription(String fieldName) {
            ViaPanelIntrospector.FieldMeta meta = ViaPanelIntrospector.field(configClass(), fieldName);
            if (meta != null && !meta.descFor(ViaPanelApi.getGlobalLanguage()).getString().isBlank()) {
                return meta.descFor(ViaPanelApi.getGlobalLanguage()).copy();
            }
            return ViaPanelProvider.super.fieldDescription(fieldName);
        }

        @Override
        public void reload(CommandSourceStack source) {
            copyFromLoad(config, config.getClass());
            if (onReload != null) {
                onReload.run();
            }
        }

        @Override
        public void onFieldUpdated(String fieldName, CommandSourceStack source) {
            if (onFieldUpdated != null) {
                onFieldUpdated.accept(fieldName, source);
            }
        }

        @Override
        public void applyGlobalLanguage(String languageCode, CommandSourceStack source) {
            if (languageHook != null) {
                languageHook.accept(languageCode);
            }
        }
    }

    /**
     * Copies values from a freshly loaded config instance into the live one, using
     * the standard ecosystem pattern: a no-arg {@code load()} method returning a
     * new config object (static or instance). Falls back to a warning when absent.
     */
    static void copyFromLoad(Object live, Class<?> cfgClass) {
        Method load;
        try {
            load = cfgClass.getMethod("load");
        } catch (NoSuchMethodException e) {
            ViaPanelMod.LOGGER.warn("[viaPanel] {} has no load(); panel reload is a no-op", cfgClass.getName());
            return;
        }
        try {
            Object fresh = Modifier.isStatic(load.getModifiers()) ? load.invoke(null) : load.invoke(live);
            if (fresh == null || fresh.getClass() != cfgClass || fresh == live) {
                return;
            }
            for (ViaPanelIntrospector.FieldMeta meta : ViaPanelIntrospector.fields(cfgClass)) {
                meta.field().set(live, meta.field().get(fresh));
            }
        } catch (ReflectiveOperationException e) {
            ViaPanelMod.LOGGER.warn("[viaPanel] Reload of {} failed: {}", cfgClass.getName(), e.getMessage());
        }
    }
}
