import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import {
  AlternativePharmacyOption,
  PatientDefaultPharmacyResponse,
  PerMedicineAlternative,
  PrescriptionAlternativeResponse,
  PrescriptionLineResponse,
  PrescriptionResponse,
  PrescriptionStatus
} from '../../../shared/models/pharmacy.model';

@Component({
  selector: 'app-patient-prescription-details',
  templateUrl: './patient-prescription-details.component.html',
  styleUrls: ['./patient-prescription-details.component.scss']
})
export class PatientPrescriptionDetailsComponent implements OnInit {
  loading = true;
  alternativesLoading = false;
  assigning = false;
  downloadingInsuranceDocument = false;

  errorMessage = '';
  alternativesErrorMessage = '';
  locationMessage = '';
  successMessage = '';

  prescriptionId: number | null = null;
  prescription: PrescriptionResponse | null = null;
  defaultPharmacy: PatientDefaultPharmacyResponse | null = null;
  lines: PrescriptionLineResponse[] = [];
  alternatives: PrescriptionAlternativeResponse | null = null;
  selectedAlternativePharmacy: AlternativePharmacyOption | null = null;
  selectedMapLatitude: number | null = null;
  selectedMapLongitude: number | null = null;

  currentLatitude: number | null = null;
  currentLongitude: number | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly pharmacyService: PharmacyService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.errorMessage = 'Invalid prescription id.';
      this.loading = false;
      return;
    }

    this.prescriptionId = id;
    this.loadDefaultPharmacy();
    this.loadPrescription(id);
  }

  backToPatientPharmacy(): void {
    this.router.navigate(['/pharmacy/patient']);
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
        this.errorMessage = err.error?.message || 'Failed to download stamped prescription document.';
      }
    });
  }

  useAlternativePharmacy(option: AlternativePharmacyOption): void {
    if (!this.prescriptionId || this.currentLatitude == null || this.currentLongitude == null) {
      this.alternativesErrorMessage = 'Location is required before selecting an alternative pharmacy.';
      return;
    }

    if (!window.confirm(`Send this prescription to "${option.pharmacyName}"?`)) {
      return;
    }

    this.assigning = true;
    this.successMessage = '';
    this.alternativesErrorMessage = '';

    this.pharmacyService.reassignPrescriptionPharmacy(this.prescriptionId, {
      pharmacyId: option.pharmacyId,
      latitude: this.currentLatitude,
      longitude: this.currentLongitude
    }).subscribe({
      next: () => {
        this.assigning = false;
        this.successMessage = `Prescription is now assigned to ${option.pharmacyName}.`;
        this.backToPatientPharmacy();
      },
      error: (err) => {
        this.assigning = false;
        this.alternativesErrorMessage = err.error?.message || 'Failed to reassign prescription pharmacy.';
      }
    });
  }

  statusClass(status: PrescriptionStatus): string {
    return `status-pill status-pill-${status.toLowerCase()}`;
  }

  statusLabel(status: PrescriptionStatus): string {
    return status.split('_').join(' ');
  }

  hasPendingStatus(): boolean {
    return this.prescription?.status === 'PENDING';
  }

  trackByLineId(index: number, line: PrescriptionLineResponse): string {
    return `${line.id}-${index}`;
  }

  trackByAlternativeId(_: number, option: AlternativePharmacyOption): number {
    return option.pharmacyId;
  }

  trackByPerMedicine(index: number, item: PerMedicineAlternative): string {
    return `${item.lineId}-${index}`;
  }

  recommendationSummary(result: PrescriptionAlternativeResponse): string {
    if (result.recommendedMode === 'FULL_MATCH') {
      return 'A nearby pharmacy can prepare your full prescription now.';
    }

    if (result.recommendedMode === 'PARTIAL_FALLBACK') {
      return 'Your default pharmacy does not currently cover all medicines. See nearby pharmacies by medicine below.';
    }

    return 'No nearby pharmacy currently has enough stock for your full prescription. Keep your default pharmacy and check later.';
  }

  defaultPharmacyMessage(result: PrescriptionAlternativeResponse): string {
    if (!this.defaultPharmacy) {
      return 'You do not have a default pharmacy selected yet.';
    }

    const defaultPharmacyId = this.defaultPharmacy.pharmacyId;
    const defaultPharmacyName = this.defaultPharmacy.pharmacyName;
    const fullMatchIncludesDefault = result.fullMatchPharmacies.some((option) => option.pharmacyId === defaultPharmacyId);

    if (fullMatchIncludesDefault) {
      return `Your default pharmacy (${defaultPharmacyName}) has your full prescription in stock.`;
    }

    if (result.recommendedMode === 'FULL_MATCH') {
      return `Your default pharmacy (${defaultPharmacyName}) may not have the full prescription right now. Suggested pharmacies below can prepare all medicines.`;
    }

    if (result.recommendedMode === 'PARTIAL_FALLBACK') {
      return `Your default pharmacy (${defaultPharmacyName}) does not currently have all required medicines.`;
    }

    return `Your default pharmacy (${defaultPharmacyName}) and nearby options do not currently have complete stock.`;
  }

  perMedicineMessage(line: PerMedicineAlternative): string {
    if (line.pharmacies.length === 0) {
      return `No pharmacy within 20 km currently has enough ${line.medicationName}.`;
    }

    if (line.pharmacies.length === 1) {
      return `${line.pharmacies[0].pharmacyName} has enough ${line.medicationName}.`;
    }

    return `${line.pharmacies.length} nearby pharmacies have enough ${line.medicationName}.`;
  }

  canShowSelectableSection(result: PrescriptionAlternativeResponse): boolean {
    return result.selectablePharmacies.length > 0 && result.recommendedMode !== 'FULL_MATCH';
  }

  selectAlternativeForPreview(option: AlternativePharmacyOption): void {
    this.selectedAlternativePharmacy = option;
    this.selectedMapLatitude = option.latitude ?? null;
    this.selectedMapLongitude = option.longitude ?? null;
  }

  isSelectedAlternative(option: AlternativePharmacyOption): boolean {
    return this.selectedAlternativePharmacy?.pharmacyId === option.pharmacyId;
  }

  private loadPrescription(id: number): void {
    this.loading = true;
    this.errorMessage = '';

    this.pharmacyService.getPrescriptionById(id).subscribe({
      next: (item) => {
        this.prescription = item;
        this.lines = this.resolveLines(item);
        this.loading = false;

        if (item.status === 'PENDING') {
          this.loadAlternativesByLocation();
        }
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load prescription details.';
        this.loading = false;
      }
    });
  }

  private loadDefaultPharmacy(): void {
    this.pharmacyService.getMyDefaultPharmacy().subscribe({
      next: (response) => {
        this.defaultPharmacy = response;
      },
      error: () => {
        this.defaultPharmacy = null;
      }
    });
  }

  private loadAlternativesByLocation(): void {
    if (!navigator.geolocation) {
      this.locationMessage = 'Geolocation is not supported. You can keep your default pharmacy.';
      return;
    }

    this.alternativesLoading = true;
    this.locationMessage = '';
    this.alternativesErrorMessage = '';

    navigator.geolocation.getCurrentPosition(
      (position) => {
        this.currentLatitude = position.coords.latitude;
        this.currentLongitude = position.coords.longitude;
        this.loadAlternatives(this.currentLatitude, this.currentLongitude);
      },
      () => {
        this.alternativesLoading = false;
        this.locationMessage = 'Location permission was denied. You can keep your default pharmacy.';
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  }

  private loadAlternatives(latitude: number, longitude: number): void {
    if (!this.prescriptionId) {
      this.alternativesLoading = false;
      return;
    }

    this.pharmacyService.getPrescriptionAlternatives(this.prescriptionId, latitude, longitude).subscribe({
      next: (response) => {
        this.alternatives = response;
        this.selectedAlternativePharmacy = this.resolveInitialAlternativeForPreview(response);
        this.selectedMapLatitude = this.selectedAlternativePharmacy?.latitude ?? null;
        this.selectedMapLongitude = this.selectedAlternativePharmacy?.longitude ?? null;
        this.alternativesLoading = false;
      },
      error: (err) => {
        this.selectedAlternativePharmacy = null;
        this.selectedMapLatitude = null;
        this.selectedMapLongitude = null;
        this.alternativesErrorMessage = err.error?.message || 'Failed to load pharmacy alternatives.';
        this.alternativesLoading = false;
      }
    });
  }

  private resolveLines(item: PrescriptionResponse): PrescriptionLineResponse[] {
    if (item.medicineLines && item.medicineLines.length > 0) {
      return item.medicineLines;
    }

    return [{
      id: item.id,
      medicationName: item.medicationName || '-',
      dosage: item.dosage || '-',
      quantity: item.quantity ?? 0,
      instructions: item.instructions
    }];
  }

  private resolveInitialAlternativeForPreview(
    result: PrescriptionAlternativeResponse
  ): AlternativePharmacyOption | null {
    if (result.fullMatchPharmacies.length > 0) {
      return result.fullMatchPharmacies[0];
    }

    if (result.selectablePharmacies.length > 0) {
      return result.selectablePharmacies[0];
    }

    for (const perMedicine of result.perMedicineAlternatives) {
      if (perMedicine.pharmacies.length > 0) {
        return perMedicine.pharmacies[0];
      }
    }

    return null;
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
