# ME Packing Tape

[Wiki contents](../README.en.md) · [Project home](../../../README.en.md) · [简体中文](../zh/packing-tape.md)

> [User guide](../README.en.md) / [AE networks and automation](../README.en.md#ae-networks-and-automation)

Move interfaces or drives while retaining their patterns, cells and native settings.

## Getting and using tape

Find **ME Packing Tape** in NEI. It uses the unchanged ExtendedAE tape and package textures used by GTO, lasts 64 uses, and follows GTO's fluix dust / duct tape / slimeball / ender pearl dust recipe.

Sneak-right-click a device to drop a **Packaged Device**, then right-click with that package to restore it. Supported defaults: normal and dual ME interfaces (block and part forms), plus ME drives. Packing a part leaves the cable and its other parts intact. Native patterns, upgrades, buffered items/fluids and persistent settings are retained; drives retain their cells. Moving disconnects the device temporarily, so ongoing crafting jobs are not guaranteed to survive.

## Permissions and device whitelist

Blocked or unauthorized placement keeps the package. No optimizer component is required. Both client and server need this mod. The device whitelist is `packingtape.deviceWhitelist` in `config/structureterminal.cfg`, using `modid:item@metadata` entries; additional devices require validation.

---

Related guides: [Integrated Pattern](integrated-patterns.md)
