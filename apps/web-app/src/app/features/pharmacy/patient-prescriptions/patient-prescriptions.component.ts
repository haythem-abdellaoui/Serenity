import { Component, OnInit } from '@angular/core';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import {
  PatientDefaultPharmacyResponse,
  PrescriptionResponse,
  PrescriptionStatus
} from '../../../shared/models/pharmacy.model';

@Component({
  selector: 'app-patient-prescriptions',
  templateUrl: './patient-prescriptions.component.html',
  styleUrls: ['./patient-prescriptions.component.scss']
})
export class PatientPrescriptionsComponent implements OnInit {
  loading = true;
  errorMessage = '';

  prescriptions: PrescriptionResponse[] = [];
  defaultPharmacy: PatientDefaultPharmacyResponse | null = null;

  constructor(private readonly pharmacyService: PharmacyService) {}

  ngOnInit(): void {
    this.loadData();
  }

  private loadData(): void {
    this.loading = true;
    this.errorMessage = '';

    this.pharmacyService.getMyPrescriptions().subscribe({
      next: (items) => {
        this.prescriptions = items;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load your prescriptions';
        this.loading = false;
      }
    });

    this.pharmacyService.getMyDefaultPharmacy().subscribe({
      next: (selected) => {
        this.defaultPharmacy = selected;
      },
      error: () => {
        this.defaultPharmacy = null;
      }
    });
  }

  statusClass(status: PrescriptionStatus): string {
    return `status ${status.toLowerCase()}`;
  }

  isReadyForPickup(status: PrescriptionStatus): boolean {
    return status === 'READY_FOR_PICKUP';
  }

  primaryMedication(item: PrescriptionResponse): string {
    return item.medicineLines?.[0]?.medicationName || item.medicationName || '-';
  }

  primaryDosage(item: PrescriptionResponse): string {
    return item.medicineLines?.[0]?.dosage || item.dosage || '-';
  }

  primaryQuantity(item: PrescriptionResponse): number | string {
    return item.medicineLines?.[0]?.quantity ?? item.quantity ?? '-';
  }

  additionalLinesCount(item: PrescriptionResponse): number {
    return item.medicineLines && item.medicineLines.length > 1 ? item.medicineLines.length - 1 : 0;
  }
}
