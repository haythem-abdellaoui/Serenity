import { Medicine } from './medicine.model';

export interface PrescriptionItem {
  id: number;
  medicine: Medicine;
  dosage: string;
  frequency: string;
  quantity: number;
  startDate: string;
  endDate: string | null;
  instructions: string | null;
  isAiRecommended?: boolean;
}

export interface PrescriptionItemRequest {
  medicineId: number;
  dosage: string;
  frequency: string;
  quantity: number;
  startDate: string;
  endDate?: string | null;
  instructions?: string | null;
  isAiRecommended?: boolean;
}

export interface Prescription {
  id: number;
  medicalRecordId: number;
  patientId: number;
  doctorId: number;
  status: string;
  items: PrescriptionItem[];
  imageUrl?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PrescriptionRequest {
  medicalRecordId: number;
  patientId: number;
  doctorId?: number;
  status?: string;
  items: PrescriptionItemRequest[];
  imageBase64?: string;
}
