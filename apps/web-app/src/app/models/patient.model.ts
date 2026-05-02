/**
 * Aligné sur PatientResponseDTO / entité Patient (champs exposés API)
 */
export interface Patient {
  id: number;
  firstName: string;
  lastName: string;
  dateOfBirth: string | null;
  gender: string | null;
  bloodType: string | null;
  allergies: string | null;
  phone: string | null;
  createdAt: string;
  updatedAt: string;
}

/**
 * Aligné sur PatientRequestDTO (création / mise à jour)
 */
export interface PatientRequest {
  firstName: string;
  lastName: string;
  dateOfBirth: string | null;
  gender: string | null;
  bloodType: string | null;
  allergies: string | null;
  phone: string | null;
}
