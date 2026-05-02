import { Component, OnDestroy, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { InsuranceService } from '../../../core/services/insurance.service';
import { AuthService } from '../../../core/services/auth.service';
import { ClaimRiskScoreResponse, InsuranceClaimResponse, InsuranceClaimTransition } from '../../../shared/models/insurance.model';
import { environment } from '../../../../environments/environment';
import { UserService } from '../../../core/services/user.service';
import { UserResponse } from '../../../shared/models/user.model';

@Component({
  selector: 'app-claim-detail',
  templateUrl: './claim-detail.component.html',
  styleUrls: ['./claim-detail.component.scss']
})
export class ClaimDetailComponent implements OnInit, OnDestroy {
  claim: InsuranceClaimResponse | null = null;
  timeline: InsuranceClaimTransition[] = [];
  loading = true;
  errorMessage = '';
  reimbursementAmount: number | null = null;
  isAdmin = false;
  isPatient = false;
  processing = false;
  editDescription = '';
  editAmount: number | null = null;
  editInsuranceGrade: number | null = null;
  responseMessage = '';
  responseFiles: File[] = [];
  responseFileError = '';
  currentUserDisplayName = '';
  riskLoading = false;
  riskError = '';
  riskResult: ClaimRiskScoreResponse | null = null;
  private routeSub?: Subscription;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly http: HttpClient,
    private readonly insuranceService: InsuranceService,
    private readonly authService: AuthService,
    private readonly userService: UserService
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.authService.isAdmin();
    this.isPatient = this.authService.isPatient();
    this.loadCurrentUserDisplayName();
    this.routeSub = this.route.paramMap.subscribe(params => {
      const id = Number(params.get('id'));
      if (id) {
        this.loadClaim(id);
        this.loadTimeline(id);
      }
    });
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
  }

  loadClaim(id: number): void {
    this.loading = true;
    this.errorMessage = '';
    this.insuranceService.getClaimById(id).subscribe({
      next: (claim) => {
        this.claim = claim;
        this.reimbursementAmount = claim.amount;
        this.editDescription = claim.description || '';
        this.editAmount = claim.amount;
        this.editInsuranceGrade = claim.insuranceGrade;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load claim';
        this.loading = false;
      }
    });
  }

  loadTimeline(id: number): void {
    this.insuranceService.getClaimTimeline(id).subscribe({
      next: (rows) => {
        this.timeline = rows || [];
      },
      error: () => {
        this.timeline = [];
      }
    });
  }

  fetchRiskScore(): void {
    if (!this.isAdmin || !this.claim) return;
    this.riskLoading = true;
    this.riskError = '';
    this.riskResult = null;
    this.insuranceService.getClaimRiskScore(this.claim.id).subscribe({
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

  getRiskBandClass(band?: string | null): string {
    switch ((band || '').toUpperCase()) {
      case 'HIGH': return 'risk-badge risk-high';
      case 'MEDIUM': return 'risk-badge risk-medium';
      case 'LOW': return 'risk-badge risk-low';
      default: return 'risk-badge';
    }
  }

  approveClaim(): void {
    if (!this.claim || !this.reimbursementAmount) return;
    this.processing = true;
    this.insuranceService.approveClaim(this.claim.id, this.reimbursementAmount).subscribe({
      next: (updated) => {
        this.claim = updated;
        this.processing = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to approve claim';
        this.processing = false;
      }
    });
  }

  rejectClaim(): void {
    if (!this.claim) return;
    this.processing = true;
    this.insuranceService.rejectClaim(this.claim.id).subscribe({
      next: (updated) => {
        this.claim = updated;
        this.processing = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to reject claim';
        this.processing = false;
      }
    });
  }

  onResponseFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files || []);
    this.responseFileError = '';
    this.responseFiles = files;
  }

  submitAdditionalDocuments(): void {
    if (!this.claim || !this.isPatient) return;
    const description = this.editDescription.trim();
    const amount = this.editAmount;
    const insuranceGrade = this.editInsuranceGrade;

    if (!description || description.length < 10) {
      this.responseFileError = 'Description must be at least 10 characters.';
      return;
    }
    if (amount == null || amount <= 0) {
      this.responseFileError = 'Please enter a valid amount.';
      return;
    }
    if (insuranceGrade == null || insuranceGrade < 1 || insuranceGrade > 5) {
      this.responseFileError = 'Insurance grade must be between 1 and 5.';
      return;
    }

    const hasFieldChanges =
      description !== (this.claim.description || '')
      || Number(amount) !== Number(this.claim.amount)
      || Number(insuranceGrade) !== Number(this.claim.insuranceGrade);
    const hasMessage = !!this.responseMessage.trim();
    const hasFiles = this.responseFiles.length > 0;
    if (!hasFieldChanges && !hasMessage && !hasFiles) {
      this.responseFileError = 'Please edit claim data, add a message, or attach at least one file.';
      return;
    }
    this.processing = true;
    this.errorMessage = '';
    this.responseFileError = '';
    this.insuranceService.submitAdditionalDocuments(
      this.claim.id,
      {
        message: this.responseMessage,
        description,
        amount: Number(amount),
        insuranceGrade: Number(insuranceGrade)
      },
      this.responseFiles
    ).subscribe({
      next: (updated) => {
        this.claim = updated;
        this.processing = false;
        this.responseFiles = [];
        this.responseMessage = '';
        this.editDescription = updated.description;
        this.editAmount = updated.amount;
        this.editInsuranceGrade = updated.insuranceGrade;
        this.loadTimeline(updated.id);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to submit additional documents';
        this.processing = false;
      }
    });
  }

  deleteClaim(): void {
    if (!this.claim) return;
    this.processing = true;
    this.insuranceService.deleteClaim(this.claim.id).subscribe({
      next: () => {
        this.goBack();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to delete claim';
        this.processing = false;
      }
    });
  }

  getFileUrl(path: string): string {
    return `${environment.insuranceApiUrl}/files/open?path=${encodeURIComponent(path)}`;
  }

  getFileName(path: string): string {
    const parts = path.split('/');
    const full = parts[parts.length - 1];
    const underscoreIdx = full.indexOf('_');
    return underscoreIdx > -1 ? full.substring(underscoreIdx + 1) : full;
  }

  openFile(path: string, event: Event): void {
    event.preventDefault();
    const url = this.getFileUrl(path);
    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const objectUrl = window.URL.createObjectURL(blob);
        window.open(objectUrl, '_blank');
        setTimeout(() => window.URL.revokeObjectURL(objectUrl), 60_000);
      },
      error: () => {
        this.errorMessage = 'Failed to open attachment';
      }
    });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'APPROVED': return 'badge badge-success';
      case 'PAID': return 'badge badge-success';
      case 'REJECTED': return 'badge badge-danger';
      case 'NEEDS_INFO': return 'badge badge-warning';
      case 'PARTIALLY_APPROVED': return 'badge badge-primary';
      case 'UNDER_REVIEW': return 'badge badge-primary';
      case 'SUBMITTED': return 'badge badge-primary';
      default: return 'badge badge-primary';
    }
  }

  canPatientSubmitDocs(): boolean {
    return !!this.claim && this.isPatient && this.claim.status === 'NEEDS_INFO';
  }

  getTimelineActorDisplayName(transition: InsuranceClaimTransition): string {
    const role = (transition.changedByRole || '').toUpperCase();
    if (role === 'SYSTEM') {
      return 'Insurance Company Portal';
    }
    if (role === 'ADMIN') {
      return 'Insurance Admin';
    }
    if (role === 'USER') {
      const actorId = transition.changedByUserId;
      const claimOwnerId = this.claim?.userId;
      if (claimOwnerId != null && actorId === claimOwnerId) {
        const ownerName = (this.claim?.userFullName || '').trim();
        if (ownerName) return ownerName;
      }
      const meId = this.authService.getCurrentUser()?.userId;
      if (meId != null && actorId === meId && this.currentUserDisplayName) {
        return this.currentUserDisplayName;
      }
      return `User #${actorId}`;
    }
    return role || 'Unknown';
  }

  private loadCurrentUserDisplayName(): void {
    this.userService.getCurrentUser().subscribe({
      next: (user: UserResponse) => {
        const first = (user.firstName || '').trim();
        const last = (user.lastName || '').trim();
        this.currentUserDisplayName = `${first} ${last}`.trim() || user.email || `User #${user.id}`;
      },
      error: () => {
        this.currentUserDisplayName = '';
      }
    });
  }

  goBack(): void {
    if (this.isAdmin) {
      this.router.navigate(['/admin/insurance']);
    } else {
      this.router.navigate(['/insurance']);
    }
  }
}
