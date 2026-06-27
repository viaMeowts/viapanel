# References

## Entry points
- `src/main/java/com/viameowts/viapanel/ViaPanelMod.java:15` — `onInitializeServer()`
- `src/main/java/com/viameowts/viapanel/command/ViaPanelCommand.java:36` — `register()`

## Key patterns
- `ViaPanelProvider` interface in `api/` — mods implement this to expose config
- `ViaPanelApi` static registry — thread-safe `ConcurrentHashMap`
- `ViaPanelSection` record — groups config fields under a named section
- `ViaPanelPermissionHelper` — LP detection + OP fallback
- `ViaPanelConfig` — hand-rolled TOML parser (no library)
