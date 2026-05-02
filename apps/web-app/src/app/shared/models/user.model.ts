export interface UserRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
  dateOfBirth?: string;
  insuranceCompany?: string;
  role?: string;
}

export interface UserResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone: string;
  dateOfBirth: string;
  insuranceCompany?: string;
  isActive: boolean;
  isPermanentlyBanned?: boolean;
  bannedUntil?: string | null;
  createdAt: string;
  role: string;
  profile: UserProfile;
}

export type BanDuration = 'ONE_DAY' | 'THREE_DAYS' | 'ONE_WEEK' | 'ONE_MONTH' | 'PERMANENT';

export interface UserProfile {
  id: number;
  avatar: string;
  bio: string;
  isAnonymous: boolean;
  preferredLanguage: string;
}

export interface ProfileUpdateRequest {
  firstName: string;
  lastName: string;
  phone?: string;
  dateOfBirth?: string;
  insuranceCompany?: string;
  bio?: string;
  avatar?: string;
  preferredLanguage?: string;
  isAnonymous?: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  userId: number;
  email: string;
  role: string;
  /** Synced from profile (`/users/me`) for pending-doctor polling; `1` means active. */
  is_active?: number;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ForgotPasswordVerifyRequest {
  email: string;
  otp: string;
}

export interface ForgotPasswordResetRequest {
  token: string;
  newPassword: string;
}

export interface MessageResponse {
  message: string;
}

export interface ForgotPasswordVerifyResponse extends MessageResponse {
  resetToken: string;
}

/** Short user row for doctor/patient pickers (GET /api/users/lookup/doctors | .../patients). */
export interface UserLookup {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
}

export function formatUserLookupName(u: { firstName?: string | null; lastName?: string | null }): string {
  const parts = [u.firstName, u.lastName].filter(
    (x): x is string => typeof x === 'string' && x.trim().length > 0
  );
  return parts.length > 0 ? parts.join(' ') : '—';
}
