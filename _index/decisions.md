# Architecture Decisions

## 2026-06-27 — Provider registration pattern
**Context**: Other mods need to expose config sections to viaPanel.
**Decision**: Registry pattern via `ViaPanelApi` with `ViaPanelProvider` interface.
**Consequences**: Mods call `ViaPanelApi.register()` in their init; viaPanel auto-discovers providers.

## 2026-06-27 — Reflection-based config access
**Context**: Config fields are edited generically without per-mod serialisation code.
**Decision**: Providers expose `configClass()` and `configInstance()`; viaPanel reads/writes fields via `java.lang.reflect.Field`.
**Consequences**: Config classes must have `public` fields and a `save()` method. Supported types: `boolean`, `int`, `double`, `String`.

## 2026-06-27 — LuckPerms via reflection
**Context**: Avoid hard compile-time dependency on LuckPerms.
**Decision**: All LP API calls use `Class.forName()` + `Method.invoke()` at runtime.
**Consequences**: LP is optional; no linkage errors if absent; minor perf overhead on permission checks.
