package com.viameowts.viapanel.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.viameowts.viapanel.api.ViaPanelApi;
import com.viameowts.viapanel.api.ViaPanelProvider;
import com.viameowts.viapanel.api.ViaPanelSection;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.List;

public class ViaPanelCommand {

    private static final String CMD = "/viapanel";
    private static final TextColor COLOR_HEADER = TextColor.fromRgb(0xFBD06A);
    private static final TextColor COLOR_OK = TextColor.fromRgb(0x0BDA51);
    private static final TextColor COLOR_ERROR = TextColor.fromRgb(0xFF2C2C);
    private static final TextColor COLOR_GRAY_LIGHT = TextColor.fromRgb(0xD9D0D5);
    private static final TextColor COLOR_GRAY_DARK = TextColor.fromRgb(0xA89FA4);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("viapanel")
                .executes(ViaPanelCommand::showMain)
                .then(CommandManager.argument("mod", StringArgumentType.word())
                        .executes(ViaPanelCommand::showModMain)
                        .then(CommandManager.argument("section", StringArgumentType.word())
                                .executes(ViaPanelCommand::showSection)))
                .then(CommandManager.literal("toggle")
                        .then(CommandManager.argument("mod", StringArgumentType.word())
                                .then(CommandManager.argument("field", StringArgumentType.word())
                                        .executes(ViaPanelCommand::toggleField))))
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("mod", StringArgumentType.word())
                                .then(CommandManager.argument("field", StringArgumentType.word())
                                        .then(CommandManager.argument("value", StringArgumentType.greedyString())
                                                .executes(ViaPanelCommand::setField)))))
                .then(CommandManager.literal("reload")
                        .then(CommandManager.argument("mod", StringArgumentType.word())
                        .executes(ViaPanelCommand::reloadModConfig)))
                .then(CommandManager.literal("lang")
                    .then(CommandManager.argument("code", StringArgumentType.word())
                        .executes(ViaPanelCommand::setGlobalLanguage)))
        );
    }

    private static int showMain(CommandContext<ServerCommandSource> ctx) {
        send(ctx, Text.literal(""));
        send(ctx, Text.literal("  " + tr("installed_mods_title")).styled(s -> s.withColor(COLOR_HEADER)));
        send(ctx, Text.literal(""));

        List<ModContainer> mods = FabricLoader.getInstance().getAllMods().stream()
                .sorted(Comparator.comparing(m -> m.getMetadata().getName().toLowerCase()))
                .toList();

        for (ModContainer mod : mods) {
            String modId = mod.getMetadata().getId();
            String modName = mod.getMetadata().getName();
            ViaPanelProvider provider = ViaPanelApi.getProvider(modId);
            boolean supported = provider != null;
            boolean hasAccess = supported && provider.hasPermission(ctx.getSource());

            MutableText line = Text.literal("  ");
            line.append(Text.literal(supported ? "▸ " : "• ").styled(s -> s.withColor(supported ? COLOR_HEADER : COLOR_GRAY_DARK)));

            MutableText title = Text.literal(modName + " [" + modId + "]");
                TextColor titleColor = supported
                    ? (hasAccess ? COLOR_GRAY_LIGHT : COLOR_ERROR)
                    : COLOR_GRAY_DARK;
                title.styled(s -> s.withColor(titleColor));

            if (supported && hasAccess) {
                title.styled(s -> s
                        .withClickEvent(new ClickEvent.RunCommand(CMD + " " + modId))
                        .withHoverEvent(new HoverEvent.ShowText(Text.literal(tr("open_panel")))));
            } else if (supported) {
                title.styled(s -> s.withHoverEvent(new HoverEvent.ShowText(Text.literal(tr("no_permission")))));
            } else {
                title.styled(s -> s.withHoverEvent(new HoverEvent.ShowText(Text.literal(tr("no_api")))));
            }

            line.append(title);
            send(ctx, line);
        }

        send(ctx, Text.literal(""));
        return 1;
    }

    private static int showModMain(CommandContext<ServerCommandSource> ctx) {
        String modId = StringArgumentType.getString(ctx, "mod");
        ViaPanelProvider provider = requireProvider(ctx, modId);
        if (provider == null) return 0;

        send(ctx, Text.literal(""));
        send(ctx, Text.literal("  ").append(provider.panelTitle()));
        send(ctx, Text.literal(""));

        for (ViaPanelSection section : provider.sections()) {
            MutableText line = Text.literal("  ▸ ").styled(s -> s.withColor(COLOR_HEADER))
                    .append(section.title().copy().styled(s -> s
                    .withColor(COLOR_GRAY_LIGHT)
                            .withClickEvent(new ClickEvent.RunCommand(CMD + " " + modId + " " + section.id()))
                    .withHoverEvent(new HoverEvent.ShowText(Text.literal(tr("open_section"))))));
            send(ctx, line);
        }

        send(ctx, Text.literal(""));
        send(ctx, Text.literal("  ").append(Text.literal("⟳ " + tr("reload_config")).styled(s -> s
            .withColor(COLOR_HEADER)
                .withClickEvent(new ClickEvent.RunCommand(CMD + " reload " + modId))
            .withHoverEvent(new HoverEvent.ShowText(Text.literal(tr("reload_hover")))))));
        send(ctx, Text.literal(""));
        return 1;
    }

    private static int showSection(CommandContext<ServerCommandSource> ctx) {
        String modId = StringArgumentType.getString(ctx, "mod");
        String sectionId = StringArgumentType.getString(ctx, "section");

        ViaPanelProvider provider = requireProvider(ctx, modId);
        if (provider == null) return 0;

        ViaPanelSection section = provider.sections().stream()
                .filter(s -> s.id().equalsIgnoreCase(sectionId))
                .findFirst().orElse(null);

        if (section == null) {
            ctx.getSource().sendError(Text.literal(tr("unknown_section") + ": " + sectionId).styled(s -> s.withColor(COLOR_ERROR)));
            return 0;
        }

        Object config = provider.configInstance();
        if (config == null) {
            ctx.getSource().sendError(Text.literal(tr("config_not_loaded")).styled(s -> s.withColor(COLOR_ERROR)));
            return 0;
        }

        send(ctx, Text.literal(""));
        send(ctx, Text.literal("  ").append(section.title()));
        send(ctx, Text.literal(""));

        for (String fieldName : section.fields()) {
            try {
                Field field = provider.configClass().getField(fieldName);
                Object value = field.get(config);
                MutableText line = Text.literal("  ").append(provider.fieldDisplayName(fieldName)).append(Text.literal(": ").styled(s -> s.withColor(COLOR_GRAY_LIGHT)));

                if (value instanceof Boolean bool) {
                    line.append(Text.literal(bool ? "[ON]" : "[OFF]").styled(s -> s
                            .withColor(bool ? COLOR_OK : COLOR_ERROR)
                            .withClickEvent(new ClickEvent.RunCommand(CMD + " toggle " + modId + " " + fieldName))
                            .withHoverEvent(new HoverEvent.ShowText(buildFieldHover(provider.fieldDescription(fieldName), provider.toggleHintText(), null)))));
                } else if (value instanceof Double d) {
                    line.append(Text.literal(String.valueOf(d)).styled(s -> s
                            .withColor(COLOR_GRAY_LIGHT)
                            .withClickEvent(new ClickEvent.SuggestCommand(CMD + " set " + modId + " " + fieldName + " " + d))
                            .withHoverEvent(new HoverEvent.ShowText(buildFieldHover(provider.fieldDescription(fieldName), provider.editHintText(), null)))));
                } else if (value instanceof Integer i) {
                    line.append(Text.literal(String.valueOf(i)).styled(s -> s
                            .withColor(COLOR_GRAY_LIGHT)
                            .withClickEvent(new ClickEvent.SuggestCommand(CMD + " set " + modId + " " + fieldName + " " + i))
                            .withHoverEvent(new HoverEvent.ShowText(buildFieldHover(provider.fieldDescription(fieldName), provider.editHintText(), null)))));
                } else if (value instanceof String str) {
                    String display = str.length() > 25 ? str.substring(0, 22) + "..." : str;
                    line.append(Text.literal("\"" + display + "\"").styled(s -> s
                            .withColor(COLOR_GRAY_LIGHT)
                            .withClickEvent(new ClickEvent.SuggestCommand(CMD + " set " + modId + " " + fieldName + " " + str))
                            .withHoverEvent(new HoverEvent.ShowText(buildFieldHover(provider.fieldDescription(fieldName), provider.editHintText(), str)))));
                } else {
                    line.append(Text.literal("(" + tr("unsupported") + ")").styled(s -> s.withColor(COLOR_GRAY_DARK)));
                }

                send(ctx, line);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                send(ctx, Text.literal("  " + fieldName + ": ").styled(s -> s.withColor(COLOR_GRAY_DARK))
                        .append(Text.literal("(error)").styled(s -> s.withColor(COLOR_ERROR))));
            }
        }

        send(ctx, Text.literal(""));
        send(ctx, Text.literal("  ◄ " + tr("back")).styled(s -> s
                .withColor(COLOR_HEADER)
                .withClickEvent(new ClickEvent.RunCommand(CMD + " " + modId))
            .withHoverEvent(new HoverEvent.ShowText(Text.literal(tr("back_hover"))))));
        send(ctx, Text.literal(""));

        return 1;
    }

    private static int toggleField(CommandContext<ServerCommandSource> ctx) {
        String modId = StringArgumentType.getString(ctx, "mod");
        String fieldName = StringArgumentType.getString(ctx, "field");

        ViaPanelProvider provider = requireProvider(ctx, modId);
        if (provider == null) return 0;

        Object cfg = provider.configInstance();
        if (cfg == null) {
            ctx.getSource().sendError(Text.literal(tr("config_not_loaded")).styled(s -> s.withColor(COLOR_ERROR)));
            return 0;
        }

        try {
            Field field = provider.configClass().getField(fieldName);
            if (field.getType() != boolean.class) {
                ctx.getSource().sendError(provider.fieldNotBooleanText());
                return 0;
            }

            boolean current = field.getBoolean(cfg);
            field.setBoolean(cfg, !current);

            saveConfig(cfg);
            provider.onFieldUpdated(fieldName, ctx.getSource());

            boolean newVal = !current;
            ctx.getSource().sendFeedback(
                    () -> provider.fieldDisplayName(fieldName).copy()
                            .append(Text.literal(": "))
                        .append(Text.literal(newVal ? "ON" : "OFF").styled(s -> s.withColor(newVal ? COLOR_OK : COLOR_ERROR)))
                            .append(provider.savedSuffixText()),
                    false
            );
            return 1;
        } catch (NoSuchFieldException e) {
            ctx.getSource().sendError(provider.unknownFieldText());
        } catch (IllegalAccessException e) {
            ctx.getSource().sendError(Text.literal(tr("cannot_access_field") + ": " + fieldName).styled(s -> s.withColor(COLOR_ERROR)));
        }

        return 0;
    }

    private static int setField(CommandContext<ServerCommandSource> ctx) {
        String modId = StringArgumentType.getString(ctx, "mod");
        String fieldName = StringArgumentType.getString(ctx, "field");
        String rawValue = StringArgumentType.getString(ctx, "value");

        ViaPanelProvider provider = requireProvider(ctx, modId);
        if (provider == null) return 0;

        Object cfg = provider.configInstance();
        if (cfg == null) {
            ctx.getSource().sendError(Text.literal(tr("config_not_loaded")).styled(s -> s.withColor(COLOR_ERROR)));
            return 0;
        }

        try {
            Field field = provider.configClass().getField(fieldName);
            Class<?> type = field.getType();

            if (type == String.class) {
                field.set(cfg, rawValue);
            } else if (type == double.class) {
                field.setDouble(cfg, Double.parseDouble(rawValue));
            } else if (type == int.class) {
                field.setInt(cfg, Integer.parseInt(rawValue));
            } else if (type == boolean.class) {
                field.setBoolean(cfg, Boolean.parseBoolean(rawValue));
            } else {
                ctx.getSource().sendError(Text.literal(tr("unsupported_field_type")).styled(s -> s.withColor(COLOR_ERROR)));
                return 0;
            }

            saveConfig(cfg);
            provider.onFieldUpdated(fieldName, ctx.getSource());

            if ("defaultLanguage".equals(fieldName)) {
                ViaPanelApi.applyGlobalLanguageToAll(rawValue, ctx.getSource());
            }

            ctx.getSource().sendFeedback(
                    () -> provider.fieldDisplayName(fieldName).copy()
                            .append(Text.literal(" = "))
                        .append(Text.literal(rawValue).styled(s -> s.withColor(COLOR_GRAY_LIGHT)))
                            .append(provider.savedSuffixText()),
                    false);
            return 1;
        } catch (NoSuchFieldException e) {
            ctx.getSource().sendError(provider.unknownFieldText());
        } catch (NumberFormatException e) {
            ctx.getSource().sendError(provider.invalidNumberText());
        } catch (IllegalAccessException e) {
            ctx.getSource().sendError(Text.literal(tr("cannot_access_field") + ": " + fieldName).styled(s -> s.withColor(COLOR_ERROR)));
        }

        return 0;
    }

    private static int reloadModConfig(CommandContext<ServerCommandSource> ctx) {
        String modId = StringArgumentType.getString(ctx, "mod");

        ViaPanelProvider provider = requireProvider(ctx, modId);
        if (provider == null) return 0;

        provider.reload(ctx.getSource());
        ctx.getSource().sendFeedback(provider::reloadDoneText, false);
        return 1;
    }

    private static int setGlobalLanguage(CommandContext<ServerCommandSource> ctx) {
        String code = StringArgumentType.getString(ctx, "code").trim().toLowerCase();
        if (!"ru".equals(code) && !"en".equals(code)) {
            ctx.getSource().sendError(Text.literal(tr("invalid_lang_code")).styled(s -> s.withColor(COLOR_ERROR)));
            return 0;
        }

        ViaPanelApi.applyGlobalLanguageToAll(code, ctx.getSource());
        ctx.getSource().sendFeedback(
                () -> Text.literal(tr("lang_applied") + ": " + code).styled(s -> s.withColor(COLOR_GRAY_LIGHT)),
                false
        );
        return 1;
    }

    private static MutableText buildFieldHover(Text description, Text action, String rawValue) {
        MutableText hover = description.copy();
        hover.append(Text.literal("\n"));
        hover.append(action.copy());
        if (rawValue != null && !rawValue.isBlank()) {
            hover.append(Text.literal("\n" + rawValue).styled(s -> s.withColor(COLOR_GRAY_DARK)));
        }
        return hover;
    }

    private static ViaPanelProvider requireProvider(CommandContext<ServerCommandSource> ctx, String modId) {
        ViaPanelProvider provider = ViaPanelApi.getProvider(modId);
        if (provider == null) {
            ctx.getSource().sendError(Text.literal(tr("mod_no_api") + ": " + modId).styled(s -> s.withColor(COLOR_ERROR)));
            return null;
        }
        if (!provider.hasPermission(ctx.getSource())) {
            ctx.getSource().sendError(Text.literal(tr("no_permission_panel") + ": " + modId).styled(s -> s.withColor(COLOR_ERROR)));
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
            case "mod_no_api" -> ru ? "Мод не предоставляет viaPanel API" : "Mod does not expose viaPanel API";
            case "no_permission_panel" -> ru ? "Нет прав для панели" : "No permission for panel";
            case "invalid_lang_code" -> ru ? "Неверный код языка. Используй: ru или en." : "Invalid language code. Use: ru or en.";
            case "lang_applied" -> ru ? "Глобальный язык применён" : "Global language applied";
            default -> key;
        };
    }

    private static void saveConfig(Object config) {
        try {
            config.getClass().getMethod("save").invoke(config);
        } catch (Exception ignored) {
        }
    }

    private static void send(CommandContext<ServerCommandSource> ctx, Text text) {
        ctx.getSource().sendFeedback(() -> text, false);
    }
}
