"""
============================================================
  DATASET GENERATOR — Drug Recommendation (Mental Health Focus)
  Generates 12,000 rows mapping mental health diagnoses to drugs
============================================================
"""

import csv
import random
import os

# ═══════════════════════════════════════════════════════════
#  PSYCHIATRIC KNOWLEDGE BASE — Diagnosis -> Recommended Drugs
# ═══════════════════════════════════════════════════════════

DIAGNOSIS_DRUGS = {
    # ── DEPRESSIVE DISORDERS ──
    "Major depressive disorder, single episode": ["Sertraline", "Fluoxetine", "Escitalopram"],
    "Major depressive disorder, recurrent": ["Venlafaxine", "Duloxetine", "Bupropion"],
    "Persistent depressive disorder (Dysthymia)": ["Citalopram", "Sertraline", "Mirtazapine"],
    "Seasonal affective disorder": ["Bupropion", "Fluoxetine", "Sertraline"],

    # ── ANXIETY DISORDERS ──
    "Generalized anxiety disorder": ["Sertraline", "Buspirone", "Venlafaxine"],
    "Panic disorder": ["Alprazolam", "Clonazepam", "Paroxetine"],
    "Social anxiety disorder": ["Sertraline", "Propranolol", "Venlafaxine"],
    "Agoraphobia": ["Paroxetine", "Sertraline", "Fluoxetine"],
    "Nervousness or severe stress": ["Hydroxyzine", "Buspirone", "Propranolol"],

    # ── BIPOLAR DISORDERS ──
    "Bipolar I disorder, manic episode": ["Lithium", "Valproate", "Quetiapine"],
    "Bipolar I disorder, depressed episode": ["Lurasidone", "Quetiapine", "Lamotrigine"],
    "Bipolar II disorder": ["Lamotrigine", "Lithium", "Quetiapine"],
    "Cyclothymic disorder": ["Valproate", "Lithium", "Lamotrigine"],

    # ── SCHIZOPHRENIA SPECTRUM ──
    "Schizophrenia, unspecified": ["Risperidone", "Olanzapine", "Aripiprazole"],
    "Schizoaffective disorder": ["Paliperidone", "Clozapine", "Haloperidol"],
    "Brief psychotic disorder": ["Haloperidol", "Risperidone", "Olanzapine"],
    "Delusional disorder": ["Aripiprazole", "Risperidone", "Pimozide"],

    # ── OBSESSIVE-COMPULSIVE DISORDER (OCD) ──
    "Obsessive-compulsive disorder": ["Fluvoxamine", "Clomipramine", "Fluoxetine"],
    "Body dysmorphic disorder": ["Sertraline", "Fluoxetine", "Escitalopram"],
    "Hoarding disorder": ["Venlafaxine", "Paroxetine", "Sertraline"],

    # ── TRAUMA- AND STRESSOR-RELATED ──
    "Post-traumatic stress disorder (PTSD)": ["Sertraline", "Paroxetine", "Prazosin"],
    "Acute stress disorder": ["Clonazepam", "Propranolol", "Hydroxyzine"],
    "Adjustment disorder with depressed mood": ["Escitalopram", "Sertraline", "Bupropion"],

    # ── EATING DISORDERS ──
    "Anorexia nervosa": ["Olanzapine", "Fluoxetine", "Mirtazapine"],
    "Bulimia nervosa": ["Fluoxetine", "Topiramate", "Sertraline"],
    "Binge-eating disorder": ["Lisdexamfetamine", "Topiramate", "Fluoxetine"],

    # ── NEURODEVELOPMENTAL (Adult/Teen) ──
    "Attention-deficit hyperactivity disorder (ADHD)": ["Methylphenidate", "Amphetamine", "Atomoxetine"],
    "Autism spectrum disorder (irritability)": ["Risperidone", "Aripiprazole", "Guanfacine"],
    "Tourette's disorder": ["Haloperidol", "Pimozide", "Clonidine"],

    # ── SLEEP-WAKE DISORDERS ──
    "Primary insomnia": ["Zolpidem", "Eszopiclone", "Trazodone"],
    "Narcolepsy": ["Modafinil", "Armodafinil", "Sodium oxybate"],
    "Restless legs syndrome": ["Pramipexole", "Ropinirole", "Gabapentin"],

    # ── PERSONALITY DISORDERS (Symptom management) ──
    "Borderline personality disorder": ["Lamotrigine", "Aripiprazole", "Fluoxetine"],
    "Schizotypal personality disorder": ["Risperidone", "Olanzapine", "Haloperidol"],

    # ── SUBSTANCE-RELATED & ADDICTIVE DISORDERS ──
    "Alcohol withdrawal syndrome": ["Chlordiazepoxide", "Diazepam", "Lorazepam"],
    "Alcohol dependence": ["Naltrexone", "Acamprosate", "Disulfiram"],
    "Opioid withdrawal": ["Buprenorphine", "Methadone", "Clonidine"],
    "Opioid dependence": ["Buprenorphine", "Naltrexone", "Methadone"],
    "Nicotine dependence": ["Varenicline", "Bupropion", "Nicotine patch"],
}


