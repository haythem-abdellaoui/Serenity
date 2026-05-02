import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { InsuranceService } from '../../../core/services/insurance.service';
import { AuthService } from '../../../core/services/auth.service';
import { ClaimRiskScoreResponse, InsuranceClaimResponse } from '../../../shared/models/insurance.model';
import { UserService } from '../../../core/services/user.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-claim-list',
  templateUrl: './claim-list.component.html',
  styleUrls: ['./claim-list.component.scss']
})
export class ClaimListComponent implements OnInit, OnDestroy {
  private static readonly PAGE_SIZE = 12;

  claims: InsuranceClaimResponse[] = [];
  currentPage = 0;
  totalPages = 0;
  totalElements = 0;
  loading = true;
  errorMessage = '';
  isAdmin = false;
  private readonly userLabelsByUserId = new Map<number, string>();
  private queryParamsSub?: Subscription;

  statusFilter: 'ALL' | 'PENDING' | 'SUBMITTED' | 'UNDER_REVIEW' | 'NEEDS_INFO' | 'APPROVED' | 'PARTIALLY_APPROVED' | 'PAID' | 'REJECTED' = 'ALL';
  fromDate = '';
  toDate = '';
  userFilter = 'ALL';
  sortBy: 'DATE' | 'REIMBURSEMENT' = 'DATE';
  sortDirection: 'ASC' | 'DESC' = 'DESC';
  openDropdown: 'status' | 'user' | 'sortBy' | 'sortDirection' | null = null;

  showRiskModal = false;
  riskLoading = false;
  riskError = '';
  riskTargetClaim: InsuranceClaimResponse | null = null;
  riskResult: ClaimRiskScoreResponse | null = null;
  holdActionLoadingByClaimId = new Map<number, boolean>();

  constructor(
    private readonly insuranceService: InsuranceService,
    private readonly authService: AuthService,
    private readonly userService: UserService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.authService.isAdmin();
    if (this.isAdmin) {
      this.loadUsernames();
    }
    this.queryParamsSub = this.route.queryParamMap.subscribe((params) => {
      this.applyQueryParams(params);
      this.loadClaims();
    });
  }

  ngOnDestroy(): void {
    this.queryParamsSub?.unsubscribe();
  }

  loadClaims(): void {
    this.loading = true;
    const selectedUserId =
      this.isAdmin && this.userFilter !== 'ALL' ? Number(this.userFilter) : undefined;
    const backendFilters = {
      status: this.statusFilter === 'ALL' ? undefined : this.statusFilter,
      fromDate: this.fromDate || undefined,
      toDate: this.toDate || undefined,
      sortBy: this.sortBy === 'DATE' ? 'claimDate' : 'reimbursementAmount',
      sortDir: this.sortDirection.toLowerCase(),
      userId: Number.isFinite(selectedUserId) ? selectedUserId : undefined,
      page: this.currentPage,
      size: ClaimListComponent.PAGE_SIZE
    };

    const source$ = this.isAdmin
      ? this.insuranceService.getAllClaimsPaged(backendFilters)
      : this.insuranceService.getMyClaimsPaged(backendFilters);

    source$.subscribe({
      next: (response) => {
        this.claims = response.content || [];
        this.totalElements = response.totalElements || 0;
        this.totalPages = response.totalPages || 0;
        if (this.totalPages > 0 && this.currentPage >= this.totalPages) {
          this.updateQueryParams({ page: this.totalPages - 1 });
          return;
        }
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load claims';
        this.loading = false;
      }
    });
  }

  isHoldActionLoading(claimId: number): boolean {
    return this.holdActionLoadingByClaimId.get(claimId) === true;
  }

  sendHeldToPortal(claim: InsuranceClaimResponse, event?: MouseEvent): void {
    event?.stopPropagation();
    if (!this.isAdmin || claim.status !== 'UNDER_REVIEW') return;
    if (this.isHoldActionLoading(claim.id)) return;
    this.holdActionLoadingByClaimId.set(claim.id, true);
    this.insuranceService.sendHeldClaimToPortal(claim.id).subscribe({
      next: () => {
        this.holdActionLoadingByClaimId.delete(claim.id);
        this.loadClaims();
      },
      error: (err) => {
        this.holdActionLoadingByClaimId.delete(claim.id);
        this.errorMessage = err?.error?.message || 'Failed to send claim to portal';
      }
    });
  }

