"""
============================================================
  FLASK API — Medical Severity Prediction Service
  Endpoint: POST /predict
  Port: 5001
============================================================
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import joblib
import os

app = Flask(__name__)
CORS(app)

# ═══════════════════════════════════════════════════════════
#  LOAD MODEL AT STARTUP
# ═══════════════════════════════════════════════════════════

MODEL_DIR = "model"

try:
    model = joblib.load(os.path.join(MODEL_DIR, "severity_model.pkl"))
    tfidf = joblib.load(os.path.join(MODEL_DIR, "tfidf_vectorizer.pkl"))
    label_encoder = joblib.load(os.path.join(MODEL_DIR, "label_encoder.pkl"))
    print("Model, TF-IDF vectorizer, and label encoder loaded successfully.")
except FileNotFoundError:
    print("Model files not found! Run 'python train_model.py' first.")
    model = None
    tfidf = None
    label_encoder = None

try:
    rec_model = joblib.load(os.path.join(MODEL_DIR, "recommender_model.pkl"))
    rec_tfidf = joblib.load(os.path.join(MODEL_DIR, "recommender_tfidf.pkl"))
    rec_mlb = joblib.load(os.path.join(MODEL_DIR, "recommender_mlb.pkl"))
    print("Drug Recommender model, TF-IDF, and MLB loaded successfully.")
except FileNotFoundError:
    print("Drug Recommender files not found! Run 'python train_recommender.py' first.")
    rec_model = None
    rec_tfidf = None
    rec_mlb = None


# ═══════════════════════════════════════════════════════════
#  PREDICTION ENDPOINTS
# ═══════════════════════════════════════════════════════════

@app.route("/predict", methods=["POST"])
def predict():
    """
    Predict the severity of a medical diagnosis.

    Request body (JSON):
        {"diagnosis": "G43.9 - Migraine, unspecified"}

    Response (JSON):
        {
            "severity": "MEDIUM",
            "confidence": 0.87,
            "probabilities": {"LOW": 0.05, "MEDIUM": 0.87, "HIGH": 0.08}
        }
    """
    if model is None:
        return jsonify({"error": "Model not loaded. Train the model first."}), 503

    data = request.get_json()
    if not data or "diagnosis" not in data:
        return jsonify({"error": "Missing 'diagnosis' field in request body."}), 400

    diagnosis_text = data["diagnosis"].strip().lower()
    if not diagnosis_text:
        return jsonify({"error": "'diagnosis' field cannot be empty."}), 400

    # Vectorize the input
    X = tfidf.transform([diagnosis_text])

    # Predict class
    prediction_encoded = model.predict(X)[0]
    severity = label_encoder.inverse_transform([prediction_encoded])[0]

    # Get probabilities (if the model supports it)
    probabilities = {}
    try:
        proba = model.predict_proba(X)[0]
        for idx, label in enumerate(label_encoder.classes_):
            probabilities[label] = round(float(proba[idx]), 4)
        confidence = round(float(max(proba)), 4)
    except AttributeError:
        confidence = 1.0
        probabilities = {severity: 1.0}

    return jsonify({
        "severity": severity,
        "confidence": confidence,
        "probabilities": probabilities,
    })


@app.route("/recommend-drugs", methods=["POST"])
def recommend_drugs():
    """
    Recommend drugs based on a medical diagnosis.

    Request body (JSON):
        {"diagnosis": "Major depressive disorder"}

    Response (JSON):
        {
            "recommended_drugs": ["Sertraline", "Fluoxetine", "Escitalopram"]
        }
    """
    if rec_model is None:
        return jsonify({"error": "Recommender model not loaded. Train it first."}), 503

    data = request.get_json()
    if not data or "diagnosis" not in data:
        return jsonify({"error": "Missing 'diagnosis' field in request body."}), 400

    diagnosis_text = data["diagnosis"].strip().lower()
    if not diagnosis_text:
        return jsonify({"error": "'diagnosis' field cannot be empty."}), 400

    # Vectorize input
    X = rec_tfidf.transform([diagnosis_text])

    # Predict
    # Since it's multi-output Random Forest, it outputs a binary matrix
    prediction = rec_model.predict(X)
    
    # Inverse transform to get drug names
    drugs = rec_mlb.inverse_transform(prediction)[0]

    # If the model is not confident enough and returns empty, provide a fallback
    drug_list = list(drugs)
    if not drug_list:
        try:
            proba = rec_model.predict_proba(X)
            # RF predict_proba returns a list of n_classes, each an array of shape (n_samples, 2)
            class_probs = []
            for i in range(len(rec_mlb.classes_)):
                # Probability of being class 1 (True)
                prob_true = proba[i][0][1]
                class_probs.append((prob_true, rec_mlb.classes_[i]))
            
            # Sort by probability descending
            class_probs.sort(key=lambda x: x[0], reverse=True)
            
            # Take top 3 drugs with highest probability (even if < 0.5)
            drug_list = [c[1] for c in class_probs[:3] if c[0] > 0]
        except Exception as e:
            print("Fallback error:", str(e))

    # Build confidence details for each recommended drug
    drug_details = []
    try:
        proba = rec_model.predict_proba(X)
        for drug_name in drug_list:
            idx = list(rec_mlb.classes_).index(drug_name)
            conf = round(float(proba[idx][0][1]), 4)
            drug_details.append({"drug": drug_name, "confidence": conf})
    except Exception:
        drug_details = [{"drug": d, "confidence": None} for d in drug_list]

    return jsonify({
        "recommended_drugs": drug_list,
        "details": drug_details
    })

# ═══════════════════════════════════════════════════════════
#  HEALTH CHECK
# ═══════════════════════════════════════════════════════════

@app.route("/health", methods=["GET"])
def health():
    return jsonify({
        "status": "UP",
        "model_loaded": model is not None,
        "service": "ai-severity-derbel-service",
    })


# ═══════════════════════════════════════════════════════════
#  RUN SERVER
# ═══════════════════════════════════════════════════════════

if __name__ == "__main__":
    print("\nAI Severity Prediction Service starting on port 5001...")
    host = os.getenv("HOST", "127.0.0.1")
    port = int(os.getenv("PORT", "5001"))
    debug = os.getenv("DEBUG", "true").lower() in {"1", "true", "yes", "y"}
    app.run(host=host, port=port, debug=debug)
