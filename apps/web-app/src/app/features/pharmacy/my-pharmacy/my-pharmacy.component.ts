import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import { PharmacyUpsertRequest } from '../../../shared/models/pharmacy.model';
import { PickerLocation } from '../../../shared/components/location-picker/location-picker.component';

@Component({
  selector: 'app-my-pharmacy',
  templateUrl: './my-pharmacy.component.html',
  styleUrls: ['./my-pharmacy.component.scss']
})
export class MyPharmacyComponent implements OnInit {
  form!: FormGroup;
  loading = true;
  saving = false;
  deleting = false;
  hasExistingPharmacy = false;
  successMessage = '';
  errorMessage = '';
  private openingHoursPrefix = '';

  constructor(
    private readonly fb: FormBuilder,
    private readonly pharmacyService: PharmacyService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      licenseNumber: ['', [Validators.required]],
      phone: ['', [Validators.pattern(/^\d{8}$/)]],
      openingHours: [''],
      openingFrom: [''],
      openingTo: [''],
      addressLine: ['', [Validators.required, Validators.minLength(2)]],
      city: ['', [Validators.required, Validators.minLength(2)]],
      governorate: ['', [Validators.required, Validators.minLength(2)]],
      latitude: [null, [Validators.required, Validators.min(-90), Validators.max(90)]],
      longitude: [null, [Validators.required, Validators.min(-180), Validators.max(180)]],
      supportsEmergency: [false]
    });

    this.load();
  }

  load(): void {
    this.loading = true;
    this.pharmacyService.getMyPharmacy().subscribe({
      next: (pharmacy) => {
        this.hasExistingPharmacy = true;
        this.form.patchValue(pharmacy);
        this.applyOpeningHours(pharmacy.openingHours);
        this.loading = false;
      },
      error: () => {
        // First-time pharmacists may not have a profile yet.
        this.hasExistingPharmacy = false;
        this.loading = false;
      }
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      if (this.form.get('latitude')?.invalid || this.form.get('longitude')?.invalid) {
        this.errorMessage = 'Please select your pharmacy exact location on the map.';
      }
      return;
    }

    if (!window.confirm('Save pharmacy profile changes?')) {
      return;
    }

    this.saving = true;
    this.deleting = false;
    this.successMessage = '';
    this.errorMessage = '';

    const payload: PharmacyUpsertRequest = {
      ...this.form.value,
      openingHours: this.buildOpeningHours(),
      latitude: Number(this.form.get('latitude')?.value),
      longitude: Number(this.form.get('longitude')?.value)
    };

    this.pharmacyService.upsertMyPharmacy(payload).subscribe({
      next: () => {
        this.successMessage = 'Pharmacy profile saved successfully.';
        this.saving = false;
        this.router.navigate(['/pharmacy']);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to save pharmacy profile';
        this.saving = false;
      }
    });
  }

  backToWorkspace(): void {
    this.cancel();
  }

  cancel(): void {
    if (this.form.dirty && !window.confirm('Discard unsaved pharmacy changes?')) {
      return;
    }
    this.router.navigate(['/pharmacy']);
  }

  deletePharmacy(): void {
    if (!this.hasExistingPharmacy || this.deleting || this.saving) {
      return;
    }

    if (!window.confirm('Delete your pharmacy profile? This will clear stock, unassign linked prescriptions, and remove default references.')) {
      return;
    }

    this.deleting = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.pharmacyService.deleteMyPharmacy().subscribe({
      next: () => {
        this.deleting = false;
        this.successMessage = 'Pharmacy deleted successfully.';
        this.router.navigate(['/pharmacy']);
      },
      error: (err) => {
        this.deleting = false;
        this.errorMessage = err.error?.message || 'Failed to delete pharmacy';
      }
    });
  }

  onLocationSelected(location: PickerLocation): void {
    this.form.patchValue({
      latitude: location.latitude,
      longitude: location.longitude
    });
    this.form.get('latitude')?.markAsTouched();
    this.form.get('longitude')?.markAsTouched();
    this.form.get('latitude')?.updateValueAndValidity();
    this.form.get('longitude')?.updateValueAndValidity();
  }

  private applyOpeningHours(openingHours?: string): void {
    if (!openingHours) {
      return;
    }
    const match = openingHours.match(/^(.*?)(\d{1,2}:\d{2})\s*-\s*(\d{1,2}:\d{2})/);
    if (!match) {
      return;
    }
    this.openingHoursPrefix = match[1].trim();
    this.form.patchValue({
      openingFrom: match[2],
      openingTo: match[3]
    });
  }

  private buildOpeningHours(): string {
    const from = String(this.form.get('openingFrom')?.value || '').trim();
    const to = String(this.form.get('openingTo')?.value || '').trim();
    const existing = String(this.form.get('openingHours')?.value || '').trim();

    if (from && to) {
      const prefix = this.openingHoursPrefix ? `${this.openingHoursPrefix} ` : '';
      return `${prefix}${from}-${to}`.trim();
    }

    return existing;
  }
}
