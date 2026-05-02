import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuthResponse,
  ForgotPasswordResetRequest,
  ForgotPasswordVerifyRequest,
  ForgotPasswordVerifyResponse,
  LoginRequest,
  MessageResponse,
  UserRequest
} from '../../shared/models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly API_URL = `${environment.apiUrl}/auth`;
  private readonly TOKEN_KEY = 'auth_token';
  private readonly USER_KEY = 'auth_user';

  private readonly currentUserSubject = new BehaviorSubject<AuthResponse | null>(this.getStoredUser());
  currentUser$ = this.currentUserSubject.asObservable();

  private readonly onLogoutCallbacks: (() => void)[] = [];

  constructor(private readonly http: HttpClient) {}

  register(request: UserRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/register`, request).pipe(
      tap(response => this.storeAuth(response))
    );
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/login`, request).pipe(
      tap(response => this.storeAuth(response))
    );
  }

  loginWithGoogle(idToken: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/oauth2/google`, { token: idToken }).pipe(
      tap(response => this.storeAuth(response))
    );
  }

  loginWithFacebook(accessToken: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/oauth2/facebook`, { token: accessToken }).pipe(
      tap(response => this.storeAuth(response))
    );
  }

  requestPasswordOtp(email: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.API_URL}/password/forgot/request`, { email });
  }

  verifyPasswordOtp(email: string, otp: string): Observable<ForgotPasswordVerifyResponse> {
    const request: ForgotPasswordVerifyRequest = { email, otp };
    return this.http.post<ForgotPasswordVerifyResponse>(`${this.API_URL}/password/forgot/verify`, request);
  }

  resetForgottenPassword(token: string, newPassword: string): Observable<MessageResponse> {
    const request: ForgotPasswordResetRequest = { token, newPassword };
    return this.http.post<MessageResponse>(`${this.API_URL}/password/forgot/reset`, request);
  }

  onLogout(callback: () => void): void {
    this.onLogoutCallbacks.push(callback);
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.currentUserSubject.next(null);
    this.onLogoutCallbacks.forEach(cb => cb());
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  hasRole(role: string): boolean {
    const user = this.currentUserSubject.value;
    const actualRole = this.normalizeRole(user?.role);
    const requiredRole = this.normalizeRole(role);
    if (!actualRole || !requiredRole) {
      return false;
    }
    return actualRole === requiredRole;
  }

  isAdmin(): boolean {
    return this.hasRole('ADMIN');
  }

  isPatient(): boolean {
    return this.hasRole('PATIENT');
  }

  isDoctor(): boolean {
    return this.hasRole('DOCTOR');
  }

  getCurrentUser(): AuthResponse | null {
    return this.currentUserSubject.value;
  }

  getUserId(): number | null {
    return this.currentUserSubject.value?.userId ?? null;
  }

  /** Updates stored auth user with activation flag from `UserService` / profile API (`isActive` → `is_active`). */
  mergeProfileActivation(isActive: boolean): void {
    const cur = this.getCurrentUser();
    if (!cur) {
      return;
    }
    const next: AuthResponse = { ...cur, is_active: isActive ? 1 : 0 };
    localStorage.setItem(this.USER_KEY, JSON.stringify(next));
    this.currentUserSubject.next(next);
  }

  storeAuth(response: AuthResponse): void {
    localStorage.setItem(this.TOKEN_KEY, response.accessToken);
    localStorage.setItem(this.USER_KEY, JSON.stringify(response));
    this.currentUserSubject.next(response);
  }

  private getStoredUser(): AuthResponse | null {
    const stored = localStorage.getItem(this.USER_KEY);
    return stored ? JSON.parse(stored) : null;
  }

  updateUserRole(role: string) {
    const token = this.getToken();
    if (!token) throw new Error('Not authenticated');

    return this.http
      .put<AuthResponse>(
        `${environment.apiUrl}/users/update-role?role=${role.toUpperCase()}`,
        {},
        { headers: { Authorization: `Bearer ${token}` } }
      )
      .pipe(
        tap((response: AuthResponse) => {
          console.log('Role updated with new JWT:', response.role);
          console.log('New accessToken:', response.accessToken);
          
          // Store the new token and user data
          this.storeAuth(response);
        })
      );
  }

  addDoctor(userId: number, speciality: string, image: File) {
    const formData = new FormData()

    formData.append('speciality', speciality)
    formData.append('image', image)

    return this.http.post(`${environment.apiUrl}/doctors/${userId}`, formData)
  }

  addDoctorVerification( cv: File, diploma: File, licenseNumber: string, nationalId: string) {
    const formData = new FormData()
    
    const token = this.getToken()
    
    formData.append('cv', cv)
    formData.append('diploma', diploma)
    formData.append('licenseNumber', licenseNumber)
    formData.append('nationalId', nationalId)

    return this.http.post(
      `${environment.apiUrl}/doctor-verifications/add_verification`,
      formData,
      { headers: { Authorization: `Bearer ${token}` } }
    ).pipe(
      tap((response: any) => {
        console.log('Doctor verification submitted:', response);
        // Backend should handle role/token updates if needed
      })
    );
  }


  private normalizeRole(role: string | null | undefined): string {
    const value = role?.trim().toUpperCase() ?? '';
    return value.startsWith('ROLE_') ? value.substring(5) : value;
  }

  setToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }
}
