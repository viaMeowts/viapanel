# viaPanel

`viaPanel` is a standalone server-side Fabric mod that provides a shared chat-based config menu for multiple mods.

## What it does

- Adds `/viapanel`
- Auto-discovers all installed mods
- Opens per-mod config pages for mods that register `ViaPanelProvider`
- Supports clickable `toggle`, `set`, and `reload` actions

## API docs

See the workspace-level API reference:

- `VIAPANEL_API.md`

## Command examples

- `/viapanel`
- `/viapanel viastyle`
- `/viapanel viastyle chat`
- `/viapanel toggle viastyle showTimestamp`
- `/viapanel set viastyle timestampFormat HH:mm:ss`
- `/viapanel reload viastyle`
