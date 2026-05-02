"""Generate a synthetic insurance claims dataset (~150k rows).

Columns mirror the real InsuranceClaim + InsuranceClaimOcrAudit entities
so the trained model aligns with what the insurance-service actually stores.
"""

import csv
import random
import datetime
from pathlib import Path

random.seed(42)

NUM_ROWS = 150_000
OUTPUT = Path(__file__).parent / "claims_risk_raw.csv"

COMPANIES = ["CNAM", "STAR", "GAT", "CARTE", "MAE", "AMI", "MAGHREBIA", "ASTREE", "LLOYD", "BH"]
DATE_START = datetime.date(2024, 1, 1)
DATE_END = datetime.date(2026, 3, 31)
DATE_RANGE_DAYS = (DATE_END - DATE_START).days

OCR_DECISIONS = ["PASS", "MINOR_MISMATCH", "MAJOR_BLOCKED"]
FRAUD_RATE = 0.12

HEADERS = [
    "claim_id", "user_id", "amount", "reimbursement_amount",
    "insurance_company", "insurance_grade", "file_count",
    "claim_date", "status",
    "ocr_decision", "ocr_mismatch_count", "ocr_major_count", "ocr_minor_count",
    "ocr_submitted_amount", "ocr_extracted_amount",
    "user_total_claims", "user_claims_30d", "days_since_last_claim",
    "user_rejected_claims",
    "fraud_flag",
]


def rand_date():
    return DATE_START + datetime.timedelta(days=random.randint(0, DATE_RANGE_DAYS))


def maybe_missing(value, rate=0.02):
    return "" if random.random() < rate else value


def generate_row(idx, is_fraud):
    claim_id = idx
    user_id = random.randint(1, 5000)
    company = random.choice(COMPANIES)
    grade = round(random.uniform(1.0, 5.0), 1)

    amount = round(random.uniform(20, 10000), 2)
    if is_fraud:
        reimb_ratio = random.uniform(0.6, 1.0)
    else:
        reimb_ratio = random.uniform(0.5, 0.95)
    reimbursement = round(amount * reimb_ratio, 2)

    file_count = random.randint(0, 8) if not is_fraud else random.choice([0, 0, 1, 1, 2, 6, 8])
    claim_date = rand_date().isoformat()

    statuses = ["SUBMITTED", "UNDER_REVIEW", "NEEDS_INFO", "APPROVED",
                "PARTIALLY_APPROVED", "PAID", "REJECTED"]
    if is_fraud:
        status = random.choices(statuses, weights=[15, 20, 20, 5, 5, 2, 33])[0]
    else:
        status = random.choices(statuses, weights=[10, 15, 10, 25, 15, 20, 5])[0]

    # OCR fields — ~15% of claims haven't been OCR-analyzed yet
    has_ocr = random.random() > 0.15
    if has_ocr:
        if is_fraud:
            ocr_decision = random.choices(OCR_DECISIONS, weights=[15, 40, 45])[0]
        else:
            ocr_decision = random.choices(OCR_DECISIONS, weights=[70, 25, 5])[0]

        if ocr_decision == "MAJOR_BLOCKED":
            mismatch = random.randint(2, 6)
            major = random.randint(1, mismatch)
            minor = mismatch - major
        elif ocr_decision == "MINOR_MISMATCH":
            mismatch = random.randint(1, 3)
            major = 0
            minor = mismatch
        else:
            mismatch = 0
            major = 0
            minor = 0

        ocr_submitted = amount
        if is_fraud and random.random() < 0.45:
            ocr_extracted = round(amount * random.uniform(0.5, 0.92), 2)
        else:
            ocr_extracted = round(amount * random.uniform(0.95, 1.05), 2)
    else:
        ocr_decision = ""
        mismatch = ""
        major = ""
        minor = ""
        ocr_submitted = ""
        ocr_extracted = ""

    # User history enrichment
    if is_fraud:
        user_total = random.randint(3, 40)
        user_30d = random.randint(1, 8)
        days_last = random.choice([0, 1, 2, 3, 5, 7, 10])
        user_rejected = random.randint(1, 10)
    else:
        user_total = random.randint(0, 20)
        user_30d = random.randint(0, 3)
        days_last = random.randint(5, 365)
        user_rejected = random.randint(0, 2)

    row = [
        claim_id, user_id,
        maybe_missing(str(amount)), maybe_missing(str(reimbursement)),
        company, grade, file_count,
        claim_date, status,
        maybe_missing(str(ocr_decision)),
        maybe_missing(str(mismatch)), maybe_missing(str(major)), maybe_missing(str(minor)),
        maybe_missing(str(ocr_submitted)), maybe_missing(str(ocr_extracted)),
        maybe_missing(str(user_total)), maybe_missing(str(user_30d)),
        maybe_missing(str(days_last)), maybe_missing(str(user_rejected)),
        1 if is_fraud else 0,
    ]
    return row


def main():
    rows = []
    for i in range(1, NUM_ROWS + 1):
        is_fraud = random.random() < FRAUD_RATE
        rows.append(generate_row(i, is_fraud))

    random.shuffle(rows)

    with open(OUTPUT, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(HEADERS)
        writer.writerows(rows)

    fraud_count = sum(1 for r in rows if r[-1] == 1)
    print(f"Generated {len(rows)} rows -> {OUTPUT}")
    print(f"Fraud: {fraud_count} ({fraud_count / len(rows) * 100:.1f}%)")
    print(f"Legit: {len(rows) - fraud_count} ({(len(rows) - fraud_count) / len(rows) * 100:.1f}%)")


if __name__ == "__main__":
    main()
