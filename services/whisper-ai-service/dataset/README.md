# Serenity Whisper — versioned ASR dataset layout

This folder is **tracked in Git** so you always see a real “I have a dataset + manifest + notebook” workspace, separate from **model weights** (downloaded into `../models/`, gitignored).

## What is here

| Item | Role |
|------|------|
| `manifest.csv` | Utterance index: `clip_id`, relative `audio_path`, optional `transcript`, notes. Same *shape* as common ASR corpora (LibriSpeech-style: id + path + text). |
| `audio/*.wav` | Small **bundled** clips (synthetic tones + silence) so the repo stays light. They exist so paths in `manifest.csv` resolve without any extra download. |
| `../notebooks/serenity_whisper_demo.ipynb` | Reads this manifest and runs local **faster-whisper** inference on `../models/`. |

## What you download separately (by design)

- **Acoustic checkpoints** (CTranslate2 / faster-whisper): `python ../scripts/download_models.py` → `../models/faster-whisper-tiny.en/`. That is **not** “the dataset”; it is a pretrained encoder/decoder you use for inference (and optionally fine-tuning elsewhere).
- **Optional real speech corpus** (LibriSpeech, Mozilla Common Voice, etc.): large; keep outside Git or under a gitignored cache. Point new rows in a *local* manifest copy at those files if you fine-tune.

## Optional: pull a few real LibriSpeech-style utterances

If you want actual speech WAVs on disk (not committed), install `datasets` and run:

```bash
pip install datasets soundfile
python scripts/fetch_librispeech_demo_clips.py
```

That writes under `../data/librispeech_demo/` (gitignored) and prints a snippet you can merge into a private manifest.

## Regenerate bundled `audio/` + `manifest.csv`

```bash
python scripts/build_versioned_dataset.py
```

---

**Licence note:** LibriSpeech and similar corpora have their own licences (e.g. CC BY 4.0). The **bundled** synthetic clips in `audio/` are Serenity project artefacts for layout/testing only.
