# viaPanel

Server-side Fabric mod providing a shared chat-based configuration panel for via ecosystem mods.

## Directory overview
- `src/main/java/com/viameowts/viapanel/` — core mod source
  - `ViaPanelMod.java` — Fabric server initializer
  - `ViaPanelConfig.java` — TOML config loader/saver
  - `ViaPanelPermissionHelper.java` — LuckPerms integration via reflection
  - `command/ViaPanelCommand.java` — `/viapanel` command tree (Brigadier)
  - `api/` — public API for other mods
- `src/main/resources/` — `fabric.mod.json`
- `build.gradle`, `settings.gradle`, `gradle.properties` — Gradle build

## Key files
- `src/main/java/com/viameowts/viapanel/ViaPanelMod.java:15` — entry point
- `src/main/java/com/viameowts/viapanel/command/ViaPanelCommand.java:36` — command registration
- `src/main/java/com/viameowts/viapanel/api/ViaPanelApi.java:15` — provider registry
