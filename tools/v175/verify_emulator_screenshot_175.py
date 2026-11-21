#!/usr/bin/env python3
"""Verify a complete Android emulator screenshot without arbitrary PNG-size thresholds.

A mostly white or otherwise simple valid screenshot may compress to much less
than 10 KB. PNG integrity, decoded pixel data, and actual dimensions are
stronger checks than compressed byte count. This is a smoke test, not visual QA.
"""
from pathlib import Path
import struct
import sys
import zlib


def inspect_png(path: Path):
    raw = path.read_bytes()
    if raw[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError(f"{path.name}: invalid PNG signature ({len(raw)} bytes)")
    cursor = 8
    dimensions = None
    compressed = bytearray()
    chunks = 0
    iend = False

    while cursor + 12 <= len(raw):
        length = struct.unpack_from(">I", raw, cursor)[0]
        kind = raw[cursor + 4:cursor + 8]
        end = cursor + 12 + length
        if length > 50_000_000 or end > len(raw):
            raise ValueError(f"{path.name}: truncated PNG chunk {kind!r}")
        payload = raw[cursor + 8:cursor + 8 + length]
        expected_crc = struct.unpack_from(">I", raw, cursor + 8 + length)[0]
        actual_crc = zlib.crc32(kind + payload) & 0xFFFFFFFF
        if expected_crc != actual_crc:
            raise ValueError(f"{path.name}: corrupt PNG chunk {kind!r}")
        chunks += 1
        if kind == b"IHDR":
            if dimensions is not None or length != 13:
                raise ValueError(f"{path.name}: invalid IHDR")
            width, height = struct.unpack_from(">II", payload, 0)
            if width < 300 or height < 300 or width > 5000 or height > 5000:
                raise ValueError(f"{path.name}: unexpected size {width}x{height}")
            dimensions = (width, height)
        elif kind == b"IDAT":
            compressed.extend(payload)
        elif kind == b"IEND":
            if length != 0 or end != len(raw):
                raise ValueError(f"{path.name}: malformed PNG trailer")
            iend = True
            break
        cursor = end

    if not iend or dimensions is None or not compressed:
        raise ValueError(f"{path.name}: missing PNG image data or IEND")

    inflater = zlib.decompressobj()
    total_decoded = 0
    for start in range(0, len(compressed), 65536):
        block = bytes(compressed[start:start + 65536])
        while block:
            decoded = inflater.decompress(block, 16_000_000)
            total_decoded += len(decoded)
            if total_decoded > 100_000_000:
                raise ValueError(f"{path.name}: decoded image too large")
            block = inflater.unconsumed_tail
    total_decoded += len(inflater.flush())
    if not inflater.eof or total_decoded < dimensions[0] * dimensions[1]:
        raise ValueError(f"{path.name}: incomplete decoded pixels")
    return dimensions, len(raw), total_decoded, chunks


def verify_ui_content(path: Path, size: tuple[int, int]) -> float:
    """Require real application content in the central viewport, not just OS bars."""
    from PIL import Image
    from statistics import median

    with Image.open(path) as image:
        image = image.convert("RGB")
        width, height = size
        frame = image.crop((
            int(width * 0.07), int(height * 0.15),
            int(width * 0.93), int(height * 0.78),
        )).resize((160, 160))
        pixels = list(frame.getdata())

    center = tuple(int(median(pixel[channel] for pixel in pixels))
                   for channel in range(3))
    significant = sum(
        max(abs(pixel[channel] - center[channel]) for channel in range(3)) >= 30
        for pixel in pixels
    ) / len(pixels)
    if significant < 0.07:
        raise ValueError(
            f"{path.name}: central viewport looks empty or still loading "
            f"(foreground fraction {significant:.1%})."
        )
    return significant


if __name__ == "__main__":
    if len(sys.argv) < 2:
        raise SystemExit("Usage: verify_emulator_screenshot_175.py PATH [--require-ui] [--expect=WxH]")
    file_path = Path(sys.argv[1])
    opts = sys.argv[2:]
    try:
        dimensions, bytes_on_disk, raw_pixels, chunks = inspect_png(file_path)
        expected = next((value.split("=", 1)[1] for value in opts
                         if value.startswith("--expect=")), None)
        if expected and f"{dimensions[0]}x{dimensions[1]}" != expected:
            raise ValueError(f"Unexpected screen size {dimensions}, expected {expected}")
        ui_fraction = verify_ui_content(file_path, dimensions) if "--require-ui" in opts else None
    except (ValueError, OSError, zlib.error) as exc:
        print(f"INVALID SCREENSHOT: {exc}", file=sys.stderr)
        raise SystemExit(1)
    print(f"PASS: verified PNG {dimensions[0]}x{dimensions[1]}, "
          f"{bytes_on_disk} compressed bytes, {raw_pixels} decoded bytes, "
          f"{chunks} chunks, app foreground fraction {ui_fraction}; "
          "human visual review still required")
