import io
import logging
import os
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import fitz
import numpy as np
import torch
from PIL import Image

# Safer Paddle defaults on CPU; avoids known PIR/oneDNN runtime failures on some setups.
os.environ.setdefault("FLAGS_use_mkldnn", "0")
os.environ.setdefault("FLAGS_enable_pir_api", "0")
os.environ.setdefault("FLAGS_use_pir_api", "0")

from paddleocr import PaddleOCR
from transformers import AutoModelForSequenceClassification, AutoModelForTokenClassification, AutoProcessor


LOGGER = logging.getLogger(__name__)

ALLOWED_EXTENSIONS = {".pdf", ".png", ".jpg", ".jpeg"}
REQUIRED_ENTITIES = ["AUTHORITY", "NAME", "REG_NUMBER", "ISSUE_DATE", "COUNTRY", "TITLE", "SIGNATURE", "STAMP"]
STRICT_ENTITIES_FOR_ACCEPT = ["AUTHORITY", "NAME", "REG_NUMBER", "ISSUE_DATE", "COUNTRY", "TITLE"]
CNOPT_STRONG_PATTERNS = [
    r"\bcnop\b",
    r"\bcnopt\b",
    r"\bordre\s+national\b",
    r"\bordre\s+des\s+pharmaciens\b",
]
CNOPT_CONTEXT_PATTERNS = [
    r"\bordre\b",
    r"\bpharmac",
    r"\bcertificat\b",
    r"\binscription\b",
]


@dataclass
class CnoptRuntimeConfig:
    processor_dir: str
    ner_model_dir: str
    doc_model_dir: str
    max_pdf_pages: int
    pdf_dpi: int
    image_max_side: int
    max_ocr_tokens: int
    token_max_length: int
    ocr_lang: str

    @staticmethod
    def from_env() -> "CnoptRuntimeConfig":
        return CnoptRuntimeConfig(
            processor_dir=os.getenv("PHARMACY_ML_CNOPT_PROCESSOR_DIR", "microsoft/layoutlmv3-base"),
            ner_model_dir=os.getenv(
                "PHARMACY_ML_CNOPT_NER_MODEL_DIR",
                "layoutlmv3_cnopt_v4_ner_output/checkpoint-200",
            ),
            doc_model_dir=os.getenv(
                "PHARMACY_ML_CNOPT_DOC_MODEL_DIR",
                "layoutlmv3_cnopt_v4_doc_status_output/checkpoint-150",
            ),
            max_pdf_pages=max(1, int(os.getenv("PHARMACY_ML_CNOPT_MAX_PDF_PAGES", "1"))),
            pdf_dpi=max(96, int(os.getenv("PHARMACY_ML_CNOPT_PDF_DPI", "150"))),
            image_max_side=max(800, int(os.getenv("PHARMACY_ML_CNOPT_IMAGE_MAX_SIDE", "1800"))),
            max_ocr_tokens=max(64, int(os.getenv("PHARMACY_ML_CNOPT_MAX_OCR_TOKENS", "220"))),
            token_max_length=max(128, min(512, int(os.getenv("PHARMACY_ML_CNOPT_TOKEN_MAX_LENGTH", "256")))),
            ocr_lang=os.getenv("PHARMACY_ML_CNOPT_OCR_LANG", "en"),
        )


