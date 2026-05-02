# Serenity Monitoring AI (`monitoring -ai`)

Python FastAPI service that predicts **HIGH_RISK / MEDIUM_RISK / LOW_RISK** from rolling 7‑day mood features. Intended to run on **port 5150** (separate from `claim-risk-model` on 5123 and other AI services).

## Safety behavior (important)

The service now uses a **hybrid decision**:
- Numeric model (existing features) for baseline risk.
- Text safety policy from `mood_description_text` and `trigger_text`.
- Trained text classifier (TF-IDF + Logistic Regression) for LOW/MEDIUM/HIGH.
- Medium-risk subtype prediction (present only when risk is `MEDIUM_RISK`).

Policy behaviors:
- Suicidal/self-harm language => forced `HIGH_RISK`.
- Contradiction (very high numeric mood + alarming text) => escalation.
- Distress/risky trigger language => escalation to at least `MEDIUM_RISK`.
- Guarded de-escalation by one level only when numeric signals are stable and text is strongly protective.

## Prerequisites

- Python 3.10+
- Trained artifacts under `models/`:
  - `crisis_model.pkl`
  - `label_encoder.pkl`
  - `feature_columns.pkl`
  - `text_vectorizer.pkl`
  - `text_risk_model.pkl`
  - `medium_subtype_model.pkl` (optional)
  - `medium_subtype_labels.pkl` (optional)

Generate them with:

```bash
cd "Serenity/services/monitoring -ai"
pip install -r requirements.txt
python training/generate_data.py
python training/train_model.py --combined-csv "C:\Users\Rayen\Desktop\pi\Serenity\services\monitoring -ai\training\Combined Data.csv" --notebook "C:\Users\Rayen\Desktop\pi\Serenity\services\monitoring -ai\training\mental-health-sentiment-analysis-nlp-ml.ipynb"
```

If `models/` is missing, the API still responds using a **heuristic** fallback (same JSON shape).

Training also writes a metrics report: `training/training_report.json`.

## Run (port 5150)

```bash
cd "Serenity/services/monitoring -ai"
uvicorn main:app --host 0.0.0.0 --port 5150 --reload
```

Or:

```bash
set PORT=5150
uvicorn main:app --host 0.0.0.0 --port %PORT%
```

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/health` | Liveness + `model_loaded` |
| POST | `/predict/crisis` | Single patient feature vector → risk |
| POST | `/predict/crisis/batch` | Batch |
| GET | `/model/info` | Model metadata |

### POST `/predict/crisis` (example)

```json
{
  "patient_id": 1,
  "avg_mood_7days": 4.2,
  "crisis_entries_count": 2,
  "days_of_silence": 1,
  "trigger_intensity_avg": 5.0,
  "mood_trend": -1,
  "min_mood_7days": 2,
  "trigger_count": 3,
  "total_entries": 5,
  "mood_description_text": "I feel trapped and I want to end my life",
  "trigger_text": "fight at school; conflict with family"
}
```

Spring Boot **monitoring-service** (8085) calls this URL after each new mood entry when `app.monitoring-ai.enabled=true`.

Response includes `medium_risk_type` only when `risk_level` is `MEDIUM_RISK`.

