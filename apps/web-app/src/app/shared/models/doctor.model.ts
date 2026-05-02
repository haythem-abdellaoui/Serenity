export interface UserProfile {
  id: number;
  avatar: string;
  bio: string;
  isAnonymous: boolean;
  preferredLanguage: string;
  user?: any;
}

export interface DoctorResponse {
  id: number;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  authProvider: string;
  phone: string;
  dateOfBirth: string;
  isActive: boolean;
  createdAt: string;
  role: string;
  profile: UserProfile;
  specialty: string;
  profilePictureUrl: string;
}

export interface DoctorInfo {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  fullName: string;
}

export interface PatientInfo {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  fullName: string;
  assignedDoctorId?: number;
  assignedDoctorName?: string;
}
