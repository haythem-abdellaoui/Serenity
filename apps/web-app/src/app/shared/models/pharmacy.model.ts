export type PrescriptionStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'READY_FOR_PICKUP'
  | 'COLLECTED'
  | 'EXPIRED';

export interface PharmacyUpsertRequest {
  name: string;
  licenseNumber: string;
  phone?: string;
  openingHours?: string;
  addressLine?: string;
  city?: string;
  governorate?: string;
  latitude: number;
  longitude: number;
  supportsEmergency?: boolean;
}

export interface PharmacyResponse {
  id: number;
  ownerUserId: number;
  name: string;
  licenseNumber: string;
  phone?: string;
  openingHours?: string;
  addressLine?: string;
  city?: string;
  governorate?: string;
  latitude?: number;
  longitude?: number;
  supportsEmergency?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PatientDefaultPharmacyRequest {
  pharmacyId: number;
}

export interface PatientDefaultPharmacyResponse {
  patientId: number;
  pharmacyId: number;
  pharmacyName: string;
  phone?: string;
  openingHours?: string;
  addressLine?: string;
  city?: string;
  governorate?: string;
  latitude?: number;
  longitude?: number;
  supportsEmergency?: boolean;
  selectedAt?: string;
}

export interface PharmacyCandidateResponse {
  id: number;
  name: string;
  phone?: string;
  openingHours?: string;
  addressLine?: string;
  city?: string;
  governorate?: string;
  latitude?: number;
  longitude?: number;
  supportsEmergency?: boolean;
  distanceKm?: number;
}

export interface PrescriptionStatusUpdateRequest {
  status: PrescriptionStatus;
  rejectionReason?: string;
}

export interface PrescriptionResponse {
  id: number;
  pharmacyId?: number;
  pharmacyName?: string;
  doctorId: number;
  patientId: number;
  doctorName: string;
  patientName: string;
  assignedToPharmacy?: boolean;
  assignmentMessage?: string;
  medicationName?: string;
  dosage?: string;
  quantity?: number;
  instructions?: string;
  medicineLines?: PrescriptionLineResponse[];
  status: PrescriptionStatus;
  rejectionReason?: string;
  readyAt?: string;
  insuranceDocumentAvailable?: boolean;
  insuranceDocumentUploadedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AlternativePharmacyOption {
  pharmacyId: number;
  pharmacyName: string;
  addressLine?: string;
  city?: string;
  governorate?: string;
  latitude?: number;
  longitude?: number;
  distanceKm?: number;
  availableQuantity?: number;
}

export interface PerMedicineAlternative {
  lineId: number;
  medicationName: string;
  requiredQuantity: number;
  pharmacies: AlternativePharmacyOption[];
}

export type RecommendationMode = 'FULL_MATCH' | 'PARTIAL_FALLBACK' | 'NONE';

export interface PrescriptionAlternativeResponse {
  prescriptionId: number;
  status: PrescriptionStatus;
  latitude: number;
  longitude: number;
  fullMatchRadiusKm: number;
  partialRadiusKm: number;
  recommendedMode: RecommendationMode;
  message: string;
  fullMatchPharmacies: AlternativePharmacyOption[];
  perMedicineAlternatives: PerMedicineAlternative[];
  selectablePharmacies: AlternativePharmacyOption[];
}

export interface PrescriptionPharmacyReassignRequest {
  pharmacyId: number;
  latitude: number;
  longitude: number;
}

export interface PrescriptionLineResponse {
  id: number;
  medicationName: string;
  dosage: string;
  frequency?: string;
  quantity: number;
  startDate?: string;
  endDate?: string;
  instructions?: string;
}

export type StockState = 'IN_STOCK' | 'OUT_OF_STOCK';

export type PharmacyApplicationStatus = 'SUBMITTED' | 'REJECTED' | 'APPROVED';

export interface PharmacyApplicationSubmitRequest {
  firstName: string;
  lastName: string;
  email: string;
  cinNumber: string;
  cnopNumber: string;
  pharmacyName: string;
  authorizationReferenceNumber: string;
  phone?: string;
  openingHours?: string;
  addressLine: string;
  city: string;
  governorate: string;
  latitude: number;
  longitude: number;
}

export interface PharmacyApplicationResponse {
  id: number;
  userId: number;
  status: PharmacyApplicationStatus;
  submittedAt?: string;
  reviewedAt?: string;
  reviewedByAdminId?: number;
  reviewComment?: string;
  firstName: string;
  lastName: string;
  email: string;
  cinNumber: string;
  cnopNumber: string;
  pharmacyName: string;
  authorizationReferenceNumber: string;
  phone?: string;
  openingHours?: string;
  addressLine: string;
  city: string;
  governorate: string;
  latitude: number;
  longitude: number;
  cinDocumentUploaded: boolean;
  cnoptProofUploaded: boolean;
  legalDocumentUploaded: boolean;
}

export interface AdminPharmacyApplicationSummary {
  id: number;
  userId: number;
  applicantName: string;
  email: string;
  pharmacyName: string;
  city: string;
  governorate: string;
  status: PharmacyApplicationStatus;
  submittedAt?: string;
  reviewedAt?: string;
}

export interface AdminPharmacyApplicationDetails {
  id: number;
  userId: number;
  status: PharmacyApplicationStatus;
  submittedAt?: string;
  reviewedAt?: string;
  reviewedByAdminId?: number;
  reviewComment?: string;
  firstName: string;
  lastName: string;
  email: string;
  cinNumber: string;
  cnopNumber: string;
  pharmacyName: string;
  authorizationReferenceNumber: string;
  phone?: string;
  openingHours?: string;
  addressLine: string;
  city: string;
  governorate: string;
  latitude: number;
  longitude: number;
  cinDocumentUrl?: string;
  cnoptProofUrl?: string;
  legalDocumentUrl?: string;
  cnoptMlDecision?: string;
  cnoptMlFraudStatus?: string;
  cnoptMlMessage?: string;
  cnoptMlRiskScore?: number;
  cnoptMlCheckedAt?: string;
}

export interface StockItemCreateRequest {
  medicineName: string;
  quantity: number;
  imageUrl?: string;
  description?: string;
}

export interface StockQuantityIncrementRequest {
  incrementBy: number;
}

export interface StockItemRenameRequest {
  medicineName: string;
}

export interface StockItemResponse {
  id: number;
  pharmacyId: number;
  medicineName: string;
  quantity: number;
  imageUrl?: string;
  description?: string;
  state: StockState;
  archived: boolean;
  createdAt: string;
  updatedAt: string;
}
