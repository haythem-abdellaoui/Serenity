from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder

NUMERIC_FEATURE_COLUMNS = [
    "avg_mood_7days",
    "crisis_entries_count",
    "days_of_silence",
    "trigger_intensity_avg",
    "mood_trend",
    "min_mood_7days",
    "trigger_count",
    "total_entries",
]

STATUS_TO_RISK = {
    "Normal": "LOW_RISK",
    "Anxiety": "MEDIUM_RISK",
    "Depression": "MEDIUM_RISK",
    "Stress": "MEDIUM_RISK",
    "Personality disorder": "MEDIUM_RISK",
    "Bipolar": "HIGH_RISK",
    "Suicidal": "HIGH_RISK",
}

STATUS_TO_MEDIUM_SUBTYPE = {
    "Anxiety": "ANXIETY_DISTRESS",
    "Depression": "DEPRESSIVE_MOOD",
    "Stress": "STRESS_OVERLOAD",
    "Personality disorder": "PERSONALITY_DYSREGULATION",
}

STATUS_TO_HIGH_SUBTYPE = {
    "Suicidal": "SUICIDAL_CRISIS",
    "Bipolar": "BIPOLAR_EPISODE",
}


def clean_text(text: str) -> str:
    text = str(text).lower().strip()
    text = re.sub(r"https?://\S+|www\.\S+", " ", text)
    text = re.sub(r"[^a-z0-9\s']", " ", text)
    text = re.sub(r"\s+", " ", text)
    return text.strip()


def random_oversample_text(X_train: pd.Series, y_train: pd.Series, random_state: int = 42) -> tuple[pd.Series, pd.Series]:
    """Notebook-aligned random over-sampling on training split only."""
    train_df = pd.DataFrame({"text": X_train.astype(str), "label": y_train.astype(str)})
    max_count = train_df["label"].value_counts().max()

    balanced_parts = []
    for label, group in train_df.groupby("label"):
        if len(group) < max_count:
            sampled = group.sample(n=max_count, replace=True, random_state=random_state)
        else:
            sampled = group
        balanced_parts.append(sampled)

    balanced = pd.concat(balanced_parts, ignore_index=True).sample(frac=1.0, random_state=random_state)
    return balanced["text"], balanced["label"]


def train_numeric_model(base_dir: Path, model_dir: Path, report: dict) -> None:
    data_path = base_dir / "training" / "crisis_training_data.csv"
    df = pd.read_csv(data_path)
    X = df[NUMERIC_FEATURE_COLUMNS]
    y = df["label"]

    label_encoder = LabelEncoder()
    y_encoded = label_encoder.fit_transform(y)

    X_train, X_test, y_train, y_test = train_test_split(
        X,
        y_encoded,
        test_size=0.2,
        random_state=42,
        stratify=y_encoded,
    )

    model = RandomForestClassifier(
        n_estimators=220,
        max_depth=15,
        min_samples_split=5,
        random_state=42,
        class_weight="balanced",
    )
    model.fit(X_train, y_train)

    y_pred = model.predict(X_test)
    accuracy = float(accuracy_score(y_test, y_pred))

    report["numeric_model"] = {
        "dataset_rows": int(len(df)),
        "accuracy": round(accuracy, 4),
        "classification_report": classification_report(
            y_test,
            y_pred,
            target_names=label_encoder.classes_.tolist(),
            output_dict=True,
            digits=4,
        ),
        "confusion_matrix": confusion_matrix(
            y_test,
            y_pred,
            labels=list(range(len(label_encoder.classes_))),
        ).tolist(),
    }

    print(f"[numeric] accuracy={accuracy:.4f} rows={len(df)}")
    joblib.dump(model, model_dir / "crisis_model.pkl")
    joblib.dump(label_encoder, model_dir / "label_encoder.pkl")
    joblib.dump(NUMERIC_FEATURE_COLUMNS, model_dir / "feature_columns.pkl")


