export interface InsuranceClaimRequest {
  description: string;
  amount: number;
  insuranceCompany: string;
  insuranceGrade: number;
}

export interface InsuranceClaimResponse {
  id: number;
  description: string;
  claimDate: string;
  amount: number;
  insuranceCompany: string;
  insuranceGrade: number;
  reimbursementAmount: number;
  reason?: string | null;
  status: string;
  externalRef: string;
  filePaths: string[];
  userId: number;
  userFullName: string;
  infoRequestReason?: string | null;
  infoRequestDeadline?: string | null;
  infoRequestedAt?: string | null;
  infoRespondedAt?: string | null;
  remboursements: RemboursementResponse[];
}

export interface InsuranceClaimTransition {
  id: number;
  fromStatus?: string | null;
  toStatus: string;
  changedByUserId: number;
  changedByRole: string;
  reason?: string | null;
  changedAt: string;
}

export interface ClaimRiskScoreResponse {
  riskScore: number;
  riskBand: 'LOW' | 'MEDIUM' | 'HIGH' | string;
  topReasons: string[];
}

export const INSURANCE_COMPANIES = Array.from(
  { length: 10 },
  (_, index) => `Insurance ${index + 1}`
);

export const INSURANCE_GRADES: { value: number; label: string; percentage: number }[] = [
  { value: 1, label: 'Grade 1', percentage: 10 },
  { value: 2, label: 'Grade 2', percentage: 12 },
  { value: 3, label: 'Grade 3', percentage: 18 },
  { value: 4, label: 'Grade 4', percentage: 25 },
  { value: 5, label: 'Grade 5', percentage: 45 }
];

export interface RemboursementResponse {
  id: number;
  montant: number;
  date: string;
  statut: string;
  claimId: number;
}

/** Backend JPQL report: claim + joined remboursement totals + OCR audit count. */
export interface ClaimRemittanceOcrSummary {
  claimId: number;
  externalRef?: string | null;
  userId: number;
  status: string;
  amount: number;
  reimbursementAmount: number;
  insuranceCompany: string;
  claimDate: string;
  totalRemboursementPaid: number;
  remboursementCount: number;
  ocrAuditCount: number;
}

export interface InsuranceNotification {
  id: number;
  userId: number;
  claimId: number | null;
  type:
    | 'CLAIM_SENT_TO_INSURER'
    | 'CLAIM_APPROVED'
    | 'CLAIM_REJECTED'
    | 'DOCUMENTS_REQUESTED'
    | 'DOCUMENTS_SUBMITTED'
    | 'OCR_MINOR_MISMATCH'
    | 'OCR_MAJOR_BLOCKED';
  title: string;
  message: string;
  isRead: boolean;
  createdAt: string;
}

export interface NotificationUnreadCountResponse {
  unreadCount: number;
}
