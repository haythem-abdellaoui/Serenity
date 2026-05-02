"""
Serenity monitoring crisis-risk API (FastAPI).
Default port: 5150 (does not conflict with claim-risk-model :5123 or other services).

Run: uvicorn main:app --host 0.0.0.0 --port 5150
"""
from __future__ import annotations

import os
import re
from contextlib import asynccontextmanager
from pathlib import Path
from typing import List

import joblib
import numpy as np
import pandas as pd
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field, field_validator

SERVICE_PORT = int(os.environ.get("PORT", "5150"))

model = None
label_encoder = None
feature_columns: list[str] | None = None
text_vectorizer = None
text_risk_model = None
medium_subtype_model = None
medium_subtype_labels: list[str] | None = None
high_subtype_model = None
high_subtype_labels: list[str] | None = None


class CrisisPredictionRequest(BaseModel):
    patient_id: int = Field(..., ge=1)
    avg_mood_7days: float
    crisis_entries_count: int = Field(..., ge=0)
    days_of_silence: int = Field(..., ge=0)
    trigger_intensity_avg: float = Field(..., ge=0.0)
    mood_trend: int
    min_mood_7days: int = Field(..., ge=1, le=10)
    trigger_count: int = Field(..., ge=0)
    total_entries: int = Field(..., ge=0)
    mood_description_text: str | None = Field(default=None, max_length=2000)
    trigger_text: str | None = Field(default=None, max_length=1500)

    @field_validator("mood_trend")
    @classmethod
    def mood_trend_must_be_valid(cls, value: int) -> int:
        if value not in (-1, 0, 1):
            raise ValueError("mood_trend must be -1, 0, or 1")
        return value

    @field_validator("avg_mood_7days")
    @classmethod
    def mood_must_be_in_range(cls, value: float) -> float:
        if not 0.0 <= value <= 10.0:
            raise ValueError("avg_mood_7days must be between 0 and 10")
        return value


class CrisisPredictionResponse(BaseModel):
    patient_id: int
    risk_level: str
    confidence: float
    message: str
    recommendation: str
    risk_score: int
    risk_type: str | None = None
    medium_risk_type: str | None = None


class BatchPredictionRequest(BaseModel):
    patients: List[CrisisPredictionRequest]


class BatchPredictionResponse(BaseModel):
    predictions: List[CrisisPredictionResponse]


def load_model_artifacts() -> None:
    global model, label_encoder, feature_columns, text_vectorizer, text_risk_model
    global medium_subtype_model, medium_subtype_labels, high_subtype_model, high_subtype_labels

    base_dir = Path(__file__).resolve().parent
    model_path = base_dir / "models" / "crisis_model.pkl"
    encoder_path = base_dir / "models" / "label_encoder.pkl"
    features_path = base_dir / "models" / "feature_columns.pkl"
    text_vectorizer_path = base_dir / "models" / "text_vectorizer.pkl"
    text_risk_model_path = base_dir / "models" / "text_risk_model.pkl"
    medium_subtype_model_path = base_dir / "models" / "medium_subtype_model.pkl"
    medium_subtype_labels_path = base_dir / "models" / "medium_subtype_labels.pkl"
    high_subtype_model_path = base_dir / "models" / "high_subtype_model.pkl"
    high_subtype_labels_path = base_dir / "models" / "high_subtype_labels.pkl"

    try:
        model = joblib.load(model_path)
        label_encoder = joblib.load(encoder_path)
        feature_columns = joblib.load(features_path)
        print("Numeric model artifacts loaded successfully from models/.")
    except FileNotFoundError:
        model = None
        label_encoder = None
        feature_columns = None
        print("Numeric model files not found under models/. Using heuristic fallback until you run training/train_model.py")
    except Exception as exc:
        model = None
        label_encoder = None
        feature_columns = None
        print(f"Numeric model loading failed: {exc}. Using heuristic fallback.")

    try:
        text_vectorizer = joblib.load(text_vectorizer_path)
        text_risk_model = joblib.load(text_risk_model_path)
        if medium_subtype_model_path.exists():
            medium_subtype_model = joblib.load(medium_subtype_model_path)
        else:
            medium_subtype_model = None
        if medium_subtype_labels_path.exists():
            medium_subtype_labels = joblib.load(medium_subtype_labels_path)
        else:
            medium_subtype_labels = None
        if high_subtype_model_path.exists():
            high_subtype_model = joblib.load(high_subtype_model_path)
        else:
            high_subtype_model = None
        if high_subtype_labels_path.exists():
            high_subtype_labels = joblib.load(high_subtype_labels_path)
        else:
            high_subtype_labels = None
        print("Text model artifacts loaded successfully from models/.")
    except FileNotFoundError:
        text_vectorizer = None
        text_risk_model = None
        medium_subtype_model = None
        medium_subtype_labels = None
        high_subtype_model = None
        high_subtype_labels = None
        print("Text model files not found under models/. Text inference disabled until training/train_model.py runs.")
    except Exception as exc:
        text_vectorizer = None
        text_risk_model = None
        medium_subtype_model = None
        medium_subtype_labels = None
        high_subtype_model = None
        high_subtype_labels = None
        print(f"Text model loading failed: {exc}. Text inference disabled.")


