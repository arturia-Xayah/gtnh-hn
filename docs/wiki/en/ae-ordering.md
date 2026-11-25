# AE order optimization

[Wiki contents](../README.en.md) · [Project home](../../../README.en.md) · [简体中文](../zh/ae-ordering.md)

> [User guide](../README.en.md) / [AE networks and automation](../README.en.md#ae-networks-and-automation)

**On this page**

- [Component and setup](#component-and-setup)
- [ME output overflow](#me-output-overflow)
- [Route checks and reports](#route-checks-and-reports)
- [Batch dispatch](#batch-dispatch)
- [Craftable input badges](#craftable-input-badges)
- [Assembly-line auto-renaming](#assembly-line-auto-renaming)
- [Batch Dispatch Limit Card](#batch-dispatch-limit-card)

## Component and setup

Craft the optimizer with gold/quartz/gold, redstone/crafting table/redstone, gold/quartz/gold. Connect one to the submitting grid; right-click for detail mode. It uses 1 AE/t and no channel. Merged-grid duplicates disconnect until the conflict is resolved.

## ME output overflow

The optimizer also has an independent **ME output overflow** switch, defaulting to ON for new and existing worlds. It covers native ME Output Buses, ME Output Hatches and Dual ME Output Buses on the output device’s own grid. ON accepts completion overflow while leaving new recipe checks at native capacity. OFF restores native acceptance without truncating existing excess or stopping its normal flush. The setting persists; isolated subnets are independent. It does not disable planning, batch dispatch, the limit card or renaming.

## Route checks and reports

An active optimizer selects the modified Lite planner and supported bulk dispatch. Without it, native planning and dispatch remain. The submitting grid governs cross-grid pattern borrowing. Selected-route cycles or multi-output patterns stop the order and report the issue. Detail mode reports main outputs and recipe counts without changing native route selection. Cycles display their main-output chain. Colored, underlined outputs link to provider coordinates and a 30-second outline in the current dimension; other dimensions report name and coordinates. Links expire after ten minutes; recalculate after moving patterns. Missing stock or patterns can still prevent crafting.

## Batch dispatch

Keep unit patterns. Batches contain at most 4096 crafts and follow stock, energy, coprocessor and receiver limits. Native ME interfaces support batching; unsupported receivers and workbench recipes retain unit dispatch. Strict Blocking retains unit dispatch unless the interface has a Batch Dispatch Limit Card. Batching does not add processing parallelism.

The current validator expands the selected pattern graph before stock consumption, so it may inspect upstream patterns even for stocked intermediates.

## Craftable input badges

The input **+** badge needs no optimizer. It indicates a pattern in the current grid, not enough stock for every dependency. Item identity preserves metadata/NBT; fluids follow this AE version's native matching.

## Assembly-line auto-renaming

**Assembly-line auto-renaming:** enable the optional switch in the optimizer (off by default). NEI assembly-line imports retain the first occurrence of each exact item and name later occurrences `重命名1`, `重命名2`, etc. Different items number independently; counts/positions, fluids and existing custom names are preserved. Occupied names are skipped. Names are fixed across client languages and match a Forming Press using a Name Mold with that exact name. The second, default-off switch creates missing 1:1 renaming processing patterns immediately on import and delivers them to your inventory. The main pattern is still encoded normally. Blanks are consumed from the terminal slot, your inventory, then the powered ME network. Install the auxiliary patterns at Forming Presses with matching Name Molds; molds are not consumed recipe inputs. Exact unit processing patterns already installed in the current crafting grid, carried in your inventory or in the terminal output slot are skipped. Loose patterns in other containers/ME storage are not scanned. Insufficient space or blanks cancels the import without replacing the old matrix; a partial network extraction is retained as blank patterns in the terminal slot. The terminal's own active grid controls this feature; existing patterns, manual edits and other recipe maps are unaffected.


## ME Interface P2P batch distribution

Open the **input** ME Interface and click the mode button on the left. Standard and Dual ME Interface tunnels store this setting per input. The submitting network needs an optimizer and the processing pattern must support batch dispatch.

| Mode | Output behavior |
| --- | --- |
| Default | Keeps the existing dispatch order. |
| Even split | Divides whole crafts among linked outputs and rotates the remainder. Ten crafts across four ports give two ports three crafts and two ports two. A blocked port waits independently. |
| Batch round robin (resume) | Starts each batch at the next output, skips busy ports, and waits 10 ticks after a complete pass. |
| Batch round robin (first available) | Starts each batch at the first output and tries subsequent outputs until one accepts. With smart blocking, the first output can take the same pattern again. A different pattern can pass to a later output while ingredients remain at the first. |

Each output retains its own limit card and blocking rules. Ingredients remain in the CPU until accepted. Changing mode or adding or removing ports redistributes outstanding crafts.

## Batch Dispatch Limit Card

Craft an Advanced Card with a redstone comparator (shapeless). Install one card in the **sending** block or part ME interface, including AE2FC dual interfaces. Use the left quantity icon button to set independent per-item-type and per-fluid-type caps. Fluids use mB; `0` is unlimited; K/M/G/T are decimal suffixes. Limits apply only when the **ordering** network has an optimizer. They persist on the interface after card removal but remain inactive without a card.

Repeated ingredients are added before division; item NBT variants remain distinct. A recipe larger than a cap pauses this interface without withdrawing inputs. The 1M-fluid / 2M-item example (100k, 50k, 20k mB and 250k items per recipe) sends eight sets per batch. One batch consumes one CPU operation.

Strict Blocking may now send a whole batch, then uses native waiting rules. Smart Blocking retains consecutive dispatch and may send several batches in a tick. The previous forced-empty guard is removed. Limits do not reserve tank space or change native partial-acceptance queues; size them for the receiving inventory. Editing limits does not recall materials already queued for delivery.

Development builds also support ME Interface P2P and Dual Interface P2P outputs. Install and configure the card at each sending output. Limits are local to that endpoint and survive rebinding. For strict blocking on a receiving subnet, install an Advanced Blocking Card in its receiving interface. Partial acceptance can leave the rest of a batch queued at the sending output.

The card uses AE upgrade-slot validation/counting and per-interface limits, but keeps its own mod item ID. Hold Shift for detailed rules and the localized supported-device list. Too-small caps show a warning on the interface button/settings and in rate-limited server logs; AE's crafting status currently uses its generic blocking reason.

---

Related guides: [Dual ME Output Bus](dual-output-bus.md) · [ME Advanced Level Emitter](level-emitter.md)
