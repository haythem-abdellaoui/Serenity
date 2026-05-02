"""
Optional: download a handful of real LibriSpeech ASR utterances (Hugging Face)
into gitignored ../data/librispeech_demo/ — for a believable "I pulled a
public dataset" workflow without bloating Git.

Requires: pip install datasets soundfile
"""
from __future__ import annotations

import csv
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / "data" / "librispeech_demo"
MANIFEST = OUT_DIR / "manifest_hf_demo.csv"


def main() -> None:
    try:
        import soundfile as sf
        from datasets import Audio, load_dataset
    except ImportError as e:
        raise SystemExit("Install: pip install datasets soundfile") from e

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    # Small split slice; first rows are enough for a local demo.
    ds = load_dataset("librispeech_asr", "clean", split="test[:5]")
    ds = ds.cast_column("audio", Audio(sampling_rate=16_000))
    rows = []
    for i, ex in enumerate(ds):
        aid = f"ls_clean_test_{i:04d}"
        wav_path = OUT_DIR / f"{aid}.wav"
        sf.write(str(wav_path), ex["audio"]["array"], 16_000)
        text = (ex.get("text") or "").strip()
        rows.append([aid, str(wav_path.resolve()), text, "librispeech_asr clean test (HF)"])

    with MANIFEST.open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["clip_id", "audio_path", "transcript", "source_notes"])
        for r in rows:
            w.writerow(r)
    print("Wrote", len(rows), "clips under", OUT_DIR)
    print("Manifest:", MANIFEST)


if __name__ == "__main__":
    main()
