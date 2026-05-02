import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { Router, RouterLink } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { startWith, switchMap } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { DoctorVerificationService } from '../../../core/services/doctor-verification.service';
import { UserService } from '../../../core/services/user.service';
import { DoctorVerification } from '../../../shared/models/doctor-verification.model';
import { environment } from '../../../../environments/environment';

const POLL_INTERVAL_MS = 5000;

@Component({
  selector: 'app-doctor-verification-pending',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './doctor-verification-pending.component.html',
  styleUrl: './doctor-verification-pending.component.scss'
})
export class DoctorVerificationPendingComponent implements OnInit, OnDestroy {
  form!: FormGroup;
  verification: DoctorVerification | null = null;
  loadingVerification = true;
  saving = false;
  successMessage = '';
  errorMessage = '';

  cvPreviewUrl: SafeUrl | null = null;
  diplomaPreviewUrl: SafeUrl | null = null;

  // New file selection
  cvFile: File | null = null;
  diplomaFile: File | null = null;
  newCvPreview: string | null = null;
  newDiplomaPreview: string | null = null;

  readonly pendingMessage =
    'Your verification is pending approval by the admin. You can update your details below while you wait.';

  private readonly objectUrls: string[] = [];
  private pollSub?: Subscription;

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly doctorVerificationService: DoctorVerificationService,
    private readonly userService: UserService,
    private readonly router: Router,
    private readonly http: HttpClient,
    private readonly sanitizer: DomSanitizer
  ) {}

  private get filesBaseUrl(): string {
    return environment.apiUrl.replace(/\/api\/?$/, '');
  }

  ngOnInit(): void {
    this.form = this.fb.group({
      licenseNumber: [
        '',
        [Validators.required, Validators.minLength(5), Validators.maxLength(20), Validators.pattern('^[A-Za-z0-9 ]+$')]
      ],
      nationalId: [
        '',
        [Validators.required, Validators.minLength(8), Validators.maxLength(8), Validators.pattern('^[0-9 ]+$')]
      ]
    });

    const doctorId = Number(localStorage.getItem('userId'));
    const token = this.authService.getToken();
    console.log('Token:', token);
    console.log('Doctor ID:', doctorId);
    if (!doctorId) {
      this.loadingVerification = false;
      this.errorMessage = 'You must be signed in to view this page.';
      return;
    }

    this.doctorVerificationService.getVerificationByDoctorId(doctorId).subscribe({
      next: (v) => {
        this.loadingVerification = false;
        if (!v) {
          this.errorMessage =
            'No verification record was found. Please submit your verification first.';
          return;
        }
        this.verification = v;
        this.form.patchValue({
          licenseNumber: v.licenseNumber,
          nationalId: v.nationalId ?? ''
        });
        this.loadDocPreview('cv', v.cv);
        this.loadDocPreview('diploma', v.diploma);
      },
      error: () => {
        this.loadingVerification = false;
        this.errorMessage = 'Could not load your verification. Please try again later.';
      }
    });

    this.pollSub = interval(POLL_INTERVAL_MS)
      .pipe(
        startWith(0),
        switchMap(() => this.userService.refreshCurrentUser())
      )
      .subscribe({
        next: (user) => {
          this.authService.mergeProfileActivation(!!user.isActive);
          const authUser = this.authService.getCurrentUser();
          if (authUser?.is_active === 1) {
            void this.router.navigate(['/']);
          }
        },
        error: () => {
          /* Polling should not block the UI; failures are ignored until the next tick. */
        }
      });
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
    this.objectUrls.forEach((u) => URL.revokeObjectURL(u));
  }

  // --- File selection handlers ---

  onCvSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFile(input.files[0], 'cv');
    }
    input.value = ''; // reset so re-selecting the same file triggers change
  }

  onDiplomaSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFile(input.files[0], 'diploma');
    }
    input.value = '';
  }

  private handleFile(file: File, kind: 'cv' | 'diploma'): void {
    const validTypes = ['image/png', 'image/jpeg', 'image/jpg'];
    if (!validTypes.includes(file.type)) {
      this.errorMessage = `${kind === 'cv' ? 'CV' : 'Diploma'} must be an image (PNG, JPG).`;
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.errorMessage = `${kind === 'cv' ? 'CV' : 'Diploma'} file must be less than 5 MB.`;
      return;
    }
    this.errorMessage = '';

    if (kind === 'cv') {
      this.cvFile = file;
    } else {
      this.diplomaFile = file;
    }

    // Generate local preview
    const reader = new FileReader();
    reader.onload = (e) => {
      if (kind === 'cv') {
        this.newCvPreview = e.target?.result as string;
      } else {
        this.newDiplomaPreview = e.target?.result as string;
      }
    };
    reader.readAsDataURL(file);
  }

  removeCv(): void {
    this.cvFile = null;
    this.newCvPreview = null;
  }

  removeDiploma(): void {
    this.diplomaFile = null;
    this.newDiplomaPreview = null;
  }

  getFileName(file: File | null): string {
    return file ? file.name : '';
  }

  getFileSize(file: File | null): string {
    if (!file) return '';
    const mb = (file.size / (1024 * 1024)).toFixed(2);
    return `${mb} MB`;
  }

  // --- Submit ---

  onSubmit(): void {
  if (this.form.invalid || !this.verification) {
    this.form.markAllAsTouched();
    return;
  }

  this.saving = true;
  this.successMessage = '';
  this.errorMessage = '';

  const { licenseNumber, nationalId } = this.form.getRawValue();

  const formData = new FormData();
  formData.append('licenseNumber', licenseNumber.trim());
  formData.append('nationalId', String(nationalId).replace(/\s/g, ''));

  if (this.cvFile) {
    formData.append('cv', this.cvFile, this.cvFile.name);
  }

  if (this.diplomaFile) {
    formData.append('diploma', this.diplomaFile, this.diplomaFile.name);
  }

  this.doctorVerificationService.updateVerification(this.verification.verification_id, formData)
    .subscribe({
      next: (res) => {
        this.saving = false;
        this.verification = res;
        this.successMessage = 'Your verification was updated successfully.';

        this.cvFile = null;
        this.diplomaFile = null;
        this.newCvPreview = null;
        this.newDiplomaPreview = null;

        this.cvPreviewUrl = null;
        this.diplomaPreviewUrl = null;
        this.loadDocPreview('cv', res.cv);
        this.loadDocPreview('diploma', res.diploma);
      },
      error: (err) => {
        this.saving = false;
        this.errorMessage = err.error?.message || 'Failed to update verification. Please try again.';
      }
    });
}

  private loadDocPreview(kind: 'cv' | 'diploma', path: string | undefined): void {
    if (!path) {
      return;
    }
    const normalizedPath = path.replace(/\\/g, '/');
    const url = `${this.filesBaseUrl}/${normalizedPath}`;
    const token = this.authService.getToken();
    const headers = token
      ? new HttpHeaders({ Authorization: `Bearer ${token}` })
      : undefined;
    this.http.get(url, { responseType: 'blob', headers }).subscribe({
      next: (blob) => {
        const objectUrl = URL.createObjectURL(blob);
        this.objectUrls.push(objectUrl);
        const safe = this.sanitizer.bypassSecurityTrustUrl(objectUrl);
        if (kind === 'cv') {
          this.cvPreviewUrl = safe;
        } else {
          this.diplomaPreviewUrl = safe;
        }
      },
      error: () => {
        /* Preview optional; document path may require different auth or format. */
      }
    });
  }
}