@asynccontextmanager
async def lifespan(_app: FastAPI):
    load_model_artifacts()
    yield


app = FastAPI(
    title="Serenity Monitoring AI — Crisis risk",
    version="1.1.0",
    lifespan=lifespan,
)

# allow_credentials must be False when allow_origins is "*"
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:4200",
        "http://localhost:8085",
        "http://localhost:8081",
        "http://127.0.0.1:8085",
    ],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)


def build_risk_message(risk_level: str) -> tuple[str, str, int]:
    if risk_level == "HIGH_RISK":
        return (
            "CRITICAL: Patient is showing severe crisis indicators across multiple dimensions this week.",
            "Immediate teleconsultation required within 24 hours. Consider emergency protocol if patient is unreachable.",
            3,
        )
    if risk_level == "MEDIUM_RISK":
        return (
            "ATTENTION: Patient is showing moderate distress signals that require close monitoring.",
            "Schedule a session within 3-5 days. Review recent mood entries and trigger patterns.",
            2,
        )
    return (
        "STABLE: Patient is showing healthy mood patterns and consistent engagement.",
        "Continue regular monitoring schedule. Next scheduled session as planned.",
        1,
    )


SUICIDE_PATTERNS = [
    r"\bkill\s+myself\b",
    r"\bend\s+my\s+life\b",
    r"\bwant\s+to\s+die\b",
    r"\bdon[' ]?t\s+want\s+to\s+live\b",
    r"\bgoing\s+to\s+do\s+something\s+bad\s+to\s+myself\b",
    r"\bdo\s+something\s+bad\s+to\s+myself\b",
    r"\bdo\s+something\s+to\s+myself\b",
    r"\bcommit\s+suicide\b",
    r"\bsuicid(?:e|al)\b",
    r"\bself\s*harm\b",
    r"\bhurt\s+myself\b",
    r"\bme\s+suicider\b",
    r"\bje\s+vais\s+me\s+suicider\b",
    r"\bje\s+veux\s+mourir\b",
    r"\bme\s+tuer\b",
]

SEVERE_DISTRESS_PATTERNS = [
    r"\bhopeless\b",
    r"\bworthless\b",
    r"\bcan[' ]?t\s+go\s+on\b",
    r"\bbreakdown\b",
    r"\bpanic\s+attack\b",
    r"\bje\s+n[' ]?en\s+peux\s+plus\b",
    r"\bdepressed\b",
    r"\bvery\s+sad\b",
]

DEPRESSION_IMPAIRMENT_PATTERNS = [
    r"\b(feel(?:ing)?\s+)?tired\b",
    r"\bfatigue(?:d)?\b",
    r"\bno\s+energy\b",
    r"\bexhausted\b",
    r"\bdon[' ]?t\s+want\s+to\s+do\s+(?:anything|nothing)\b",
    r"\bdon[' ]?t\s+want\s+to\s+move\b",
    r"\bdon[' ]?t\s+want\s+to\s+get\s+up\b",
    r"\bnothing\s+matters\b",
]

PROTECTIVE_PATTERNS = [
    r"\bi\s+feel\s+better\b",
    r"\bi\s+am\s+safe\b",
    r"\bcalm\b",
    r"\bhopeful\b",
    r"\bsupported\b",
    r"\bthankful\b",
    r"\bgrateful\b",
    r"\bgoing\s+well\b",
    r"\ball\s+good\b",
    r"\brelaxed\b",
    r"\bje\s+vais\s+bien\b",
    r"\bca\s+va\s+bien\b",
]

