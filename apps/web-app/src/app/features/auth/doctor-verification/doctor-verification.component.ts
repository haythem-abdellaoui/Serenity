import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { AuthService } from 'src/app/core/services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-doctor-verification',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './doctor-verification.component.html',
  styleUrl: './doctor-verification.component.scss'
})
export class DoctorVerificationComponent implements OnInit {
  verificationForm!: FormGroup;
  loading = false;
  successMessage = '';
  errorMessage = '';
  currentStep = 1;

  cvFile: File | null = null;
  diplomaFile: File | null = null;
  cvPreview: string | null = null;
  diplomaPreview: string | null = null;

  cvDragOver = false;
  diplomaDragOver = false;

  constructor(private readonly fb: FormBuilder, private readonly authService: AuthService, private readonly router: Router) {}

  ngOnInit(): void {
    /*const user = this.authService.getCurrentUser();
    console.log('Current user:', user);
    if (!user || user.role !== 'DOCTOR') {
      this.router.navigate(['/auth/login']);
      return;
    }*/
    this.initForm();
  }

  initForm(): void {
    this.verificationForm = this.fb.group({
      licenseNumber: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(20), Validators.pattern('^[A-Za-z0-9 ]+$')]],
      nationalId: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(8), Validators.pattern('^[0-9 ]+$')]],
      cv: [null, Validators.required],
      diploma: [null, Validators.required]
    });
  }

  // License Number
  get licenseNumber() {
    return this.verificationForm.get('licenseNumber');
  }

  // National ID
  get nationalId() {
    return this.verificationForm.get('nationalId');
  }

  // CV File handling
  onCvDragOver(event: DragEvent): void {
    event.preventDefault();
    this.cvDragOver = true;
  }

  onCvDragLeave(): void {
    this.cvDragOver = false;
  }

  onCvDropped(event: DragEvent): void {
    event.preventDefault();
    this.cvDragOver = false;
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.handleCvFile(files[0]);
    }
  }

  onCvSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleCvFile(input.files[0]);
    }
  }

  handleCvFile(file: File): void {
    const validTypes = ['application/pdf', 'image/png', 'image/jpeg', 'image/jpg'];
    if (!validTypes.includes(file.type)) {
      this.errorMessage = 'CV must be PDF or Image (PNG, JPG)';
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.errorMessage = 'CV file must be less than 5MB';
      return;
    }
    this.cvFile = file;
    this.verificationForm.patchValue({ cv: file });
    this.generatePreview(file, 'cv');
    this.errorMessage = '';
  }

  removeCv(): void {
    this.cvFile = null;
    this.cvPreview = null;
    this.verificationForm.patchValue({ cv: null });
  }

  // Diploma File handling
  onDiplomaDragOver(event: DragEvent): void {
    event.preventDefault();
    this.diplomaDragOver = true;
  }

  onDiplomaDragLeave(): void {
    this.diplomaDragOver = false;
  }

  onDiplomaDropped(event: DragEvent): void {
    event.preventDefault();
    this.diplomaDragOver = false;
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.handleDiplomaFile(files[0]);
    }
  }

  onDiplomaSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleDiplomaFile(input.files[0]);
    }
  }

  handleDiplomaFile(file: File): void {
    const validTypes = ['application/pdf', 'image/png', 'image/jpeg', 'image/jpg'];
    if (!validTypes.includes(file.type)) {
      this.errorMessage = 'Diploma must be PDF or Image (PNG, JPG)';
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.errorMessage = 'Diploma file must be less than 5MB';
      return;
    }
    this.diplomaFile = file;
    this.verificationForm.patchValue({ diploma: file });
    this.generatePreview(file, 'diploma');
    this.errorMessage = '';
  }

  removeDiploma(): void {
    this.diplomaFile = null;
    this.diplomaPreview = null;
    this.verificationForm.patchValue({ diploma: null });
  }

  generatePreview(file: File, type: 'cv' | 'diploma'): void {
    if (file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onload = (e) => {
        if (type === 'cv') {
          this.cvPreview = e.target?.result as string;
        } else {
          this.diplomaPreview = e.target?.result as string;
        }
      };
      reader.readAsDataURL(file);
    }
  }

  nextStep(): void {
    if (this.currentStep === 1) {
      if (this.licenseNumber?.valid && this.nationalId?.valid) {
        this.currentStep = 2;
      }
    }
  }

  prevStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  onSubmit(): void {
  if (this.verificationForm.invalid) {
    this.errorMessage = 'Please complete all required fields';
    console.warn('Form invalid:', this.verificationForm.value);
    return;
  }

  this.loading = true;

  const token = this.authService.getToken();
  console.log('JWT Token being sent:', token);

  // Prepare FormData
  const formData = new FormData();
  formData.append('cv', this.cvFile as File);
  formData.append('diploma', this.diplomaFile as File);
  formData.append('licenseNumber', this.verificationForm.value.licenseNumber);
  formData.append('nationalId', this.verificationForm.value.nationalId);

  // Debug: show all FormData entries
  console.log('FormData contents:');
  formData.forEach((value, key) => {
    console.log(key, value);
  });

  this.authService.addDoctorVerification(
    this.cvFile as File,
    this.diplomaFile as File,
    this.verificationForm.value.licenseNumber,
    this.verificationForm.value.nationalId
  ).subscribe({
    next: (res) => {
      console.log('Server response:', res);
      this.loading = false;
      this.successMessage = 'Verification submitted successfully! We will review your documents.';
      setTimeout(() => {
        this.successMessage = '';
        void this.router.navigate(['/auth/doctor-verification/pending']);
      }, 1200);
    },
    error: (err) => {
      console.error('Error from backend:', err);
      if (err.status) {
        console.error('HTTP Status:', err.status);
      }
      if (err.error) {
        console.error('Error body:', err.error);
      }
      this.loading = false;
      this.errorMessage = 'Error while submitting verification';
    }
  });
}

  getFileName(file: File | null): string {
    return file ? file.name : '';
  }

  getFileSize(file: File | null): string {
    if (!file) return '';
    const mb = (file.size / (1024 * 1024)).toFixed(2);
    return `${mb} MB`;
  }
}
