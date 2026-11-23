# ME Cell Aggregation Storage

[Wiki contents](../README.en.md) · [Project home](../../../README.en.md) · [简体中文](../zh/storage.md)

> [User guide](../README.en.md) / [ME storage](../README.en.md#me-storage)

**On this page**

- [Structure](#structure)
- [Appearance](#appearance)
- [Casing recipe](#casing-recipe)
- [Importing cells and components](#importing-cells-and-components)
- [Capacity contributions](#capacity-contributions)
- [Returning capacity sources](#returning-capacity-sources)
- [Controller interface](#controller-interface)
- [Exporting to AE](#exporting-to-ae)
- [Finding accepted output types](#finding-accepted-output-types)
- [Shared capacity](#shared-capacity)
- [Exact quantities and display units](#exact-quantities-and-display-units)
- [AE Network Tool](#ae-network-tool)
- [Read-cache configuration](#read-cache-configuration)
- [Moving and backups](#moving-and-backups)

Pool storage-cell capacity in one multiblock and manage items and fluids together.

## Structure

Build a hollow 3×3×3 shell: outward-facing controller at the center of any face, 23 ME Resonance Casings, one ordinary GT input bus and one Storage Link Hatch or Export Bus, air in the center. The bus and interface may occupy any remaining shell cell, including top/bottom, edges and corners. Neither may be shared between vaults; conflicts prevent both structures from forming and recover after separation. Buses reserved by a registered native GT multiblock are rejected too. No cores. The controller and both ME ports use LuV assembler recipes (30,720 EU/t), with LuV hulls/circuits and components; there is no workbench fallback. Connect the interface to powered AE: one channel, 2 AE/t. ME virtual buses are excluded. Structure Terminal building and NEI multiblock previews are supported. Preview size 1/2/3 shows the link, active-export and cell-return configurations.

## Appearance

Unformed casings share the controller's white/cyan/violet material. The casing has no inventory, AE node or ticking tile entity.

Forming replaces the whole shell appearance with a smooth blue/white/violet skin. Each complete face shares one continuous texture without repeated block borders. Native screen and ME/pipe port overlays remain visible, with resource-pack support. Controller and connector/port indicators stay visible; native blocks, bus tier, dye and saved texture are never rewritten. After upgrading from 0.4.0, replace the native LuV shell with ME Resonance Casings and recheck the structure. Keep the original controller; its capacity and inventory binding are retained. The inventory-format upgrade limitation above still applies.

## Casing recipe

Craft **1 ME Resonance Casing** with this shaped workbench recipe:

| ME Drive | LuV Machine Casing | ME Drive |
| --- | --- | --- |
| LuV Machine Casing | Superconducting Coil Block | LuV Machine Casing |
| ME Drive | LuV Machine Casing | ME Drive |

Use Machine Casings, not LuV Machine Hulls.

## Importing cells and components

Insert supported item/fluid cells or AE2 1k–16384k components. Ordinary filled cells migrate their contents and upgrade cards first, then contribute nominal bytes; housing is consumed. Cards become stored items. Cell partitions/filters are not copied into vault rules. Unsupported, creative, void and spatial sources remain intact. Right-click the controller for status and absorption controls. Each import batch forces a dimension save and may pause the server briefly.

0.3.0 only accepts empty Singularity/Universe cells. Singularity cells credit `(nominal bytes + 1) / 32768`: 16 TiB for item cells and 64 GiB for fluid cells. Universe cells contribute their full nominal bytes: 512 PiB minus one byte for items and 4 PiB minus one byte for fluids. Nonempty high-end cells remain intact; upgrade cards still migrate as ordinary items. The empty Avaritiaddons `InfinityChest` contributes 2,147,483,646 B; chests carrying any NBT other than the import token are retained. Avaritia `Infinity_Chest` is armor, not a supported chest.

## Capacity contributions

Open **Cell details** on the controller for six-row pages of source item icons, absorbed counts, cumulative credited bytes and percentage of total capacity. Counts and credited bytes use BigInteger. Registry item + metadata defines a source type; original cell inventory/filter NBT is not copied into its icon. Contribution history survives restarts and normal relocation. The blue-white native detail page includes capacity-share bars, complete names in hover details and a compact layout for smaller GUI scales. History stores bytes actually credited at absorption time; changing rules does not retroactively recalculate old records.

## Returning capacity sources

Optionally replace one of the 23 casings with **one ordinary GT item output bus**. This physical return bus is separate from the required Storage Link Hatch / ME Export Bus choice. It must not be shared with another machine. Open Cell details, click a source row, enter an integer quantity or use **All**, then confirm. All fills the remaining absorbed count; actual delivery is limited by free capacity and physical output room. A partial return reports the delivered count. Returned sources are empty, while stock and migrated upgrade cards stay in the vault. Contribution count and capacity decrease only by the actual return. A full or differently locked output bus makes no debit. The original cell's partition/filter/upgrade configuration is not reconstructed.

Returns use durable debit intents and native-bus delivery receipts for restart recovery. Pending delivery pauses stock IO/imports; retain the original output bus in its original position for recovery. Imports and returns force a dimension save and may briefly pause the server. Complete-world backups remain required.

## Controller interface

The controller is named **ME Cell Aggregation Storage**. Its blue-white dashboard animates the ring, sweep, highlights and number changes, with a compact layout for smaller GUI sizes. The animation toggle is client-local (`config/gtnh-modernization-client.cfg`, `client.storageAnimations`), adds no server packets, and snaps to current values when disabled. Hover metrics for non-interpolated current values (very long numbers are summarized). Invalid/locked structures stop decorative motion. Use exactly one Storage Link Hatch **or** Export Bus in the port position. Right-click the link for the native AE2 priority GUI; native item/fluid routing and Storage Reshuffle are supported. Larger priorities insert first, smaller priorities extract first, following AE2's other ordering rules.

![Dashboard animation illustration (headless shared-geometry render)](../../assets/vault/dashboard/animation-en.gif)

## Exporting to AE

The Export Bus instead actively injects into its connected, powered AE grid and never exposes the source stock as network storage. Its 4×9 native phantom slots accept item/fluid NEI markers. Empty filters export all. Supported upgrades: one fuzzy, inverter and ore-filter card each. Non-empty ore expressions override slot markers; an empty expression restores slot filtering. Empty markers plus an empty expression export all, including with an inverter. Transfers use native AE power and security, bounded type batches and fair scanning. Full/offline destinations retain stock. The absorption toggle controls cell imports only. Normal harvesting preserves filters, real upgrade cards and settings.

## Finding accepted output types

Unfiltered output eventually reaches destination-accepted types despite other rejected types. Many types and backoff can delay discovery; use a precise whitelist for direct lookup. The 0.3.0 build requires a new inventory when upgrading from 0.2.x; see the quantity preflight for performance and format limits.

## Shared capacity

Items and fluids share an exact budget: 8 items or 2048 mB per byte, no type overhead. There is no artificial type cap, but practical size depends on memory, disk and AE enumeration performance. Broken/unloaded structures or an inactive interface stop access until restored.

## Exact quantities and display units

0.3.0 uses BigInteger for capacity, usage and exact per-resource quantities. AE-visible totals and individual AE requests remain signed longs; positive-overflow additions saturate. Sneak-right-click the controller while holding an item to query exact stock; filled containers also query their fluid. AVS6 reads local-development AVS5, but does not migrate AVS2/AVS3/AVS4 files from 0.2.x. Export old stock before upgrading, then create a new vault and supply new capacity sources. Display units extend to ZiB/YiB/RiB, with compact scientific notation beyond the named range. Type counts use `int` and practical size depends on memory and Java collections.

## AE Network Tool

A formed, connected vault appears in both item and fluid storage statistics. Each tab shows its channel usage/types and channel-used bytes plus shared free capacity. The free pool is shared, so do not add both tabs as independent capacities. The network tool uses AE's approximate numeric display; the vault retains exact big-integer quantities. An export-only bus is not counted as accessible storage.

## Read-cache configuration

LRU entry and byte budgets are in `config/gtnh-modernization.cfg`. Restart the game/server after editing. These do not change stored capacity. [Options and defaults](storage-cache.md).

## Moving and backups

Normal harvesting retains the controller UUID; replacement reconnects the same inventory. Duplicate controllers cannot mount one UUID together. External files are `<world>/data/aestoragevault/<UUID>.snapshot` and `.wal`. Always back up and restore the complete stopped world including chunks and players. Missing/corrupt files lock rather than replace stock. Keep files/logs and restore a complete backup. Migration receipts prevent duplicate capacity credit; ordinary cross-container transfers are not a distributed transaction with all other mods. Partial rollback or abrupt power loss can still create inconsistencies.

---

Related guides: [Read-cache settings](storage-cache.md) · [Compatibility and configuration](compatibility.md)
