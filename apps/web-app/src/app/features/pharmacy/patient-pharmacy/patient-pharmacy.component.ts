import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import { PickerMarker } from '../../../shared/components/location-picker/location-picker.component';
import {
  PatientDefaultPharmacyResponse,
  PharmacyApplicationResponse,
  PharmacyCandidateResponse,
  PrescriptionLineResponse,
  PrescriptionResponse
} from '../../../shared/models/pharmacy.model';

interface PrescriptionCardView {
  raw: PrescriptionResponse;
  lines: PrescriptionLineResponse[];
  primaryLine: PrescriptionLineResponse;
  extraLinesCount: number;
}

@Component({
  selector: 'app-patient-pharmacy',
  templateUrl: './patient-pharmacy.component.html',
  styleUrls: ['./patient-pharmacy.component.scss']
})
export class PatientPharmacyComponent implements OnInit {
  loading = false;
  nearestLoading = false;
  saving = false;
  prescriptionsLoading = true;
  applicationLoading = true;
  hasSearchedCandidates = false;

  errorMessage = '';
  successMessage = '';
  prescriptionsErrorMessage = '';

  cityFilter = '';
  governorateFilter = '';

  defaultPharmacy: PatientDefaultPharmacyResponse | null = null;
  pharmacyApplication: PharmacyApplicationResponse | null = null;
  candidateResults: PharmacyCandidateResponse[] = [];
  prescriptionCards: PrescriptionCardView[] = [];
  mapMarkers: PickerMarker[] = [];
  mapMessage = 'Map will show your default pharmacy location.';

  get showApplicationCard(): boolean {
    return !this.applicationLoading && this.pharmacyApplication != null;
  }

  ngOnInit(): void {
    this.loadDefaultPharmacy();
    this.loadPrescriptions();
    this.loadPharmacyApplication();
  }

  constructor(
    private readonly pharmacyService: PharmacyService,
    private readonly router: Router
  ) {}

  loadDefaultPharmacy(): void {
    this.pharmacyService.getMyDefaultPharmacy().subscribe({
      next: (response) => {
        this.defaultPharmacy = response;
        this.refreshMapMarkers();
      },
      error: (err) => {
        if (err.status !== 404) {
          this.errorMessage = err.error?.message || 'Failed to load your default pharmacy';
        }
        this.defaultPharmacy = null;
        this.refreshMapMarkers();
      }
    });
  }

  loadPrescriptions(): void {
    this.prescriptionsLoading = true;
    this.prescriptionsErrorMessage = '';

    this.pharmacyService.getMyPrescriptions().subscribe({
      next: (items) => {
        this.prescriptionCards = items.map((item) => this.toPrescriptionCard(item));
        this.prescriptionsLoading = false;
      },
      error: (err) => {
        this.prescriptionCards = [];
        this.prescriptionsErrorMessage = err.error?.message || 'Failed to load your prescriptions';
        this.prescriptionsLoading = false;
      }
    });
  }

  loadPharmacies(): void {
    this.loading = true;
    this.hasSearchedCandidates = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.pharmacyService.listPatientPharmacies(this.cityFilter, this.governorateFilter).subscribe({
      next: (items) => {
        this.candidateResults = items;
        this.refreshMapMarkers();
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load pharmacies';
        this.candidateResults = [];
        this.refreshMapMarkers();
        this.loading = false;
      }
    });
  }

