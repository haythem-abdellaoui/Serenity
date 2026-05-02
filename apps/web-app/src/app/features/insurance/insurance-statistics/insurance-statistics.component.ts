import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { InsuranceService } from '../../../core/services/insurance.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import {
  ClaimRemittanceOcrSummary,
  InsuranceClaimResponse
} from '../../../shared/models/insurance.model';
import { UserResponse } from '../../../shared/models/user.model';

interface StatusBucket {
  label: string;
  key: 'PENDING' | 'APPROVED' | 'REJECTED';
  count: number;
}

@Component({
  selector: 'app-insurance-statistics',
  templateUrl: './insurance-statistics.component.html',
  styleUrls: ['./insurance-statistics.component.scss']
})
export class InsuranceStatisticsComponent implements OnInit {
  loading = true;
  errorMessage = '';
  forbidden = false;

  claims: InsuranceClaimResponse[] = [];

  totalClaims = 0;
  totalAmount = 0;
  totalReimbursed = 0;

  // Animated display values for KPIs
  displayTotalClaims = 0;
  displayTotalAmount = 0;
  displayTotalReimbursed = 0;
  displayReimbursementRate = 0;

  statusBuckets: StatusBucket[] = [
    { key: 'PENDING', label: 'Pending', count: 0 },
    { key: 'APPROVED', label: 'Approved', count: 0 },
    { key: 'REJECTED', label: 'Rejected', count: 0 }
  ];

  companyStats: { company: string; count: number; amount: number; reimbursed: number }[] = [];
  gradeStats: { grade: number; count: number; amount: number; reimbursed: number }[] = [];
  monthlyStats: { month: string; count: number; amount: number; reimbursed: number }[] = [];
  maxMonthlyCount = 0;

  timeRange: 'ALL' | 'LAST_7' | 'LAST_30' | 'LAST_90' | 'THIS_YEAR' = 'ALL';

  deltaClaims: number | null = null;
  deltaReimbursed: number | null = null;

  insights: string[] = [];

  remittanceOcrSummary: ClaimRemittanceOcrSummary[] = [];

  private readonly userLabelsByUserId = new Map<number, string>();

  readonly remittanceReportPageSize = 10;
  remittanceReportPageIndex = 0;

  constructor(
    private readonly insuranceService: InsuranceService,
    private readonly userService: UserService,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    if (!this.authService.isAdmin()) {
      this.forbidden = true;
      this.loading = false;
      return;
    }

    forkJoin({
      claims: this.insuranceService.getAllClaims(),
      remittanceOcrSummary: this.insuranceService.getRemittanceOcrSummaryReport(),
      users: this.userService.getAllUsers().pipe(catchError(() => of([])))
    }).subscribe({
      next: ({ claims, remittanceOcrSummary, users }) => {
        this.claims = claims || [];
        this.remittanceOcrSummary = remittanceOcrSummary || [];
        this.remittanceReportPageIndex = 0;
        this.buildUserLabelMap((users || []) as UserResponse[]);
        this.recomputeAll();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load insurance statistics';
        this.loading = false;
      }
    });
  }

  onTimeRangeChange(range: 'ALL' | 'LAST_7' | 'LAST_30' | 'LAST_90' | 'THIS_YEAR'): void {
    if (this.timeRange === range) return;
    this.timeRange = range;
    this.recomputeAll();
  }

  private recomputeAll(): void {
    this.loading = false;
    this.computeStats();
    this.computeInsights();
    this.animateKpis();
  }

