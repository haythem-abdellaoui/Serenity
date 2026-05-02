import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { LocationGeocodingService } from '../../../core/services/location-geocoding.service';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import { UserService } from '../../../core/services/user.service';
import { PickerLocation } from '../../../shared/components/location-picker/location-picker.component';
import {
  PharmacyApplicationResponse,
  PharmacyApplicationSubmitRequest
} from '../../../shared/models/pharmacy.model';

@Component({
  selector: 'app-pharmacy-application',
  templateUrl: './pharmacy-application.component.html',
  styleUrls: ['./pharmacy-application.component.scss']
})
export class PharmacyApplicationComponent implements OnInit {
  loading = true;
  submitting = false;

  errorMessage = '';
  successMessage = '';
  locationAutoFillMessage = '';
  locationAutoFillLoading = false;

  application: PharmacyApplicationResponse | null = null;

  cinDocumentFile: File | null = null;
  cnoptProofDocumentFile: File | null = null;
  legalProofDocumentFile: File | null = null;
  private openingHoursPrefix = '';

  readonly applicationForm = this.formBuilder.group({
    firstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(60)]],
    lastName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(60)]],
    email: ['', [Validators.required, Validators.email]],
    cinNumber: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(8), Validators.pattern(/^\d{8}$/)]],
    cnopNumber: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(40)]],
    pharmacyName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(120)]],
    authorizationReferenceNumber: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(80)]],
    phone: ['', [Validators.pattern(/^\d{8}$/)]],
    openingHours: ['', [Validators.maxLength(120)]],
    openingFrom: [''],
    openingTo: [''],
    addressLine: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(255)]],
    city: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    governorate: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    latitude: [null as number | null, [Validators.required, Validators.min(-90), Validators.max(90)]],
    longitude: [null as number | null, [Validators.required, Validators.min(-180), Validators.max(180)]]
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly geocodingService: LocationGeocodingService,
    private readonly pharmacyService: PharmacyService,
    private readonly userService: UserService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.prefillIdentityFromCurrentUser();
    this.loadMyApplication();
  }

  get isReadOnly(): boolean {
    return this.application?.status === 'SUBMITTED' || this.application?.status === 'APPROVED';
  }

  get canSubmit(): boolean {
    return !this.loading && !this.submitting && !this.isReadOnly;
  }

  onLocationSelected(location: PickerLocation): void {
    this.applicationForm.patchValue({
      latitude: location.latitude,
      longitude: location.longitude
    });

    this.locationAutoFillLoading = true;
    this.locationAutoFillMessage = 'Trying to autofill address, city and governorate from map...';
    this.geocodingService.reverseGeocode(location.latitude, location.longitude).subscribe({
      next: (result) => {
        this.locationAutoFillLoading = false;
        if (!result) {
          this.locationAutoFillMessage = 'Could not autofill location fields. You can fill them manually.';
          return;
        }

        this.applicationForm.patchValue({
          addressLine: result.addressLine || this.applicationForm.value.addressLine,
          city: result.city || this.applicationForm.value.city,
          governorate: result.governorate || this.applicationForm.value.governorate
        });
        this.locationAutoFillMessage = 'Location fields autofilled. You can edit them if needed.';
      },
      error: () => {
        this.locationAutoFillLoading = false;
        this.locationAutoFillMessage = 'Could not autofill location fields. You can fill them manually.';
      }
    });
  }

  onFileSelected(event: Event, type: 'cin' | 'cnopt' | 'legal'): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    if (type === 'cin') {
      this.cinDocumentFile = file;
      return;
    }
    if (type === 'cnopt') {
      this.cnoptProofDocumentFile = file;
      return;
    }
    this.legalProofDocumentFile = file;
  }

  submit(): void {
    if (!this.canSubmit) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';
    this.applicationForm.markAllAsTouched();

    if (this.applicationForm.invalid) {
      this.errorMessage = 'Please complete all required fields before submitting.';
      return;
    }

    if (!this.hasAllRequiredDocuments()) {
      this.errorMessage = 'Please provide the 3 required documents before submitting.';
      return;
    }

    if (this.hasPartialOpeningHours()) {
      this.errorMessage = 'Please select both opening and closing time, or leave both empty.';
      return;
    }

    const raw = this.applicationForm.getRawValue();
    const payload: PharmacyApplicationSubmitRequest = {
      firstName: String(raw.firstName || '').trim(),
      lastName: String(raw.lastName || '').trim(),
      email: String(raw.email || '').trim(),
      cinNumber: String(raw.cinNumber || '').trim(),
      cnopNumber: String(raw.cnopNumber || '').trim(),
      pharmacyName: String(raw.pharmacyName || '').trim(),
      authorizationReferenceNumber: String(raw.authorizationReferenceNumber || '').trim(),
      addressLine: String(raw.addressLine || '').trim(),
      city: String(raw.city || '').trim(),
      governorate: String(raw.governorate || '').trim(),
      latitude: Number(raw.latitude),
      longitude: Number(raw.longitude),
      openingHours: this.buildOpeningHours()
    };

    const phone = String(raw.phone || '').trim();
    if (phone) {
      payload.phone = phone;
    }

    this.submitting = true;

    this.pharmacyService.submitMyPharmacyApplication(
      payload,
      this.cinDocumentFile,
      this.cnoptProofDocumentFile,
      this.legalProofDocumentFile
    ).subscribe({
      next: (response) => {
        this.application = response;
        this.submitting = false;
        this.successMessage = 'Application submitted successfully. You can continue using the app while waiting for review.';
        this.applicationForm.disable();
        this.resetLocalFiles();
      },
      error: (err) => {
        this.submitting = false;
        this.errorMessage = err.error?.message || 'Failed to submit application';
      }
    });
  }

  continueAsPatient(): void {
    this.router.navigate(['/pharmacy/patient']);
  }

  goToPharmacistWorkspace(): void {
    this.router.navigate(['/pharmacy']);
  }

  private loadMyApplication(): void {
    this.loading = true;
    this.pharmacyService.getMyPharmacyApplication().subscribe({
      next: (application) => {
        this.application = application;
        this.patchFormFromApplication(application);
        this.loading = false;

        if (this.isReadOnly) {
          this.applicationForm.disable();
        }
      },
      error: (err) => {
        this.loading = false;
        if (err.status !== 404) {
          this.errorMessage = err.error?.message || 'Failed to load your pharmacist application.';
        }
      }
    });
  }

  private patchFormFromApplication(application: PharmacyApplicationResponse): void {
    this.applicationForm.patchValue({
      firstName: application.firstName || '',
      lastName: application.lastName || '',
      email: application.email || '',
      cinNumber: application.cinNumber || '',
      cnopNumber: application.cnopNumber || '',
      pharmacyName: application.pharmacyName || '',
      authorizationReferenceNumber: application.authorizationReferenceNumber || '',
      phone: application.phone || '',
      openingHours: application.openingHours || '',
      addressLine: application.addressLine || '',
      city: application.city || '',
      governorate: application.governorate || '',
      latitude: application.latitude ?? null,
      longitude: application.longitude ?? null
    });

    this.applyOpeningHours(application.openingHours || '');
  }

  private prefillIdentityFromCurrentUser(): void {
    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        this.applicationForm.patchValue({
          firstName: user.firstName || '',
          lastName: user.lastName || '',
          email: user.email || ''
        });
      },
      error: () => {
        // Keep the form usable even if user profile loading fails.
      }
    });
  }

  private hasAllRequiredDocuments(): boolean {
    const cinReady = !!this.cinDocumentFile || !!this.application?.cinDocumentUploaded;
    const cnoptReady = !!this.cnoptProofDocumentFile || !!this.application?.cnoptProofUploaded;
    const legalReady = !!this.legalProofDocumentFile || !!this.application?.legalDocumentUploaded;
    return cinReady && cnoptReady && legalReady;
  }

  private resetLocalFiles(): void {
    this.cinDocumentFile = null;
    this.cnoptProofDocumentFile = null;
    this.legalProofDocumentFile = null;
  }

  fileNameFor(type: 'cin' | 'cnopt' | 'legal'): string {
    if (type === 'cin') {
      return this.cinDocumentFile?.name || 'No file selected';
    }
    if (type === 'cnopt') {
      return this.cnoptProofDocumentFile?.name || 'No file selected';
    }
    return this.legalProofDocumentFile?.name || 'No file selected';
  }

  hasStoredDocument(type: 'cin' | 'cnopt' | 'legal'): boolean {
    if (type === 'cin') {
      return !!this.application?.cinDocumentUploaded;
    }
    if (type === 'cnopt') {
      return !!this.application?.cnoptProofUploaded;
    }
    return !!this.application?.legalDocumentUploaded;
  }

  hasNewDocument(type: 'cin' | 'cnopt' | 'legal'): boolean {
    if (type === 'cin') {
      return !!this.cinDocumentFile;
    }
    if (type === 'cnopt') {
      return !!this.cnoptProofDocumentFile;
    }
    return !!this.legalProofDocumentFile;
  }

  private applyOpeningHours(openingHours: string): void {
    const source = String(openingHours || '').trim();
    if (!source) {
      return;
    }

    const match = source.match(/^(.*?)(\d{1,2}:\d{2})\s*-\s*(\d{1,2}:\d{2})$/);
    if (!match) {
      return;
    }

    const openingFrom = this.normalizeTimeValue(match[2]);
    const openingTo = this.normalizeTimeValue(match[3]);
    if (!openingFrom || !openingTo) {
      return;
    }

    this.openingHoursPrefix = match[1].trim();
    this.applicationForm.patchValue({
      openingFrom,
      openingTo
    });
  }

  private buildOpeningHours(): string {
    const from = String(this.applicationForm.get('openingFrom')?.value || '').trim();
    const to = String(this.applicationForm.get('openingTo')?.value || '').trim();
    const existing = String(this.applicationForm.get('openingHours')?.value || '').trim();

    if (from && to) {
      const prefix = this.openingHoursPrefix ? `${this.openingHoursPrefix} ` : '';
      return `${prefix}${from}-${to}`.trim();
    }

    return existing;
  }

  private hasPartialOpeningHours(): boolean {
    const from = String(this.applicationForm.get('openingFrom')?.value || '').trim();
    const to = String(this.applicationForm.get('openingTo')?.value || '').trim();
    return (!!from && !to) || (!from && !!to);
  }

  private normalizeTimeValue(value: string): string {
    const match = String(value || '').trim().match(/^(\d{1,2}):(\d{2})$/);
    if (!match) {
      return '';
    }

    const hours = Number(match[1]);
    const minutes = Number(match[2]);
    if (!Number.isFinite(hours) || !Number.isFinite(minutes) || hours < 0 || hours > 23 || minutes < 0 || minutes > 59) {
      return '';
    }

    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`;
  }
}