RISKY_TRIGGER_PATTERNS = [
    r"\bfight\b",
    r"\bviolence\b",
    r"\babuse\b",
    r"\bharassment\b",
    r"\btrauma\b",
    r"\bbullying\b",
    r"\bself\s*harm\b",
]

NEGATION_PATTERNS = [
    r"\bnot\s+going\s+to\s+kill\s+myself\b",
    r"\bnot\s+suicidal\b",
    r"\bi\s+am\s+safe\b",
    r"\bpas\s+suicidaire\b",
]


def normalize_text(text: str) -> str:
    cleaned = re.sub(r"[^a-z0-9\s']", " ", text.lower())
    return re.sub(r"\s+", " ", cleaned).strip()


def detect_suicidal_language(text: str | None) -> list[str]:
    if not text:
        return []
    normalized = normalize_text(text)
    if not normalized:
        return []

    for neg in NEGATION_PATTERNS:
        if re.search(neg, normalized):
            return []

    matches: list[str] = []
    for pattern in SUICIDE_PATTERNS:
        if re.search(pattern, normalized):
            matches.append(pattern)
    return matches


def detect_pattern_matches(text: str | None, patterns: list[str]) -> list[str]:
    if not text:
        return []
    normalized = normalize_text(text)
    if not normalized:
        return []

    matches: list[str] = []
    for pattern in patterns:
        if re.search(pattern, normalized):
            matches.append(pattern)
    return matches


def combine_text(*chunks: str | None) -> str:
    return " || ".join(c.strip() for c in chunks if c and c.strip())


RISK_TO_SCORE = {
    "LOW_RISK": 1,
    "MEDIUM_RISK": 2,
    "HIGH_RISK": 3,
}

SCORE_TO_RISK = {
    1: "LOW_RISK",
    2: "MEDIUM_RISK",
    3: "HIGH_RISK",
}


def with_risk_level(
    prediction: CrisisPredictionResponse,
    risk_level: str,
    confidence: float,
    reason: str,
) -> CrisisPredictionResponse:
    message, recommendation, risk_score = build_risk_message(risk_level)
    return CrisisPredictionResponse(
        patient_id=prediction.patient_id,
        risk_level=risk_level,
        confidence=round(float(confidence), 2),
        message=f"{message} [{reason}]",
        recommendation=recommendation,
        risk_score=risk_score,
        risk_type=prediction.risk_type,
        medium_risk_type=prediction.medium_risk_type if risk_level == "MEDIUM_RISK" else None,
    )


def apply_text_override(
    prediction: CrisisPredictionResponse,
    request: CrisisPredictionRequest,
) -> CrisisPredictionResponse:
    full_text = combine_text(request.mood_description_text, request.trigger_text)

    suicidal_matches = detect_suicidal_language(full_text)
    severe_matches = detect_pattern_matches(full_text, SEVERE_DISTRESS_PATTERNS)
    depressive_impairment_matches = detect_pattern_matches(full_text, DEPRESSION_IMPAIRMENT_PATTERNS)
    risky_trigger_matches = detect_pattern_matches(full_text, RISKY_TRIGGER_PATTERNS)
    protective_matches = detect_pattern_matches(full_text, PROTECTIVE_PATTERNS)

    if suicidal_matches:
        return CrisisPredictionResponse(
            patient_id=request.patient_id,
            risk_level="HIGH_RISK",
            confidence=max(prediction.confidence, 0.99),
            message=(
                "CRITICAL: Suicidal/self-harm language detected in patient text. "
                "Escalate immediately."
            ),
            recommendation=(
                "Immediate safety protocol required: urgent contact now, same-day psychiatrist review, "
                "and emergency services if patient is unreachable or has active plan/means."
            ),
            risk_score=3,
            risk_type="SUICIDAL_CRISIS",
            medium_risk_type=None,
        )

    base_score = RISK_TO_SCORE.get(prediction.risk_level, prediction.risk_score)

    # Contradiction scenario: high numeric mood but alarming language.
    contradiction = request.avg_mood_7days >= 8.0 and bool(severe_matches or risky_trigger_matches)
    if contradiction and base_score < 3:
        return with_risk_level(
            prediction,
            "HIGH_RISK",
            max(prediction.confidence, 0.9),
            "text_numeric_contradiction_escalation",
        )

    # Escalate to at least medium for strong distress text or risky trigger narratives.
    if (severe_matches or risky_trigger_matches) and base_score < 2:
        return with_risk_level(
            prediction,
            "MEDIUM_RISK",
            max(prediction.confidence, 0.8),
            "distress_text_escalation",
        )

    # Functional-impairment language should not remain LOW, even with high numeric mood.
    if depressive_impairment_matches and base_score < 2:
        floor_confidence = 0.85 if request.avg_mood_7days >= 7.5 else 0.78
        return with_risk_level(
            prediction,
            "MEDIUM_RISK",
            max(prediction.confidence, floor_confidence),
            "depressive_impairment_escalation",
        )

    # Guarded one-step de-escalation only when clinical numeric signals are stable.
    stable_numeric = (
        request.crisis_entries_count == 0
        and request.trigger_intensity_avg <= 3.0
        and request.days_of_silence <= 2
        and request.mood_trend >= 0
        and request.min_mood_7days >= 2
    )
    strong_protective_text = len(protective_matches) >= 2
    has_distress = bool(severe_matches or risky_trigger_matches or depressive_impairment_matches)

    if stable_numeric and strong_protective_text and not has_distress and base_score >= 2:
        downgraded_score = base_score - 1
        downgraded_level = SCORE_TO_RISK[downgraded_score]
        return with_risk_level(
            prediction,
            downgraded_level,
            min(prediction.confidence, 0.72),
            "guarded_protective_deescalation",
        )

    return prediction


