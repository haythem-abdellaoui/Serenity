from __future__ import annotations

import logging
import os
import tempfile
from pathlib import Path

from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from faster_whisper import WhisperModel
from lara_sdk import Credentials, LaraApiError, LaraError, Translator
from pydantic import AliasChoices, BaseModel, ConfigDict, Field

logger = logging.getLogger("whisper-ai")

app = FastAPI(title="Speech ASR", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:4200",
        "http://127.0.0.1:4200",
        "http://localhost:8082",
        "http://127.0.0.1:8082",
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_MODEL_DIR = ROOT / "models" / "faster-whisper-tiny"

_model: WhisperModel | None = None
_lara: Translator | None = None


def _env_int(name: str, default: int) -> int:
    raw = os.environ.get(name)
    if raw is None:
        return default
    try:
        return int(raw.strip())
    except (TypeError, ValueError):
        return default


def _env_float(name: str, default: float) -> float:
    raw = os.environ.get(name)
    if raw is None:
        return default
    try:
        return float(raw.strip())
    except (TypeError, ValueError):
        return default


def _get_model() -> WhisperModel:
    global _model
    if _model is not None:
        return _model

    # Prefer explicit/local model dirs first (including notebook-friendly location).
    configured_model_dir = Path(os.environ.get("WHISPER_MODEL_DIR", str(DEFAULT_MODEL_DIR)))
    notebook_model_dir = ROOT / "notebooks" / "models" / "faster-whisper-tiny"
    model_dir_candidates = [configured_model_dir, DEFAULT_MODEL_DIR, notebook_model_dir]
    resolved_model_dir = next((p for p in model_dir_candidates if p.exists()), None)

    # If no local dir exists, fallback to a model name so faster-whisper can auto-download to cache.
    model_name = os.environ.get("WHISPER_MODEL_NAME", "tiny").strip() or "tiny"
    model_source = str(resolved_model_dir) if resolved_model_dir is not None else model_name
    device = os.environ.get("WHISPER_DEVICE", "cpu")
    compute = os.environ.get("WHISPER_COMPUTE_TYPE", "int8")
    logger.info("load model source=%s device=%s compute=%s", model_source, device, compute)
    _model = WhisperModel(model_source, device=device, compute_type=compute)
    return _model


def _get_lara() -> Translator:
    global _lara
    if _lara is not None:
        return _lara
    access_key_id = os.environ.get("LARA_ACCESS_KEY_ID", "").strip()
    access_key_secret = os.environ.get("LARA_ACCESS_KEY_SECRET", "").strip()
    if not access_key_id or not access_key_secret:
        raise HTTPException(
            status_code=503,
            detail="Lara Translate not configured. Set LARA_ACCESS_KEY_ID and LARA_ACCESS_KEY_SECRET.",
        )
    creds = Credentials(access_key_id=access_key_id, access_key_secret=access_key_secret)
    _lara = Translator(creds)
    return _lara


def _lara_target_locale(short_code: str) -> str:
    s = (short_code or "fr").strip().lower()
    mapping = {
        "fr": "fr-FR",
        "en": "en-US",
        "ar": "ar-SA",
        "de": "de-DE",
        "es": "es-ES",
        "it": "it-IT",
    }
    if s in mapping:
        return mapping[s]
    if len(s) == 2:
        return f"{s}-{s.upper()}"
    return s


def _lara_source_locale(source: str | None) -> str | None:
    if not source:
        return None
    s = source.strip().lower()
    if s in ("auto", "detect", ""):
        return None
    return _lara_target_locale(s)


def _lara_translation_to_plain_text(translation) -> str:
    """Lara `TextResult.translation` may be str, list[str], or list[TextBlock]."""
    if translation is None:
        return ""
    if isinstance(translation, str):
        return translation.strip()
    if isinstance(translation, (list, tuple)):
        parts: list[str] = []
        for item in translation:
            if isinstance(item, str):
                parts.append(item)
            else:
                t = getattr(item, "text", None)
                if isinstance(t, str) and t.strip():
                    parts.append(t.strip())
        return "\n".join(parts).strip()
    return str(translation).strip()


class TranslateRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    text: str = Field(..., min_length=1, max_length=50_000)
    target_lang: str = Field(
        default="fr",
        validation_alias=AliasChoices("targetLang", "target_lang"),
    )
    source_lang: str = Field(
        default="auto",
        validation_alias=AliasChoices("sourceLang", "source_lang"),
    )


class TranslateResponse(BaseModel):
    translated_text: str
    provider: str
    note: str | None = None


@app.get("/api/whisper/health")
def health():
    configured_model_dir = Path(os.environ.get("WHISPER_MODEL_DIR", str(DEFAULT_MODEL_DIR)))
    notebook_model_dir = ROOT / "notebooks" / "models" / "faster-whisper-tiny"
    model_dir_candidates = [configured_model_dir, DEFAULT_MODEL_DIR, notebook_model_dir]
    resolved_model_dir = next((p for p in model_dir_candidates if p.exists()), None)
    fallback_model_name = os.environ.get("WHISPER_MODEL_NAME", "tiny").strip() or "tiny"
    lara_ok = bool(
        os.environ.get("LARA_ACCESS_KEY_ID", "").strip()
        and os.environ.get("LARA_ACCESS_KEY_SECRET", "").strip()
    )
    return {
        "status": "ok",
        "model_source": str(resolved_model_dir) if resolved_model_dir is not None else fallback_model_name,
        "model_present": resolved_model_dir is not None,
        "lara_configured": lara_ok,
    }


@app.post("/api/whisper/transcribe")
async def transcribe(
    file: UploadFile = File(...),
    source_lang: str = Form(default="auto", alias="sourceLang"),
):
    suffix = Path(file.filename or "clip").suffix
    if not suffix or len(suffix) > 8:
        suffix = ".webm"
    data = await file.read()
    if len(data) < 64:
        raise HTTPException(status_code=400, detail="Audio chunk too small")

    model = _get_model()
    src = (source_lang or "auto").strip().lower()
    whisper_lang = None if src in ("", "auto", "detect") else src
    prompt = os.environ.get("WHISPER_INITIAL_PROMPT", "").strip() or None
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        tmp.write(data)
        path = tmp.name
    try:
        segments, info = model.transcribe(
            path,
            beam_size=max(1, _env_int("WHISPER_BEAM_SIZE", 8)),
            best_of=max(1, _env_int("WHISPER_BEST_OF", 5)),
            temperature=_env_float("WHISPER_TEMPERATURE", 0.0),
            condition_on_previous_text=True,
            vad_filter=os.environ.get("WHISPER_VAD_FILTER", "true").strip().lower() in {"1", "true", "yes", "on"},
            language=whisper_lang,
            initial_prompt=prompt,
        )
        text = "".join(seg.text for seg in segments).strip()
        return {"text": text, "language": info.language, "duration": info.duration}
    finally:
        try:
            os.unlink(path)
        except OSError:
            pass


@app.post("/api/whisper/translate", response_model=TranslateResponse)
def translate(req: TranslateRequest):
    src = _lara_source_locale(req.source_lang)
    target = _lara_target_locale(req.target_lang)
    lara_id = os.environ.get("LARA_ACCESS_KEY_ID", "").strip()
    lara_secret = os.environ.get("LARA_ACCESS_KEY_SECRET", "").strip()
    if not lara_id or not lara_secret:
        # Local dev: avoid empty "translated" UI when Lara keys are not set.
        return TranslateResponse(
            translated_text=req.text.strip(),
            provider="passthrough",
            note="Lara not configured; showing source text. Set LARA_ACCESS_KEY_ID and LARA_ACCESS_KEY_SECRET to enable translation.",
        )
    lara = _get_lara()
    try:
        if src:
            result = lara.translate(req.text, source=src, target=target)
        else:
            result = lara.translate(req.text, target=target)
    except HTTPException:
        raise
    except (LaraApiError, LaraError) as e:
        logger.warning("lara translate: %s", e)
        raise HTTPException(status_code=502, detail=str(e)) from e
    except Exception as e:
        logger.exception("lara translate failed")
        raise HTTPException(status_code=502, detail=str(e)) from e

    out = _lara_translation_to_plain_text(getattr(result, "translation", None))
    if not out:
        # Same text so the UI is not blank if Lara returns an unexpected structure.
        logger.warning("Lara returned empty translation; falling back to source text")
        return TranslateResponse(
            translated_text=req.text.strip(),
            provider="laratranslate",
            note="Lara returned no extractable text; source returned as-is.",
        )
    return TranslateResponse(translated_text=out, provider="laratranslate", note=None)