  private computeStats(): void {
    const { current, previous } = this.filterByTimeRange();

    this.totalClaims = current.length;
    this.totalAmount = 0;
    this.totalReimbursed = 0;

    const statusCounts: Record<string, number> = { PENDING: 0, APPROVED: 0, REJECTED: 0 };
    const companyMap = new Map<string, { count: number; amount: number; reimbursed: number }>();
    const gradeMap = new Map<number, { count: number; amount: number; reimbursed: number }>();
    const monthMap = new Map<string, { count: number; amount: number; reimbursed: number }>();

    for (const claim of current) {
      const reimbursed = claim.reimbursementAmount ?? 0;
      this.totalAmount += claim.amount;
      this.totalReimbursed += reimbursed;

      if (statusCounts[claim.status] != null) {
        statusCounts[claim.status]++;
      }

      const companyKey = claim.insuranceCompany || 'Unknown';
      const companyBucket = companyMap.get(companyKey) || { count: 0, amount: 0, reimbursed: 0 };
      companyBucket.count++;
      companyBucket.amount += claim.amount;
      companyBucket.reimbursed += reimbursed;
      companyMap.set(companyKey, companyBucket);

      const gradeKey = claim.insuranceGrade;
      const gradeBucket = gradeMap.get(gradeKey) || { count: 0, amount: 0, reimbursed: 0 };
      gradeBucket.count++;
      gradeBucket.amount += claim.amount;
      gradeBucket.reimbursed += reimbursed;
      gradeMap.set(gradeKey, gradeBucket);

      const date = new Date(claim.claimDate);
      const monthKey = isNaN(date.getTime())
        ? 'Unknown'
        : `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
      const monthBucket = monthMap.get(monthKey) || { count: 0, amount: 0, reimbursed: 0 };
      monthBucket.count++;
      monthBucket.amount += claim.amount;
      monthBucket.reimbursed += reimbursed;
      monthMap.set(monthKey, monthBucket);
    }

    this.statusBuckets = this.statusBuckets.map(b => ({
      ...b,
      count: statusCounts[b.key] || 0
    }));

    this.companyStats = Array.from(companyMap.entries())
      .map(([company, v]) => ({ company, ...v }))
      .sort((a, b) => b.count - a.count);

    this.gradeStats = Array.from(gradeMap.entries())
      .map(([grade, v]) => ({ grade, ...v }))
      .sort((a, b) => a.grade - b.grade);

    this.monthlyStats = Array.from(monthMap.entries())
      .map(([month, v]) => ({ month, ...v }))
      .sort((a, b) => a.month.localeCompare(b.month));

    this.maxMonthlyCount = this.monthlyStats.reduce((max, m) => Math.max(max, m.count), 0);

    const prevTotalClaims = previous.length;
    const prevReimbursed = previous.reduce((sum, c) => sum + (c.reimbursementAmount ?? 0), 0);
    this.deltaClaims = prevTotalClaims > 0 ? this.totalClaims - prevTotalClaims : null;
    this.deltaReimbursed = prevReimbursed > 0 ? this.totalReimbursed - prevReimbursed : null;
  }

  get reimbursementRate(): number {
    if (!this.totalAmount) return 0;
    return (this.totalReimbursed / this.totalAmount) * 100;
  }

  private animateKpis(): void {
    const duration = 600;
    const start = performance.now();

    const startClaims = 0;
    const startAmount = 0;
    const startReimbursed = 0;
    const startRate = 0;

    const targetClaims = this.totalClaims;
    const targetAmount = this.totalAmount;
    const targetReimbursed = this.totalReimbursed;
    const targetRate = this.reimbursementRate;

    const easeOut = (t: number) => 1 - Math.pow(1 - t, 3);

    const step = (now: number) => {
      const elapsed = now - start;
      const t = Math.min(1, elapsed / duration);
      const e = easeOut(t);

      this.displayTotalClaims = Math.round(startClaims + (targetClaims - startClaims) * e);
      this.displayTotalAmount = Math.round(startAmount + (targetAmount - startAmount) * e);
      this.displayTotalReimbursed = Math.round(startReimbursed + (targetReimbursed - startReimbursed) * e);
      this.displayReimbursementRate = +(startRate + (targetRate - startRate) * e).toFixed(1);

      if (t < 1) {
        requestAnimationFrame(step);
      }
    };

    requestAnimationFrame(step);
  }

  goToClaimsWithStatus(status: 'PENDING' | 'APPROVED' | 'REJECTED'): void {
    if (!this.totalClaims) return;
    this.router.navigate(['/admin/insurance'], {
      queryParams: { status }
    });
  }

  private filterByTimeRange(): { current: InsuranceClaimResponse[]; previous: InsuranceClaimResponse[] } {
    const all = this.claims;
    if (this.timeRange === 'ALL') {
      return { current: all, previous: [] };
    }

    const now = new Date();

    const daysForRange = (range: 'LAST_7' | 'LAST_30' | 'LAST_90') =>
      range === 'LAST_7' ? 7 : range === 'LAST_30' ? 30 : 90;

    if (this.timeRange === 'LAST_7' || this.timeRange === 'LAST_30' || this.timeRange === 'LAST_90') {
      const days = daysForRange(this.timeRange);
      const msPerDay = 24 * 60 * 60 * 1000;
      const currentFrom = new Date(now.getTime() - days * msPerDay);
      const prevFrom = new Date(currentFrom.getTime() - days * msPerDay);

      const current = all.filter(c => {
        const d = new Date(c.claimDate);
        return d >= currentFrom && d <= now;
      });

      const previous = all.filter(c => {
        const d = new Date(c.claimDate);
        return d >= prevFrom && d < currentFrom;
      });

      return { current, previous };
    }

    const year = now.getFullYear();
    const current = all.filter(c => {
      const d = new Date(c.claimDate);
      return d.getFullYear() === year;
    });

    const previous = all.filter(c => {
      const d = new Date(c.claimDate);
      return d.getFullYear() === year - 1;
    });

    return { current, previous };
  }

  private computeInsights(): void {
    const insights: string[] = [];

    if (this.totalClaims > 0) {
      const topCompany = this.companyStats[0];
      if (topCompany) {
        const share = (topCompany.count / this.totalClaims) * 100;
        insights.push(
          `Most claims in this period come from ${topCompany.company} (${topCompany.count} claims, ${share.toFixed(1)}%).`
        );
      }

      const rejectedBucket = this.statusBuckets.find(b => b.key === 'REJECTED');
      if (rejectedBucket && rejectedBucket.count > 0) {
        const rejectionRate = (rejectedBucket.count / this.totalClaims) * 100;
        insights.push(`Current rejection rate is ${rejectionRate.toFixed(1)}% across all claims.`);
      }
    }

    this.insights = insights.slice(0, 2);
  }

  goToClaims(): void {
    this.router.navigate(['/admin/insurance']);
  }

  get remittanceReportPagedRows(): ClaimRemittanceOcrSummary[] {
    const start = this.remittanceReportPageIndex * this.remittanceReportPageSize;
    return this.remittanceOcrSummary.slice(start, start + this.remittanceReportPageSize);
  }

  get remittanceReportPageCount(): number {
    if (!this.remittanceOcrSummary.length) {
      return 0;
    }
    return Math.ceil(this.remittanceOcrSummary.length / this.remittanceReportPageSize);
  }

  get remittanceReportRangeFrom(): number {
    if (!this.remittanceOcrSummary.length) {
      return 0;
    }
    return this.remittanceReportPageIndex * this.remittanceReportPageSize + 1;
  }

  get remittanceReportRangeTo(): number {
    return Math.min(
      (this.remittanceReportPageIndex + 1) * this.remittanceReportPageSize,
      this.remittanceOcrSummary.length
    );
  }

  remittanceReportGoPrev(): void {
    if (this.remittanceReportPageIndex > 0) {
      this.remittanceReportPageIndex--;
    }
  }

  remittanceReportGoNext(): void {
    if (this.remittanceReportPageIndex < this.remittanceReportPageCount - 1) {
      this.remittanceReportPageIndex++;
    }
  }

  getRemittanceUserLabel(userId: number): string {
    return this.userLabelsByUserId.get(userId) ?? `User #${userId}`;
  }

  remittanceClaimReferenceLabel(row: ClaimRemittanceOcrSummary): string {
    const ref = row.externalRef?.trim();
    if (ref) {
      return ref;
    }
    return `Claim #${row.claimId}`;
  }

  private buildUserLabelMap(users: UserResponse[]): void {
    this.userLabelsByUserId.clear();
    for (const user of users) {
      const isAnonymous = !!user.profile?.isAnonymous;
      const full = `${(user.firstName || '').trim()} ${(user.lastName || '').trim()}`.trim();
      const label = isAnonymous
        ? `Anonymous user #${user.id}`
        : full || user.email?.split('@')[0]?.trim() || `User #${user.id}`;
      this.userLabelsByUserId.set(user.id, label);
    }
  }
}

