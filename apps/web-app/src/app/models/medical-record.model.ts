/** Aligné sur tn.esprit.arctic.derbelmicroservice.entity.enums.Severity */
export type Severity = 'LOW' | 'MEDIUM' | 'HIGH';

/**
 * Aligné sur MedicalRecordResponseDTO
 */
export interface MedicalRecord {
  id: number;
  diagnosis: string;
  notes: string | null;
  date: string;
  severity: Severity;
  status: string;
  patientId: number;
  patientFirstName: string | null;
  patientLastName: string | null;
  doctorId: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * Aligné sur MedicalRecordRequestDTO
 */
export interface MedicalRecordRequest {
  diagnosis: string;
  notes: string | null;
  date: string;
  severity: Severity;
  status: string;
  patientId: number;
  doctorId: number;
}
