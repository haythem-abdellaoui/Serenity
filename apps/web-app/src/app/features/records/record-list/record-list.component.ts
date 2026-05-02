import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MedicalRecordService } from '../../../core/services/medical-record.service';
import { MedicalRecord } from '../../../models/medical-record.model';
import { PatientService } from '../../../core/services/patient.service';
import { Patient } from '../../../models/patient.model';
import { NotificationService } from '../../../shared/services/notification.service';
import { getParamFromRouteTree } from '../../../shared/utils/route-params';

@Component({
  selector: 'app-record-list',
  templateUrl: './record-list.component.html',
  styleUrls: ['./record-list.component.scss']
})
export class RecordListComponent implements OnInit {
  patientId: number | null = null;
  patient: Patient | null = null;
  records: MedicalRecord[] = [];
  loading = false;

  deleteConfirm: { id: number } | null = null;

  // Search
  searchDiagnosis = '';
  searchStatus = '';
  searchSeverity = '';
  isSearching = false;
  searchLoading = false;
  searchResults: MedicalRecord[] = [];

  readonly statuses = ['', 'ACTIVE', 'CLOSED'] as const;
  readonly severities = ['', 'LOW', 'MEDIUM', 'HIGH'] as const;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly recordService: MedicalRecordService,
    private readonly patientService: PatientService,
    private readonly notification: NotificationService
  ) {}

  ngOnInit(): void {
    const pid = getParamFromRouteTree(this.route, 'patientId');
    if (!pid) {
      this.router.navigate(['/patients']);
      return;
    }
    this.patientId = +pid;
    this.patientService.getPatientById(this.patientId).subscribe({
      next: (p) => { this.patient = p; }
    });
    this.load();
  }

  load(): void {
    if (this.patientId == null) return;
    this.loading = true;
    this.recordService.getRecordsByPatientId(this.patientId).subscribe({
      next: (list) => {
        this.records = list;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  search(): void {
    const diag = this.searchDiagnosis.trim();
    if (!diag && !this.searchStatus && !this.searchSeverity) {
      this.clearSearch();
      return;
    }
    this.isSearching = true;
    this.searchLoading = true;
    this.recordService
      .searchRecords(
        diag || undefined,
        this.searchStatus || undefined,
        this.searchSeverity || undefined
      )
      .subscribe({
        next: (results) => {
          this.searchResults = results;
          this.searchLoading = false;
        },
        error: () => {
          this.searchLoading = false;
        }
      });
  }

  clearSearch(): void {
    this.searchDiagnosis = '';
    this.searchStatus = '';
    this.searchSeverity = '';
    this.isSearching = false;
    this.searchResults = [];
  }

  openDeleteRecord(id: number): void {
    this.deleteConfirm = { id };
  }

  closeDeleteConfirm(): void {
    this.deleteConfirm = null;
  }

  confirmDeleteRecord(): void {
    if (!this.deleteConfirm) return;
    const { id } = this.deleteConfirm;
    this.deleteConfirm = null;
    this.recordService.deleteRecord(id).subscribe({
      next: () => {
        this.notification.success('Record deleted');
        this.load();
        if (this.isSearching) this.search();
      },
      error: () => {
        /* toast */
      }
    });
  }

  // ── JPQL Complexe: Filtre "Active Treatments" ──
  filterActiveOnly = false;

  toggleActiveTreatments(): void {
    this.filterActiveOnly = !this.filterActiveOnly;
    if (this.filterActiveOnly && this.patientId != null) {
      this.loading = true;
      this.recordService.getRecordsWithActiveTreatment(this.patientId).subscribe({
        next: (list) => {
          this.records = list;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        }
      });
    } else {
      this.load();
    }
  }
}