  rejectHeld(claim: InsuranceClaimResponse, event?: MouseEvent): void {
    event?.stopPropagation();
    if (!this.isAdmin || claim.status !== 'UNDER_REVIEW') return;
    if (this.isHoldActionLoading(claim.id)) return;
    const reason = window.prompt('Reject reason (optional):') || '';
    this.holdActionLoadingByClaimId.set(claim.id, true);
    this.insuranceService.rejectHeldClaim(claim.id, reason.trim()).subscribe({
      next: () => {
        this.holdActionLoadingByClaimId.delete(claim.id);
        this.loadClaims();
      },
      error: (err) => {
        this.holdActionLoadingByClaimId.delete(claim.id);
        this.errorMessage = err?.error?.message || 'Failed to reject claim';
      }
    });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'APPROVED': return 'badge badge-success';
      case 'PAID': return 'badge badge-success';
      case 'REJECTED': return 'badge badge-danger';
      case 'NEEDS_INFO': return 'badge badge-warning';
      default: return 'badge badge-primary';
    }
  }

  getTotalReimbursed(claim: InsuranceClaimResponse): number {
    return claim.remboursements?.reduce((sum, r) => sum + r.montant, 0) || 0;
  }

  get filteredClaims(): InsuranceClaimResponse[] {
    return this.claims;
  }

  get availableUsers(): { key: string; label: string }[] {
    return Array.from(this.userLabelsByUserId.entries())
      .map(([id, label]) => ({ key: String(id), label }))
      .filter((user) => user.key.trim().length > 0)
      .sort((a, b) => a.label.localeCompare(b.label));
  }

  clearFilters(): void {
    this.userFilter = 'ALL';
    this.updateQueryParams({
      user: null,
      status: null,
      fromDate: null,
      toDate: null,
      sortBy: null,
      sortDir: null,
      page: null
    });
  }

  onFromDateChange(): void {
    if (this.fromDate && this.toDate && this.toDate < this.fromDate) {
      this.toDate = this.fromDate;
    }
    this.updateQueryParams({
      fromDate: this.fromDate || null,
      toDate: this.toDate || null,
      page: 0
    });
  }

  onToDateChange(): void {
    if (this.fromDate && this.toDate && this.toDate < this.fromDate) {
      this.toDate = this.fromDate;
    }
    this.updateQueryParams({
      fromDate: this.fromDate || null,
      toDate: this.toDate || null,
      page: 0
    });
  }

  toggleDropdown(name: 'status' | 'user' | 'sortBy' | 'sortDirection', event: MouseEvent): void {
    event.stopPropagation();
    this.openDropdown = this.openDropdown === name ? null : name;
  }

  selectStatus(value: 'ALL' | 'PENDING' | 'SUBMITTED' | 'UNDER_REVIEW' | 'NEEDS_INFO' | 'APPROVED' | 'PARTIALLY_APPROVED' | 'PAID' | 'REJECTED'): void {
    this.statusFilter = value;
    this.openDropdown = null;
    this.updateQueryParams({
      status: value === 'ALL' ? null : value,
      page: 0
    });
  }

  selectUser(value: string): void {
    this.userFilter = value;
    this.openDropdown = null;
    this.updateQueryParams({
      user: value === 'ALL' ? null : value,
      page: 0
    });
  }

  selectSortBy(value: 'DATE' | 'REIMBURSEMENT'): void {
    this.sortBy = value;
    this.openDropdown = null;
    this.updateQueryParams({
      sortBy: value === 'DATE' ? 'claimDate' : 'reimbursementAmount',
      page: 0
    });
  }

  selectSortDirection(value: 'ASC' | 'DESC'): void {
    this.sortDirection = value;
    this.openDropdown = null;
    this.updateQueryParams({
      sortDir: value.toLowerCase(),
      page: 0
    });
  }

  get statusFilterLabel(): string {
    switch (this.statusFilter) {
      case 'PENDING': return 'Pending';
      case 'SUBMITTED': return 'Submitted';
      case 'UNDER_REVIEW': return 'Under review';
      case 'NEEDS_INFO': return 'Needs info';
      case 'APPROVED': return 'Approved';
      case 'PARTIALLY_APPROVED': return 'Partially approved';
      case 'PAID': return 'Paid';
      case 'REJECTED': return 'Rejected';
      default: return 'All';
    }
  }

  get sortByLabel(): string {
    return this.sortBy === 'DATE' ? 'Date' : 'Reimbursement amount';
  }

  get sortDirectionLabel(): string {
    return this.sortDirection === 'DESC' ? 'Descending' : 'Ascending';
  }

  get selectedUserLabel(): string {
    if (this.userFilter === 'ALL') {
      return 'All users';
    }
    return this.userLabelsByUserId.get(Number(this.userFilter)) || 'All users';
  }

  get pageNumbers(): number[] {
    if (this.totalPages <= 1) {
      return [];
    }
    const start = Math.max(0, this.currentPage - 2);
    const end = Math.min(this.totalPages - 1, this.currentPage + 2);
    const pages: number[] = [];
    for (let p = start; p <= end; p++) {
      pages.push(p);
    }
    return pages;
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages || page === this.currentPage) {
      return;
    }
    this.updateQueryParams({ page });
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.openDropdown = null;
  }

  openRiskModal(claim: InsuranceClaimResponse, event?: Event): void {
    event?.stopPropagation();
    if (!this.isAdmin) return;
    this.showRiskModal = true;
    this.riskTargetClaim = claim;
    this.riskResult = null;
    this.riskError = '';
    this.riskLoading = true;

    this.insuranceService.getClaimRiskScore(claim.id).subscribe({
      next: (res) => {
        this.riskResult = res;
        this.riskLoading = false;
      },
      error: (err) => {
        this.riskError = err.error?.message || err.error?.detail || 'Failed to fetch risk score';
        this.riskLoading = false;
      }
    });
  }

  closeRiskModal(): void {
    this.showRiskModal = false;
    this.riskTargetClaim = null;
    this.riskResult = null;
    this.riskError = '';
    this.riskLoading = false;
  }

  getRiskBandClass(band?: string | null): string {
    switch ((band || '').toUpperCase()) {
      case 'HIGH': return 'risk-badge risk-high';
      case 'MEDIUM': return 'risk-badge risk-medium';
      case 'LOW': return 'risk-badge risk-low';
      default: return 'risk-badge';
    }
  }

  private loadUsernames(): void {
    this.userService.getAllUsers().subscribe({
      next: (users) => {
        this.userLabelsByUserId.clear();
        for (const user of users) {
          const isAnonymous = !!user.profile?.isAnonymous;
          const label = isAnonymous
            ? `AnonymousUser#${user.id}`
            : (user.firstName?.trim() || user.email?.split('@')[0]?.trim() || `User #${user.id}`);
          this.userLabelsByUserId.set(user.id, label);
        }
      },
      error: () => {
        // Keep graceful fallback labels when user list cannot be loaded.
      }
    });
  }

  private getUserLabel(claim: InsuranceClaimResponse): string {
    const label = this.userLabelsByUserId.get(claim.userId);
    if (label) {
      return label;
    }
    return claim.userFullName?.trim() || `User #${claim.userId}`;
  }

  private getUserKey(claim: InsuranceClaimResponse): string {
    return String(claim.userId);
  }

  private applyQueryParams(params: import('@angular/router').ParamMap): void {
    const statusParam = params.get('status');
    this.statusFilter =
      statusParam === 'PENDING'
      || statusParam === 'SUBMITTED'
      || statusParam === 'UNDER_REVIEW'
      || statusParam === 'NEEDS_INFO'
      || statusParam === 'APPROVED'
      || statusParam === 'PARTIALLY_APPROVED'
      || statusParam === 'PAID'
      || statusParam === 'REJECTED'
        ? statusParam
        : 'ALL';

    const fromDate = params.get('fromDate') || '';
    const toDate = params.get('toDate') || '';
    this.fromDate = fromDate;
    this.toDate = toDate;

    const sortByParam = params.get('sortBy');
    this.sortBy = sortByParam === 'reimbursementAmount' ? 'REIMBURSEMENT' : 'DATE';

    const sortDirParam = params.get('sortDir');
    this.sortDirection = sortDirParam === 'asc' ? 'ASC' : 'DESC';

    const userParam = params.get('user');
    this.userFilter = this.isAdmin && userParam ? userParam : 'ALL';

    const pageParam = Number(params.get('page'));
    this.currentPage = Number.isInteger(pageParam) && pageParam >= 0 ? pageParam : 0;
  }

  private updateQueryParams(queryParams: {
    user?: string | null;
    status?: string | null;
    fromDate?: string | null;
    toDate?: string | null;
    sortBy?: string | null;
    sortDir?: string | null;
    page?: number | null;
  }): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      queryParamsHandling: 'merge'
    });
  }
}
