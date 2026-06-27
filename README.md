# viaPanel

`viaPanel` is a standalone server-side Fabric mod that provides a shared chat-based configuration panel for multiple mods. Other mods register `ViaPanelProvider` implementations, and viaPanel auto-discovers them at runtime, rendering a browsable, interactive config UI entirely through Minecraft chat.

## Features

- `/viapanel` — browse all installed mods, open per-mod config pages
- `toggle`, `set`, `reload` actions on config fields via clickable chat messages
- Per-mod permission control — each provider decides who can view/edit its panel
- Global language switch (`/viapanel lang ru|en`) — applied to all providers at once
- Optional LuckPerms integration for fine-grained permission checks
- Dual language UI (English / Russian) with per-provider `applyGlobalLanguage()` support

## API for mod developers

Other mods integrate by implementing `ViaPanelProvider` and registering via `ViaPanelApi.register()`. See [`VIAPANEL_API.md`](VIAPANEL_API.md) for the full API reference.

Quick start:

```java
public class MyMod implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        Config = MyConfig.load();
        ViaPanelApi.register(new MyPanelProvider());
    }
}
```

## Command reference

| Command | Description |
|---|---|
| `/viapanel` | List all installed mods with indicators |
| `/viapanel <mod>` | Open a mod's panel showing its sections |
| `/viapanel <mod> <section>` | View and edit config fields in a section |
| `/viapanel toggle <mod> <field>` | Toggle a boolean field |
| `/viapanel set <mod> <field> <value>` | Set a field (supports String, int, double, boolean) |
| `/viapanel reload <mod>` | Reload a mod's config from disk |
| `/viapanel lang <ru\|en>` | Switch global language for all providers |

## Permissions

- Each `ViaPanelProvider` controls access to its own panel via `hasPermission()`.
- `/viapanel lang` respects a configurable permission node (`global_language_permission`) with OP level fallback (`global_language_op_level`).

## Configuration

Config file: `config/viaPanel/viaPanel.toml`

```toml
# Permission node checked for /viapanel lang <ru|en> (when LuckPerms is present)
global_language_permission = "viapanel.command.lang"

# Vanilla OP fallback level for /viapanel lang (0..4)
global_language_op_level = 3
```

## Build

```sh
./gradlew clean build
```

Requires Java 21+ and a JDK 25 toolchain.

## Dependencies

- Minecraft 1.21.11
- Fabric Loader >=0.18.4
- Fabric API (any version for 1.21.11)
- LuckPerms (optional)

## License

MIT
