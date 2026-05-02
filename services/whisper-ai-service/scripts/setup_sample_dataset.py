"""
Legacy entry point: the **versioned** corpus lives under `dataset/` (tracked in Git).

- Run `python scripts/build_versioned_dataset.py` to regenerate `dataset/audio/` + `dataset/manifest.csv`.
- Optional scratch clips still go under `data/` (gitignored); see `dataset/README.md`.
"""
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parent.parent


def main() -> None:
    script = ROOT / "scripts" / "build_versioned_dataset.py"
    print("Delegating to versioned dataset under dataset/ …")
    subprocess.check_call([sys.executable, str(script)], cwd=str(ROOT))


if __name__ == "__main__":
    main()
