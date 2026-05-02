import os
import logging
from datetime import datetime
from typing import Optional

from fastapi import FastAPI, File, Form, Header, HTTPException, UploadFile
from fastapi.concurrency import run_in_threadpool
from cnopt_verification import CnoptVerificationService


app = FastAPI(title="Pharmacy ML Service", version="1.0.0")
INTERNAL_API_KEY = os.getenv("PHARMACY_ML_INTERNAL_API_KEY", "serenity-internal-key-dev")
LOGGER = logging.getLogger(__name__)
CNOPT_VERIFIER = CnoptVerificationService()


def _ensure_internal_key(x_internal_key: Optional[str]) -> None:
    if x_internal_key != INTERNAL_API_KEY:
        raise HTTPException(status_code=401, detail="Unauthorized internal key")


@app.get("/internal/health")
def healthcheck() -> dict:
    return {
        "status": "ok",
        "cnoptVerifier": {
            "available": CNOPT_VERIFIER.available,
            "error": CNOPT_VERIFIER.last_error if not CNOPT_VERIFIER.available else None,
        },
    }


@app.post("/internal/pharmacy-applications/verify-cnopt")
async def verify_cnopt_document(
    file: UploadFile = File(...),
    filename: Optional[str] = Form(default=None),
    x_internal_key: Optional[str] = Header(default=None, alias="X-Internal-Key"),
) -> dict:
    _ensure_internal_key(x_internal_key)
    if file is None:
        raise HTTPException(status_code=400, detail="Missing CNOPT document file")

    file_bytes = await file.read()
    effective_filename = (filename or file.filename or "cnopt-document").strip()

    try:
        result = await run_in_threadpool(
            CNOPT_VERIFIER.verify_file,
            file_bytes,
            effective_filename,
            file.content_type,
        )
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=f"CNOPT verifier unavailable: {exc}") from exc
    except Exception as exc:  # pylint: disable=broad-except
        LOGGER.exception("Unexpected CNOPT verification error: %s", exc)
        raise HTTPException(status_code=500, detail="CNOPT verification failed unexpectedly") from exc

    return {"runAt": datetime.now().isoformat(), **result}
