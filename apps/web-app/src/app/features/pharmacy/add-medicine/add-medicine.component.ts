import { Component } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { PharmacyService } from '../../../core/services/pharmacy.service';

@Component({
  selector: 'app-add-medicine',
  templateUrl: './add-medicine.component.html',
  styleUrls: ['./add-medicine.component.scss']
})
export class AddMedicineComponent {
  saving = false;
  uploadingImage = false;
  successMessage = '';
  errorMessage = '';
  imagePreviewUrl = '';

  form: FormGroup;

  constructor(
    private readonly fb: FormBuilder,
    private readonly pharmacyService: PharmacyService,
    private readonly router: Router
  ) {
    this.form = this.fb.group({
      medicineName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(80)]],
      quantity: [0, [Validators.required, Validators.min(0), Validators.max(9999), Validators.pattern(/^\d+$/)]],
      imageUrl: ['', [Validators.maxLength(500), this.imageUrlValidator]],
      description: ['', [Validators.maxLength(500)]]
    });
  }

  getControl(name: string): AbstractControl | null {
    return this.form.get(name);
  }

  onImageSelected(event: Event): void {
    const target = event.target as HTMLInputElement;
    const file = target.files?.[0];
    if (!file) {
      return;
    }

    if (!file.type.startsWith('image/')) {
      this.errorMessage = 'Please choose a valid image file';
      target.value = '';
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      this.errorMessage = 'Image size must be 5MB or less';
      target.value = '';
      return;
    }

    this.uploadingImage = true;
    this.errorMessage = '';

    const reader = new FileReader();
    reader.onload = () => {
      const result = typeof reader.result === 'string' ? reader.result : '';
      this.form.patchValue({ imageUrl: result });
      this.imagePreviewUrl = result;
      this.uploadingImage = false;
    };
    reader.onerror = () => {
      this.uploadingImage = false;
      this.errorMessage = 'Failed to read image file';
    };

    reader.readAsDataURL(file);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const medicineName = String(this.form.get('medicineName')?.value || '').trim();
    const quantity = Number(this.form.get('quantity')?.value || 0);
    if (!window.confirm(`Add "${medicineName}" with quantity ${quantity}?`)) {
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.pharmacyService.createStockItem(this.form.value).subscribe({
      next: () => {
        this.saving = false;
        this.successMessage = 'Medicine added successfully';
        setTimeout(() => this.router.navigate(['/pharmacy/stock']), 700);
      },
      error: (err) => {
        this.saving = false;
        this.errorMessage = err.error?.message || 'Failed to add medicine';
      }
    });
  }

  cancel(): void {
    const hasPendingChanges = this.form.dirty || !!this.imagePreviewUrl;
    if (hasPendingChanges && !window.confirm('Discard your changes and leave this page?')) {
      return;
    }
    this.router.navigate(['/pharmacy/stock']);
  }

  private imageUrlValidator(control: AbstractControl): ValidationErrors | null {
    const rawValue = String(control.value ?? '').trim();
    if (!rawValue) {
      return null;
    }
    if (rawValue.startsWith('data:image/')) {
      return null;
    }
    if (/^https?:\/\/\S+$/i.test(rawValue)) {
      return null;
    }
    return { invalidUrl: true };
  }
}
