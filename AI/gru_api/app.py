from fastapi import FastAPI
from pydantic import BaseModel
from typing import List
import torch
import torch.nn as nn
import json
import nltk
import re


class MessagesInput(BaseModel):
    messages: List[str]

    
nltk.download('punkt_tab', quiet=True)

app = FastAPI(title="Mental Health Text Classifier")

# ── Load config & vocabulary ──────────────────────────────────────
with open("config.json") as f:
    cfg = json.load(f)

with open("word2idx.json") as f:
    word2idx = json.load(f)

# ── Reproduce the same model architecture from your notebook ──────
class AdvancedGRUClassifier(nn.Module):
    def __init__(self, vocab_size, embedding_dim, hidden_dim, output_dim):
        super().__init__()
        self.embedding = nn.Embedding(vocab_size, embedding_dim, padding_idx=0)
        self.gru = nn.GRU(embedding_dim, hidden_dim, batch_first=True,
                          bidirectional=True, num_layers=1, dropout=0.0)
        self.fc = nn.Linear(hidden_dim * 2, output_dim)
        self.dropout = nn.Dropout(0.3)

    def forward(self, x, lengths):
        embedded = self.dropout(self.embedding(x))
        packed = nn.utils.rnn.pack_padded_sequence(
            embedded, lengths.cpu(), batch_first=True, enforce_sorted=False)
        _, hidden = self.gru(packed)
        hidden = torch.cat((hidden[-2], hidden[-1]), dim=1)
        return self.fc(self.dropout(hidden))

# ── Load model ────────────────────────────────────────────────────
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
model = AdvancedGRUClassifier(
    cfg["vocab_size"], cfg["embedding_dim"],
    cfg["hidden_dim"], cfg["output_dim"]
).to(device)
model.load_state_dict(torch.load("gru_model.pth", map_location=device))
model.eval()

# Label mapping — update if your labels differ
LABELS = {0: "Anxiety", 1: "Depression", 2: "Normal", 3: "Suicidal"}

# ── Preprocessing (same as your notebook) ────────────────────────
def preprocess(text):
    text = re.sub(r"http\S+", "", text)
    text = re.sub(r"[^a-zA-Z\s]", "", text.lower())
    tokens = nltk.word_tokenize(text)
    indices = [word2idx.get(t, word2idx.get("<UNK>", 1)) for t in tokens]
    if not indices:
        indices = [0]
    max_len = cfg["max_len"]
    indices = indices[:max_len]
    indices += [word2idx.get("<PAD>", 0)] * (max_len - len(indices))
    return indices

# ── API ───────────────────────────────────────────────────────────
class TextInput(BaseModel):
    text: str

@app.post("/predict")
def predict(input: TextInput):
    tokens = preprocess(input.text)
    tensor = torch.tensor([tokens], dtype=torch.long).to(device)
    length = torch.tensor(
        [sum(1 for t in tokens if t != word2idx.get("<PAD>", 0))],
        dtype=torch.long
    )
    with torch.no_grad():
        output = model(tensor, length)
        probs = torch.softmax(output, dim=1)[0]
        pred = torch.argmax(probs).item()

    return {
        "prediction": LABELS[pred],
        "confidence": round(probs[pred].item(), 4),
        "probabilities": {LABELS[i]: round(p.item(), 4) for i, p in enumerate(probs)}
    }

@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/predict-conversation")
def predict_conversation(input: MessagesInput):
    full_text = " ".join(input.messages)

    tokens = preprocess(full_text)
    tensor = torch.tensor([tokens], dtype=torch.long).to(device)
    length = torch.tensor(
        [sum(1 for t in tokens if t != word2idx.get("<PAD>", 0))],
        dtype=torch.long
    )

    with torch.no_grad():
        output = model(tensor, length)
        probs = torch.softmax(output, dim=1)[0]
        pred = torch.argmax(probs).item()

    return {
        "prediction": LABELS[pred],
        "confidence": round(probs[pred].item(), 4)
    }