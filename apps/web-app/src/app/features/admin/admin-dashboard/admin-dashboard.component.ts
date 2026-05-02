import { Component, OnInit } from '@angular/core';
import { UserService } from '../../../core/services/user.service';
import { InsuranceService } from '../../../core/services/insurance.service';
import { UserResponse } from '../../../shared/models/user.model';
import { InsuranceClaimResponse } from '../../../shared/models/insurance.model';

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {
  users: UserResponse[] = [];
  claims: InsuranceClaimResponse[] = [];
  loading = true;
  private pendingRequests = 0;

  constructor(
    private readonly userService: UserService,
    private readonly insuranceService: InsuranceService
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.loading = true;
    this.pendingRequests = 2;

    this.userService.getAllUsers().subscribe({
      next: (users) => {
        this.users = users;
      },
      error: () => {},
      complete: () => this.markRequestDone()
    });

    this.insuranceService.getAllClaims().subscribe({
      next: (claims) => {
        this.claims = claims;
      },
      error: () => {},
      complete: () => this.markRequestDone()
    });
  }

  private markRequestDone(): void {
    this.pendingRequests -= 1;
    if (this.pendingRequests <= 0) {
      this.loading = false;
    }
  }

  // ─── User Stats ──────────────────────────────
  get totalUsers(): number { return this.users.length; }
  get activeUsers(): number { return this.users.filter(u => u.isActive).length; }
  get doctorCount(): number { return this.users.filter(u => u.role === 'DOCTOR').length; }
  get patientCount(): number { return this.users.filter(u => u.role === 'PATIENT').length; }

  // ─── Insurance Stats ─────────────────────────
  get totalClaims(): number { return this.claims.length; }
  get approvedClaims(): number { return this.claims.filter(c => c.status === 'APPROVED').length; }
  get pendingClaims(): number { return this.claims.filter(c => c.status === 'PENDING').length; }
  get totalReimbursement(): number { return this.claims.filter(c => c.status === 'APPROVED').reduce((sum, c) => sum + (c.reimbursementAmount || 0), 0); }

  get recentClaims(): InsuranceClaimResponse[] {
    return this.claims.slice(0, 5);
  }
}
