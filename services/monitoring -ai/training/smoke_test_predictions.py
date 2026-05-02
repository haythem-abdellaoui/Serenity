from __future__ import annotations

from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from main import CrisisPredictionRequest, predict_one_patient, load_model_artifacts


def run() -> None:
    load_model_artifacts()

    scenarios = [
        (
            "suicidal_text_high_override",
            CrisisPredictionRequest(
                patient_id=1,
                avg_mood_7days=9.4,
                crisis_entries_count=0,
                days_of_silence=0,
                trigger_intensity_avg=0.5,
                mood_trend=1,
                min_mood_7days=8,
                trigger_count=0,
                total_entries=6,
                mood_description_text="I am going to kill myself",
                trigger_text="",
            ),
        ),
        (
            "depressive_impairment_escalation",
            CrisisPredictionRequest(
                patient_id=2,
                avg_mood_7days=8.6,
                crisis_entries_count=0,
                days_of_silence=1,
                trigger_intensity_avg=1.2,
                mood_trend=0,
                min_mood_7days=7,
                trigger_count=1,
                total_entries=5,
                mood_description_text="im felling tired and i dont want to do nothing",
                trigger_text="stress and bad weather",
            ),
        ),
        (
            "medium_with_subtype",
            CrisisPredictionRequest(
                patient_id=3,
                avg_mood_7days=5.2,
                crisis_entries_count=0,
                days_of_silence=1,
                trigger_intensity_avg=2.0,
                mood_trend=0,
                min_mood_7days=3,
                trigger_count=2,
                total_entries=6,
                mood_description_text="I feel anxious and stressed about exams",
                trigger_text="academic pressure and stress",
            ),
        ),
    ]

    for name, request in scenarios:
        prediction = predict_one_patient(request)
        print(
            f"{name}: risk={prediction.risk_level}, "
            f"confidence={prediction.confidence}, "
            f"medium_risk_type={prediction.medium_risk_type}, "
            f"message={prediction.message}"
        )


if __name__ == "__main__":
    run()


