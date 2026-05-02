"""
============================================================
  MODEL TRAINING — Medical Severity Classification
  Compares: Random Forest, SVM, Naive Bayes
  Saves the best model + TF-IDF vectorizer
============================================================
"""

import os
import pandas as pd
import numpy as np
import matplotlib
matplotlib.use('Agg')  # Non-interactive backend
import matplotlib.pyplot as plt
import seaborn as sns
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.ensemble import RandomForestClassifier
from sklearn.svm import SVC
from sklearn.naive_bayes import MultinomialNB
from sklearn.metrics import (
    classification_report,
    confusion_matrix,
    accuracy_score,
)
from sklearn.preprocessing import LabelEncoder
import joblib

# ═══════════════════════════════════════════════════════════
#  1. LOAD & CLEAN DATA
# ═══════════════════════════════════════════════════════════

print("=" * 60)
print("  STEP 1: Loading & Cleaning Dataset")
print("=" * 60)

df = pd.read_csv("dataset/medical_severity.csv")

print(f"\nDataset shape: {df.shape}")
print(f"Columns: {list(df.columns)}")
print(f"\nSample rows:")
print(df.head(10).to_string(index=False))

# Check for missing values
print(f"\nMissing values:\n{df.isnull().sum()}")

# Drop duplicates
before = len(df)
df.drop_duplicates(inplace=True)
print(f"Removed {before - len(df)} duplicates. Remaining: {len(df)} rows.")

# Clean text
df["diagnosis"] = df["diagnosis"].str.strip().str.lower()

# Class distribution
print(f"\nClass distribution:")
print(df["severity"].value_counts().to_string())

# ═══════════════════════════════════════════════════════════
#  2. DATA VISUALIZATION
# ═══════════════════════════════════════════════════════════

print("\n" + "=" * 60)
print("  STEP 2: Data Visualization")
print("=" * 60)

os.makedirs("plots", exist_ok=True)

# Plot 1: Class distribution bar chart
fig, ax = plt.subplots(figsize=(8, 5))
colors = {"LOW": "#3b82f6", "MEDIUM": "#f59e0b", "HIGH": "#ef4444"}
counts = df["severity"].value_counts()
bars = ax.bar(counts.index, counts.values, color=[colors.get(s, "#888") for s in counts.index])
ax.set_title("Distribution of Severity Classes", fontsize=14, fontweight="bold")
ax.set_xlabel("Severity Level")
ax.set_ylabel("Number of Samples")
for bar, val in zip(bars, counts.values):
    ax.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 5, str(val),
            ha="center", fontweight="bold")
plt.tight_layout()
plt.savefig("plots/class_distribution.png", dpi=150)
plt.close()
print("Saved: plots/class_distribution.png")

# Plot 2: Diagnosis text length distribution
df["text_len"] = df["diagnosis"].str.len()
fig, ax = plt.subplots(figsize=(8, 5))
for sev in ["LOW", "MEDIUM", "HIGH"]:
    subset = df[df["severity"] == sev]["text_len"]
    ax.hist(subset, bins=20, alpha=0.6, label=sev, color=colors[sev])
ax.set_title("Diagnosis Text Length by Severity", fontsize=14, fontweight="bold")
ax.set_xlabel("Character Count")
ax.set_ylabel("Frequency")
ax.legend()
plt.tight_layout()
plt.savefig("plots/text_length_distribution.png", dpi=150)
plt.close()
print("Saved: plots/text_length_distribution.png")

# ═══════════════════════════════════════════════════════════
#  3. FEATURE ENGINEERING (TF-IDF)
# ═══════════════════════════════════════════════════════════

print("\n" + "=" * 60)
print("  STEP 3: Feature Engineering (TF-IDF Vectorization)")
print("=" * 60)

# Encode labels
le = LabelEncoder()
df["severity_encoded"] = le.fit_transform(df["severity"])
print(f"Label mapping: {dict(zip(le.classes_, le.transform(le.classes_)))}")

# Split data
X_train, X_test, y_train, y_test = train_test_split(
    df["diagnosis"], df["severity_encoded"],
    test_size=0.2, random_state=42, stratify=df["severity_encoded"]
)
print(f"Train: {len(X_train)} | Test: {len(X_test)}")

# TF-IDF Vectorization
tfidf = TfidfVectorizer(
    max_features=5000,
    ngram_range=(1, 2),   # unigrams + bigrams
    stop_words="english",
    sublinear_tf=True,
)
X_train_tfidf = tfidf.fit_transform(X_train)
X_test_tfidf = tfidf.transform(X_test)
print(f"TF-IDF features: {X_train_tfidf.shape[1]}")

# ═══════════════════════════════════════════════════════════
#  4. TRAIN & COMPARE 3 MODELS
# ═══════════════════════════════════════════════════════════

print("\n" + "=" * 60)
print("  STEP 4: Training & Comparing Models")
print("=" * 60)

models = {
    "Random Forest": RandomForestClassifier(n_estimators=200, random_state=42, n_jobs=-1),
    "SVM (Linear)": SVC(kernel="linear", probability=True, random_state=42),
    "Naive Bayes": MultinomialNB(alpha=0.1),
}

results = {}

