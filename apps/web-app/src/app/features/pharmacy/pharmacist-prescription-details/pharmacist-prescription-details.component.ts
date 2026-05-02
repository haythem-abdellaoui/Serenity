import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import {
  PrescriptionLineResponse,
  PrescriptionResponse,
  StockItemResponse,
  PrescriptionStatus,
  PrescriptionStatusUpdateRequest
} from '../../../shared/models/pharmacy.model';

@Component({
  selector: 'app-pharmacist-prescription-details',
  templateUrl: './pharmacist-prescription-details.component.html',
  styleUrls: ['./pharmacist-prescription-details.component.scss']
})
export class PharmacistPrescriptionDetailsComponent implements OnInit, OnDestroy {
  loading = true;
  statusUpdating = false;
  errorMessage = '';
  successMessage = '';
  prescription: PrescriptionResponse | null = null;
  showRejectPanel = false;
  rejectReason = '';
  confirmDialogOpen = false;
  confirmDialogTitle = '';
  confirmDialogMessage = '';
  private pendingStatusUpdate: PrescriptionStatus | null = null;
  showInsuranceUploadPanel = false;
  selectedInsuranceUploadMode: 'mobile' | 'pc' | null = null;
  insuranceUploadFile: File | null = null;
  uploadingInsuranceDocument = false;
  watchingMobileUpload = false;
  downloadingInsuranceDocument = false;
  private mobileWatchIntervalId: ReturnType<typeof setInterval> | null = null;
  private mobileWatchBaselineUploadedAt: string | null = null;
  private stockByMedicine = new Map<string, number>();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly pharmacyService: PharmacyService
  ) {}

  ngOnInit(): void {
    this.loadStockSnapshot();
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/pharmacy/inbox']);
      return;
    }
    this.load(id);
  }

  ngOnDestroy(): void {
    this.stopMobileUploadWatch();
  }

  load(id: number): void {
    this.loading = true;
    this.pharmacyService.getPrescriptionById(id).subscribe({
      next: (item) => {
        this.prescription = item;
        if (this.watchingMobileUpload && this.didMobileUploadComplete(item)) {
          this.successMessage = `Insurance prescription uploaded at ${this.formatDate(item.insuranceDocumentUploadedAt)}.`;
          this.stopMobileUploadWatch();
        }
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load prescription details';
        this.loading = false;
      }
    });
  }

  updateStatus(status: PrescriptionStatus): void {
    if (!this.prescription) {
      return;
    }
    if (this.statusUpdating) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    if (status === 'REJECTED') {
      this.showRejectPanel = true;
      return;
    }

    this.showRejectPanel = false;
    this.rejectReason = '';
    this.pendingStatusUpdate = status;
    this.confirmDialogTitle = 'Confirm Status Update';
    this.confirmDialogMessage = `Mark this prescription as ${this.statusLabel(status)}?`;
    this.confirmDialogOpen = true;
  }

  canProcess(status: PrescriptionStatus): boolean {
    return status === 'PENDING' || status === 'ACCEPTED';
  }

  statusClass(status: PrescriptionStatus): string {
    return `status-pill status-pill-${status.toLowerCase()}`;
  }

  statusLabel(status: PrescriptionStatus): string {
    return status.split('_').join(' ');
  }

  medicineLines(): PrescriptionLineResponse[] {
    const row = this.prescription;
    if (!row) return [];
    if (row.medicineLines && row.medicineLines.length > 0) {
      return row.medicineLines;
    }

    return [
      {
        id: row.id,
        medicationName: row.medicationName || '-',
        dosage: row.dosage || '-',
        quantity: row.quantity ?? 0,
        instructions: row.instructions
      }
    ];
  }

  stockMessageForLine(line: { medicationName: string; quantity: number }): string {
    const available = this.stockQuantityFor(line.medicationName);
    if (available == null) {
      return `In stock: not found | Prescribed: ${line.quantity}`;
    }
    return `In stock: ${available} | Prescribed: ${line.quantity}`;
  }

  goBack(): void {
    this.router.navigate(['/pharmacy/inbox']);
  }

  canShowInsuranceUpload(): boolean {
    return this.prescription?.status === 'READY_FOR_PICKUP';
  }

  cancelRejectPanel(): void {
    this.showRejectPanel = false;
    this.rejectReason = '';
  }

  confirmReject(): void {
    const reason = this.rejectReason.trim();
    if (!reason) {
      this.errorMessage = 'Rejection reason is required.';
      return;
    }
    this.performStatusUpdate('REJECTED', reason);
  }

  cancelStatusConfirmation(): void {
    this.confirmDialogOpen = false;
    this.pendingStatusUpdate = null;
  }

  confirmStatusUpdate(): void {
    const status = this.pendingStatusUpdate;
    this.cancelStatusConfirmation();
    if (!status) {
      return;
    }
    this.performStatusUpdate(status);
  }

  openInsuranceUploadPanel(): void {
    this.showInsuranceUploadPanel = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  closeInsuranceUploadPanel(): void {
    this.showInsuranceUploadPanel = false;
    this.selectedInsuranceUploadMode = null;
    this.insuranceUploadFile = null;
    this.stopMobileUploadWatch();
  }

  chooseInsuranceUploadMode(mode: 'mobile' | 'pc'): void {
    this.selectedInsuranceUploadMode = mode;
    this.errorMessage = '';
    this.successMessage = '';
    this.insuranceUploadFile = null;

    if (mode === 'mobile') {
      this.startMobileUploadWatch();
    } else {
      this.stopMobileUploadWatch();
    }
  }

  onInsuranceFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    if (!file) {
      this.insuranceUploadFile = null;
      return;
    }

    if (!file.type.startsWith('image/')) {
      this.errorMessage = 'Please choose an image file (JPG, JPEG, PNG).';
      this.insuranceUploadFile = null;
      input.value = '';
      return;
    }

    this.insuranceUploadFile = file;
  }

  uploadInsuranceDocumentFromComputer(): void {
    if (!this.prescription) {
      return;
    }

    if (!this.insuranceUploadFile) {
      this.errorMessage = 'Please choose a prescription image first.';
      return;
    }

    this.uploadingInsuranceDocument = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.pharmacyService.uploadPrescriptionInsuranceDocument(this.prescription.id, this.insuranceUploadFile).subscribe({
      next: (updated) => {
        this.prescription = updated;
        this.uploadingInsuranceDocument = false;
        this.insuranceUploadFile = null;
        this.successMessage = `Insurance prescription uploaded at ${this.formatDate(updated.insuranceDocumentUploadedAt)}.`;
      },
      error: (err) => {
        this.uploadingInsuranceDocument = false;
        this.errorMessage = err.error?.message || 'Failed to upload insurance prescription document';
      }
    });
  }

  downloadInsuranceDocument(): void {
    if (!this.prescription) {
      return;
    }

    this.downloadingInsuranceDocument = true;
    this.errorMessage = '';

    this.pharmacyService.downloadPrescriptionInsuranceDocument(this.prescription.id).subscribe({
      next: (blob) => {
        this.downloadingInsuranceDocument = false;
        const extension = this.resolveExtensionFromBlob(blob);
        const fileName = `prescription-${this.prescription?.id ?? 'document'}-insurance.${extension}`;
        this.triggerBrowserDownload(blob, fileName);
      },
      error: (err) => {
        this.downloadingInsuranceDocument = false;
        this.errorMessage = err.error?.message || 'Failed to download insurance prescription document';
      }
    });
  }

  private loadStockSnapshot(): void {
    this.pharmacyService.listStock(undefined, false).subscribe({
      next: (items) => {
        this.stockByMedicine = this.buildStockLookup(items);
      },
      error: () => {
        this.stockByMedicine.clear();
      }
    });
  }

  private buildStockLookup(items: StockItemResponse[]): Map<string, number> {
    const lookup = new Map<string, number>();
    for (const item of items) {
      if (item.archived) {
        continue;
      }

      const key = this.normalizeMedicineName(item.medicineName);
      if (!key) {
        continue;
      }

      const quantity = Number.isFinite(item.quantity) ? Math.max(0, item.quantity) : 0;
      lookup.set(key, (lookup.get(key) ?? 0) + quantity);
    }
    return lookup;
  }

  private stockQuantityFor(medicationName?: string): number | null {
    const key = this.normalizeMedicineName(medicationName);
    if (!key || !this.stockByMedicine.has(key)) {
      return null;
    }
    return this.stockByMedicine.get(key) ?? null;
  }

  private normalizeMedicineName(value?: string): string {
    return (value ?? '').trim().toLowerCase();
  }

  private performStatusUpdate(status: PrescriptionStatus, rejectionReason = ''): void {
    if (!this.prescription) {
      return;
    }

    const payload: PrescriptionStatusUpdateRequest = { status, rejectionReason };
    this.statusUpdating = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.pharmacyService.updatePrescriptionStatus(this.prescription.id, payload).subscribe({
      next: (updated) => {
        this.prescription = updated;
        this.statusUpdating = false;
        this.showRejectPanel = false;
        this.rejectReason = '';
        this.successMessage = `Prescription updated to ${this.statusLabel(updated.status)}.`;
      },
      error: (err) => {
        this.statusUpdating = false;
        this.errorMessage = err.error?.message || 'Failed to update prescription status';
      }
    });
  }

  private startMobileUploadWatch(): void {
    if (!this.prescription) {
      return;
    }

    this.mobileWatchBaselineUploadedAt = this.prescription.insuranceDocumentUploadedAt ?? null;
    this.watchingMobileUpload = true;

    if (this.mobileWatchIntervalId != null) {
      return;
    }

    this.mobileWatchIntervalId = setInterval(() => {
      if (!this.prescription) {
        return;
      }

      this.pharmacyService.getPrescriptionById(this.prescription.id).subscribe({
        next: (updated) => {
          this.prescription = updated;
          if (this.didMobileUploadComplete(updated)) {
            this.successMessage = `Insurance prescription uploaded at ${this.formatDate(updated.insuranceDocumentUploadedAt)}.`;
            this.stopMobileUploadWatch();
          }
        },
        error: () => {
          // Ignore polling failures and keep trying.
        }
      });
    }, 2500);
  }

  private stopMobileUploadWatch(): void {
    this.watchingMobileUpload = false;
    this.mobileWatchBaselineUploadedAt = null;
    if (this.mobileWatchIntervalId != null) {
      clearInterval(this.mobileWatchIntervalId);
      this.mobileWatchIntervalId = null;
    }
  }

  private didMobileUploadComplete(prescription: PrescriptionResponse): boolean {
    if (!prescription.insuranceDocumentAvailable || !prescription.insuranceDocumentUploadedAt) {
      return false;
    }

    return this.mobileWatchBaselineUploadedAt == null
      || prescription.insuranceDocumentUploadedAt !== this.mobileWatchBaselineUploadedAt;
  }

  private formatDate(raw?: string): string {
    if (!raw) {
      return '-';
    }

    const value = new Date(raw);
    if (Number.isNaN(value.getTime())) {
      return raw;
    }
    return value.toLocaleString();
  }

  private resolveExtensionFromBlob(blob: Blob): string {
    const type = (blob.type || '').toLowerCase();
    if (type.includes('png')) {
      return 'png';
    }
    return 'jpg';
  }

  private triggerBrowserDownload(blob: Blob, fileName: string): void {
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = fileName;
    anchor.click();
    window.URL.revokeObjectURL(url);
  }
}
