import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import {
  PrescriptionResponse,
  PrescriptionStatus,
  PrescriptionStatusUpdateRequest
} from '../../../shared/models/pharmacy.model';

@Component({
  selector: 'app-prescription-inbox',
  templateUrl: './prescription-inbox.component.html',
  styleUrls: ['./prescription-inbox.component.scss']
})
export class PrescriptionInboxComponent implements OnInit {
  loading = true;
  errorMessage = '';
  successMessage = '';
  prescriptions: PrescriptionResponse[] = [];

  constructor(
    private readonly pharmacyService: PharmacyService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadInbox();
  }

  loadInbox(): void {
    this.loading = true;
    this.errorMessage = '';
    this.pharmacyService.getInbox().subscribe({
      next: (rows) => {
        this.prescriptions = rows;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load prescription inbox';
        this.loading = false;
      }
    });
  }

  updateStatus(row: PrescriptionResponse, status: PrescriptionStatus): void {
    const actionLabel = this.statusActionLabel(status);
    if (!window.confirm(`Are you sure you want to ${actionLabel} this prescription?`)) {
      return;
    }

    let rejectionReason = '';
    if (status === 'REJECTED') {
      rejectionReason = prompt('Please provide rejection reason') || '';
      if (!rejectionReason.trim()) {
        this.errorMessage = 'Rejection reason is required.';
        return;
      }
    }

    const payload: PrescriptionStatusUpdateRequest = { status, rejectionReason };
    this.errorMessage = '';
    this.successMessage = '';

    this.pharmacyService.updatePrescriptionStatus(row.id, payload).subscribe({
      next: (updated) => {
        const idx = this.prescriptions.findIndex(p => p.id === updated.id);
        if (idx !== -1) this.prescriptions[idx] = updated;
        this.successMessage = `Prescription updated to ${updated.status}.`;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to update prescription status';
      }
    });
  }

  canProcess(status: PrescriptionStatus): boolean {
    return status === 'PENDING' || status === 'ACCEPTED';
  }

  backToWorkspace(): void {
    this.router.navigate(['/pharmacy']);
  }

  openPrescription(row: PrescriptionResponse): void {
    this.router.navigate(['/pharmacy/inbox', row.id]);
  }

  medicineLines(row: PrescriptionResponse): Array<{ medicationName: string; dosage: string; quantity: number; instructions?: string }> {
    if (row.medicineLines && row.medicineLines.length > 0) {
      return row.medicineLines;
    }

    return [
      {
        medicationName: row.medicationName || '-',
        dosage: row.dosage || '-',
        quantity: row.quantity ?? 0,
        instructions: row.instructions
      }
    ];
  }

  summaryMedication(row: PrescriptionResponse): string {
    const lines = this.medicineLines(row);
    return lines[0]?.medicationName || '-';
  }

  summaryDosage(row: PrescriptionResponse): string {
    const lines = this.medicineLines(row);
    return lines[0]?.dosage || '-';
  }

  summaryQuantity(row: PrescriptionResponse): number | string {
    const lines = this.medicineLines(row);
    return lines[0]?.quantity ?? '-';
  }

  extraLinesCount(row: PrescriptionResponse): number {
    const lines = this.medicineLines(row);
    return lines.length > 1 ? lines.length - 1 : 0;
  }

  private statusActionLabel(status: PrescriptionStatus): string {
    switch (status) {
      case 'ACCEPTED':
        return 'accept';
      case 'REJECTED':
        return 'reject';
      case 'READY_FOR_PICKUP':
        return 'mark as ready for pickup';
      case 'COLLECTED':
        return 'mark as collected';
      case 'EXPIRED':
        return 'mark as expired';
      default:
        return `set to ${status.toLowerCase().replace(/_/g, ' ')}`;
    }
  }
}
