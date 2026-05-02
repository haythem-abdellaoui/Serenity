import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { InsuranceService } from '../../../core/services/insurance.service';
import { UserService } from '../../../core/services/user.service';
import { INSURANCE_COMPANIES, INSURANCE_GRADES } from '../../../shared/models/insurance.model';

@Component({
  selector: 'app-claim-form',
  templateUrl: './claim-form.component.html',
  styleUrls: ['./claim-form.component.scss']
})
export class ClaimFormComponent implements OnInit {
  private static readonly MAX_DESCRIPTION_LENGTH = 500;
  private static readonly MAX_AMOUNT = 100000;
  private static readonly MAX_FILES = 5;
  private static readonly MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
  private static readonly ALLOWED_FILE_TYPES = ['application/pdf', 'image/jpeg', 'image/png'];

  claimForm: FormGroup;
  files: File[] = [];
  submitting = false;
  errorMessage = '';
  fileErrorMessage = '';
  loadingUserInsurance = true;
  userInsuranceCompany = '';

  readonly companies = INSURANCE_COMPANIES;
  readonly grades = INSURANCE_GRADES;
  estimatedReimbursement: number | null = null;

  constructor(
    private readonly fb: FormBuilder,
    private readonly insuranceService: InsuranceService,
    private readonly userService: UserService,
    private readonly router: Router
  ) {
    this.claimForm = this.fb.group({
      description: ['', [
        Validators.required,
        Validators.minLength(10),
        Validators.maxLength(ClaimFormComponent.MAX_DESCRIPTION_LENGTH),
        this.nonWhitespaceValidator()
      ]],
      amount: [null, [
        Validators.required,
        Validators.min(0.01),
        Validators.max(ClaimFormComponent.MAX_AMOUNT)
      ]],
      insuranceCompany: [{ value: '', disabled: true }, [Validators.required, this.allowedCompanyValidator()]],
      insuranceGrade: [null, [Validators.required, this.allowedGradeValidator()]]
    });

    this.claimForm.get('amount')?.valueChanges.subscribe(() => this.calculateReimbursement());
    this.claimForm.get('insuranceGrade')?.valueChanges.subscribe(() => this.calculateReimbursement());
  }

  ngOnInit(): void {
    this.loadUserInsuranceCompany();
  }

  calculateReimbursement(): void {
    const amount = this.claimForm.get('amount')?.value;
    const gradeValue = this.claimForm.get('insuranceGrade')?.value;
    if (amount && gradeValue) {
      const grade = this.grades.find(g => g.value === Number(gradeValue));
      if (grade) {
        this.estimatedReimbursement = Math.round(amount * (grade.percentage / 100) * 100) / 100;
        return;
      }
    }
    this.estimatedReimbursement = null;
  }

  getSelectedGradePercentage(): number | null {
    const gradeValue = this.claimForm.get('insuranceGrade')?.value;
    if (!gradeValue) return null;
    const grade = this.grades.find(g => g.value === Number(gradeValue));
    return grade ? grade.percentage : null;
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.fileErrorMessage = '';
    if (!input.files) {
      return;
    }

    const selectedFiles = Array.from(input.files);
    const availableSlots = ClaimFormComponent.MAX_FILES - this.files.length;
    if (availableSlots <= 0) {
      this.fileErrorMessage = `You can upload up to ${ClaimFormComponent.MAX_FILES} files.`;
      input.value = '';
      return;
    }

    const filesToProcess = selectedFiles.slice(0, availableSlots);
    const rejected: string[] = [];
    const accepted: File[] = [];

    for (const file of filesToProcess) {
      if (!ClaimFormComponent.ALLOWED_FILE_TYPES.includes(file.type)) {
        rejected.push(`${file.name}: unsupported file type`);
        continue;
      }
      if (file.size > ClaimFormComponent.MAX_FILE_SIZE_BYTES) {
        rejected.push(`${file.name}: exceeds 10 MB`);
        continue;
      }
      accepted.push(file);
    }

    this.files = [...this.files, ...accepted];

    if (selectedFiles.length > filesToProcess.length) {
      rejected.push(`Only ${ClaimFormComponent.MAX_FILES} files are allowed`);
    }
    if (rejected.length > 0) {
      this.fileErrorMessage = rejected.join('. ');
    }
    input.value = '';
  }

  removeFile(index: number): void {
    this.files.splice(index, 1);
    if (this.fileErrorMessage && this.files.length < ClaimFormComponent.MAX_FILES) {
      this.fileErrorMessage = '';
    }
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / 1048576).toFixed(1) + ' MB';
  }

  onSubmit(): void {
    if (this.loadingUserInsurance) {
      return;
    }
    if (!this.userInsuranceCompany) {
      this.errorMessage = 'Please set your insurance company in your profile before submitting a claim.';
      return;
    }
    if (this.claimForm.invalid) {
      this.claimForm.markAllAsTouched();
      return;
    }
    if (this.fileErrorMessage) {
      return;
    }

    this.submitting = true;
    this.errorMessage = '';

    const formRawValue = this.claimForm.getRawValue();
    const description = String(formRawValue.description || '').trim().replace(/\s+/g, ' ');
    const formValue = {
      ...formRawValue,
      description,
      insuranceCompany: this.userInsuranceCompany,
      amount: Number(formRawValue.amount),
      insuranceGrade: Number(formRawValue.insuranceGrade)
    };

    this.insuranceService.submitClaim(formValue, this.files).subscribe({
      next: () => {
        this.router.navigate(['/insurance']);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to submit claim. Please try again.';
        this.submitting = false;
      }
    });
  }

  get descriptionLength(): number {
    return String(this.claimForm.get('description')?.value || '').length;
  }

  isControlInvalid(controlName: string): boolean {
    const control = this.claimForm.get(controlName);
    return !!control && control.invalid && (control.touched || control.dirty);
  }

  private nonWhitespaceValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const value = String(control.value ?? '');
      return value.trim().length > 0 ? null : { whitespace: true };
    };
  }

  private allowedCompanyValidator(): ValidatorFn {
    const allowed = new Set(this.companies);
    return (control: AbstractControl): ValidationErrors | null => {
      const value = String(control.value ?? '').trim();
      if (!value) return null;
      return allowed.has(value) ? null : { invalidCompany: true };
    };
  }

  private allowedGradeValidator(): ValidatorFn {
    const allowed = new Set(this.grades.map((g) => g.value));
    return (control: AbstractControl): ValidationErrors | null => {
      if (control.value == null || control.value === '') return null;
      const numeric = Number(control.value);
      return Number.isFinite(numeric) && allowed.has(numeric) ? null : { invalidGrade: true };
    };
  }

  private loadUserInsuranceCompany(): void {
    this.loadingUserInsurance = true;
    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        const company = (user.insuranceCompany || '').trim();
        this.userInsuranceCompany = company;
        this.claimForm.patchValue({ insuranceCompany: company });
        this.loadingUserInsurance = false;
        if (!company) {
          this.errorMessage = 'No insurance company is set on your profile. Please update your profile first.';
        }
      },
      error: () => {
        this.loadingUserInsurance = false;
        this.errorMessage = 'Failed to load your insurance company. Please refresh and try again.';
      }
    });
  }
}
