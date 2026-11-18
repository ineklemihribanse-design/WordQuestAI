"""Generate the original, gentle, copyright-free correct-answer chime."""
from pathlib import Path
import math
import struct
import wave

SAMPLE_RATE = 22050
DURATION_SECONDS = 0.34


def note(t: float, freq: float, start: float, duration: float, strength: float) -> float:
    dt = t - start
    if dt < 0 or dt > duration:
        return 0.0
    envelope = min(1.0, dt / .022) * math.exp(-4.7 * dt / duration) * min(1.0, (duration - dt) / .045)
    return strength * envelope * (math.sin(2 * math.pi * freq * dt) + .10 * math.sin(4 * math.pi * freq * dt))


def generate(destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    values = []
    for i in range(round(SAMPLE_RATE * DURATION_SECONDS)):
        t = i / SAMPLE_RATE
        sample = note(t, 523.25, 0, .275, .38) + note(t, 659.25, .115, .205, .30)
        values.append(int(max(-1, min(1, sample)) * 32767))
    with wave.open(str(destination), "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(SAMPLE_RATE)
        wav.writeframes(struct.pack("<%dh" % len(values), *values))


if __name__ == "__main__":
    generate(Path(__file__).resolve().parent.parent / "app/src/main/res/raw/correct_soft.wav")
