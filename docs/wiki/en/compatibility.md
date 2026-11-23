# Compatibility and configuration

[Wiki contents](../README.en.md) · [Project home](../../../README.en.md) · [简体中文](../zh/compatibility.md)

> [User guide](../README.en.md) / [Compatibility and configuration](../README.en.md#compatibility-and-configuration)

## Versions and saves

- Target: **GTNH 2.9.1**. Use an adapted build for other pack versions.
- The approved original GTO terminal icon is retained. Legacy internal module IDs preserve existing saves; all features ship in one JAR.
- Terminal settings remain in `config/structureterminal.cfg`; restart after editing. Vault files remain in `<world>/data/aestoragevault/`.
- The vault supports Structure Terminal construction and NEI multiblock previews. Preview size 1/2/3 shows storage link, active export, or physical cell-return bus examples. Actual port positions remain flexible.
- Back up, migrate and restore the complete stopped world. Do not roll back only inventory files or controller NBT. Removing this mod makes its added blocks and inventories unavailable.

## Configuration files

| File | Purpose |
| --- | --- |
| `config/structureterminal.cfg` | Structure Terminal, packing whitelist and portal-scene options |
| `config/gtnh-modernization.cfg` | Vault read-cache entry limits and byte budgets |
| `config/gtnh-modernization-client.cfg` | Local terminal theme and vault animation preferences |
| `config/gtnh-modernization-updates.cfg` | Update checks, pack detection and download settings |

Legacy filenames are retained so renaming the project preserves existing settings. See each feature’s instructions for when changes take effect.

---

Related guides: [Installation and upgrading](installation.md) · [ME Cell Aggregation Storage](storage.md)
