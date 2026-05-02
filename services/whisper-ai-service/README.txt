Serenity Whisper AI (local inference, not OpenAI cloud)

1) Python 3.10+ recommended. Create venv and install:
   python -m venv .venv
   .venv\Scripts\activate
   pip install -r requirements.txt

2) Model source options:
   - Preferred local folder (fast startup): python scripts/download_models.py
   - Or rely on auto-download cache at runtime using WHISPER_MODEL_NAME=tiny (configured in .env)

3) Versioned ASR mini-corpus (visible in Git under dataset/):
   - Already contains manifest + audio/ after clone; to regenerate:
     python scripts/build_versioned_dataset.py
   - Optional real LibriSpeech-style clips (large download, gitignored under data/):
     pip install datasets soundfile
     python scripts/fetch_librispeech_demo_clips.py
   - Legacy alias: python scripts/setup_sample_dataset.py  (same as build_versioned_dataset)

4) Run API (default port 5002):
   python -m uvicorn app.main:app --host 0.0.0.0 --port 5002 --env-file .env

5) Gateway routes /api/whisper/** to this service; Angular calls http://localhost:8082/api/whisper/...

6) Translation (Lara Translate — laratranslate.com):
   Set environment variables (do not commit real values to Git):
     LARA_ACCESS_KEY_ID=your-access-key-id
     LARA_ACCESS_KEY_SECRET=your-access-key-secret
   Optional: copy .env.example to .env in this folder and fill values (see .gitignore for .env).

FFmpeg must be on PATH (faster-whisper uses it for many audio containers).
