"""
Download faster-whisper CTranslate2 weights into ./models (gitignored).
Uses Hugging Face Hub — run once after pip install -r requirements.txt.
"""
from pathlib import Path

from huggingface_hub import snapshot_download

TARGET = Path(__file__).resolve().parent.parent / "models" / "faster-whisper-tiny"


def main() -> None:
    TARGET.parent.mkdir(parents=True, exist_ok=True)
    print("Downloading Systran/faster-whisper-tiny into", TARGET)
    snapshot_download(
        repo_id="Systran/faster-whisper-tiny",
        local_dir=str(TARGET),
        local_dir_use_symlinks=False,
    )
    print("Done. Set WHISPER_MODEL_DIR to:", TARGET)


if __name__ == "__main__":
    main()
