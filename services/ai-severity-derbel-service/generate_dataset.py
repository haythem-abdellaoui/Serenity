"""
============================================================
  DATASET GENERATOR — Medical Severity Classification
  Based on real ICD-10 codes and medical knowledge
============================================================
"""

import csv
import random
import os

# ═══════════════════════════════════════════════════════════
#  MEDICAL KNOWLEDGE BASE (ICD-10 codes with severity)
# ═══════════════════════════════════════════════════════════

DIAGNOSES = {
    "LOW": [
        # Common cold & minor infections
        ("J06.9", "Acute upper respiratory infection, unspecified"),
        ("J00", "Acute nasopharyngitis (common cold)"),
        ("J02.9", "Acute pharyngitis, unspecified"),
        ("J03.90", "Acute tonsillitis, unspecified"),
        ("B34.9", "Viral infection, unspecified"),
        # Skin & minor conditions
        ("L30.9", "Dermatitis, unspecified"),
        ("L50.9", "Urticaria, unspecified"),
        ("L70.0", "Acne vulgaris"),
        ("B07.9", "Viral wart, unspecified"),
        ("L60.0", "Ingrowing nail"),
        # Musculoskeletal minor
        ("M79.3", "Panniculitis, unspecified"),
        ("M54.5", "Low back pain"),
        ("M25.50", "Pain in unspecified joint"),
        ("M79.1", "Myalgia"),
        ("M77.10", "Lateral epicondylitis"),
        # Eye & Ear minor
        ("H10.9", "Conjunctivitis, unspecified"),
        ("H66.90", "Otitis media, unspecified"),
        ("H61.20", "Impacted cerumen, unspecified ear"),
        # Digestive minor
        ("K30", "Functional dyspepsia"),
        ("K29.70", "Gastritis, unspecified"),
        ("K59.00", "Constipation, unspecified"),
        ("R10.9", "Unspecified abdominal pain"),
        # Allergies
        ("J30.1", "Allergic rhinitis due to pollen"),
        ("J30.9", "Allergic rhinitis, unspecified"),
        ("T78.40", "Allergy, unspecified"),
        # Mental health mild
        ("F51.01", "Primary insomnia"),
        ("R45.0", "Nervousness"),
        # Minor injuries
        ("S60.0", "Contusion of finger"),
        ("S90.0", "Contusion of ankle"),
        ("T14.0", "Superficial injury of unspecified body region"),
        # Dental
        ("K02.9", "Dental caries, unspecified"),
        ("K05.10", "Chronic gingivitis"),
        # Nutrition minor
        ("E56.0", "Vitamin E deficiency"),
        ("D50.9", "Iron deficiency anemia, unspecified"),
        # Urinary minor
        ("N39.0", "Urinary tract infection, site not specified"),
        ("R30.0", "Dysuria"),
    ],

    "MEDIUM": [
        # Respiratory moderate
        ("J45.20", "Mild intermittent asthma, uncomplicated"),
        ("J20.9", "Acute bronchitis, unspecified"),
        ("J18.9", "Pneumonia, unspecified organism"),
        ("J44.1", "Chronic obstructive pulmonary disease with exacerbation"),
        # Cardiovascular moderate
        ("I10", "Essential (primary) hypertension"),
        ("I25.10", "Atherosclerotic heart disease"),
        ("I48.0", "Paroxysmal atrial fibrillation"),
        ("I73.9", "Peripheral vascular disease, unspecified"),
        # Neurological moderate
        ("G43.909", "Migraine, unspecified, not intractable"),
        ("G40.909", "Epilepsy, unspecified, not intractable"),
        ("G47.33", "Obstructive sleep apnea"),
        ("G20", "Parkinson disease"),
        # Endocrine moderate
        ("E11.9", "Type 2 diabetes mellitus without complications"),
        ("E03.9", "Hypothyroidism, unspecified"),
        ("E05.90", "Thyrotoxicosis, unspecified"),
        ("E78.5", "Dyslipidemia, unspecified"),
        # Digestive moderate
        ("K21.0", "Gastro-esophageal reflux with esophagitis"),
        ("K50.90", "Crohn disease, unspecified"),
        ("K51.90", "Ulcerative colitis, unspecified"),
        ("K80.20", "Calculus of gallbladder without obstruction"),
        ("K57.30", "Diverticulosis of large intestine"),
        # Mental health moderate
        ("F32.1", "Major depressive disorder, single episode, moderate"),
        ("F41.1", "Generalized anxiety disorder"),
        ("F43.10", "Post-traumatic stress disorder, unspecified"),
        ("F31.9", "Bipolar disorder, unspecified"),
        # Musculoskeletal moderate
        ("M06.9", "Rheumatoid arthritis, unspecified"),
        ("M81.0", "Age-related osteoporosis without fracture"),
        ("M47.812", "Spondylosis with radiculopathy, cervical"),
        # Genitourinary moderate
        ("N18.3", "Chronic kidney disease, stage 3"),
        ("N40.0", "Benign prostatic hyperplasia without obstruction"),
        # Skin moderate
        ("L40.0", "Psoriasis vulgaris"),
        ("L20.9", "Atopic dermatitis, unspecified"),
        # Blood moderate
        ("D64.9", "Anemia, unspecified"),
        ("D69.6", "Thrombocytopenia, unspecified"),
        # Infectious moderate
        ("A09", "Infectious gastroenteritis and colitis"),
        ("B18.2", "Chronic viral hepatitis C"),
    ],

    "HIGH": [
        # Cardiovascular severe
        ("I21.9", "Acute myocardial infarction, unspecified"),
        ("I63.9", "Cerebral infarction, unspecified (stroke)"),
        ("I50.9", "Heart failure, unspecified"),
        ("I26.99", "Pulmonary embolism without acute cor pulmonale"),
        ("I71.3", "Abdominal aortic aneurysm, ruptured"),
        ("I46.9", "Cardiac arrest, cause unspecified"),
        # Cancer / Neoplasms
        ("C34.90", "Malignant neoplasm of unspecified part of bronchus or lung"),
        ("C50.919", "Malignant neoplasm of unspecified site of breast"),
        ("C18.9", "Malignant neoplasm of colon, unspecified"),
        ("C61", "Malignant neoplasm of prostate"),
        ("C25.9", "Malignant neoplasm of pancreas, unspecified"),
        ("C71.9", "Malignant neoplasm of brain, unspecified"),
        ("C22.0", "Liver cell carcinoma"),
        ("C43.9", "Malignant melanoma of skin, unspecified"),
        # Respiratory severe
        ("J96.00", "Acute respiratory failure, unspecified"),
        ("J80", "Acute respiratory distress syndrome"),
        ("J43.9", "Emphysema, unspecified"),
        # Neurological severe
        ("G35", "Multiple sclerosis"),
        ("G12.21", "Amyotrophic lateral sclerosis (ALS)"),
        ("G30.9", "Alzheimer disease, unspecified"),
        ("G91.9", "Hydrocephalus, unspecified"),
        # Renal severe
        ("N18.6", "End stage renal disease"),
        ("N17.9", "Acute kidney failure, unspecified"),
        # Endocrine severe
        ("E10.10", "Type 1 diabetes with ketoacidosis"),
        ("E11.65", "Type 2 diabetes with hyperglycemia"),
        # Infectious severe
        ("A41.9", "Sepsis, unspecified organism"),
        ("B20", "Human immunodeficiency virus (HIV) disease"),
        ("A49.9", "Bacterial infection, unspecified (severe)"),
        # Hepatic severe
        ("K72.00", "Acute and subacute hepatic failure"),
        ("K74.60", "Unspecified cirrhosis of liver"),
        # Trauma severe
        ("S06.9", "Intracranial injury, unspecified"),
        ("T79.4", "Traumatic shock"),
        # Blood severe
        ("D65", "Disseminated intravascular coagulation"),
        ("C95.00", "Acute leukemia of unspecified cell type"),
        # Mental health severe
        ("F20.9", "Schizophrenia, unspecified"),
    ],
}

