import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { UserService } from '../../core/services/user.service';
import { AuthService } from '../../core/services/auth.service';
import { DoctorService } from '../../core/services/doctor.service';
import { INSURANCE_COMPANIES } from '../../shared/models/insurance.model';
import { UserResponse } from '../../shared/models/user.model';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {
  profileForm!: FormGroup;
  doctorForm!: FormGroup;
  user: UserResponse | null = null;
  loading = true;
  saving = false;
  errorMessage = '';
  successMessage = '';
  editMode = false;
  isDoctor = false;
  imagePreview: string | null = null;
  selectedFile: File | null = null;
  showDeleteModal = false;
  deleteAccountLoading = false;

  selectedAvatarFile: File | null = null;
  avatarPreviewUrl: string | null = null;
  avatarCacheBuster = Date.now();
  languages = [
    { value: 'en', label: 'English' },
    { value: 'fr', label: 'Français' },
    { value: 'es', label: 'Español' },
    { value: 'de', label: 'Deutsch' },
    { value: 'ar', label: 'العربية' }
  ];
  insuranceCompanies = INSURANCE_COMPANIES;

  constructor(
    private readonly fb: FormBuilder,
    private readonly userService: UserService,
    public readonly authService: AuthService,
    private readonly doctorService: DoctorService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    // 👇 check role from JWT
    const currentUser = this.authService.getCurrentUser();
    this.isDoctor = currentUser?.role === 'DOCTOR';

    this.profileForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      lastName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      phone: ['', [Validators.pattern(/^\d{8}$/)]],
      dateOfBirth: [''],
      insuranceCompany: [''],
      bio: ['', [Validators.maxLength(1000)]],
      avatar: [''],
      preferredLanguage: ['en'],
      isAnonymous: [false]
    });

    this.doctorForm = this.fb.group({
      specialty: ['', Validators.required],      // 👈 specialty pas speciality
      profilePictureUrl: ['']                     // 👈 profilePictureUrl
    });

    this.loadProfile();
  }

  loadProfile(): void {
    this.loading = true;
    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        this.user = user;
        this.profileForm.patchValue({
          firstName: user.firstName,
          lastName: user.lastName,
          phone: user.phone || '',
          dateOfBirth: this.formatDateForInput(user.dateOfBirth),
          insuranceCompany: user.insuranceCompany || '',
          bio: user.profile?.bio || '',
          avatar: user.profile?.avatar || '',
          preferredLanguage: user.profile?.preferredLanguage || 'en',
          isAnonymous: user.profile?.isAnonymous || false
        });

        // 👇 prefill doctor fields if available
        if (this.isDoctor) {
          this.doctorForm.patchValue({
            specialty: (user as any).specialty || '',
            profilePictureUrl: (user as any).profilePictureUrl || ''
          });
        }

        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load profile';
        this.loading = false;
      }
    });
  }

  private formatDateForInput(dateStr: string): string {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    const year = d.getUTCFullYear();
    const month = String(d.getUTCMonth() + 1).padStart(2, '0');
    const day = String(d.getUTCDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  toggleEdit(): void {
    this.editMode = !this.editMode;
    this.successMessage = '';
    this.errorMessage = '';
    if (this.editMode) {
      this.selectedAvatarFile = null;
      this.avatarPreviewUrl = this.getAvatarSrc(this.user?.profile?.avatar) || null;
    }
  }

  cancelEdit(): void {
    this.editMode = false;
    this.successMessage = '';
    this.errorMessage = '';
    this.selectedAvatarFile = null;
    this.avatarPreviewUrl = null;
    if (this.user) {
      this.profileForm.patchValue({
        firstName: this.user.firstName,
        lastName: this.user.lastName,
        phone: this.user.phone || '',
        dateOfBirth: this.formatDateForInput(this.user.dateOfBirth),
        insuranceCompany: this.user.insuranceCompany || '',
        bio: this.user.profile?.bio || '',
        avatar: this.user.profile?.avatar || '',
        preferredLanguage: this.user.profile?.preferredLanguage || 'en',
        isAnonymous: this.user.profile?.isAnonymous || false
      });
      if (this.isDoctor) {
        this.doctorForm.patchValue({
          specialty: (this.user as any).specialty || '',
          profilePictureUrl: (this.user as any).profilePictureUrl || ''
        });
      }
    }
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
    }
  }

  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length > 0 ? input.files[0] : null;
    this.selectedAvatarFile = file;
    if (file) {
      this.avatarPreviewUrl = URL.createObjectURL(file);
      // keep form control in sync; backend will overwrite with uploaded URL
      this.profileForm.patchValue({ avatar: '' });
    } else {
      this.avatarPreviewUrl = this.getAvatarSrc(this.user?.profile?.avatar) || null;
    }
  }

  getAvatarSrc(avatar: string | null | undefined): string | null {
    if (!avatar) return null;

    const value = avatar.trim();
    if (!value) return null;

    // Already absolute (http(s), blob, data URLs)
    if (/^(https?:)?\/\//i.test(value) || value.startsWith('blob:') || value.startsWith('data:')) {
      return this.appendCacheBuster(value);
    }

    // Backend commonly returns relative paths like: "uploads/..." or "/uploads/..."
    const base = environment.apiUrl.replace(/\/api\/?$/, '');
    const path = value.startsWith('/') ? value : `/${value}`;
    return this.appendCacheBuster(`${base}${path}`);
  }

  private appendCacheBuster(url: string): string {
    const sep = url.includes('?') ? '&' : '?';
    return `${url}${sep}v=${this.avatarCacheBuster}`;
  }

