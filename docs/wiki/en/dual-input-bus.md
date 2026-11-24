# Dual ME Stocking Input Bus

[Wiki contents](../README.en.md) · [Project home](../../../README.en.md) · [简体中文](../zh/dual-input-bus.md)

> [User guide](../README.en.md) / [AE networks and automation](../README.en.md#ae-networks-and-automation)

## Crafting and slots

Both recipes run in the Assembler at EV (1,920 EU/t). Use unhoused ME storage components and any IV-tier circuit.

| Output | Ingredients | Base duration |
|---|---|---|
| Dual ME Stocking Input Bus | 1 Stocking Input Bus (ME), 4 IV circuits, 16 16k item storage components and 16 16k fluid storage components | 60 seconds |
| Advanced Dual ME Stocking Input Bus | 1 basic Dual ME Stocking Input Bus, 16 IV circuits, 8 64k item storage components and 8 64k fluid storage components | 120 seconds |

Place a configured dual input bus alone in the inventory crafting grid or a crafting table. Take the same tier of bus to reset filters, virtual circuit, colour, connection sides and auto-pull settings. The custom name, covers, upgrades and manual-slot items are retained.

Both share 16 item-or-fluid filter slots, with available amounts shown on the right, and retain the manual item slot, circuit slot, colouring and configuration copying.

## Auto-pull and machine support

Advanced auto-pull alternates item and fluid candidates within the same 16 slots, with a minimum quantity and refresh interval. Install these as input buses. Fluids participate in GT's standard multiblock ingredient collection; special machines that directly require dedicated fluid-hatch positions, such as assembly lines, still require those hatches. No Order Optimizer is needed.

---

Related guides: [Dual ME Output Bus](dual-output-bus.md)
