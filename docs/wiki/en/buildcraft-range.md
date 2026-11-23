# BuildCraft work range

[Wiki contents](../README.en.md) · [Project home](../../../README.en.md) · [简体中文](../zh/buildcraft-range.md)

> [User guide](../README.en.md) / [Building and machine controls](../README.en.md#building-and-machine-controls)

**On this page**

- [Setting the range](#setting-the-range)
- [Moving and copying](#moving-and-copying)
- [Draft, preview and apply](#draft-preview-and-apply)

Enter the working area in the machine GUI, preview it in the world, then apply it.

## Setting the range

Choose **Set range** in the Filler or Architect Table. Right-click an unconfigured Architect Table with an empty hand. The first block touching the front is included. Looking in the machine’s forward direction, positive width extends right and negative width left. Positive height extends up and negative height down. Depth is always positive. Each dimension is limited by the server’s BC marker range. Preview the outline in the world, then apply. If the machine is busy, the last confirmed range takes effect after its current job.

## Moving and copying

Mining the machine saves the last confirmed dimensions on its dropped item. Placing it again anchors the range to its new front. In Matter Manipulator copy mode, a blue outline previews each destination machine’s work range, including horizontal rotations and arrays.

## Draft, preview and apply

Valid edits are retained as a draft when closing and reopening the menu. **Preview** closes the menu and leaves a world outline for 60 seconds, allowing free camera movement. It does not alter the working area. Reopen and choose **Apply** to commit; **Reset** restores the current actual area. Queued changes on busy machines are separate from the actual area.

---

Related guides: [Advanced Structure Terminal](structure-terminal.md)