onSubmit(): void {
    if (this.profileForm.invalid) return;
    if (this.isDoctor && this.doctorForm.invalid) return;

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    if (this.isDoctor) {
      // Doctor update path with FormData
      const formData = new FormData();
      const fieldsToExclude = ['avatar', 'profilePictureUrl'];
      Object.entries(this.profileForm.value).forEach(([key, value]: [string, any]) => {
        if (!fieldsToExclude.includes(key) && value !== null && value !== undefined) {
          formData.append(key, value.toString());
        }
      });

      formData.append('specialty', this.doctorForm?.get('specialty')?.value || '');

      if (this.selectedFile) {
        formData.append('image', this.selectedFile, this.selectedFile.name);
      }

      const doctorId = this.authService.getCurrentUser()?.userId;
      if (!doctorId) {
        this.saving = false;
        this.errorMessage = 'No doctor ID found';
        return;
      }

      this.doctorService.updateDoctor(doctorId, formData).subscribe({
        next: (res: any) => {
          this.saving = false;
          this.editMode = false;
          this.successMessage = 'Profile updated successfully';
          // Reload profile so avatar/url updates reflect backend response.
          this.loadProfile();
        },
        error: (err: any) => {
          this.saving = false;
          this.errorMessage = err.error?.message || 'Failed to update doctor info';
        }
      });
    } else {
      // Regular user update path
      const formValue = { ...this.profileForm.value };
      if (formValue.dateOfBirth) {
        formValue.dateOfBirth = new Date(formValue.dateOfBirth).toISOString();
      } else {
        delete formValue.dateOfBirth;
      }

      const request$ = this.selectedAvatarFile
        ? this.userService.uploadAvatar(this.selectedAvatarFile)
        : this.userService.updateProfile(formValue);

      request$
        .pipe(finalize(() => (this.saving = false)))
        .subscribe({
          next: (user: any) => {
            this.user = user;
            this.editMode = false;
            this.selectedAvatarFile = null;
            this.avatarPreviewUrl = null;
            // Force <img> refresh even if URL string stays the same.
            this.avatarCacheBuster = Date.now();
            this.successMessage = 'Profile updated successfully';
          },
          error: (err: any) => {
            this.errorMessage = err.error?.message || 'Failed to update profile';
          }
        });
    }
  }

  getInitials(): string {
    if (!this.user) return '?';
    return (this.user.firstName?.charAt(0) || '') + (this.user.lastName?.charAt(0) || '');
  }

  getMemberSince(): string {
    if (!this.user?.createdAt) return '';
    return new Date(this.user.createdAt).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  onDoctorImageSelected(event: any): void {
    const file = event.target?.files?.[0];
    if (!file) return;

    this.selectedFile = file; // ← this gets sent to backend

    const reader = new FileReader();
    reader.onload = () => (this.imagePreview = reader.result as string);
    reader.readAsDataURL(file);
  }

  openDeleteModal(): void {
    this.showDeleteModal = true;
    this.errorMessage = '';
  }

  cancelDelete(): void {
    this.showDeleteModal = false;
  }

  confirmDelete(): void {
    const doctorId = this.authService.getCurrentUser()?.userId;
    if (doctorId == null) {
      this.errorMessage = 'Unable to delete account. Please sign in again.';
      this.showDeleteModal = false;
      return;
    }

    this.deleteAccountLoading = true;
    this.errorMessage = '';

    this.doctorService.deleteDoctor(doctorId).subscribe({
      next: () => {
        this.deleteAccountLoading = false;
        this.showDeleteModal = false;
        this.authService.logout();
        void this.router.navigate(['/login']);
      },
      error: (err) => {
        this.deleteAccountLoading = false;
        this.showDeleteModal = false;
        this.errorMessage = err.error?.message || 'Failed to delete account';
      }
    });
  }
}