for name, model in models.items():
    print(f"\nTraining: {name}...")

    # Cross-validation (5-fold)
    cv_scores = cross_val_score(model, X_train_tfidf, y_train, cv=5, scoring="accuracy")

    # Train on full training set
    model.fit(X_train_tfidf, y_train)

    # Predict on test set
    y_pred = model.predict(X_test_tfidf)
    acc = accuracy_score(y_test, y_pred)

    results[name] = {
        "model": model,
        "accuracy": acc,
        "cv_mean": cv_scores.mean(),
        "cv_std": cv_scores.std(),
        "y_pred": y_pred,
    }

    print(f"   Test Accuracy: {acc:.4f}")
    print(f"   Cross-Val: {cv_scores.mean():.4f} +/- {cv_scores.std():.4f}")
    print(f"\n   Classification Report:")
    print(classification_report(y_test, y_pred, target_names=le.classes_))

# ═══════════════════════════════════════════════════════════
#  5. MODEL COMPARISON VISUALIZATION
# ═══════════════════════════════════════════════════════════

print("\n" + "=" * 60)
print("  STEP 5: Model Comparison & Visualization")
print("=" * 60)

# Plot 3: Accuracy comparison
fig, ax = plt.subplots(figsize=(9, 5))
model_names = list(results.keys())
accuracies = [results[n]["accuracy"] for n in model_names]
cv_means = [results[n]["cv_mean"] for n in model_names]

x = np.arange(len(model_names))
width = 0.35
bars1 = ax.bar(x - width/2, accuracies, width, label="Test Accuracy", color="#6c63ff")
bars2 = ax.bar(x + width/2, cv_means, width, label="Cross-Val Mean", color="#48c6ef")

ax.set_title("Model Comparison — Accuracy", fontsize=14, fontweight="bold")
ax.set_ylabel("Accuracy")
ax.set_xticks(x)
ax.set_xticklabels(model_names)
ax.legend()
ax.set_ylim(0, 1.1)

for bar, val in zip(bars1, accuracies):
    ax.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 0.01,
            f"{val:.2%}", ha="center", fontweight="bold", fontsize=9)
for bar, val in zip(bars2, cv_means):
    ax.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 0.01,
            f"{val:.2%}", ha="center", fontweight="bold", fontsize=9)

plt.tight_layout()
plt.savefig("plots/model_comparison.png", dpi=150)
plt.close()
print("Saved: plots/model_comparison.png")

# Plot 4: Confusion matrices for all models
fig, axes = plt.subplots(1, 3, figsize=(18, 5))
for idx, name in enumerate(model_names):
    cm = confusion_matrix(y_test, results[name]["y_pred"])
    sns.heatmap(cm, annot=True, fmt="d", cmap="Blues",
                xticklabels=le.classes_, yticklabels=le.classes_,
                ax=axes[idx])
    axes[idx].set_title(f"{name}\nAccuracy: {results[name]['accuracy']:.2%}", fontweight="bold")
    axes[idx].set_xlabel("Predicted")
    axes[idx].set_ylabel("Actual")

plt.tight_layout()
plt.savefig("plots/confusion_matrices.png", dpi=150)
plt.close()
print("Saved: plots/confusion_matrices.png")

# Plot 5: ROC Curve (Multi-class, best model only)
from sklearn.metrics import roc_curve, auc
from sklearn.preprocessing import label_binarize

y_test_bin = label_binarize(y_test, classes=list(range(len(le.classes_))))
best_name_temp = max(results, key=lambda n: results[n]["accuracy"])
best_model_temp = results[best_name_temp]["model"]

try:
    y_score = best_model_temp.predict_proba(X_test_tfidf)

    fig, ax = plt.subplots(figsize=(8, 6))
    colors_roc = ["#3b82f6", "#f59e0b", "#ef4444"]
    for i, label in enumerate(le.classes_):
        fpr, tpr, _ = roc_curve(y_test_bin[:, i], y_score[:, i])
        roc_auc = auc(fpr, tpr)
        ax.plot(fpr, tpr, color=colors_roc[i % len(colors_roc)], linewidth=2,
                label=f'{label} (AUC = {roc_auc:.3f})')

    ax.plot([0, 1], [0, 1], 'k--', alpha=0.5, linewidth=1)
    ax.set_title(f"ROC Curve — {best_name_temp}", fontsize=14, fontweight="bold")
    ax.set_xlabel("False Positive Rate", fontsize=12)
    ax.set_ylabel("True Positive Rate", fontsize=12)
    ax.legend(fontsize=11, loc="lower right")
    ax.set_xlim([0, 1])
    ax.set_ylim([0, 1.05])
    ax.grid(alpha=0.3)
    plt.tight_layout()
    plt.savefig("plots/roc_curve.png", dpi=150)
    plt.close()
    print("Saved: plots/roc_curve.png")
except Exception as e:
    print(f"ROC curve skipped (model may not support predict_proba): {e}")

# ═══════════════════════════════════════════════════════════
#  6. SAVE BEST MODEL
# ═══════════════════════════════════════════════════════════

print("\n" + "=" * 60)
print("  STEP 6: Saving Best Model")
print("=" * 60)

best_name = max(results, key=lambda n: results[n]["accuracy"])
best_model = results[best_name]["model"]
best_acc = results[best_name]["accuracy"]

print(f"\nBEST MODEL: {best_name} (Accuracy: {best_acc:.4f})")

os.makedirs("model", exist_ok=True)
joblib.dump(best_model, "model/severity_model.pkl")
joblib.dump(tfidf, "model/tfidf_vectorizer.pkl")
joblib.dump(le, "model/label_encoder.pkl")

print("Saved: model/severity_model.pkl")
print("Saved: model/tfidf_vectorizer.pkl")
print("Saved: model/label_encoder.pkl")

print("\n" + "=" * 60)
print("  TRAINING COMPLETE!")
print(f"  Best: {best_name} — {best_acc:.2%} accuracy")
print("=" * 60)
