export interface DoctorVerification {
  verification_id: number;
  doctorId: number;
  cv?: string;
  diploma?: string;
  nationalId?: string;
  licenseNumber: string;
  status: 'PENDING' | 'VERIFIED' | 'REJECTED';
  submittedAt: string;
  createdAt?: string;
  updatedAt?: string;
  contractApproved?: boolean;
}
