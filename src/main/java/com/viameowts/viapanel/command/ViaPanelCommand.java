package com.viameowts.viapanel.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.viameowts.viapanel.ViaPanelConfig;
import com.viameowts.viapanel.api.ViaPanelApi;
import com.viameowts.viapanel.ViaPanelMod;
import com.viameowts.viapanel.ViaPanelPermissionHelper;
import com.viameowts.viapanel.api.ViaPanelProvider;
import com.viameowts.viapanel.api.ViaPanelIntrospector;
import com.viameowts.viapanel.api.ViaPanelSection;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.List;

public class ViaPanelCommand {

    private static final String CMD = "/viapanel";
    private static final TextColor COLOR_HEADER = TextColor.fromRgb(0xFFC64C);
    private static final TextColor COLOR_OK = TextColor.fromRgb(0x98FB98);
    private static final TextColor COLOR_ERROR = TextColor.fromRgb(0xFF5555);
    private static final TextColor COLOR_GRAY_LIGHT = TextColor.fromRgb(0xD9D0D5);
    private static final TextColor COLOR_GRAY_DARK = TextColor.fromRgb(0xB0C4DE);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                CommandBuildContext registryAccess,
                                Commands.CommandSelection environment) {
        dispatcher.register(Commands.literal("viapanel")
                .executes(ViaPanelCommand::showMain)
                .then(Commands.argument("mod", StringArgumentType.word())
                        .executes(ViaPanelCommand::showModMain)
                        .then(Commands.argument("section", StringArgumentType.word())
                                .executes(ViaPanelCommand::showSection)))
                .then(Commands.literal("toggle")
                        .then(Commands.argument("mod", StringArgumentType.word())
                                .then(Commands.argument("field", StringArgumentType.word())
                                        .executes(ViaPanelCommand::toggleField))))
                .then(Commands.literal("set")
                        .then(Commands.argument("mod", StringArgumentType.word())
                                .then(Commands.argument("field", StringArgumentType.word())
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                .executes(ViaPanelCommand::setField)))))
                .then(Commands.literal("reload")
                        .then(Commands.argument("mod", StringArgumentType.word())
                        .executes(ViaPanelCommand::reloadModConfig)))
                .then(Commands.literal("lang")
                    .requires(ViaPanelCommand::hasGlobalAdminPermission)
                    .then(Commands.argument("code", StringArgumentType.word())
                        .executes(ViaPanelCommand::setGlobalLanguage)))
        );
    }

    private static int showMain(CommandContext<CommandSourceStack> ctx) {
        send(ctx, Component.literal(""));
        send(ctx, Component.literal("  " + tr("installed_mods_title")).withStyle(s -> s.withColor(COLOR_HEADER)));
        send(ctx, Component.literal(""));

        List<ModContainer> mods = FabricLoader.getInstance().getAllMods().stream()
                .sorted(Comparator.comparing(m -> m.getMetadata().getName().toLowerCase()))
                .toList();

        for (ModContainer mod : mods) {
            String modId = mod.getMetadata().getId();
            String modName = mod.getMetadata().getName();
            ViaPanelProvider provider = ViaPanelApi.getProvider(modId);
            boolean supported = provider != null;
            boolean hasAccess = supported && provider.hasPermission(ctx.getSource());

            MutableComponent line = Component.literal("  ");
            line.append(Component.literal(supported ? "▸ " : "• ").withStyle(s -> s.withColor(supported ? COLOR_HEADER : COLOR_GRAY_DARK)));

            MutableComponent title = Component.literal(modName + " [" + modId + "]");
                TextColor titleColor = supported
                    ? (hasAccess ? COLOR_GRAY_LIGHT : COLOR_ERROR)
                    : COLOR_GRAY_DARK;
                title.withStyle(s -> s.withColor(titleColor));

            if (supported && hasAccess) {
                title.withStyle(s -> s
                        .withClickEvent(new ClickEvent.RunCommand(CMD + " " + modId))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(tr("open_panel")))));
            } else if (supported) {
                title.withStyle(s -> s.withHoverEvent(new HoverEvent.ShowText(Component.literal(tr("no_permission")))));
            } else {
                title.withStyle(s -> s.withHoverEvent(new HoverEvent.ShowText(Component.literal(tr("no_api")))));
            }

            line.append(title);
            send(ctx, line);
        }

        send(ctx, Component.literal(""));
        send(ctx, buildGlobalLanguageLine(ctx.getSource()));

        send(ctx, Component.literal(""));
        return 1;
    }

    private static MutableComponent buildGlobalLanguageLine(CommandSourceStack source) {
        boolean canChange = hasGlobalAdminPermission(source);
        String current = ViaPanelApi.getGlobalLanguage();

        MutableComponent line = Component.literal("  ");
        line.append(Component.literal("▸ ").withStyle(s -> s.withColor(COLOR_HEADER)));
        line.append(Component.literal(tr("global_language_label") + ": ").withStyle(s -> s.withColor(COLOR_GRAY_LIGHT)));

        line.append(languageButton("en", current, canChange));
        line.append(Component.literal(" ").withStyle(s -> s.withColor(COLOR_GRAY_DARK)));
        line.append(languageButton("ru", current, canChange));

        if (!canChange) {
            line.append(Component.literal("  [" + tr("global_language_locked") + "]").withStyle(s -> s.withColor(COLOR_GRAY_DARK)));
        }

        return line;
    }

    private static MutableComponent languageButton(String code, String current, boolean canChange) {
        boolean active = code.equalsIgnoreCase(current);
        TextColor color = active ? COLOR_OK : COLOR_GRAY_LIGHT;
        MutableComponent button = Component.literal("[" + code.toUpperCase() + "]").withStyle(s -> s.withColor(color));

        if (canChange) {
            button.withStyle(s -> s
                    .withClickEvent(new ClickEvent.RunCommand(CMD + " lang " + code))
                    .withHoverEvent(new HoverEvent.ShowText(Component.literal(tr("global_language_click_hint")))));
        } else {
            button.withStyle(s -> s
                    .withHoverEvent(new HoverEvent.ShowText(Component.literal(tr("global_language_permission_hint")))));
        }

        return button;
    }

    private static int showModMain(CommandContext<CommandSourceStack> ctx) {
        String modId = StringArgumentType.getString(ctx, "mod");
        ViaPanelProvider provider = requireProvider(ctx, modId);
        if (provider == null) return 0;

        send(ctx, Component.literal(""));
        send(ctx, Component.literal("  ").append(provider.panelTitle()));
        send(ctx, Component.literal(""));

        for (ViaPanelSection section : provider.sections()) {
            MutableComponent line = Component.literal("  ▸ ").withStyle(s -> s.withColor(COLOR_HEADER))
                    .append(section.title().copy().withStyle(s -> s
                    .withColor(COLOR_GRAY_LIGHT)
                            .withClickEvent(new ClickEvent.RunCommand(CMD + " " + modId + " " + section.id()))
                    .withHoverEvent(new HoverEvent.ShowText(Component.literal(tr("open_section"))))));
            send(ctx, line);
        }

        send(ctx, Component.literal(""));
        send(ctx, Component.literal("  ").append(Component.literal("⟳ " + tr("reload_config")).withStyle(s -> s
            .withColor(COLOR_HEADER)
                .withClickEvent(new ClickEvent.RunCommand(CMD + " reload " + modId))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal(tr("reload_hover")))))));
        send(ctx, Component.literal(""));
        return 1;
    }

    private static int showSection(CommandContext<CommandSourceStack> ctx) {
        String modId = StringArgumentType.getString(ctx, "mod");
        String sectionId = StringArgumentType.getString(ctx, "section");

        ViaPanelProvider provider = requireProvider(ctx, modId);
        if (provider == null) return 0;

        ViaPanelSection section = provider.sections().stream()
                .filter(s -> s.id().equalsIgnoreCase(sectionId))
                .findFirst().orElse(null);

        if (section == null) {
            ctx.getSource().sendFailure(Component.literal(tr("unknown_section") + ": " + sectionId).withStyle(s -> s.withColor(COLOR_ERROR)));
            return 0;
        }

        Object config = provider.configInstance();
        if (config == null) {
            ctx.getSource().sendFailure(Component.literal(tr("config_not_loaded")).withStyle(s -> s.withColor(COLOR_ERROR)));
            return 0;
        }

        send(ctx, Component.literal(""));
        send(ctx, Component.literal("  ").append(section.title()));
        send(ctx, Component.literal(""));

        for (String fieldName : section.fields()) {
            ViaPanelIntrospector.FieldMeta meta =
                    ViaPanelIntrospector.field(provider.configClass(), fieldName);
            try {
                MutableComponent line = Component.literal("  ").append(provider.fieldDisplayName(fieldName)).append(Component.literal(": ").withStyle(s -> s.withColor(COLOR_GRAY_LIGHT)));

                if (meta != null) {
                    appendAnnotatedValue(ctx, provider, config, meta, line);
                } else {
                    Field field = provider.configClass().getField(fieldName);
                    Object value = field.get(config);
                    appendLegacyValue(ctx, provider, fieldName, value, line);
                }

                send(ctx, line);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                send(ctx, Component.literal("  " + fieldName + ": ").withStyle(s -> s.withColor(COLOR_GRAY_DARK))
                        .append(Component.literal("(error)").withStyle(s -> s.withColor(COLOR_ERROR))));
            }
        }

        send(ctx, Component.literal(""));
        send(ctx, Component.literal("  ◄ " + tr("back")).withStyle(s -> s
                .withColor(COLOR_HEADER)
                .withClickEvent(new ClickEvent.RunCommand(CMD + " " + modId))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal(tr("back_hover"))))));
        send(ctx, Component.literal(""));

        return 1;
    }

    private static void appendAnnotatedValue(CommandContext<CommandSourceStack> ctx, ViaPanelProvider provider,
                                             Object config, ViaPanelIntrospector.FieldMeta meta,
                                             MutableComponent line) throws IllegalAccessException {
        String modId = provider.modId();
        Class<?> t = meta.type();
        Component langDesc = meta.descFor(ViaPanelApi.getGlobalLanguage());
        if (langDesc.getString().isBlank()) {
            langDesc = provider.fieldDescription(meta.key());
        }
        final Component desc = langDesc;

        if (t == boolean.class) {
            boolean v = meta.field().getBoolean(config);
            line.append(Component.literal(v ? "[ON]" : "[OFF]").withStyle(s -> s
                    .withColor(v ? COLOR_OK : COLOR_ERROR)
                    .withClickEvent(new ClickEvent.RunCommand(CMD + " toggle " + modId + " " + meta.key()))
                    .withHoverEvent(new HoverEvent.ShowText(buildFieldHover(desc, provider.toggleHintText(), null)))));
        } else if (t.isEnum() && !meta.secret()) {
            String current = ((Enum<?>) meta.field().get(config)).name();
            String next = ViaPanelIntrospector.nextEnumValue(config, meta);
            StringBuilder values = new StringBuilder();
            for (Object c : t.getEnumConstants()) {
                if (!values.isEmpty()) {
                    values.append(" / ");
                }
                values.append(((Enum<?>) c).name());
            }
            String hoverExtra = next != null
                    ? tr("enum_cycle") + " -> " + next
                    : tr("enum_options") + ": " + values;
            line.append(Component.literal(current).withStyle(s -> s
                    .withColor(COLOR_GRAY_LIGHT)
                    .withClickEvent(next != null
                            ? new ClickEvent.RunCommand(CMD + " set " + modId + " " + meta.key() + " " + next)
                            : null)
                    .withHoverEvent(new HoverEvent.ShowText(buildFieldHover(desc,
                            Component.literal(hoverExtra), null)))));
        } else {
            String display = ViaPanelIntrospector.displayValue(config, meta);
            String suggest = CMD + " set " + modId + " " + meta.key()
                    + (meta.secret() ? " " : " " + ViaPanelIntrospector.suggestValue(config, meta));
            String shown = display.length() > 25 ? display.substring(0, 22) + "..." : display;
            if (!meta.secret() && t == String.class) {
                shown = "\"" + shown + "\"";
            }
            Component rangeHint = meta.bounded()
                    ? Component.literal(tr("range") + " [" +
                            (Double.isNaN(meta.min()) ? "-inf" : ViaPanelIntrospector.formatNumber(meta.min()))
                            + " .. " +
                            (Double.isNaN(meta.max()) ? "+inf" : ViaPanelIntrospector.formatNumber(meta.max())) + "]")
                    : null;
            MutableComponent hoverBase = rangeHint != null ? desc.copy().append("\n").append(rangeHint) : desc.copy();
            hoverBase.append("\n").append(provider.editHintText());
            if (!meta.secret() && !suggest.endsWith(" ")) {
                hoverBase.append("\n").append(Component.literal(ViaPanelIntrospector.suggestValue(config, meta))
                        .withStyle(s2 -> s2.withColor(COLOR_GRAY_DARK)));
            }
            line.append(Component.literal(shown).withStyle(s -> s
                    .withColor(COLOR_GRAY_LIGHT)
                    .withClickEvent(new ClickEvent.SuggestCommand(suggest))
                    .withHoverEvent(new HoverEvent.ShowText(hoverBase))));
        }
    }

    private static void appendLegacyValue(CommandContext<CommandSourceStack> ctx, ViaPanelProvider provider,
                                          String fieldName, Object value, MutableComponent line) {
        String modId = provider.modId();
        if (value instanceof Boolean bool) {
            line.append(Component.literal(bool ? "[ON]" : "[OFF]").withStyle(s -> s
                    .withColor(bool ? COLOR_OK : COLOR_ERROR)
                    .withClickEvent(new ClickEvent.RunCommand(CMD + " toggle " + modId + " " + fieldName))
                    .withHoverEvent(new HoverEvent.ShowText(buildFieldHover(provider.fieldDescription(fieldName), provider.toggleHintText(), null)))));
        } else if (value instanceof Double d) {
            line.append(Component.literal(String.valueOf(d)).withStyle(s -> s
                    .withColor(COLOR_GRAY_LIGHT)
                    .withClickEvent(new ClickEvent.SuggestCommand(CMD + " set " + modId + " " + fieldName + " " + d))
                    .withHoverEvent(new HoverEvent.ShowText(buildFieldHover(provider.fieldDescription(fieldName), provider.editHintText(), null)))));
        } else if (value instanceof Integer i) {
            line.append(Component.literal(String.valueOf(i)).withStyle(s -> s
                    .withColor(COLOR_GRAY_LIGHT)
                    .withClickEvent(new ClickEvent.SuggestCommand(CMD + " set " + modId + " " + fieldName + " " + i))
                    .withHoverEvent(new HoverEvent.ShowText(buildFieldHover(provider.fieldDescription(fieldName), provider.editHintText(), null)))));
        } else if (value instanceof Float f) {
            line.append(Component.literal(String.valueOf(f)).withStyle(s -> s
                    .withColor(COLOR_GRAY_LIGHT)
                    .withClickEvent(new ClickEvent.SuggestCommand(CMD + " set " + modId + " " + fieldName + " " + f))
                    .withHoverEvent(new HoverEvent.ShowText(buildFieldHover(provider.fieldDescription(fieldName), provider.editHintText(), null)))));
        } else if (value instanceof Long l) {
            line.append(Component.literal(String.valueOf(l)).withStyle(s -> s
                    .withColor(COLOR_GRAY_LIGHT)
                    .withClickEvent(new ClickEvent.SuggestCommand(CMD + " set " + modId + " " + fieldName + " " + l))
                    .withHoverEvent(new HoverEvent.ShowText(buildFieldHover(provider.fieldDescription(fieldName), provider.editHintText(), null)))));
        } else if (value instanceof String str) {
            String display = str.length() > 25 ? str.substring(0, 22) + "..." : str;
            line.append(Component.literal("\"" + display + "\"").withStyle(s -> s
                    .withColor(COLOR_GRAY_LIGHT)
                    .withClickEvent(new ClickEvent.SuggestCommand(CMD + " set " + modId + " " + fieldName + " " + str))
                    .withHoverEvent(new HoverEvent.ShowText(buildFieldHover(provider.fieldDescription(fieldName), provider.editHintText(), str)))));
        } else {
            line.append(Component.literal("(" + tr("unsupported") + ")").withStyle(s -> s.withColor(COLOR_GRAY_DARK)));
        }
    }

    private static int toggleField(CommandContext<CommandSourceStack> ctx) {
        String modId = StringArgumentType.getString(ctx, "mod");
        String fieldName = StringArgumentType.getString(ctx, "field");

        ViaPanelProvider provider = requireProvider(ctx, modId);
        if (provider == null) return 0;

        Object cfg = provider.configInstance();
        if (cfg == null) {
            ctx.getSource().sendFailure(Component.literal(tr("config_not_loaded")).withStyle(s -> s.withColor(COLOR_ERROR)));
            return 0;
        }

        try {
            Field field = provider.configClass().getField(fieldName);
            if (field.getType() != boolean.class) {
                ctx.getSource().sendFailure(provider.fieldNotBooleanText());
                return 0;
            }

            boolean current = field.getBoolean(cfg);
            field.setBoolean(cfg, !current);

            if (!saveConfig(cfg, ctx.getSource())) {
                return 0;
            }
            provider.onFieldUpdated(fieldName, ctx.getSource());

            boolean newVal = !current;
            ctx.getSource().sendSuccess(
                    () -> provider.fieldDisplayName(fieldName).copy()
                            .append(Component.literal(": "))
                        .append(Component.literal(newVal ? "ON" : "OFF").withStyle(s -> s.withColor(newVal ? COLOR_OK : COLOR_ERROR)))
                            .append(provider.savedSuffixText()),
                    false
            );
            return 1;
        } catch (NoSuchFieldException e) {
            ctx.getSource().sendFailure(provider.unknownFieldText());
        } catch (IllegalAccessException e) {
            ctx.getSource().sendFailure(Component.literal(tr("cannot_access_field") + ": " + fieldName).withStyle(s -> s.withColor(COLOR_ERROR)));
        }

        return 0;
    }

    private static int setField(CommandContext<CommandSourceStack> ctx) {
        String modId = StringArgumentType.getString(ctx, "mod");
        String fieldName = StringArgumentType.getString(ctx, "field");
        String rawValue = StringArgumentType.getString(ctx, "value");

        ViaPanelProvider provider = requireProvider(ctx, modId);
        if (provider == null) return 0;

        Object cfg = provider.configInstance();
        if (cfg == null) {
            ctx.getSource().sendFailure(Component.literal(tr("config_not_loaded")).withStyle(s -> s.withColor(COLOR_ERROR)));
            return 0;
        }

        try {
            Field field = provider.configClass().getField(fieldName);
            Class<?> type = field.getType();
            ViaPanelIntrospector.FieldMeta meta =
                    ViaPanelIntrospector.field(provider.configClass(), fieldName);

            if (meta != null) {
                Component error = ViaPanelIntrospector.applyValue(cfg, meta, rawValue);
                if (error != null) {
                    ctx.getSource().sendFailure(error.copy().withStyle(s -> s.withColor(COLOR_ERROR)));
                    return 0;
                }
            } else if (type == String.class) {
                field.set(cfg, rawValue);
            } else if (type == double.class) {
                field.setDouble(cfg, Double.parseDouble(rawValue));
            } else if (type == float.class) {
                field.setFloat(cfg, Float.parseFloat(rawValue));
            } else if (type == int.class) {
                field.setInt(cfg, Integer.parseInt(rawValue));
            } else if (type == long.class) {
                field.setLong(cfg, Long.parseLong(rawValue));
            } else if (type == boolean.class) {
                field.setBoolean(cfg, Boolean.parseBoolean(rawValue));
            } else {
                ctx.getSource().sendFailure(Component.literal(tr("unsupported_field_type")).withStyle(s -> s.withColor(COLOR_ERROR)));
                return 0;
            }

            if (!saveConfig(cfg, ctx.getSource())) {
                return 0;
            }
            provider.onFieldUpdated(fieldName, ctx.getSource());

            String shownValue = meta != null && meta.secret() ? "***" : rawValue;
            ctx.getSource().sendSuccess(
                    () -> provider.fieldDisplayName(fieldName).copy()
                            .append(Component.literal(" = "))
                        .append(Component.literal(shownValue).withStyle(s -> s.withColor(COLOR_GRAY_LIGHT)))
                            .append(provider.savedSuffixText()),
                    false);
            return 1;
        } catch (NoSuchFieldException e) {
            ctx.getSource().sendFailure(provider.unknownFieldText());
        } catch (NumberFormatException e) {
            ctx.getSource().sendFailure(provider.invalidNumberText());
        } catch (IllegalAccessException e) {
            ctx.getSource().sendFailure(Component.literal(tr("cannot_access_field") + ": " + fieldName).withStyle(s -> s.withColor(COLOR_ERROR)));
        }

        return 0;
    }

    private static int reloadModConfig(CommandContext<CommandSourceStack> ctx) {
        String modId = StringArgumentType.getString(ctx, "mod");

        ViaPanelProvider provider = requireProvider(ctx, modId);
        if (provider == null) return 0;

        provider.reload(ctx.getSource());
        ctx.getSource().sendSuccess(provider::reloadDoneText, false);
        return 1;
    }

    private static int setGlobalLanguage(CommandContext<CommandSourceStack> ctx) {
        String code = StringArgumentType.getString(ctx, "code").trim().toLowerCase();
        if (!"ru".equals(code) && !"en".equals(code)) {
            ctx.getSource().sendFailure(Component.literal(tr("invalid_lang_code")).withStyle(s -> s.withColor(COLOR_ERROR)));
            return 0;
        }

        ViaPanelApi.applyGlobalLanguageToAll(code, ctx.getSource());
        ctx.getSource().sendSuccess(
                () -> Component.literal(tr("lang_applied") + ": " + code).withStyle(s -> s.withColor(COLOR_GRAY_LIGHT)),
                false
        );
        return 1;
    }

    private static MutableComponent buildFieldHover(Component description, Component action, String rawValue) {
        MutableComponent hover = description.copy();
        hover.append(Component.literal("\n"));
        hover.append(action.copy());
        if (rawValue != null && !rawValue.isBlank()) {
            hover.append(Component.literal("\n" + rawValue).withStyle(s -> s.withColor(COLOR_GRAY_DARK)));
        }
        return hover;
    }

    private static boolean hasGlobalAdminPermission(CommandSourceStack source) {
        ViaPanelConfig cfg = ViaPanelMod.CONFIG != null ? ViaPanelMod.CONFIG : new ViaPanelConfig();
        return ViaPanelPermissionHelper.checkPermission(source, cfg.globalLanguagePermission, cfg.globalLanguageOpLevel);
    }

    private static ViaPanelProvider requireProvider(CommandContext<CommandSourceStack> ctx, String modId) {
        ViaPanelProvider provider = ViaPanelApi.getProvider(modId);
        if (provider == null) {
            ctx.getSource().sendFailure(Component.literal(tr("mod_no_api") + ": " + modId).withStyle(s -> s.withColor(COLOR_ERROR)));
            return null;
        }
        if (!provider.hasPermission(ctx.getSource())) {
            ctx.getSource().sendFailure(Component.literal(tr("no_permission_panel") + ": " + modId).withStyle(s -> s.withColor(COLOR_ERROR)));
            return null;
        }
        return provider;
    }

    private static String tr(String key) {
        boolean ru = "ru".equalsIgnoreCase(ViaPanelApi.getGlobalLanguage());
        return switch (key) {
            case "installed_mods_title" -> ru ? "viaPanel • Установленные моды" : "viaPanel • Installed mods";
            case "open_panel" -> ru ? "Открыть панель" : "Open panel";
            case "no_permission" -> ru ? "Нет прав для этой панели мода." : "No permission for this mod panel.";
            case "no_api" -> ru ? "Этот мод не зарегистрировал viaPanel API." : "This mod has not registered viaPanel API.";
            case "open_section" -> ru ? "Открыть раздел" : "Open section";
            case "reload_config" -> ru ? "Перезагрузить конфиг" : "Reload Config";
            case "reload_hover" -> ru ? "Перечитать конфиг с диска" : "Reload config from disk";
            case "unknown_section" -> ru ? "Неизвестный раздел" : "Unknown section";
            case "config_not_loaded" -> ru ? "Конфиг не загружен." : "Config is not loaded.";
            case "unsupported" -> ru ? "не поддерживается" : "unsupported";
            case "back" -> ru ? "Назад" : "Back";
            case "back_hover" -> ru ? "Назад к панели мода" : "Back to mod panel";
            case "cannot_access_field" -> ru ? "Нет доступа к полю" : "Cannot access field";
            case "unsupported_field_type" -> ru ? "Неподдерживаемый тип поля." : "Unsupported field type.";
            case "enum_options" -> ru ? "Варианты" : "Options";
            case "enum_cycle" -> ru ? "Нажмите, чтобы переключить на" : "Click to switch to";
            case "range" -> ru ? "Диапазон:" : "Range:";
            case "save_failed" -> ru ? "Ошибка сохранения конфига: " : "Config save failed: ";
            case "mod_no_api" -> ru ? "Мод не предоставляет viaPanel API" : "Mod does not expose viaPanel API";
            case "no_permission_panel" -> ru ? "Нет прав для панели" : "No permission for panel";
            case "invalid_lang_code" -> ru ? "Неверный код языка. Используй: ru или en." : "Invalid language code. Use: ru or en.";
            case "lang_applied" -> ru ? "Глобальный язык применён" : "Global language applied";
            case "global_language_label" -> ru ? "Глобальный язык" : "Global language";
            case "global_language_locked" -> ru ? "недостаточно прав" : "insufficient permission";
            case "global_language_click_hint" -> ru ? "Нажмите, чтобы применить язык ко всем модам" : "Click to apply language to all mods";
            case "global_language_permission_hint" -> ru ? "Недостаточно прав для глобального переключения языка" : "Insufficient permission for global language switch";
            default -> key;
        };
    }

    private static boolean saveConfig(Object config, CommandSourceStack source) {
        try {
            config.getClass().getMethod("save").invoke(config);
            return true;
        } catch (NoSuchMethodException e) {
            return true;
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            source.sendFailure(Component.literal(tr("save_failed") + cause.getMessage())
                    .withStyle(s -> s.withColor(COLOR_ERROR)));
            return false;
        }
    }

    private static void send(CommandContext<CommandSourceStack> ctx, Component text) {
        ctx.getSource().sendSuccess(() -> text, false);
    }
}
