# Read-cache settings

[Wiki contents](../README.en.md) · [Project home](../../../README.en.md) · [简体中文](../zh/storage-cache.md)

> [User guide](../README.en.md) / [ME storage](../README.en.md#me-storage)

Since **0.3.0-alpha**, the mod generates `config/gtnh-modernization.cfg`. Edit the lowercase `storagecache` category and restart the entire game/server process. Dedicated-server settings are authoritative; client values do not override them.

Key/prototype entry and byte limits apply separately to each item/fluid adapter. Export-bus limits combine both channels. `globalMaxBytes` is shared across all three cache kinds and all vaults. Zero disables the corresponding retention; zero global budget disables all listed caches. Inventory remains complete and readable, with uncached decoding as needed.

Values are non-negative decimal integers. Entry limits are capped at 1000000; byte limits accept signed-long non-negative values. Invalid entries are logged and reset to defaults. Byte values are accounting weights, not a JVM heap cap: encoded keys cost `2*(key bytes+payload bytes)+128`; decoded prototypes cost `2*(key bytes+payload bytes)+512`. Ledger/index/list memory, AE shared NBT and transient operations are outside this budget. Full scans only admit prototypes with available room, avoiding whole-working-set eviction on each pass.

## Options and defaults

[Example configuration](../../examples/config/gtnh-modernization.cfg). Keep the lowercase `storagecache` category and the generated `S:` prefixes.

| Option | Default | Scope |
| --- | ---: | --- |
| `keyMaxEntries` | 4096 | Identity-key entries per link hatch, per channel |
| `keyMaxBytes` | 16777216 (16 MiB) | Byte budget for each identity-key cache |
| `prototypeMaxEntries` | 16384 | Decoded prototypes per link hatch, per channel |
| `prototypeMaxBytes` | 67108864 (64 MiB) | Byte budget for each prototype cache |
| `outputMaxEntries` | 256 | Export-bus prototype entries, both channels combined |
| `outputMaxBytes` | 8388608 (8 MiB) | Byte budget per export bus |
| `maxEntryBytes` | 1048576 (1 MiB) | Maximum accounting weight of a single entry |
| `globalMaxBytes` | 268435456 (256 MiB) | Shared budget across all vault caches |

## Reducing or disabling caches

Set `globalMaxBytes` to `0` to disable all listed caches. A zero entry or byte limit disables that cache kind; `maxEntryBytes = 0` rejects all entries. Oversized resources remain readable and are decoded as needed. Cache eviction never removes inventory, capacity or types, and never changes priorities or filters.

Higher budgets can retain more resources; lower budgets can increase decoding work. These are accounting weights, not a guarantee of a particular MSPT or total heap usage. Restart the entire process to reload settings; reopening a single-player world is not enough.

---

Related guides: [ME Cell Aggregation Storage](storage.md)
