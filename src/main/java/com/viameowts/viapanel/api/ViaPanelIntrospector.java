package com.viameowts.viapanel.api;

import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scans config classes for {@link ViaPanelField} annotations and caches the
 * resulting metadata. Also provides parsing/validation of raw string values
 * typed by players into {@code /viapanel set}.
 */
public final class ViaPanelIntrospector {

    /** Immutable metadata for one annotated field. */
    public record FieldMeta(String key, Class<?> type, Text name, Text nameRu, Text desc, Text descRu,
                            String sectionId, double min, double max, int order, boolean secret, Field field) {

        public boolean bounded() {
            return !Double.isNaN(min) || !Double.isNaN(max);
        }

        /** Name resolved for the given global language code ("ru"/"en"). */
        public Text nameFor(String lang) {
            Text ru = nameRu();
            if ("ru".equalsIgnoreCase(lang) && ru != null && !ru.getString().isBlank()) {
                return ru;
            }
            return name();
        }

        /** Description resolved for the given global language code ("ru"/"en"). */
        public Text descFor(String lang) {
            Text ru = descRu();
            if ("ru".equalsIgnoreCase(lang) && ru != null && !ru.getString().isBlank()) {
                return ru;
            }
            return desc();
        }
    }

    /** Immutable metadata for one section (ordered fields). */
    public record SectionMeta(String id, Text title, List<FieldMeta> fields) {
    }

    private static final Map<Class<?>, List<FieldMeta>> CACHE = new ConcurrentHashMap<>();

    private ViaPanelIntrospector() {
    }

    /** True if the class has at least one {@link ViaPanelField} instance field. */
    public static boolean isAnnotated(Class<?> configClass) {
        return configClass != null && !fields(configClass).isEmpty();
    }

    /** All annotated fields in declaration order. Empty list for legacy configs. */
    public static List<FieldMeta> fields(Class<?> configClass) {
        if (configClass == null) {
            return List.of();
        }
        return CACHE.computeIfAbsent(configClass, ViaPanelIntrospector::scan);
    }

    /** Metadata for one field key, or null. */
    public static FieldMeta field(Class<?> configClass, String key) {
        for (FieldMeta meta : fields(configClass)) {
            if (meta.key().equals(key)) {
                return meta;
            }
        }
        return null;
    }

    /** Sections ordered by first field appearance; fields sorted by order(), then declaration. */
    public static List<SectionMeta> sections(Class<?> configClass) {
        Map<String, List<FieldMeta>> grouped = new LinkedHashMap<>();
        List<FieldMeta> all = new ArrayList<>(fields(configClass));
        for (FieldMeta meta : all) {
            grouped.computeIfAbsent(meta.sectionId(), k -> new ArrayList<>()).add(meta);
        }
        List<SectionMeta> out = new ArrayList<>(grouped.size());
        grouped.forEach((id, list) -> {
            list.sort(Comparator.comparingInt(FieldMeta::order));
            out.add(new SectionMeta(id, Text.literal(titleize(id)), List.copyOf(list)));
        });
        return List.copyOf(out);
    }

    /**
     * Parses {@code raw}, validates it against the field metadata and writes it into
     * {@code config}. Returns null on success or a localized error text.
     */
    public static Text applyValue(Object config, FieldMeta meta, String raw) {
        Class<?> t = meta.type();
        try {
            Field f = meta.field();
            if (t == boolean.class) {
                Boolean v = parseBoolean(raw);
                if (v == null) {
                    return invalidBoolean();
                }
                f.setBoolean(config, v);
            } else if (t == int.class) {
                int v = Integer.parseInt(raw.trim());
                if (meta.bounded() && (v < meta.min() || v > meta.max())) {
                    return outOfRange(meta, v);
                }
                f.setInt(config, v);
            } else if (t == long.class) {
                long v = Long.parseLong(raw.trim());
                if (meta.bounded() && (v < meta.min() || v > meta.max())) {
                    return outOfRange(meta, v);
                }
                f.setLong(config, v);
            } else if (t == float.class) {
                float v = Float.parseFloat(raw.trim());
                if (meta.bounded() && (v < meta.min() || v > meta.max())) {
                    return outOfRange(meta, v);
                }
                f.setFloat(config, v);
            } else if (t == double.class) {
                double v = Double.parseDouble(raw.trim());
                if (!Double.isFinite(v)) {
                    return invalidNumber();
                }
                if (meta.bounded() && (v < meta.min() || v > meta.max())) {
                    return outOfRange(meta, v);
                }
                f.setDouble(config, v);
            } else if (t == String.class) {
                f.set(config, raw);
            } else if (t.isEnum()) {
                Object match = matchEnum(t, raw);
                if (match == null) {
                    return invalidEnum(t);
                }
                f.set(config, match);
            } else {
                return unsupportedType();
            }
            return null;
        } catch (NumberFormatException e) {
            return t.isEnum() ? invalidEnum(t) : invalidNumber();
        } catch (IllegalAccessException e) {
            return Text.literal("Cannot access field: " + meta.key());
        }
    }

