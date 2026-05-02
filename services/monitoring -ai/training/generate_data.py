import numpy as np
import pandas as pd
from pathlib import Path

# Fixed seed for reproducible synthetic data.
np.random.seed(42)


def add_noise(value: float, min_bound: float, max_bound: float) -> float:
    """Add small Gaussian noise to float features and keep values in bounds."""
    noise = np.random.normal(0, 0.3)
    return float(np.clip(value + noise, min_bound, max_bound))


def apply_consistency_rules(row: dict) -> dict:
    """Apply clinical consistency rules to each generated patient-week row."""
    if row["days_of_silence"] >= 5:
        row["label"] = "HIGH_RISK"

    if row["crisis_entries_count"] >= 5:
        row["label"] = "HIGH_RISK"

    if row["avg_mood_7days"] >= 8.0:
        row["label"] = "LOW_RISK"

    if row["min_mood_7days"] == 1 and row["crisis_entries_count"] >= 3:
        row["label"] = "HIGH_RISK"

    # Cannot have more crisis entries than total entries.
    if row["total_entries"] < row["crisis_entries_count"]:
        row["total_entries"] = row["crisis_entries_count"]

    # If there are zero logs in a week, all 7 days are silent.
    if row["total_entries"] == 0:
        row["days_of_silence"] = 7

    # Keep safety bounds after rule adjustments.
    row["total_entries"] = int(np.clip(row["total_entries"], 0, 7))
    row["days_of_silence"] = int(np.clip(row["days_of_silence"], 0, 7))
    row["crisis_entries_count"] = int(np.clip(row["crisis_entries_count"], 0, 7))
    row["min_mood_7days"] = int(np.clip(row["min_mood_7days"], 1, 10))
    row["trigger_count"] = int(np.clip(row["trigger_count"], 0, 15))

    return row


def generate_high_risk_row() -> dict:
    row = {
        "avg_mood_7days": add_noise(np.random.uniform(1.0, 4.2), 1.0, 4.2),
        "crisis_entries_count": int(np.random.randint(3, 8)),
        "days_of_silence": int(np.random.randint(2, 6)),
        "trigger_intensity_avg": add_noise(np.random.uniform(6.5, 10.0), 6.5, 10.0),
        "mood_trend": int(np.random.choice([-1, -1, -1, 0], p=[0.7, 0.1, 0.1, 0.1])),
        "min_mood_7days": int(np.random.randint(1, 3)),
        "trigger_count": int(np.random.randint(4, 11)),
        "total_entries": int(np.random.randint(2, 6)),
        "label": "HIGH_RISK",
    }
    return apply_consistency_rules(row)


def generate_medium_risk_row() -> dict:
    row = {
        "avg_mood_7days": add_noise(np.random.uniform(3.8, 6.8), 3.8, 6.8),
        "crisis_entries_count": int(np.random.randint(0, 3)),
        "days_of_silence": int(np.random.randint(0, 3)),
        "trigger_intensity_avg": add_noise(np.random.uniform(3.5, 7.5), 3.5, 7.5),
        "mood_trend": int(np.random.choice([-1, 0, 1], p=[0.3, 0.5, 0.2])),
        "min_mood_7days": int(np.random.randint(2, 6)),
        "trigger_count": int(np.random.randint(1, 6)),
        "total_entries": int(np.random.randint(3, 7)),
        "label": "MEDIUM_RISK",
    }
    return apply_consistency_rules(row)


def generate_low_risk_row() -> dict:
    row = {
        "avg_mood_7days": add_noise(np.random.uniform(6.0, 10.0), 6.0, 10.0),
        "crisis_entries_count": int(np.random.randint(0, 2)),
        "days_of_silence": int(np.random.randint(0, 2)),
        "trigger_intensity_avg": add_noise(np.random.uniform(0.0, 4.5), 0.0, 4.5),
        "mood_trend": int(np.random.choice([0, 1, 1], p=[0.2, 0.4, 0.4])),
        "min_mood_7days": int(np.random.randint(5, 10)),
        "trigger_count": int(np.random.randint(0, 3)),
        "total_entries": int(np.random.randint(5, 8)),
        "label": "LOW_RISK",
    }
    return apply_consistency_rules(row)


def generate_high_medium_borderline_row() -> dict:
    """Generate hard borderline samples between high and medium risk."""
    row = {
        "avg_mood_7days": add_noise(np.random.uniform(3.5, 5.0), 3.5, 5.0),
        "crisis_entries_count": int(np.random.randint(2, 4)),
        "days_of_silence": int(np.random.randint(1, 4)),
        "trigger_intensity_avg": add_noise(np.random.uniform(5.0, 8.0), 0.0, 10.0),
        "mood_trend": int(np.random.choice([-1, 0], p=[0.6, 0.4])),
        "min_mood_7days": int(np.random.randint(1, 5)),
        "trigger_count": int(np.random.randint(2, 8)),
        "total_entries": int(np.random.randint(3, 6)),
        "label": str(np.random.choice(["HIGH_RISK", "MEDIUM_RISK"])),
    }
    return apply_consistency_rules(row)


def generate_medium_low_borderline_row() -> dict:
    """Generate easier-to-confuse samples between medium and low risk."""
    row = {
        "avg_mood_7days": add_noise(np.random.uniform(5.5, 7.0), 5.5, 7.0),
        "crisis_entries_count": int(np.random.randint(0, 2)),
        "days_of_silence": int(np.random.randint(0, 3)),
        "trigger_intensity_avg": add_noise(np.random.uniform(2.0, 5.5), 0.0, 10.0),
        "mood_trend": int(np.random.choice([0, 1, -1], p=[0.45, 0.4, 0.15])),
        "min_mood_7days": int(np.random.randint(3, 8)),
        "trigger_count": int(np.random.randint(0, 5)),
        "total_entries": int(np.random.randint(4, 8)),
        "label": str(np.random.choice(["MEDIUM_RISK", "LOW_RISK"])),
    }
    return apply_consistency_rules(row)


def main() -> None:
    rows = []

    # Base balanced dataset: 1000 rows per class.
    for _ in range(1000):
        rows.append(generate_high_risk_row())
    for _ in range(1000):
        rows.append(generate_medium_risk_row())
    for _ in range(1000):
        rows.append(generate_low_risk_row())

    # Add overlap zone for realistic ambiguity.
    for _ in range(100):
        rows.append(generate_high_medium_borderline_row())
    for _ in range(100):
        rows.append(generate_medium_low_borderline_row())

    df = pd.DataFrame(rows)
    df = df.sample(frac=1.0, random_state=42).reset_index(drop=True)

    output_path = Path(__file__).resolve().parent / "crisis_training_data.csv"
    df.to_csv(output_path, index=False)

    print(f"Total rows generated: {len(df)}")
    print("\nLabel distribution:")
    print(df["label"].value_counts())

    feature_columns = [
        "avg_mood_7days",
        "crisis_entries_count",
        "days_of_silence",
        "trigger_intensity_avg",
        "mood_trend",
        "min_mood_7days",
        "trigger_count",
        "total_entries",
    ]

    print("\nFeature statistics (mean, min, max):")
    print(df[feature_columns].agg(["mean", "min", "max"]).T)

    print("\nFirst 15 rows:")
    print(df.head(15))

    print("\nData generation complete. Ready for training.")


if __name__ == "__main__":
    main()

