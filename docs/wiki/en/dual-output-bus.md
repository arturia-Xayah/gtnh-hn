# Dual ME Output Bus

[Wiki contents](../README.en.md) · [Project home](../../../README.en.md) · [简体中文](../zh/dual-output-bus.md)

> [User guide](../README.en.md) / [AE networks and automation](../README.en.md#ae-networks-and-automation)

**On this page**

- [Crafting and setup](#crafting-and-setup)
- [Valid structure positions](#valid-structure-positions)
- [Caches and overflow](#caches-and-overflow)

## Crafting and setup

Shapeless recipe: **ME Output Bus + ME Output Hatch**. One AE connection handles both channels with independent native caches, caching mode, priority, strict capacity checking and output intervals. Right-click to configure each channel.

## Valid structure positions

The **same structure position must permit both an output bus and an output hatch**. A bus-only or hatch-only position rejects it, even when other positions in that machine allow the other role. Unrecognized custom position rules are rejected conservatively.

## Caches and overflow

Install an item cell in the left slot and a fluid cell in the right slot. Both slots can expand capacity and apply partition filters at the same time. An empty slot uses the native default capacity of 1,600 items or 128,000 mB. Existing buses move an installed fluid cell to the right slot when loaded. The bus works without an optimizer; enabling ME output overflow in its network’s optimizer protects both channels. Both caches are saved with the world and use their native output intervals.

For example, a new bus with a five-type fluid cell can receive hydrochloric acid and gold ingots from the same reactor. The acid uses the fluid channel and the ingots use the built-in 1,600-item buffer. For long-term storage of both, connect the bus to an ME network with storage available on both channels.

Look at the bus to see item and fluid storage separately in Waila. The fluid section shows buffered quantity, capacity and fluid names in mB, including when the bus is offline. In cell-cache mode, fluids stored in the cell appear separately.

---

Related guides: [Dual ME Stocking Input Bus](dual-input-bus.md) · [AE order optimization](ae-ordering.md)
