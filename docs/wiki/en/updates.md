# Client updates

[Wiki contents](../README.en.md) · [Project home](../../../README.en.md) · [简体中文](../zh/updates.md)

> [User guide](../README.en.md) / [Installation and updates](../README.en.md#installation-and-updates)

**On this page**

- [Checking for updates](#checking-for-updates)
- [Matching a server](#matching-a-server)
- [Downloading and installing](#downloading-and-installing)
- [Configuration and pack detection](#configuration-and-pack-detection)

## Checking for updates

Use **Check mod updates** at the top right of GTNH’s main menu, including when startup checks are off. Each launch checks once in the background. Network failures do not block play. The update screen offers download, cancellation, later and skip-version actions, plus switches for startup checks, prereleases and automatic server-version downloads.

## Matching a server

On a version-mismatch disconnect, the updater queries the server's public Forge status and looks for **that exact server version**. Disabled status queries, network failures or unavailable releases leave the ordinary connection error in place; compatibility checks are never relaxed. Server auto-download is optional, and installation still requires confirmation.

## Downloading and installing

After verification, choose **Exit and install**. A separate helper waits for the game process to exit, backs up the old JAR and atomically replaces it. Restart using your usual launcher after installation finishes. There is no hot reload or automatic launcher restart. The installed filename is preserved; the Mods screen reports the actual version. Backups, logs and results are in `.modernization-updates/update-…/` inside the instance.

## Configuration and pack detection

Settings are in `config/gtnh-modernization-updates.cfg`. The pack version is read from GTNH's main-menu resources. If detection fails, set an exact `gtnhVersionOverride`; unknown versions are never assumed compatible. Only matching complete JARs from this project's GitHub Releases are eligible. Existing old clients must manually install an updater-enabled version once.

---

Related guides: [Installation and upgrading](installation.md) · [Compatibility and configuration](compatibility.md)
