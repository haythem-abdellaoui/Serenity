import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import {
  PrescriptionResponse,
  PrescriptionStatus,
  StockItemResponse
} from '../../../shared/models/pharmacy.model';

type DashboardStatusFilter = 'ALL' | 'PENDING' | 'ACCEPTED' | 'READY_FOR_PICKUP' | 'REJECTED';
type DashboardSortBy = 'createdAt' | 'status';
type DashboardSortDirection = 'asc' | 'desc';

@Component({
  selector: 'app-pharmacist-dashboard',
  templateUrl: './pharmacist-dashboard.component.html',
  styleUrls: ['./pharmacist-dashboard.component.scss']
})
export class PharmacistDashboardComponent implements OnInit {
  loading = true;
  errorMessage = '';
  allPrescriptions: PrescriptionResponse[] = [];
  filteredPrescriptions: PrescriptionResponse[] = [];
  lowStockItems: StockItemResponse[] = [];
  insuranceMissingItems: PrescriptionResponse[] = [];

  search = '';
  statusFilter: DashboardStatusFilter = 'ALL';
  fromDate = '';
  toDate = '';
  sortBy: DashboardSortBy = 'createdAt';
  direction: DashboardSortDirection = 'desc';

  readonly statusOptions: Array<{ label: string; value: DashboardStatusFilter }> = [
    { label: 'All Statuses', value: 'ALL' },
    { label: 'Pending', value: 'PENDING' },
    { label: 'Accepted', value: 'ACCEPTED' },
    { label: 'Ready for Pickup', value: 'READY_FOR_PICKUP' },
    { label: 'Rejected', value: 'REJECTED' }
  ];

  readonly sortByOptions: Array<{ label: string; value: DashboardSortBy }> = [
    { label: 'Created Date', value: 'createdAt' },
    { label: 'Status', value: 'status' }
  ];

  readonly directionOptions: Array<{ label: string; value: DashboardSortDirection }> = [
    { label: 'Descending', value: 'desc' },
    { label: 'Ascending', value: 'asc' }
  ];

  private readonly statusRank: Record<PrescriptionStatus, number> = {
    PENDING: 1,
    ACCEPTED: 2,
    READY_FOR_PICKUP: 3,
    REJECTED: 4,
    COLLECTED: 5,
    EXPIRED: 6
  };

  constructor(
    private readonly pharmacyService: PharmacyService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
  }

  onFiltersChanged(): void {
    this.applyPrescriptionFilters();
  }

  resetFilters(): void {
    this.search = '';
    this.statusFilter = 'ALL';
    this.fromDate = '';
    this.toDate = '';
    this.sortBy = 'createdAt';
    this.direction = 'desc';
    this.applyPrescriptionFilters();
  }

  openPrescription(item: PrescriptionResponse): void {
    this.router.navigate(['/pharmacy/inbox', item.id]);
  }

  openStockManagement(): void {
    this.router.navigate(['/pharmacy/stock']);
  }

  medicineSummary(item: PrescriptionResponse): string {
    return this.medicineLines(item)[0]?.medicationName || '-';
  }

  extraLineCount(item: PrescriptionResponse): number {
    const lines = this.medicineLines(item);
    return lines.length > 1 ? lines.length - 1 : 0;
  }

  statusClass(status: PrescriptionStatus): string {
    return `status ${status.toLowerCase()}`;
  }

  private loadDashboardData(): void {
    this.loading = true;
    this.errorMessage = '';

    forkJoin({
      prescriptions: this.pharmacyService.getInbox(),
      stockItems: this.pharmacyService.listStock('', false),
      insuranceMissing: this.pharmacyService.getInsuranceMissingInbox()
    }).subscribe({
      next: ({ prescriptions, stockItems, insuranceMissing }) => {
        this.allPrescriptions = prescriptions;
        this.lowStockItems = this.buildLowStockList(stockItems);
        this.insuranceMissingItems = insuranceMissing;
        this.applyPrescriptionFilters();
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load pharmacist dashboard';
        this.loading = false;
      }
    });
  }

