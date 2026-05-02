# AI Severity Prediction - Derbel Service

A machine learning microservice that predicts the severity level (LOW, MEDIUM, HIGH)
of a medical diagnosis based on the ICD-10 code and description.

## Architecture
- **Dataset**: Synthetic medical dataset based on ICD-10 codes
- **Model**: TF-IDF + Multi-classifier comparison (Random Forest, SVM, Naive Bayes)
- **Serving**: Flask REST API
- **Integration**: Called by Spring Boot via RestTemplate

## Setup
```bash
pip install -r requirements.txt
```

## Train the model
```bash
python train_model.py
```

## Run the API
```bash
python app.py
```

The API will run on `http://localhost:5001`

## API Endpoints
- `POST /predict` — Predict severity from diagnosis text
  - Body: `{"diagnosis": "G43.9 - Migraine, unspecified"}`
  - Response: `{"severity": "MEDIUM", "confidence": 0.87, "probabilities": {...}}`