def predict_with_text_model(request: CrisisPredictionRequest) -> CrisisPredictionResponse | None:
    if text_vectorizer is None or text_risk_model is None:
        return None

    text = combine_text(request.mood_description_text, request.trigger_text)
    if not text:
        return None

    features = text_vectorizer.transform([text])
    pred = text_risk_model.predict(features)[0]
    probabilities = text_risk_model.predict_proba(features)[0]
    confidence = float(np.max(probabilities))
    risk_level = str(pred)

    message, recommendation, risk_score = build_risk_message(risk_level)
    risk_type = None
    medium_risk_type = None
    if risk_level == "MEDIUM_RISK" and medium_subtype_model is not None:
        subtype_pred = medium_subtype_model.predict(features)[0]
        risk_type = str(subtype_pred)
        medium_risk_type = risk_type
    elif risk_level == "HIGH_RISK" and high_subtype_model is not None:
        subtype_pred = high_subtype_model.predict(features)[0]
        risk_type = str(subtype_pred)

    return CrisisPredictionResponse(
        patient_id=request.patient_id,
        risk_level=risk_level,
        confidence=round(confidence, 2),
        message=message + " [text_model]",
        recommendation=recommendation,
        risk_score=risk_score,
        risk_type=risk_type,
        medium_risk_type=medium_risk_type,
    )


def fuse_numeric_and_text(
    numeric_prediction: CrisisPredictionResponse,
    text_prediction: CrisisPredictionResponse | None,
) -> CrisisPredictionResponse:
    if text_prediction is None:
        return numeric_prediction

    numeric_score = RISK_TO_SCORE.get(numeric_prediction.risk_level, numeric_prediction.risk_score)
    text_score = RISK_TO_SCORE.get(text_prediction.risk_level, text_prediction.risk_score)

    # Strong text alert can override larger numeric/text disagreements.
    if text_score >= 3 and text_prediction.confidence >= 0.72 and numeric_score <= 1:
        return with_risk_level(text_prediction, "HIGH_RISK", max(text_prediction.confidence, 0.9), "text_alert_override")

    blended_score = int(np.clip(np.round(0.62 * numeric_score + 0.38 * text_score), 1, 3))
    blended_level = SCORE_TO_RISK[blended_score]
    blended_confidence = round(float(0.65 * numeric_prediction.confidence + 0.35 * text_prediction.confidence), 2)

    message, recommendation, risk_score = build_risk_message(blended_level)
    risk_type = text_prediction.risk_type if text_prediction.risk_level == blended_level else None
    medium_risk_type = risk_type if blended_level == "MEDIUM_RISK" else None

    return CrisisPredictionResponse(
        patient_id=numeric_prediction.patient_id,
        risk_level=blended_level,
        confidence=blended_confidence,
        message=message + " [hybrid_numeric_text]",
        recommendation=recommendation,
        risk_score=risk_score,
        risk_type=risk_type,
        medium_risk_type=medium_risk_type,
    )