  private applyPrescriptionFilters(): void {
    const query = this.search.trim().toLowerCase();
    const fromBoundary = this.toStartOfDay(this.fromDate);
    const toBoundary = this.toEndOfDay(this.toDate);

    this.filteredPrescriptions = [...this.allPrescriptions]
      .filter((item) => this.matchesSearch(item, query))
      .filter((item) => this.matchesStatus(item))
      .filter((item) => this.matchesDateRange(item, fromBoundary, toBoundary))
      .sort((a, b) => this.compareRows(a, b));
  }

  private matchesSearch(item: PrescriptionResponse, query: string): boolean {
    if (!query) {
      return true;
    }
    return this.searchableText(item).includes(query);
  }

  private matchesStatus(item: PrescriptionResponse): boolean {
    return this.statusFilter === 'ALL' ? true : item.status === this.statusFilter;
  }

  private matchesDateRange(item: PrescriptionResponse, from: number | null, to: number | null): boolean {
    const createdAt = this.dateTimestamp(item.createdAt);
    if (createdAt == null) {
      return false;
    }
    if (from != null && createdAt < from) {
      return false;
    }
    if (to != null && createdAt > to) {
      return false;
    }
    return true;
  }

  private compareRows(a: PrescriptionResponse, b: PrescriptionResponse): number {
    const base = this.sortBy === 'status'
      ? this.compareByStatus(a, b)
      : this.compareByCreatedAt(a, b);
    return this.direction === 'asc' ? base : -base;
  }

  private compareByStatus(a: PrescriptionResponse, b: PrescriptionResponse): number {
    const rankDiff = this.statusScore(a.status) - this.statusScore(b.status);
    if (rankDiff !== 0) {
      return rankDiff;
    }
    return this.compareByCreatedAt(a, b);
  }

  private compareByCreatedAt(a: PrescriptionResponse, b: PrescriptionResponse): number {
    const aTime = this.dateTimestamp(a.createdAt) ?? 0;
    const bTime = this.dateTimestamp(b.createdAt) ?? 0;
    if (aTime !== bTime) {
      return aTime - bTime;
    }
    return a.id - b.id;
  }

  private statusScore(status: PrescriptionStatus): number {
    return this.statusRank[status] ?? Number.MAX_SAFE_INTEGER;
  }

  private toStartOfDay(rawDate: string): number | null {
    if (!rawDate) {
      return null;
    }
    const date = new Date(rawDate);
    if (Number.isNaN(date.getTime())) {
      return null;
    }
    date.setHours(0, 0, 0, 0);
    return date.getTime();
  }

  private toEndOfDay(rawDate: string): number | null {
    if (!rawDate) {
      return null;
    }
    const date = new Date(rawDate);
    if (Number.isNaN(date.getTime())) {
      return null;
    }
    date.setHours(23, 59, 59, 999);
    return date.getTime();
  }

  private dateTimestamp(rawDate?: string): number | null {
    if (!rawDate) {
      return null;
    }
    const date = new Date(rawDate);
    if (Number.isNaN(date.getTime())) {
      return null;
    }
    return date.getTime();
  }

  private searchableText(item: PrescriptionResponse): string {
    const lines = this.medicineLines(item).map((line) => line.medicationName || '').join(' ');
    return [
      item.patientName,
      item.doctorName,
      item.pharmacyName,
      item.medicationName,
      lines
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase();
  }

  private medicineLines(item: PrescriptionResponse): Array<{ medicationName: string }> {
    if (item.medicineLines && item.medicineLines.length > 0) {
      return item.medicineLines;
    }
    if (item.medicationName) {
      return [{ medicationName: item.medicationName }];
    }
    return [];
  }

  private buildLowStockList(stockItems: StockItemResponse[]): StockItemResponse[] {
    return stockItems
      .filter((item) => !item.archived && item.quantity <= 5)
      .sort((a, b) => {
        if (a.quantity !== b.quantity) {
          return a.quantity - b.quantity;
        }
        return a.medicineName.localeCompare(b.medicineName);
      });
  }
}