    /** Current value formatted for display; secret values are masked. */
    public static String displayValue(Object config, FieldMeta meta)
            throws IllegalAccessException {
        if (meta.secret()) {
            return "***";
        }
        Object v = meta.field().get(config);
        if (v instanceof Double d) {
            return formatNumber(d);
        }
        if (v instanceof Float f) {
            return formatNumber(f);
        }
        return String.valueOf(v);
    }

    /** Raw value for command suggestions; secret values are omitted. */
    public static String suggestValue(Object config, FieldMeta meta)
            throws IllegalAccessException {
        return meta.secret() ? "" : displayValue(config, meta);
    }

    /** Next enum constant in declaration order (cycles). Null for non-enums or on error. */
    public static String nextEnumValue(Object config, FieldMeta meta) {
        Class<?> t = meta.type();
        if (!t.isEnum()) {
            return null;
        }
        try {
            Object current = meta.field().get(config);
            Object[] constants = t.getEnumConstants();
            for (int i = 0; i < constants.length; i++) {
                if (constants[i].equals(current)) {
                    return ((Enum<?>) constants[(i + 1) % constants.length]).name();
                }
            }
            return ((Enum<?>) constants[0]).name();
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public static String formatNumber(double d) {
        if (d == Math.rint(d) && !Double.isInfinite(d)) {
            return String.valueOf((long) d);
        }
        return String.valueOf(d);
    }

    static String titleize(String sectionId) {
        String[] parts = sectionId.split("[_\\s]+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.isEmpty() ? sectionId : sb.toString();
    }

    private static List<FieldMeta> scan(Class<?> cfg) {
        List<FieldMeta> out = new ArrayList<>();
        for (Class<?> c = cfg; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                ViaPanelField ann = f.getAnnotation(ViaPanelField.class);
                if (ann == null) {
                    continue;
                }
                int mods = f.getModifiers();
                if (Modifier.isStatic(mods) || Modifier.isFinal(mods)) {
                    continue;
                }
                if (!isSupportedType(f.getType())) {
                    continue;
                }
                String key = f.getName();
                Text name = ann.value().isBlank()
                        ? humanize(key)
                        : Text.literal(ann.value());
                Text nameRu = ann.valueRu().isBlank() ? null : Text.literal(ann.valueRu());
                Text desc = Text.literal(ann.desc());
                Text descRu = ann.descRu().isBlank() ? null : Text.literal(ann.descRu());
                out.add(new FieldMeta(key, f.getType(), name, nameRu, desc, descRu, ann.section(),
                        ann.min(), ann.max(), ann.order(), ann.secret(), f));
            }
        }
        return List.copyOf(out);
    }

    private static boolean isSupportedType(Class<?> t) {
        return t == boolean.class || t == int.class || t == long.class || t == float.class
                || t == double.class || t == String.class || t.isEnum();
    }

    private static Text humanize(String key) {
        StringBuilder sb = new StringBuilder();
        for (String w : key.split("(?<=[a-z0-9])(?=[A-Z])|[_\\s]+")) {
            if (w.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toLowerCase(w.charAt(0))).append(w.substring(1));
        }
        return Text.literal(sb.isEmpty() ? key : sb.toString());
    }

    private static Boolean parseBoolean(String raw) {
        return switch (raw.trim().toLowerCase()) {
            case "true", "on", "yes", "1", "да", "вкл" -> Boolean.TRUE;
            case "false", "off", "no", "0", "нет", "выкл" -> Boolean.FALSE;
            default -> null;
        };
    }

    private static Object matchEnum(Class<?> enumClass, String raw) {
        for (Object c : enumClass.getEnumConstants()) {
            if (((Enum<?>) c).name().equalsIgnoreCase(raw.trim())) {
                return c;
            }
        }
        return null;
    }

    private static Text outOfRange(FieldMeta meta, double attempted) {
        double lo = Double.isNaN(meta.min()) ? Double.NEGATIVE_INFINITY : meta.min();
        double hi = Double.isNaN(meta.max()) ? Double.POSITIVE_INFINITY : meta.max();
        String range = (Double.isNaN(meta.min()) ? "-inf" : formatNumber(lo))
                + " .. " + (Double.isNaN(meta.max()) ? "+inf" : formatNumber(hi));
        return Text.literal("Value " + formatNumber(attempted) + " is out of range [" + range + "]");
    }

    private static Text invalidNumber() {
        return Text.literal("Not a valid number.");
    }

    private static Text invalidBoolean() {
        return Text.literal("Not a valid boolean. Use true/false.");
    }

    private static Text invalidEnum(Class<?> enumClass) {
        StringBuilder sb = new StringBuilder("Unknown value. Options: ");
        Object[] constants = enumClass.getEnumConstants();
        for (int i = 0; i < constants.length; i++) {
            if (i > 0) {
                sb.append(" / ");
            }
            sb.append(((Enum<?>) constants[i]).name());
        }
        return Text.literal(sb.toString());
    }

    private static Text unsupportedType() {
        return Text.literal("Unsupported field type.");
    }
}