  useMyLocation(): void {
    this.hasSearchedCandidates = true;
    this.errorMessage = '';
    this.successMessage = '';

    if (!navigator.geolocation) {
      this.errorMessage = 'Geolocation is not supported by this browser. Choose from the pharmacy list instead.';
      return;
    }

    this.nearestLoading = true;

    navigator.geolocation.getCurrentPosition(
      (position) => {
        this.loadNearestPharmaciesWithFallback(
          position.coords.latitude,
          position.coords.longitude
        );
      },
      () => {
        this.nearestLoading = false;
        this.errorMessage = 'Location permission was denied. Please choose from the list below.';
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  }

  setDefault(pharmacyId: number): void {
    const selectedPharmacy = this.candidateResults.find((candidate) => candidate.id === pharmacyId);
    const pharmacyName = selectedPharmacy?.name || 'this pharmacy';
    if (!window.confirm(`Set "${pharmacyName}" as your default pharmacy?`)) {
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.pharmacyService.setMyDefaultPharmacy({ pharmacyId }).subscribe({
      next: (response) => {
        this.defaultPharmacy = response;
        // Keep map lean and focused after update: show only the selected default pharmacy.
        this.candidateResults = [];
        this.hasSearchedCandidates = false;
        this.refreshMapMarkers();
        this.saving = false;
        this.successMessage = 'Default pharmacy updated successfully.';
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to set default pharmacy';
        this.saving = false;
      }
    });
  }

  isDefault(pharmacyId: number): boolean {
    return this.defaultPharmacy?.pharmacyId === pharmacyId;
  }

  isCandidatesLoading(): boolean {
    return this.loading || this.nearestLoading;
  }

  openPrescriptionDetails(card: PrescriptionCardView): void {
    this.router.navigate(['/pharmacy/patient/prescriptions', card.raw.id]);
  }

  openApplyAsPharmacist(): void {
    this.router.navigate(['/pharmacy/apply']);
  }

  trackByCandidateId(_: number, item: PharmacyCandidateResponse): number {
    return item.id;
  }

  trackByPrescriptionId(_: number, item: PrescriptionCardView): number {
    return item.raw.id;
  }

  formatDistanceKm(distanceKm?: number): string {
    if (typeof distanceKm !== 'number' || !Number.isFinite(distanceKm)) {
      return '';
    }

    const compactDistance = distanceKm >= 10
      ? Math.round(distanceKm)
      : Math.round(distanceKm * 10) / 10;

    return `${compactDistance} km`;
  }

  private toPrescriptionCard(item: PrescriptionResponse): PrescriptionCardView {
    const lines = item.medicineLines && item.medicineLines.length > 0
      ? item.medicineLines
      : [{
          id: item.id,
          medicationName: item.medicationName || '-',
          dosage: item.dosage || '-',
          quantity: item.quantity ?? 0,
          instructions: item.instructions
        }];

    return {
      raw: item,
      lines,
      primaryLine: lines[0],
      extraLinesCount: Math.max(lines.length - 1, 0)
    };
  }

  private loadPharmacyApplication(): void {
    this.applicationLoading = true;
    this.pharmacyService.getMyPharmacyApplication().subscribe({
      next: (application) => {
        this.pharmacyApplication = application;
        this.applicationLoading = false;
      },
      error: (err) => {
        if (err.status !== 404) {
          this.errorMessage = err.error?.message || 'Failed to load pharmacist application status';
        }
        this.pharmacyApplication = null;
        this.applicationLoading = false;
      }
    });
  }

  private refreshMapMarkers(): void {
    if (this.hasSearchedCandidates) {
      const markers = this.candidateResults
        .filter((candidate) => this.hasCoordinates(candidate.latitude, candidate.longitude))
        .slice(0, 30)
        .map((candidate) => ({
          latitude: candidate.latitude as number,
          longitude: candidate.longitude as number,
          label: candidate.name,
          primary: this.isDefault(candidate.id)
        }));

      this.mapMarkers = markers;

      if (markers.length > 0) {
        this.mapMessage = `${markers.length} pharmacy location${markers.length > 1 ? 's' : ''} shown on the map.`;
        return;
      }

      this.mapMessage = this.candidateResults.length > 0
        ? 'Search results have no map coordinates to display.'
        : 'No pharmacy locations to display for this search yet.';
      return;
    }

    if (this.defaultPharmacy && this.hasCoordinates(this.defaultPharmacy.latitude, this.defaultPharmacy.longitude)) {
      this.mapMarkers = [{
        latitude: this.defaultPharmacy.latitude as number,
        longitude: this.defaultPharmacy.longitude as number,
        label: this.defaultPharmacy.pharmacyName,
        primary: true
      }];
      this.mapMessage = 'Showing your current default pharmacy location.';
      return;
    }

    this.mapMarkers = [];
    this.mapMessage = 'No pharmacy coordinates are available yet.';
  }

  private hasCoordinates(latitude?: number, longitude?: number): boolean {
    return typeof latitude === 'number' && Number.isFinite(latitude)
      && typeof longitude === 'number' && Number.isFinite(longitude);
  }

  private loadNearestPharmaciesWithFallback(latitude: number, longitude: number): void {
    const primaryRadiusKm = 20;
    const fallbackRadiusKm = 35;

    this.pharmacyService.suggestNearestPharmacies(latitude, longitude, primaryRadiusKm).subscribe({
      next: (items) => {
        if (items.length > 0) {
          this.candidateResults = items;
          this.refreshMapMarkers();
          this.nearestLoading = false;
          return;
        }
        this.tryWiderNearestThenFullList(latitude, longitude, primaryRadiusKm, fallbackRadiusKm);
      },
      error: (err) => this.handleSuggestNearestError(err)
    });
  }

  private tryWiderNearestThenFullList(
    latitude: number,
    longitude: number,
    primaryRadiusKm: number,
    fallbackRadiusKm: number
  ): void {
    this.pharmacyService.suggestNearestPharmacies(latitude, longitude, fallbackRadiusKm).subscribe({
      next: (fallbackItems) => {
        if (fallbackItems.length > 0) {
          this.candidateResults = fallbackItems;
          this.refreshMapMarkers();
          this.nearestLoading = false;
          this.successMessage = `No pharmacies found within ${primaryRadiusKm} km. Showing nearest matches within ${fallbackRadiusKm} km.`;
          return;
        }
        this.loadNearestFromFullPatientPharmacyList(latitude, longitude, fallbackRadiusKm);
      },
      error: (err) => this.handleSuggestNearestError(err)
    });
  }

  /** Final fallback: nearest known pharmacies from full list, sorted by distance. */
  private loadNearestFromFullPatientPharmacyList(
    latitude: number,
    longitude: number,
    fallbackRadiusKm: number
  ): void {
    this.pharmacyService.listPatientPharmacies().subscribe({
      next: (allPharmacies) =>
        this.applySortedNearestKnownPharmacies(allPharmacies, latitude, longitude, fallbackRadiusKm),
      error: (listError) => this.handleListPatientPharmaciesError(listError)
    });
  }

  private applySortedNearestKnownPharmacies(
    allPharmacies: PharmacyCandidateResponse[],
    latitude: number,
    longitude: number,
    fallbackRadiusKm: number
  ): void {
    const nearestKnown = this.rankPharmaciesByDistance(allPharmacies, latitude, longitude);

    this.candidateResults = nearestKnown;
    this.refreshMapMarkers();
    this.nearestLoading = false;

    if (nearestKnown.length > 0) {
      this.successMessage = `No pharmacies found within ${fallbackRadiusKm} km. Showing closest available pharmacies.`;
      return;
    }

    this.errorMessage = '';
    this.successMessage = 'No nearby pharmacies found for your location. Try searching by city or governorate.';
  }

  private rankPharmaciesByDistance(
    allPharmacies: PharmacyCandidateResponse[],
    latitude: number,
    longitude: number
  ): PharmacyCandidateResponse[] {
    return allPharmacies
      .map((pharmacy) => this.withDistanceKm(pharmacy, latitude, longitude))
      .sort((a, b) => {
        const aDistance = a.distanceKm ?? Number.MAX_SAFE_INTEGER;
        const bDistance = b.distanceKm ?? Number.MAX_SAFE_INTEGER;
        return aDistance - bDistance;
      })
      .slice(0, 20);
  }

  private withDistanceKm(
    pharmacy: PharmacyCandidateResponse,
    latitude: number,
    longitude: number
  ): PharmacyCandidateResponse {
    if (!this.hasCoordinates(pharmacy.latitude, pharmacy.longitude)) {
      return { ...pharmacy, distanceKm: undefined };
    }
    const distanceKm = this.roundDistance(
      this.calculateDistanceKm(
        latitude,
        longitude,
        pharmacy.latitude as number,
        pharmacy.longitude as number
      )
    );
    return { ...pharmacy, distanceKm };
  }

  private handleSuggestNearestError(err: { error?: { message?: string } }): void {
    this.errorMessage = err.error?.message
      || 'Unable to use your location right now. You can still search by city or governorate.';
    this.candidateResults = [];
    this.refreshMapMarkers();
    this.nearestLoading = false;
  }

  private handleListPatientPharmaciesError(listError: { error?: { message?: string } }): void {
    this.errorMessage = listError.error?.message || 'Unable to load pharmacies right now.';
    this.candidateResults = [];
    this.refreshMapMarkers();
    this.nearestLoading = false;
  }

  private calculateDistanceKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
    const earthRadiusKm = 6371;
    const dLat = this.toRadians(lat2 - lat1);
    const dLon = this.toRadians(lon2 - lon1);

    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(this.toRadians(lat1)) * Math.cos(this.toRadians(lat2)) *
      Math.sin(dLon / 2) * Math.sin(dLon / 2);

    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return earthRadiusKm * c;
  }

  private roundDistance(distanceKm: number): number {
    return Math.round(distanceKm * 100) / 100;
  }

  private toRadians(value: number): number {
    return value * (Math.PI / 180);
  }
}
