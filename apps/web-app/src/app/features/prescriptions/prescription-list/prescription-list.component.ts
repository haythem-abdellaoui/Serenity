import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PrescriptionService } from '../../../core/services/prescription.service';
import { Prescription } from '../../../models/prescription.model';
import { NotificationService } from '../../../shared/services/notification.service';
import { getParamFromRouteTree } from '../../../shared/utils/route-params';

@Component({
  selector: 'app-prescription-list',
  templateUrl: './prescription-list.component.html',
  styleUrls: ['./prescription-list.component.scss']
})
export class PrescriptionListComponent implements OnInit {
  patientId: number | null = null;
  recordId: number | null = null;
  prescriptions: Prescription[] = [];
  loading = false;

  deleteConfirm: { id: number } | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly prescriptionService: PrescriptionService,
    private readonly notification: NotificationService
  ) {}

  ngOnInit(): void {
    const pid = getParamFromRouteTree(this.route, 'patientId');
    const rid = getParamFromRouteTree(this.route, 'recordId');
    if (!pid || !rid) {
      this.router.navigate(['/patients']);
      return;
    }
    this.patientId = +pid;
    this.recordId = +rid;
    this.load();
  }

  load(): void {
    if (this.recordId == null) return;
    this.loading = true;
    this.prescriptionService.getPrescriptionsByRecordId(this.recordId).subscribe({
      next: (list) => {
        this.prescriptions = list;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  downloadPdf(id: number): void {
    this.prescriptionService.downloadPdf(id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `ordonnance-${id}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: () => this.notification.error('Erreur lors du téléchargement du PDF')
    });
  }

  openDeletePrescription(id: number): void {
    this.deleteConfirm = { id };
  }

  closeDeleteConfirm(): void {
    this.deleteConfirm = null;
  }

  confirmDeletePrescription(): void {
    if (!this.deleteConfirm) return;
    const { id } = this.deleteConfirm;
    this.deleteConfirm = null;
    this.prescriptionService.deletePrescription(id).subscribe({
      next: () => {
        this.notification.success('Prescription deleted');
        this.load();
      },
      error: () => {
        /* toast */
      }
    });
  }

  // ── Keywords Complexes: Filtre "Critical Prescriptions" ──
  filterCriticalOnly = false;

  toggleCriticalPrescriptions(): void {
    this.filterCriticalOnly = !this.filterCriticalOnly;
    if (this.filterCriticalOnly && this.patientId != null) {
      this.loading = true;
      this.prescriptionService.getCriticalPrescriptions(this.patientId).subscribe({
        next: (list) => {
          this.prescriptions = list;
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
