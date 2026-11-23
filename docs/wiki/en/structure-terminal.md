# Advanced Structure Terminal

[Wiki contents](../README.en.md) · [Project home](../../../README.en.md) · [简体中文](../zh/structure-terminal.md)

> [User guide](../README.en.md) / [Building and machine controls](../README.en.md#building-and-machine-controls)

**On this page**

- [Getting the terminal](#getting-the-terminal)
- [Controls and settings](#controls-and-settings)
- [Coils and repeat count](#coils-and-repeat-count)
- [Glass selection](#glass-selection)
- [Theme and closing the screen](#theme-and-closing-the-screen)
- [Build, swap and remove](#build-swap-and-remove)
- [Materials and compatibility](#materials-and-compatibility)

Project, complete, modify and dismantle multiblocks with one tool, reducing manual block placement and rework.

## Getting the terminal

Find **Advanced Structure Terminal** in NEI. Craft it shapelessly from a **Multiblock Structure Hologram Projector + Ender Pearl + Book**.

## Controls and settings

1. **Right-click a controller to project; sneak-right-click it to execute Build, Swap or Remove.**
2. **Sneak-right-click air to open terminal settings.** Rounded controls include coil and glass selectors, repeat count, exclusive Build/Swap/Remove modes, and boolean checkboxes. The triangle opens a tier-sorted item list using NEI item rendering and full tooltips, including coil heat capacity and modifier-key details, scrolling and page buttons; select one material per group. Closing the screen saves the settings.

   Five settings pages cover controls, casings/pipes, energy/fields, special parts and size/helpers. Use the bottom arrows or Page Up/Page Down. Hover settings or candidate items to see the raw channel name. New material selectors keep native projector values; Auto restores native defaults. Different machines may interpret the same value as different materials, especially `casing`. Length accepts 1–99 and boiler width 1–3; 0 means Auto, and valid combinations remain machine-specific. No `hatch`/`height` selectors are added; the existing no-hatch toggle remains.

## Coils and repeat count

The Auto coil option retains an existing coil ring or chooses the lowest carried tier; AE supply can also provide coils when enabled. Repeat count accepts `0–99`; `0` uses the native minimum. Exact dimensions follow the GTNH machine definition.

## Glass selection

Glass selection applies to structures using the native GTNH `glass` channel. Dedicated glass and fixed materials keep their machine rules. The first list has one representative per voltage tier; its submenu selects a variant. Per-tier Auto prefers installed glass, then backpack/AE stock of that tier. The top-level Auto restores the native default.

## Theme and closing the screen

The top-right checkbox switches dark mode, saved locally. Press the inventory key (E by default) or Esc to close and save.

## Build, swap and remove

Build fills empty positions; Swap replaces existing ordinary structure blocks; Remove recovers them. Keep hatches is enabled by default; disable it to dismantle structure hatches as well, retaining only the controller. Contents follow native dismantling rules without extra item/fluid packaging; native item drops follow the demolition return route. Swap always retains hatches. Swap also offers a Coils only checkbox. No-hatch mode defaults to on and suppresses optional hatches, while exclusive hatch-only positions retain native requirements. Coil replacement returns the old coils and consumes replacement materials.
**AE demolition returns** go only to the network linked by a carried wireless terminal, with range, power and insertion permission required. Blocks remain when the destination cannot accept them; there is no backpack or ground fallback. If an actual insertion is rejected after removal, the terminal retains the remainder and displays a warning. Restore access and use it on a controller again to retry the pending return first. Without AE mode, recovery uses backpack space and skips blocks when full.

## Materials and compatibility

A construction pass executes in one server tick, skips missing materials and continues with other positions. Existing ordinary blocks and hatches are retained. Replenish materials and click again to finish. Configured scan and build limits still apply and report an interrupted pass explicitly.

Machines must expose a readable StructureLib definition. Normal building skips formed GT machines; projection does not force structure checks or stop them. Replacing coils or changing a structure can stop a running machine. AE supply requires a linked, powered wireless terminal within range and extraction permission.

---

Related guides: [ME Cell Aggregation Storage](storage.md) · [BuildCraft work range](buildcraft-range.md)