def predict_with_sklearn(request: CrisisPredictionRequest) -> CrisisPredictionResponse:
    if model is None or label_encoder is None or feature_columns is None:
        raise RuntimeError("sklearn artifacts not loaded")

    payload = request.model_dump()
    ordered_features = [payload[column] for column in feature_columns]
    features_df = pd.DataFrame([ordered_features], columns=feature_columns)

    prediction = model.predict(features_df)
    probabilities = model.predict_proba(features_df)

    confidence = round(float(np.max(probabilities[0])), 2)
    risk_level = str(label_encoder.inverse_transform(prediction)[0])
    message, recommendation, risk_score = build_risk_message(risk_level)

    return CrisisPredictionResponse(
        patient_id=request.patient_id,
        risk_level=risk_level,
        confidence=confidence,
        message=message,
        recommendation=recommendation,
        risk_score=risk_score,
        risk_type=None,
        medium_risk_type=None,
    )


def predict_with_heuristic(request: CrisisPredictionRequest) -> CrisisPredictionResponse:
    """Rule-based fallback when model/*.pkl is missing (aligned with training semantics)."""
    s = 0
    if request.avg_mood_7days <= 3.5:
        s += 3
    elif request.avg_mood_7days <= 6.0:
        s += 1
    if request.crisis_entries_count >= 5:
        s += 3
    elif request.crisis_entries_count >= 2:
        s += 1
    if request.days_of_silence >= 5:
        s += 2
    elif request.days_of_silence >= 2:
        s += 1
    if request.trigger_intensity_avg >= 7.0:
        s += 2
    elif request.trigger_intensity_avg >= 4.0:
        s += 1
    if request.mood_trend == -1:
        s += 1
    if request.min_mood_7days <= 2:
        s += 2
    elif request.min_mood_7days <= 4:
        s += 1

    if s >= 8:
        risk = "HIGH_RISK"
        confidence = 0.75
    elif s >= 4:
        risk = "MEDIUM_RISK"
        confidence = 0.65
    else:
        risk = "LOW_RISK"
        confidence = 0.70

    message, recommendation, risk_score = build_risk_message(risk)
    return CrisisPredictionResponse(
        patient_id=request.patient_id,
        risk_level=risk,
        confidence=confidence,
        message=message + " (heuristic)",
        recommendation=recommendation,
        risk_score=risk_score,
        risk_type=None,
        medium_risk_type=None,
    )


def predict_one_patient(request: CrisisPredictionRequest) -> CrisisPredictionResponse:
    base_prediction: CrisisPredictionResponse
    if model is not None and label_encoder is not None and feature_columns is not None:
        try:
            base_prediction = predict_with_sklearn(request)
        except Exception as exc:
            print(f"sklearn inference failed, falling back to heuristic: {exc}")
            base_prediction = predict_with_heuristic(request)
    else:
        base_prediction = predict_with_heuristic(request)

    text_prediction = predict_with_text_model(request)
    hybrid_prediction = fuse_numeric_and_text(base_prediction, text_prediction)
    return apply_text_override(hybrid_prediction, request)


@app.get("/health")
def health_check() -> dict:
    return {
        "service": "monitoring-ai",
        "status": "ok",
        "model_loaded": model is not None,
        "text_model_loaded": text_risk_model is not None and text_vectorizer is not None,
        "port": SERVICE_PORT,
        "version": "1.1.0",
    }


@app.post("/predict/crisis", response_model=CrisisPredictionResponse)
def predict_crisis(request: CrisisPredictionRequest) -> CrisisPredictionResponse:
    return predict_one_patient(request)


@app.post("/predict/crisis/batch", response_model=BatchPredictionResponse)
def predict_crisis_batch(request: BatchPredictionRequest) -> BatchPredictionResponse:
    predictions = [predict_one_patient(patient) for patient in request.patients]
    return BatchPredictionResponse(predictions=predictions)


@app.get("/model/info")
def model_info() -> dict:
    loaded = model is not None and label_encoder is not None and feature_columns is not None
    text_loaded = text_risk_model is not None and text_vectorizer is not None
    return {
        "model_type": "Hybrid(RandomForest+LogisticRegression+Rules)",
        "features": feature_columns if loaded else [],
        "labels": ["HIGH_RISK", "MEDIUM_RISK", "LOW_RISK"],
        "medium_risk_types": medium_subtype_labels if medium_subtype_labels else [],
        "high_risk_types": high_subtype_labels if high_subtype_labels else [],
        "status": "loaded" if loaded else "heuristic_only",
        "text_status": "loaded" if text_loaded else "numeric_only",
    }
