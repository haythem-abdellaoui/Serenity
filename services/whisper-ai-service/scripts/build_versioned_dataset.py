"""
Materialize the **versioned** Serenity ASR mini-corpus under `dataset/`
(audio + manifest). This is checked into git so the repo always shows a
real `dataset/` tree (unlike `data/`, which is gitignored scratch space).

Run after cloning if you ever delete the wavs:
  python scripts/build_versioned_dataset.py
"""
from __future__ import annotations

import csv
import math
import struct
import wave
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
AUDIO = ROOT / "dataset" / "audio"
MANIFEST = ROOT / "dataset" / "manifest.csv"


def _write_tone(path: Path, hz: float, seconds: float = 0.6, rate: int = 16_000, amp: float = 0.25) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    n = int(seconds * rate)
    with wave.open(str(path), "w") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(rate)
        frames = bytearray()
        for i in range(n):
            s = int(32_767 * amp * math.sin(2 * math.pi * hz * i / rate))
            frames.extend(struct.pack("<h", max(-32_768, min(32_767, s))))
        w.writeframes(bytes(frames))


def _write_silence(path: Path, seconds: float = 0.5, rate: int = 16_000) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    n = int(seconds * rate)
    with wave.open(str(path), "w") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(rate)
        w.writeframes(b"\x00\x00" * n)


def main() -> None:
    rows = [
        (
            "SE-CAL-440",
            "audio/serenity_eval_tone_440.wav",
            "",
            "Bundled synthetic calibration tone (440 Hz, mono 16 kHz). Same layout as LibriSpeech-style manifests.",
        ),
        (
            "SE-CAL-880",
            "audio/serenity_eval_tone_880.wav",
            "",
            "Bundled synthetic calibration tone (880 Hz).",
        ),
        (
            "SE-SIL-05",
            "audio/serenity_eval_silence_0p5s.wav",
            "",
            "Bundled silence clip (noise-floor / pipeline check).",
        ),
    ]
    _write_tone(AUDIO / "serenity_eval_tone_440.wav", 440.0)
    _write_tone(AUDIO / "serenity_eval_tone_880.wav", 880.0)
    _write_silence(AUDIO / "serenity_eval_silence_0p5s.wav", 0.5)

    MANIFEST.parent.mkdir(parents=True, exist_ok=True)
    with MANIFEST.open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["clip_id", "audio_path", "transcript", "source_notes"])
        w.writerows(rows)
    print("Wrote", MANIFEST)
    for _, rel, _, _ in rows:
        print("  ", ROOT / "dataset" / rel)


if __name__ == "__main__":
    main()
