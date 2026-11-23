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

The shared upgrade/filter slot accepts an item or fluid cell, upgrading and filtering only its channel. The other retains its native default capacity: 1,600 items or 128,000 mB. The bus works without an optimizer; enabling ME output overflow in its network’s optimizer protects both channels. Both caches are saved with the world and use their native output intervals.

---

Related guides: [Dual ME Stocking Input Bus](dual-input-bus.md) · [AE order optimization](ae-ordering.md)