# ═══════════════════════════════════════════════════════════
#  TEXT AUGMENTATION — Generate realistic psychiatry variants
# ═══════════════════════════════════════════════════════════

def augment_diagnosis_text(name):
    """Generate multiple text variants simulating a psychiatrist's notes."""
    variants = [name, name.lower()]

    # Psychiatric prefixes
    prefixes = [
        "Patient presents with ", "Suspected ", "Confirmed ",
        "History of ", "Assessment: ", "DSM-5 Diagnosis: ",
        "Acute exacerbation of ", "Chronic ", "Severe ", "Mild ",
        "High risk of "
    ]
    for prefix in random.sample(prefixes, k=4):
        variants.append(prefix + name)
        variants.append((prefix + name).lower())

    # Psychiatric clinical notes (Noise injection)
    notes = [
        "patient stable", "suicidal ideation present", "experiencing hallucinations",
        "poor sleep quality", "high anxiety levels", "mood swings reported",
        "resistant to therapy", "first psychotic episode", "compliant with meds",
        "worsening symptoms", "needs dosage adjustment", "outpatient follow-up",
        "requires hospitalization", "agitated behavior", "cognitive decline"
    ]
    for note in random.sample(notes, k=4):
        variants.append(f"{name} - {note}")
        variants.append(f"{name.lower()}, {note}")

    # Abbreviated/Shorthand (Very common in psychiatry)
    words = name.split()
    if len(words) > 1:
        variants.append(" ".join(words[:2]))
        variants.append(" ".join(words[:2]).lower())
        
    return variants


def generate_dataset(output_path, target_size=12000):
    """Generate the full training dataset CSV."""
    rows = []
    all_diagnoses = list(DIAGNOSIS_DRUGS.keys())

    # Ensure every diagnosis and its variants are included first
    for diag_name in all_diagnoses:
        drugs = DIAGNOSIS_DRUGS[diag_name]
        drugs_str = ", ".join(drugs)
        variants = augment_diagnosis_text(diag_name)
        for text in variants:
            rows.append({"diagnosis": text, "recommended_drugs": drugs_str})

    # Fill up to target size (12,000) with random selections and augmentations
    while len(rows) < target_size:
        diag_name = random.choice(all_diagnoses)
        drugs = DIAGNOSIS_DRUGS[diag_name]
        drugs_str = ", ".join(drugs)
        variants = augment_diagnosis_text(diag_name)
        text = random.choice(variants)
        rows.append({"diagnosis": text, "recommended_drugs": drugs_str})

    # Shuffle dataset
    random.shuffle(rows)

    # Save to CSV
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["diagnosis", "recommended_drugs"])
        writer.writeheader()
        writer.writerows(rows)

    print(f"Mental Health Drug dataset generated: {len(rows)} rows -> {output_path}")

    # Stats
    unique_drugs = set()
    for d in DIAGNOSIS_DRUGS.values():
        unique_drugs.update(d)
    print(f"   Unique Psychiatric Diagnoses: {len(DIAGNOSIS_DRUGS)}")
    print(f"   Unique Psychotropic Drugs: {len(unique_drugs)}")


if __name__ == "__main__":
    generate_dataset("dataset/drug_recommendation.csv", target_size=12000)
