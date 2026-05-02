"""
============================================================
  SMART DRUG RECOMMENDER TRAINING
  Multi-label classification of diagnosis to drug list
============================================================
"""

import pandas as pd
import numpy as np
import os
import joblib
import matplotlib
matplotlib.use('Agg')  # Non-interactive backend
import matplotlib.pyplot as plt
import seaborn as sns
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.preprocessing import MultiLabelBinarizer
from sklearn.multiclass import OneVsRestClassifier
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (
    accuracy_score, hamming_loss,
    f1_score, precision_score, recall_score, jaccard_score,
    classification_report
)

print("=" * 60)
print("  STEP 1: Load and Clean Drug Dataset")
print("=" * 60)

df = pd.read_csv("dataset/drug_recommendation.csv")
print(f"Dataset shape: {df.shape}")

# Clean text
df["diagnosis"] = df["diagnosis"].str.strip().str.lower()
df.drop_duplicates(inplace=True)
print(f"After dropping duplicates: {len(df)} rows")

# Convert comma-separated drugs to list
df["drugs_list"] = df["recommended_drugs"].apply(lambda x: [d.strip() for d in x.split(",")])

print("Sample rows:")
print(df[["diagnosis", "drugs_list"]].head())

# ═══════════════════════════════════════════════════════════
#  STEP 2: ENCODE LABELS AND FEATURES
# ═══════════════════════════════════════════════════════════

print("\n" + "=" * 60)
print("  STEP 2: Feature Engineering (TF-IDF & MLB)")
print("=" * 60)

# MultiLabelBinarizer for drugs
mlb = MultiLabelBinarizer()
y = mlb.fit_transform(df["drugs_list"])
print(f"Total Unique Drugs (Classes): {len(mlb.classes_)}")

# TF-IDF for diagnosis
tfidf = TfidfVectorizer(max_features=5000, stop_words="english", ngram_range=(1, 2))
X = tfidf.fit_transform(df["diagnosis"])
print(f"TF-IDF Shape: {X.shape}")

# Train/Test Split
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
print(f"Train: {X_train.shape[0]} | Test: {X_test.shape[0]}")

# ═══════════════════════════════════════════════════════════
#  STEP 3: TRAIN MULTI-LABEL MODEL
# ═══════════════════════════════════════════════════════════

print("\n" + "=" * 60)
print("  STEP 3: Training Random Forest Multi-Label")
print("=" * 60)

# RandomForest inherently supports multiclass-multioutput
clf = RandomForestClassifier(n_estimators=150, random_state=42, n_jobs=-1)
clf.fit(X_train, y_train)

# ═══════════════════════════════════════════════════════════
#  STEP 4: DETAILED EVALUATION
# ═══════════════════════════════════════════════════════════

print("\n" + "=" * 60)
print("  STEP 4: Detailed Multi-Label Evaluation")
print("=" * 60)

y_pred = clf.predict(X_test)

# Basic metrics
acc = accuracy_score(y_test, y_pred)
hl = hamming_loss(y_test, y_pred)

# Advanced multi-label metrics
f1_micro = f1_score(y_test, y_pred, average="micro", zero_division=0)
f1_macro = f1_score(y_test, y_pred, average="macro", zero_division=0)
precision_micro = precision_score(y_test, y_pred, average="micro", zero_division=0)
recall_micro = recall_score(y_test, y_pred, average="micro", zero_division=0)
jaccard = jaccard_score(y_test, y_pred, average="micro", zero_division=0)

print(f"\n{'-' * 40}")
print(f"  Exact Match Ratio (Accuracy) : {acc:.4f}")
print(f"  Hamming Loss (lower=better) : {hl:.4f}")
print(f"{'-' * 40}")
print(f"  F1-Score (Micro)             : {f1_micro:.4f}")
print(f"  F1-Score (Macro)             : {f1_macro:.4f}")
print(f"  Precision (Micro)            : {precision_micro:.4f}")
print(f"  Recall (Micro)               : {recall_micro:.4f}")
print(f"  Jaccard Similarity (Micro)   : {jaccard:.4f}")
print(f"{'-' * 40}")

# Per-drug classification report (top drugs only to keep output readable)
print("\n  Per-Drug Classification Report:")
print(classification_report(y_test, y_pred, target_names=mlb.classes_, zero_division=0))

