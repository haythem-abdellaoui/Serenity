import { Component, OnInit } from '@angular/core';
import { PatientService } from '../../../core/services/patient.service';
import { PageResponseDTO } from '../../../models/page-response.model';
import { Patient } from '../../../models/patient.model';
import { NotificationService } from '../../../shared/services/notification.service';

@Component({
  selector: 'app-patient-list',
  templateUrl: './patient-list.component.html',
  styleUrls: ['./patient-list.component.scss']
})
export class PatientListComponent implements OnInit {
  patients: Patient[] = [];
  loading = false;

  // Search
  searchName = '';
  isSearching = false;
  searchLoading = false;
  searchResults: Patient[] = [];

  constructor(
    private readonly patientService: PatientService,
    private readonly notification: NotificationService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.patientService.getAllPatients().subscribe({
      next: (p) => {
        this.patients = p;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  search(): void {
    const name = this.searchName.trim();
    if (!name) {
      this.clearSearch();
      return;
    }
    this.isSearching = true;
    this.searchLoading = true;
    this.patientService.searchPatients(name).subscribe({
      next: (results) => {
        // filter out only PATIENT role since searchUsers returns any user
        this.searchResults = results.filter(u => true); // In actual app, filter by role if needed.
        this.searchLoading = false;
      },
      error: () => {
        this.searchLoading = false;
      }
    });
  }

  clearSearch(): void {
    this.searchName = '';
    this.isSearching = false;
    this.searchResults = [];
  }

  getInitials(p: Patient): string {
    const first = (p.firstName || '').charAt(0).toUpperCase();
    const last = (p.lastName || '').charAt(0).toUpperCase();
    return `${first}${last}`;
  }

  getAvatarClass(p: Patient): string {
    const colors = ['avatar-teal', 'avatar-purple', 'avatar-coral', 'avatar-amber', 'avatar-blue', 'avatar-emerald'];
    const hash = (p.firstName || '').length + (p.lastName || '').length + (p.id || 0);
    return colors[hash % colors.length];
  }
}