def train_text_models(
    base_dir: Path,
    model_dir: Path,
    combined_path: Path,
    notebook_path: Path,
    report: dict,
    use_oversampling: bool,
) -> None:
    raw = pd.read_csv(combined_path)
    df = raw[["statement", "status"]].copy()
    df["status"] = df["status"].astype(str).str.strip()
    df["text"] = df["statement"].astype(str).map(clean_text)
    df = df[df["text"].str.len() > 3]

    df["risk_label"] = df["status"].map(STATUS_TO_RISK)
    df = df.dropna(subset=["risk_label"])
    df["medium_subtype"] = df["status"].map(STATUS_TO_MEDIUM_SUBTYPE)
    df["high_subtype"] = df["status"].map(STATUS_TO_HIGH_SUBTYPE)

    X_train, X_test, y_train, y_test = train_test_split(
        df["text"],
        df["risk_label"],
        test_size=0.2,
        random_state=42,
        stratify=df["risk_label"],
    )

    if use_oversampling:
        X_train, y_train = random_oversample_text(X_train, y_train)

    vectorizer = TfidfVectorizer(ngram_range=(1, 2), max_features=50000, min_df=2)
    X_train_vec = vectorizer.fit_transform(X_train)
    X_test_vec = vectorizer.transform(X_test)

    risk_model = LogisticRegression(
        max_iter=1200,
        class_weight="balanced",
        random_state=42,
    )
    risk_model.fit(X_train_vec, y_train)

    y_pred = risk_model.predict(X_test_vec)
    accuracy = float(accuracy_score(y_test, y_pred))

    medium_df = df[df["medium_subtype"].notna()].copy()
    medium_subtype_model = None
    medium_subtype_labels: list[str] = []
    medium_subtype_accuracy = None

    if len(medium_df) >= 200:
        X_m_train, X_m_test, y_m_train, y_m_test = train_test_split(
            medium_df["text"],
            medium_df["medium_subtype"],
            test_size=0.2,
            random_state=42,
            stratify=medium_df["medium_subtype"],
        )
        X_m_train_vec = vectorizer.transform(X_m_train)
        X_m_test_vec = vectorizer.transform(X_m_test)

        medium_subtype_model = LogisticRegression(max_iter=1000, class_weight="balanced", random_state=42)
        medium_subtype_model.fit(X_m_train_vec, y_m_train)
        y_m_pred = medium_subtype_model.predict(X_m_test_vec)
        medium_subtype_accuracy = float(accuracy_score(y_m_test, y_m_pred))
        medium_subtype_labels = sorted(medium_df["medium_subtype"].unique().tolist())
        print(f"[text-medium-subtype] accuracy={medium_subtype_accuracy:.4f} rows={len(medium_df)}")

    high_df = df[df["high_subtype"].notna()].copy()
    high_subtype_model = None
    high_subtype_labels: list[str] = []
    high_subtype_accuracy = None

    if len(high_df) >= 200:
        X_h_train, X_h_test, y_h_train, y_h_test = train_test_split(
            high_df["text"],
            high_df["high_subtype"],
            test_size=0.2,
            random_state=42,
            stratify=high_df["high_subtype"],
        )
        X_h_train_vec = vectorizer.transform(X_h_train)
        X_h_test_vec = vectorizer.transform(X_h_test)

        high_subtype_model = LogisticRegression(max_iter=1000, class_weight="balanced", random_state=42)
        high_subtype_model.fit(X_h_train_vec, y_h_train)
        y_h_pred = high_subtype_model.predict(X_h_test_vec)
        high_subtype_accuracy = float(accuracy_score(y_h_test, y_h_pred))
        high_subtype_labels = sorted(high_df["high_subtype"].unique().tolist())
        print(f"[text-high-subtype] accuracy={high_subtype_accuracy:.4f} rows={len(high_df)}")

    notebook_signals = {
        "exists": notebook_path.exists(),
        "used_for_reference": "TF-IDF and label conventions from the attached notebook",
    }

    report["text_model"] = {
        "dataset_rows": int(len(df)),
        "oversampling": bool(use_oversampling),
        "raw_status_distribution": raw["status"].astype(str).str.strip().value_counts().to_dict(),
        "risk_distribution": df["risk_label"].value_counts().to_dict(),
        "accuracy": round(accuracy, 4),
        "classification_report": classification_report(
            y_test,
            y_pred,
            labels=["HIGH_RISK", "MEDIUM_RISK", "LOW_RISK"],
            output_dict=True,
            digits=4,
            zero_division=0,
        ),
        "confusion_matrix": confusion_matrix(
            y_test,
            y_pred,
            labels=["HIGH_RISK", "MEDIUM_RISK", "LOW_RISK"],
        ).tolist(),
        "medium_subtype_accuracy": None if medium_subtype_accuracy is None else round(medium_subtype_accuracy, 4),
        "medium_subtypes": medium_subtype_labels,
        "high_subtype_accuracy": None if high_subtype_accuracy is None else round(high_subtype_accuracy, 4),
        "high_subtypes": high_subtype_labels,
        "notebook_reference": notebook_signals,
    }

    print(f"[text-risk] accuracy={accuracy:.4f} rows={len(df)}")
    joblib.dump(vectorizer, model_dir / "text_vectorizer.pkl")
    joblib.dump(risk_model, model_dir / "text_risk_model.pkl")
    if medium_subtype_model is not None:
        joblib.dump(medium_subtype_model, model_dir / "medium_subtype_model.pkl")
        joblib.dump(medium_subtype_labels, model_dir / "medium_subtype_labels.pkl")
    if high_subtype_model is not None:
        joblib.dump(high_subtype_model, model_dir / "high_subtype_model.pkl")
        joblib.dump(high_subtype_labels, model_dir / "high_subtype_labels.pkl")


def parse_args(base_dir: Path) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Train Serenity monitoring hybrid risk models.")
    parser.add_argument(
        "--combined-csv",
        default=str(base_dir / "training" / "Combined Data.csv"),
        help="Path to Combined Data.csv (text dataset).",
    )
    parser.add_argument(
        "--notebook",
        default=str(base_dir / "training" / "mental-health-sentiment-analysis-nlp-ml.ipynb"),
        help="Path to reference notebook.",
    )
    parser.add_argument(
        "--report-out",
        default=str(base_dir / "training" / "training_report.json"),
        help="Output JSON report path.",
    )
    parser.add_argument(
        "--disable-oversampling",
        action="store_true",
        help="Disable notebook-style random over-sampling for text risk training.",
    )
    return parser.parse_args()


def main() -> None:
    base_dir = Path(__file__).resolve().parent.parent
    model_dir = base_dir / "models"
    model_dir.mkdir(parents=True, exist_ok=True)

    args = parse_args(base_dir)
    combined_path = Path(args.combined_csv)
    notebook_path = Path(args.notebook)
    report_out = Path(args.report_out)

    if not combined_path.exists():
        raise FileNotFoundError(f"Combined dataset not found: {combined_path}")

    report: dict = {}
    train_numeric_model(base_dir, model_dir, report)
    train_text_models(
        base_dir,
        model_dir,
        combined_path,
        notebook_path,
        report,
        use_oversampling=not args.disable_oversampling,
    )

    report_out.parent.mkdir(parents=True, exist_ok=True)
    report_out.write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(f"Training completed. Artifacts saved to {model_dir}")
    print(f"Training report saved to {report_out}")


if __name__ == "__main__":
    main()