# ═══════════════════════════════════════════════════════════
#  STEP 5: VISUALIZATIONS
# ═══════════════════════════════════════════════════════════

print("\n" + "=" * 60)
print("  STEP 5: Generating Visualizations")
print("=" * 60)

os.makedirs("plots", exist_ok=True)

# Plot 1: Top 10 Most Predicted Drugs
drug_pred_counts = y_pred.sum(axis=0)
top_indices = drug_pred_counts.argsort()[-10:][::-1]
top_names = [mlb.classes_[i] for i in top_indices]
top_counts = [int(drug_pred_counts[i]) for i in top_indices]

fig, ax = plt.subplots(figsize=(10, 6))
bars = ax.barh(range(len(top_names)), top_counts, color='#6c63ff', edgecolor='#4a3fcf')
ax.set_yticks(range(len(top_names)))
ax.set_yticklabels(top_names, fontsize=11)
ax.set_title("Top 10 Most Predicted Drugs by AI", fontsize=14, fontweight="bold")
ax.set_xlabel("Prediction Count (Test Set)")
ax.invert_yaxis()
for bar, val in zip(bars, top_counts):
    ax.text(bar.get_width() + 2, bar.get_y() + bar.get_height()/2,
            str(val), va="center", fontweight="bold", fontsize=10)
plt.tight_layout()
plt.savefig("plots/top_predicted_drugs.png", dpi=150)
plt.close()
print("Saved: plots/top_predicted_drugs.png")

# Plot 2: Multi-Label Metrics Summary Bar Chart
fig, ax = plt.subplots(figsize=(9, 5))
metrics_names = ["Accuracy", "F1 (Micro)", "F1 (Macro)", "Precision", "Recall", "Jaccard"]
metrics_values = [acc, f1_micro, f1_macro, precision_micro, recall_micro, jaccard]
colors_list = ["#6c63ff", "#48c6ef", "#f77062", "#43e97b", "#fa709a", "#fee140"]
bars = ax.bar(metrics_names, metrics_values, color=colors_list, edgecolor="#333", linewidth=0.5)
ax.set_title("Drug Recommender — Evaluation Metrics", fontsize=14, fontweight="bold")
ax.set_ylabel("Score")
ax.set_ylim(0, 1.15)
for bar, val in zip(bars, metrics_values):
    ax.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 0.02,
            f"{val:.2%}", ha="center", fontweight="bold", fontsize=10)
plt.tight_layout()
plt.savefig("plots/recommender_metrics.png", dpi=150)
plt.close()
print("Saved: plots/recommender_metrics.png")

# Plot 3: Drug Frequency in Training Data
drug_train_counts = y_train.sum(axis=0)
top_train_indices = drug_train_counts.argsort()[-15:][::-1]
top_train_names = [mlb.classes_[i] for i in top_train_indices]
top_train_counts = [int(drug_train_counts[i]) for i in top_train_indices]

fig, ax = plt.subplots(figsize=(10, 7))
ax.barh(range(len(top_train_names)), top_train_counts, color='#43e97b', edgecolor='#2bbd5e')
ax.set_yticks(range(len(top_train_names)))
ax.set_yticklabels(top_train_names, fontsize=10)
ax.set_title("Top 15 Drugs in Training Dataset", fontsize=14, fontweight="bold")
ax.set_xlabel("Frequency")
ax.invert_yaxis()
plt.tight_layout()
plt.savefig("plots/drug_frequency_training.png", dpi=150)
plt.close()
print("Saved: plots/drug_frequency_training.png")

# ═══════════════════════════════════════════════════════════
#  STEP 6: SAVE MODEL
# ═══════════════════════════════════════════════════════════

print("\n" + "=" * 60)
print("  STEP 6: Saving Model")
print("=" * 60)

os.makedirs("model", exist_ok=True)
joblib.dump(clf, "model/recommender_model.pkl")
joblib.dump(tfidf, "model/recommender_tfidf.pkl")
joblib.dump(mlb, "model/recommender_mlb.pkl")

print("Saved: model/recommender_model.pkl")
print("Saved: model/recommender_tfidf.pkl")
print("Saved: model/recommender_mlb.pkl")

print("\n" + "=" * 60)
print("  DRUG RECOMMENDER TRAINING COMPLETE!")
print(f"  Accuracy: {acc:.2%} | F1 (Micro): {f1_micro:.2%}")
print("=" * 60)
