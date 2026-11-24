# Installation and upgrading

[Wiki contents](../README.en.md) · [Project home](../../../README.en.md) · [简体中文](../zh/installation.md)

> [User guide](../README.en.md) / [Installation and updates](../README.en.md#installation-and-updates)

## Download and version

Current version: **0.4.2**, for **GTNH 2.9.1**. [Download JAR](https://github.com/arturia-Xayah/gtnh-hn/releases/download/v0.4.2-GTNH-2.9.1/gtnh-hn-0.4.2-GTNH-2.9.1.jar) · [Changelog](../../../CHANGELOG.md).

## Installation and upgrades

The former GTNH Modernization Upgrade is now GTNH-HN. Install a renamed build manually once to move an old updater to the new repository. Historical Releases retain their original filenames.

1. Install `gtnh-hn-0.4.2-GTNH-2.9.1.jar` in `mods`. Multiplayer clients and servers need the same version; install on the client for single-player.
2. Remove older GTNH Modernization Upgrade / Structure Terminal JARs and standalone **AE Order Optimizer, Plasma Forge Power and AE Storage Vault** JARs. Their features are included; installing both creates duplicate modules.
3. Back up the complete world first. Terminal, AE component and power-setting registration IDs are retained. **0.3.0 changes the vault inventory format and cannot directly open 0.2.x vault files**. Export existing stock using the old version, then create a new vault and supply capacity sources after upgrading. Do not delete old files to clear a lock.

Dependencies are supplied by the target pack: GregTech, StructureLib, GTNHLib, AE2, ModularUI2 and UniMixins. The former standalone mods are not required.

---

Related guides: [Client updates](updates.md) · [Compatibility and configuration](compatibility.md)