class CnoptVerificationService:
    def __init__(self) -> None:
        self.config = CnoptRuntimeConfig.from_env()
        self.base_dir = Path(__file__).resolve().parent
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.available = False
        self.last_error = ""

        self.processor = None
        self.ner_model = None
        self.doc_model = None
        self.ocr = None

        self.ner_id2label: dict[int, str] = {}
        self.doc_id2label: dict[int, str] = {}

        self._load_runtime()

    def _resolve_source(self, raw: str) -> str:
        value = (raw or "").strip()
        if not value:
            return value

        candidate = Path(value)
        if candidate.exists():
            return str(candidate.resolve())

        if not candidate.is_absolute():
            candidate_from_service_dir = (self.base_dir / candidate).resolve()
            if candidate_from_service_dir.exists():
                return str(candidate_from_service_dir)

        # Probably a HuggingFace model id.
        return value

    def _load_runtime(self) -> None:
        try:
            ner_source = self._resolve_source(self.config.ner_model_dir)
            doc_source = self._resolve_source(self.config.doc_model_dir)

            processor_source = ner_source
            ner_path = Path(ner_source)
            if ner_path.exists():
                if not (ner_path / "preprocessor_config.json").exists():
                    checkpoint_parent = ner_path.parent
                    if (checkpoint_parent / "preprocessor_config.json").exists():
                        processor_source = str(checkpoint_parent)
                    else:
                        processor_source = self._resolve_source(self.config.processor_dir)
            else:
                processor_source = self._resolve_source(self.config.processor_dir)

            self.processor = AutoProcessor.from_pretrained(processor_source, apply_ocr=False)
            self.ner_model = AutoModelForTokenClassification.from_pretrained(ner_source)
            self.doc_model = AutoModelForSequenceClassification.from_pretrained(doc_source)

            self.ner_model.to(self.device).eval()
            self.doc_model.to(self.device).eval()

            self.ner_id2label = self._normalize_id2label(self.ner_model.config.id2label)
            self.doc_id2label = self._normalize_id2label(self.doc_model.config.id2label)
            self.ocr = self._build_ocr()

            self.available = True
            self.last_error = ""
            LOGGER.info(
                "CNOPT verifier loaded. device=%s, processor=%s, ner_dir=%s, doc_dir=%s",
                self.device,
                processor_source,
                ner_source,
                doc_source,
            )
        except Exception as exc:  # pylint: disable=broad-except
            self.available = False
            self.last_error = str(exc)
            LOGGER.exception("Failed to initialize CNOPT verifier runtime: %s", exc)

    def _build_ocr(self) -> PaddleOCR:
        variants = [
            {"use_angle_cls": False, "lang": self.config.ocr_lang, "show_log": False, "enable_mkldnn": False},
            {"use_angle_cls": False, "lang": self.config.ocr_lang, "enable_mkldnn": False},
            {"use_angle_cls": False, "lang": self.config.ocr_lang, "show_log": False},
            {"use_angle_cls": False, "lang": self.config.ocr_lang},
            {"use_angle_cls": True, "lang": self.config.ocr_lang, "show_log": False, "enable_mkldnn": False},
            {"use_angle_cls": True, "lang": self.config.ocr_lang, "enable_mkldnn": False},
            {"use_angle_cls": True, "lang": self.config.ocr_lang, "show_log": False},
            {"use_angle_cls": True, "lang": self.config.ocr_lang},
        ]
        last_error: Exception | None = None
        for kwargs in variants:
            try:
                return PaddleOCR(**kwargs)
            except Exception as exc:  # pylint: disable=broad-except
                last_error = exc
                continue
        raise RuntimeError(f"Unable to initialize PaddleOCR with available settings: {last_error}")

    @staticmethod
    def _normalize_id2label(raw_id2label: dict[Any, Any] | None) -> dict[int, str]:
        if not raw_id2label:
            return {}
        normalized: dict[int, str] = {}
        for key, value in raw_id2label.items():
            try:
                normalized[int(key)] = str(value)
            except Exception:
                continue
        return normalized

    def verify_file(self, file_bytes: bytes, filename: str | None, content_type: str | None = None) -> dict[str, Any]:
        if not self.available:
            raise RuntimeError(self.last_error or "CNOPT verifier runtime unavailable")
        if not file_bytes:
            raise ValueError("Uploaded CNOPT file is empty.")

        extension = self._resolve_extension(filename, content_type)
        if extension not in ALLOWED_EXTENSIONS:
            raise ValueError("Unsupported file type. Only PDF, PNG, JPG, and JPEG are allowed.")

        images = self._load_images(file_bytes, extension)
        if not images:
            raise ValueError("Unable to parse uploaded CNOPT file.")

        image = images[0]
        tokens, bboxes = self._extract_tokens_and_boxes(image)

        if not tokens:
            return {
                "documentDecision": "REJECT",
                "fraudStatus": "incomplete",
                "message": "CNOPT document is unreadable or incomplete.",
                "isCnoptDocument": False,
                "fraudRiskScore": 0.99,
                "flags": ["no_ocr_tokens"],
                "missingEntities": REQUIRED_ENTITIES,
            }

        entities, _, _ = self._predict_ner_entities(image, tokens, bboxes)
        doc_pred_status, doc_conf, doc_probs = self._predict_doc_status(image, tokens, bboxes)

        presence, missing, flags, severe_flags = self._build_fraud_flags(entities, tokens)
        decision, final_status, risk_score = self._fuse_decision(
            doc_pred_status,
            doc_conf,
            presence,
            missing,
            flags,
            severe_flags,
        )

        message = self._build_message(decision, final_status, missing, flags)
        return {
            "documentDecision": decision,
            "fraudStatus": final_status,
            "message": message,
            "isCnoptDocument": final_status != "wrong_type",
            "fraudRiskScore": round(float(risk_score), 4),
            "flags": flags,
            "missingEntities": missing,
            "docStatusModelPrediction": doc_pred_status,
            "docStatusModelConfidence": round(float(doc_conf), 4),
            "docStatusModelProbs": {k: round(float(v), 4) for k, v in doc_probs.items()},
        }

    @staticmethod
    def _resolve_extension(filename: str | None, content_type: str | None) -> str:
        if filename:
            name = filename.strip().lower()
            dot = name.rfind(".")
            if dot >= 0:
                return name[dot:]

        if content_type:
            ct = content_type.lower().strip()
            if "pdf" in ct:
                return ".pdf"
            if "png" in ct:
                return ".png"
            if "jpeg" in ct or "jpg" in ct:
                return ".jpg"

        return ""

    def _load_images(self, file_bytes: bytes, extension: str) -> list[Image.Image]:
        if extension == ".pdf":
            images = self._load_pdf_pages(file_bytes)
        else:
            images = [self._load_single_image(file_bytes)]
        return [self._resize_for_inference(image) for image in images]

    def _resize_for_inference(self, image: Image.Image) -> Image.Image:
        width, height = image.size
        current_max_side = max(width, height)
        if current_max_side <= self.config.image_max_side:
            return image

        scale = self.config.image_max_side / float(current_max_side)
        new_width = max(1, int(width * scale))
        new_height = max(1, int(height * scale))
        return image.resize((new_width, new_height), Image.BILINEAR)

    def _load_pdf_pages(self, file_bytes: bytes) -> list[Image.Image]:
        images: list[Image.Image] = []
        try:
            with fitz.open(stream=file_bytes, filetype="pdf") as doc:
                if doc.page_count == 0:
                    raise ValueError("PDF has no pages.")
                max_pages = min(self.config.max_pdf_pages, doc.page_count)
                for idx in range(max_pages):
                    page = doc.load_page(idx)
                    pix = page.get_pixmap(alpha=False, dpi=self.config.pdf_dpi)
                    mode = "RGB" if pix.n < 4 else "RGBA"
                    image = Image.frombytes(mode, [pix.width, pix.height], pix.samples)
                    images.append(image.convert("RGB"))
        except ValueError:
            raise
        except Exception as exc:
            raise ValueError(f"Unable to read PDF content: {exc}") from exc
        return images

    @staticmethod
    def _load_single_image(file_bytes: bytes) -> Image.Image:
        try:
            with Image.open(io.BytesIO(file_bytes)) as img:
                return img.convert("RGB")
        except Exception as exc:
            raise ValueError(f"Unable to decode image content: {exc}") from exc

    def _extract_tokens_and_boxes(self, image: Image.Image) -> tuple[list[str], list[list[int]]]:
        width, height = image.size
        image_bgr = np.array(image)[:, :, ::-1]

        ocr_result = self._run_ocr(image_bgr)
        lines = self._extract_ocr_lines(ocr_result)

        tokens: list[str] = []
        bboxes: list[list[int]] = []
        for points, text in lines:
            if len(tokens) >= self.config.max_ocr_tokens:
                break
            if not text:
                continue

            abs_bbox = self._quad_to_abs_bbox(points, width, height)
            line_tokens, line_boxes = self._split_text_into_word_boxes(text, abs_bbox)
            if not line_tokens:
                continue

            remaining = self.config.max_ocr_tokens - len(tokens)
            if remaining <= 0:
                break

            used_tokens = line_tokens[:remaining]
            used_boxes = line_boxes[:remaining]
            tokens.extend(used_tokens)
            for box in used_boxes:
                bboxes.append(self._normalize_bbox(box, width, height))

        return tokens, bboxes

    def _run_ocr(self, image_bgr: np.ndarray) -> Any:
        if self.ocr is None:
            return []
        ocr_fn = getattr(self.ocr, "ocr", None)
        if callable(ocr_fn):
            try:
                return ocr_fn(image_bgr, cls=True)
            except TypeError:
                return ocr_fn(image_bgr)
        predict_fn = getattr(self.ocr, "predict", None)
        if callable(predict_fn):
            return predict_fn(image_bgr)
        return []

    def _extract_ocr_lines(self, ocr_result: Any) -> list[tuple[Any, str]]:
        lines: list[tuple[Any, str]] = []
        self._collect_ocr_lines(ocr_result, lines)
        return lines

    def _collect_ocr_lines(self, node: Any, lines: list[tuple[Any, str]]) -> None:
        if node is None:
            return

        if isinstance(node, dict):
            rec_texts = node.get("rec_texts")
            if isinstance(rec_texts, (list, tuple)):
                polys = node.get("rec_polys") or node.get("dt_polys") or node.get("rec_boxes")
                poly_list = self._to_list(polys) or []
                for idx, raw_text in enumerate(rec_texts):
                    text = str(raw_text).strip()
                    if not text:
                        continue
                    points = poly_list[idx] if idx < len(poly_list) else None
                    lines.append((points, text))

            for key in ("res", "result", "results", "data", "ocr_result"):
                if key in node:
                    self._collect_ocr_lines(node.get(key), lines)
            return

        if isinstance(node, (list, tuple)):
            if self._looks_like_old_ocr_line(node):
                maybe_points = node[0]
                text = self._extract_text_from_node(node[1])
                if text:
                    lines.append((maybe_points, text))
            else:
                for item in node:
                    self._collect_ocr_lines(item, lines)
            return

        if hasattr(node, "__dict__"):
            self._collect_ocr_lines(vars(node), lines)

    def _looks_like_old_ocr_line(self, value: Any) -> bool:
        # Old PaddleOCR line shape is exactly:
        # [points, (text, score)] or [points, text]
        # Avoid matching whole-page lists (list of many lines), which caused false parsing.
        if not isinstance(value, (list, tuple)) or len(value) != 2:
            return False
        points = value[0]
        if not self._looks_like_points(points):
            return False
        return self._looks_like_text_node(value[1])

    @staticmethod
    def _looks_like_text_node(value: Any) -> bool:
        if value is None:
            return False
        if isinstance(value, str):
            return bool(value.strip())
        if isinstance(value, (list, tuple)):
            if not value:
                return False
            return isinstance(value[0], str) and bool(value[0].strip())
        if isinstance(value, dict):
            text = value.get("text")
            return isinstance(text, str) and bool(text.strip())
        return False

    @staticmethod
    def _looks_like_points(points: Any) -> bool:
        if points is None:
            return False
        if hasattr(points, "tolist"):
            try:
                points = points.tolist()
            except Exception:
                return False
        if not isinstance(points, (list, tuple)) or not points:
            return False
        first = points[0]
        if hasattr(first, "tolist"):
            try:
                first = first.tolist()
            except Exception:
                return False
        return isinstance(first, (list, tuple)) and len(first) >= 2

    @staticmethod
    def _extract_text_from_node(value: Any) -> str:
        if value is None:
            return ""
        if isinstance(value, (list, tuple)) and value:
            return str(value[0]).strip()
        if isinstance(value, str):
            return value.strip()
        return ""

    @staticmethod
    def _to_list(value: Any) -> Any:
        if value is None:
            return None
        if isinstance(value, (list, tuple)):
            return list(value)
        if hasattr(value, "tolist"):
            try:
                return value.tolist()
            except Exception:
                return None
        return None

    @staticmethod
    def _flatten_ocr_lines(ocr_result: Any) -> list[Any]:
        if not ocr_result:
            return []
        if isinstance(ocr_result, list) and len(ocr_result) == 1 and isinstance(ocr_result[0], list):
            return ocr_result[0]
        if isinstance(ocr_result, list):
            return ocr_result
        return []

    @staticmethod
    def _quad_to_abs_bbox(points: Any, width: int, height: int) -> list[float]:
        if hasattr(points, "tolist"):
            try:
                points = points.tolist()
            except Exception:
                points = None

        if not isinstance(points, (list, tuple)) or not points:
            return [0.0, 0.0, float(max(1, width - 1)), float(max(1, height - 1))]
        xs: list[float] = []
        ys: list[float] = []
        for point in points:
            if hasattr(point, "tolist"):
                try:
                    point = point.tolist()
                except Exception:
                    continue
            if isinstance(point, (list, tuple)) and len(point) >= 2:
                try:
                    xs.append(float(point[0]))
                    ys.append(float(point[1]))
                except Exception:
                    continue
        if not xs or not ys:
            return [0.0, 0.0, float(max(1, width - 1)), float(max(1, height - 1))]
        x0, x1 = max(0.0, min(xs)), min(float(width), max(xs))
        y0, y1 = max(0.0, min(ys)), min(float(height), max(ys))
        if x1 <= x0:
            x1 = min(float(width), x0 + 1.0)
        if y1 <= y0:
            y1 = min(float(height), y0 + 1.0)
        return [x0, y0, x1, y1]

    @staticmethod
    def _split_text_into_word_boxes(text: str, abs_bbox: list[float]) -> tuple[list[str], list[list[float]]]:
        words = re.findall(r"\S+", text)
        if not words:
            return [], []

        x0, y0, x1, y1 = abs_bbox
        if len(words) == 1:
            return words, [abs_bbox]

        width = max(1.0, x1 - x0)
        step = width / len(words)

        boxes: list[list[float]] = []
        for idx, _ in enumerate(words):
            wx0 = x0 + step * idx
            wx1 = x0 + step * (idx + 1)
            boxes.append([wx0, y0, wx1, y1])
        return words, boxes

    @staticmethod
    def _normalize_bbox(box: list[float], width: int, height: int) -> list[int]:
        x0, y0, x1, y1 = [float(v) for v in box]
        x0 = max(0.0, min(x0, float(width)))
        x1 = max(0.0, min(x1, float(width)))
        y0 = max(0.0, min(y0, float(height)))
        y1 = max(0.0, min(y1, float(height)))

        if width <= 0 or height <= 0:
            return [0, 0, 0, 0]

        return [
            int(1000 * x0 / width),
            int(1000 * y0 / height),
            int(1000 * x1 / width),
            int(1000 * y1 / height),
        ]

    def _predict_ner_entities(
        self, image: Image.Image, words: list[str], boxes: list[list[int]]
    ) -> tuple[dict[str, list[dict[str, Any]]], list[str], list[float]]:
        encoding = self.processor(
            images=image.convert("RGB"),
            text=words,
            boxes=boxes,
            truncation=True,
            padding="max_length",
            max_length=self.config.token_max_length,
            return_tensors="pt",
        )
        word_ids = encoding.word_ids(batch_index=0)
        model_inputs = {k: v.to(self.device) for k, v in encoding.items()}

        with torch.no_grad():
            logits = self.ner_model(**model_inputs).logits

        probs = torch.softmax(logits, dim=-1).squeeze(0).cpu()
        pred_ids = probs.argmax(-1).tolist()
        conf_scores = probs.max(dim=-1).values.tolist()

        word_labels = ["O"] * len(words)
        word_scores = [0.0] * len(words)
        seen_word = set()

        for token_idx, word_idx in enumerate(word_ids):
            if word_idx is None or word_idx in seen_word:
                continue
            seen_word.add(word_idx)
            label = self.ner_id2label.get(int(pred_ids[token_idx]), "O")
            word_labels[word_idx] = label
            word_scores[word_idx] = float(conf_scores[token_idx])

        entities = self._merge_entity_chunks(words, boxes, word_labels, word_scores)
        return entities, word_labels, word_scores

    def _predict_doc_status(
        self, image: Image.Image, words: list[str], boxes: list[list[int]]
    ) -> tuple[str, float, dict[str, float]]:
        encoding = self.processor(
            images=image.convert("RGB"),
            text=words,
            boxes=boxes,
            truncation=True,
            padding="max_length",
            max_length=self.config.token_max_length,
            return_tensors="pt",
        )
        model_inputs = {k: v.to(self.device) for k, v in encoding.items()}

        with torch.no_grad():
            logits = self.doc_model(**model_inputs).logits

        probs = torch.softmax(logits, dim=-1).squeeze(0).cpu().numpy()
        pred_idx = int(np.argmax(probs))
        pred_status = self.doc_id2label.get(pred_idx, "suspicious")
        pred_conf = float(probs[pred_idx])
        all_probs = {self.doc_id2label.get(i, str(i)): float(probs[i]) for i in range(len(probs))}
        return pred_status, pred_conf, all_probs

    @staticmethod
    def _label_to_entity(tag: str) -> str | None:
        if tag == "O":
            return None
        return tag.split("-", 1)[1] if "-" in tag else tag

    def _merge_entity_chunks(
        self, words: list[str], boxes: list[list[int]], labels: list[str], scores: list[float]
    ) -> dict[str, list[dict[str, Any]]]:
        chunks: dict[str, list[dict[str, Any]]] = {}
        active: dict[str, dict[str, Any]] = {}

        for word, box, tag, score in zip(words, boxes, labels, scores):
            entity = self._label_to_entity(tag)
            if entity is None:
                continue

            prefix = tag.split("-", 1)[0] if "-" in tag else "B"
            if prefix == "B" or entity not in active:
                chunk = {"text": [word], "boxes": [box], "scores": [score]}
                chunks.setdefault(entity, []).append(chunk)
                active[entity] = chunk
            else:
                active[entity]["text"].append(word)
                active[entity]["boxes"].append(box)
                active[entity]["scores"].append(score)

        merged: dict[str, list[dict[str, Any]]] = {}
        for entity, items in chunks.items():
            merged[entity] = []
            for item in items:
                xs0 = [b[0] for b in item["boxes"]]
                ys0 = [b[1] for b in item["boxes"]]
                xs1 = [b[2] for b in item["boxes"]]
                ys1 = [b[3] for b in item["boxes"]]
                merged[entity].append(
                    {
                        "text": " ".join(item["text"]).strip(),
                        "bbox": [int(min(xs0)), int(min(ys0)), int(max(xs1)), int(max(ys1))],
                        "score": float(np.mean(item["scores"])),
                    }
                )
        return merged

    @staticmethod
    def _entity_text(entities: dict[str, list[dict[str, Any]]], key: str) -> str:
        return " ".join(item.get("text", "") for item in entities.get(key, [])).strip()

    @staticmethod
    def _regex_any(text: str, patterns: list[str]) -> bool:
        return any(re.search(pattern, text or "", flags=re.IGNORECASE) for pattern in patterns)

    def _build_fraud_flags(
        self, entities: dict[str, list[dict[str, Any]]], tokens: list[str]
    ) -> tuple[dict[str, bool], list[str], list[str], list[str]]:
        presence = {entity: bool(entities.get(entity)) for entity in REQUIRED_ENTITIES}
        missing = [entity for entity, ok in presence.items() if not ok]
        flags: list[str] = []

        ocr_text = " ".join(tokens[:700]).strip()
        title_authority = (self._entity_text(entities, "TITLE") + " " + self._entity_text(entities, "AUTHORITY")).strip()
        strong_in_text = self._regex_any(ocr_text, CNOPT_STRONG_PATTERNS) or self._regex_any(
            title_authority, CNOPT_STRONG_PATTERNS
        )
        context_in_text = self._regex_any(ocr_text, CNOPT_CONTEXT_PATTERNS) or self._regex_any(
            title_authority, CNOPT_CONTEXT_PATTERNS
        )
        if not strong_in_text:
            flags.append("cnopt_strong_keywords_missing")
        if not context_in_text:
            flags.append("cnopt_context_keywords_missing")

        reg_text = self._entity_text(entities, "REG_NUMBER")
        if not reg_text:
            flags.append("reg_number_missing")
        elif not self._regex_any(reg_text, [r"\d{3,}", r"[A-Za-z]{1,5}[- ]?\d{2,}"]):
            flags.append("reg_number_format_unusual")

        if not presence["SIGNATURE"] and not presence["STAMP"]:
            flags.append("signature_and_stamp_missing")

        core_present_count = sum(1 for entity in STRICT_ENTITIES_FOR_ACCEPT if presence.get(entity, False))
        if core_present_count <= 2:
            flags.append("core_entities_sparse")

        severe_flags = [
            flag
            for flag in flags
            if flag in {"cnopt_strong_keywords_missing", "core_entities_sparse"}
        ]
        return presence, missing, flags, severe_flags

    def _fuse_decision(
        self,
        doc_pred_status: str,
        doc_conf: float,
        presence: dict[str, bool],
        missing: list[str],
        flags: list[str],
        severe_flags: list[str],
    ) -> tuple[str, str, float]:
        base_risk = {
            "valid": 0.15,
            "suspicious": 0.75,
            "incomplete": 0.80,
            "wrong_type": 0.95,
        }.get(doc_pred_status, 0.60)
        risk = min(1.0, base_risk + 0.08 * len(flags) + 0.10 * len(severe_flags))

        strict_missing = [entity for entity in STRICT_ENTITIES_FOR_ACCEPT if not presence.get(entity, False)]
        core_present_count = len(STRICT_ENTITIES_FOR_ACCEPT) - len(strict_missing)

        # Be conservative with hard wrong-type rejections to avoid false negatives.
        # OCR quality can vary; uncertain cases should go to manual review.
        wrong_type_trigger = doc_pred_status == "wrong_type" and (
            doc_conf >= 0.55
            or ("cnopt_strong_keywords_missing" in severe_flags and core_present_count <= 2)
        )

        if wrong_type_trigger:
            return "REJECT", "wrong_type", float(risk)
        if doc_pred_status == "incomplete" or len(strict_missing) >= 3:
            return "MANUAL_REVIEW", "incomplete", float(max(risk, 0.80))
        if doc_pred_status == "suspicious":
            return "MANUAL_REVIEW", "suspicious", float(max(risk, 0.70))
        if doc_conf < 0.55:
            return "MANUAL_REVIEW", "suspicious", float(max(risk, 0.65))
        return "ACCEPT", "valid", float(risk)

    @staticmethod
    def _build_message(decision: str, fraud_status: str, missing: list[str], flags: list[str]) -> str:
        if fraud_status == "wrong_type":
            return "Wrong CNOPT file type detected. Please upload a valid CNOPT proof document."
        if fraud_status == "incomplete":
            suffix = f" Missing entities: {', '.join(missing[:4])}." if missing else ""
            return "CNOPT document appears incomplete." + suffix
        if fraud_status == "suspicious":
            suffix = f" Flags: {', '.join(flags[:3])}." if flags else ""
            return "CNOPT document is suspicious and requires manual review." + suffix
        if decision == "ACCEPT":
            return "CNOPT document appears valid."
        return "CNOPT verification completed. Manual review recommended."
