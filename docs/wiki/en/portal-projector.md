# Portal Projector

[Wiki contents](../README.en.md) · [Project home](../../../README.en.md) · [简体中文](../zh/portal-projector.md)

> [User guide](../README.en.md) / [Portals and scenes](../README.en.md#portals-and-scenes)

**On this page**

- [Getting and pairing projectors](#getting-and-pairing-projectors)
- [Live and snapshot views](#live-and-snapshot-views)
- [Arrival direction and closing](#arrival-direction-and-closing)
- [Effects and image quality](#effects-and-image-quality)

## Getting and pairing projectors

Craft one projector with an **ME Quantum Link Chamber** in the centre, four **ME Quantum Rings** on the edges and any IV-tier circuit in each corner.

Left-click the source projector with a GT **Data Stick** to copy its settings and right-click the destination projector to paste. The preset includes geometry, colours, station name, exit direction, destination label, scene options, mute and transparency. The target keeps its frequency. A closed target stays closed. If opening or resizing would overlap another gate, the new settings are saved and this projector closes. Open it again after clearing the overlap.

Take two projectors from the Redstone creative tab. Right-click each to edit its shape, dimensions and colours. Open **Connection**, enter the same frequency at both ends, and select **Apply & open** on each. The pair transports players in both directions, including between dimensions. Opening a gate with an empty frequency shows a projection-only notice. In **Connection**, name the local station and choose whether to display the destination name above this gate. The label updates when the other station is renamed.

## Live and snapshot views

Enable **Scene** to see up to 16 blocks ahead of the exit. **Live** aims the camera from the exit centre according to your eye position relative to the entrance. **Snapshot** faces the configured exit direction and refreshes every 200 game ticks. The viewer must be within 16 blocks of the entrance centre. Only loaded destination chunks are shown. All eligible gates in view display their own destinations simultaneously, each using its selected mode. The feature defaults to off.

## Arrival direction and closing

Set the arrival direction on the destination projector. It follows the projector by default and can be set to any cardinal direction. Keep the space above the projector clear for landing. Open apertures cannot intersect. Select **Save and close gate** to release its frequency slot.

## Effects and image quality

Successful travel sends fading ripples across the entrance and exit surfaces. Nearby players can see the impact, and the traveller also sees a short arrival ripple.

Turn off **Travel sound** under **Connection** to mute both sounds on trips starting here and the local arrival sound on incoming trips. The arrival pulse and particles still play.

Under **Colors**, adjust **Portal transparency** in 10% steps. At 0% the gate keeps its original appearance. At 100% the surface, rim and destination image are invisible. Travel and the separately configured destination label still work.

Destination images follow the gate's aspect ratio, with a default longest edge of 1024 pixels and a live limit of 30 FPS. Adjust `client.portalSceneResolution` (256–2048) and `client.portalSceneFps` (5–60) in `config/structureterminal.cfg`. Higher settings cost more GPU time, especially with several visible gates.

---

Related guides: [Compatibility and configuration](compatibility.md)
