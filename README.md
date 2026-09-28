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

# Network identity of this server, shared by all via mods (viaStyle, viaLogium).
# server_id must match the server name in velocity.toml [servers]. Empty = server folder name.
server_id = ""

# Name shown to players (chat prefix, panel header). Empty = server_id.
server_display_name = ""
```

### Server networks

On a Velocity network every backend runs its own viaPanel. Set `server_id` to the backend's name in `velocity.toml` (or leave it empty when the server folder already has that name) and `server_display_name` to what players should see, e.g. `Арракис`. Other mods read it through `ViaPanelApi.getServerId()` and `ViaPanelApi.getServerDisplayName()`.

## Build

```sh
./gradlew clean build
```

Requires Java 21+ and a JDK 25 toolchain.

## Dependencies

- Minecraft 26.3
- Fabric Loader >=0.19.5
- Fabric API (any version for 26.3)
- LuckPerms (optional)

## License

MIT
