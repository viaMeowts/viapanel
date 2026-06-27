# Changelog

## [2.7.2] - 2026-06-27

### Added
- `ViaPanelConfig` — TOML-based config loader/saver for viaPanel settings (`src/main/java/com/viameowts/viapanel/ViaPanelConfig.java`)
- `ViaPanelPermissionHelper` — LuckPerms integration via reflection with OP level fallback (`src/main/java/com/viameowts/viapanel/ViaPanelPermissionHelper.java`)
- Global language switcher in main `/viapanel` menu with `[EN]` / `[RU]` clickable buttons
- Permission-gated `/viapanel lang` command with configurable permission node (`global_language_permission`) and OP fallback level (`global_language_op_level`)
- Four new translation keys for global language UI: `global_language_label`, `global_language_locked`, `global_language_click_hint`, `global_language_permission_hint`
- `hasGlobalAdminPermission()` helper to gate global language changes

### Changed
- Color palette migrated from viaStyle palette to viaPanel palette:
  - Header/accent: `#FBD06A` -> `#FFC64C`
  - Success/ON: `#0BDA51` -> `#98FB98`
  - Error/OFF: `#FF2C2C` -> `#FF5555`
  - Secondary dark: `#A89FA4` -> `#B0C4DE`
  - Main text: `#D9D0D5` (unchanged)
- `defaultLanguage` field update now requires global admin permission (`hasGlobalAdminPermission`) before broadcasting language change to all providers
- README fully rewritten with feature overview, API quickstart, command reference table, permissions section, config documentation, and build instructions
- `.gitignore` expanded from 3 entries to comprehensive set (Gradle, IntelliJ, VS Code, Eclipse, macOS, Loom cache, temp files)
- Version bumped from `2.6.8` to `2.7.2`

### Removed
- `VIAPANEL_API.md` — API reference is now documented inline in the README

### Security
- `/viapanel lang` command now enforces permission check before execution
- Language change via `defaultLanguage` field setting requires global admin permission
