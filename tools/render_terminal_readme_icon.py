#!/usr/bin/env python3
"""Export the actual terminal sprite animation for GitHub. Requires Pillow; never starts Minecraft."""
import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "src/main/resources/assets/structureterminal/textures/items/structure_terminal.png"
TARGET = ROOT / "docs/assets/terminal-in-game.gif"


def main():
    sprite = Image.open(SOURCE).convert("RGBA")
    animation = json.loads(Path(str(SOURCE) + ".mcmeta").read_text())["animation"]
    width = sprite.width
    assert sprite.height % width == 0, "Expected a vertical strip of square frames"
    count = sprite.height // width
    sequence = animation.get("frames", list(range(count)))
    frames, durations = [], []
    for entry in sequence:
        index = entry["index"] if isinstance(entry, dict) else entry
        ticks = entry.get("time", animation.get("frametime", 1)) if isinstance(entry, dict) else animation.get("frametime", 1)
        assert 0 <= index < count and ticks > 0
        frame = sprite.crop((0, index * width, width, (index + 1) * width))
        frames.append(frame.resize((128, 128), Image.Resampling.NEAREST))
        durations.append(ticks * 50)  # Minecraft texture animation advances at 20 ticks per second.

    pixels = [pixel for frame in frames for pixel in frame.getdata()]
    assert all(pixel[3] in (0, 255) for pixel in pixels), "GIF cannot preserve partial transparency"
    colours = sorted({pixel[:3] for pixel in pixels if pixel[3]})
    assert len(colours) < 256, "GIF palette would change source colours"
    palette = [(0, 0, 0)] + colours
    palette += [(0, 0, 0)] * (256 - len(palette))
    colour_index = {colour: i + 1 for i, colour in enumerate(colours)}
    encoded = []
    for frame in frames:
        indexed = Image.new("P", frame.size)
        indexed.putpalette([channel for colour in palette for channel in colour])
        indexed.putdata([colour_index[pixel[:3]] if pixel[3] else 0 for pixel in frame.getdata()])
        encoded.append(indexed)

    encoded[0].save(
        TARGET, save_all=True, append_images=encoded[1:], duration=durations,
        loop=0, transparency=0, disposal=2, optimize=False,
    )
    # Verify the exported frames, timing and transparent pixels, not just that a GIF file exists.
    with Image.open(TARGET) as result:
        assert result.n_frames == len(frames) and result.info["loop"] == 0
        for i, expected in enumerate(frames):
            result.seek(i)
            assert result.info["duration"] == durations[i]
            actual = result.convert("RGBA")
            assert all(
                a[3] == b[3] and (a[3] == 0 or a[:3] == b[:3])
                for a, b in zip(actual.getdata(), expected.getdata())
            ), "Export changed a visible pixel"
    print(json.dumps({"file": str(TARGET.relative_to(ROOT)), "frames": len(frames),
                      "duration_ms": durations, "loop": "forever", "bytes": TARGET.stat().st_size}))


if __name__ == "__main__":
    main()