# ═══════════════════════════════════════════════════════════
#  TEXT AUGMENTATION — Generate realistic diagnosis variants
# ═══════════════════════════════════════════════════════════

def augment_diagnosis(code, name, severity):
    """Generate multiple text variants for a single diagnosis."""
    variants = []

    # Format 1: "CODE - Name" (standard ICD-10 format from our app)
    variants.append(f"{code} - {name}")

    # Format 2: Just the name
    variants.append(name)

    # Format 3: Lowercase name
    variants.append(name.lower())

    # Format 4: Name with additional notes
    notes = {
        "LOW": [
            "mild symptoms, no complications",
            "patient stable, outpatient treatment",
            "minor condition, follow-up in 2 weeks",
            "no acute distress",
            "self-limiting condition",
            "routine exam",
            "slight discomfort reported",
            "minor pain, prescribed rest",
        ],
        "MEDIUM": [
            "moderate symptoms, requires monitoring",
            "chronic condition, ongoing treatment",
            "stable but requires medication adjustment",
            "needs specialist referral",
            "follow-up required in 1 week",
            "recurring symptoms",
            "flare-up event",
            "uncomfortable episode, managing with meds",
        ],
        "HIGH": [
            "severe presentation, urgent care needed",
            "critical condition, ICU admission",
            "life-threatening, immediate intervention",
            "acute emergency, rapid response",
            "advanced stage, palliative care discussed",
            "fatal prognosis risk",
            "immediate surgery required",
            "severe trauma protocol active",
        ],
    }
    
    # Add multiple randomly generated notes to ensure variety
    random_notes = random.sample(notes[severity], k=3)
    for note in random_notes:
        variants.append(f"{name} - {note}")
        variants.append(f"{code}: {name} ({note})")
        variants.append(f"{note.capitalize()} due to {name}")

    # Format 5: Code with abbreviated name
    words = name.split()
    if len(words) > 2:
        short = " ".join(words[:2])
        variants.append(f"{code} {short}")
        variants.append(f"{short.lower()} problem")

    # Format 6: Patient scenarios (noise injection)
    prefixes = ["Patient presents with ", "Suspected ", "Confirmed case of ", "History of ", "Assessment: "]
    variants.append(random.choice(prefixes) + name)
    variants.append(random.choice(prefixes).lower() + name.lower())

    return variants


def generate_dataset(output_path, target_size=10000):
    """Generate the full training dataset CSV."""
    rows = []

    for severity, diagnoses in DIAGNOSES.items():
        for code, name in diagnoses:
            variants = augment_diagnosis(code, name, severity)
            for text in variants:
                rows.append({"diagnosis": text, "severity": severity})

    # Duplicate and shuffle to reach target size
    while len(rows) < target_size:
        severity = random.choice(["LOW", "MEDIUM", "HIGH"])
        code, name = random.choice(DIAGNOSES[severity])
        variants = augment_diagnosis(code, name, severity)
        text = random.choice(variants)
        rows.append({"diagnosis": text, "severity": severity})

    random.shuffle(rows)

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["diagnosis", "severity"])
        writer.writeheader()
        writer.writerows(rows)

    print(f"Dataset generated: {len(rows)} rows -> {output_path}")

    # Stats
    from collections import Counter
    counts = Counter(r["severity"] for r in rows)
    for sev, count in sorted(counts.items()):
        print(f"   {sev}: {count} rows ({count/len(rows)*100:.1f}%)")


if __name__ == "__main__":
    generate_dataset("dataset/medical_severity.csv", target_size=10000)
