import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { InsuranceService } from '../../../core/services/insurance.service';
import { InsuranceNotification } from '../../../shared/models/insurance.model';

@Component({
  selector: 'app-ocr-notifications',
  templateUrl: './ocr-notifications.component.html',
  styleUrls: ['./ocr-notifications.component.scss']
})
export class OcrNotificationsComponent implements OnInit {
  loading = true;
  isAdmin = false;
  errorMessage = '';
  notifications: InsuranceNotification[] = [];

  constructor(
    private readonly authService: AuthService,
    private readonly insuranceService: InsuranceService
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.authService.isAdmin();
    if (!this.isAdmin) {
      this.loading = false;
      this.errorMessage = 'Only admins can view OCR notifications.';
      return;
    }
    this.loadNotifications();
  }

  get ocrNotifications(): InsuranceNotification[] {
    return this.notifications.filter((n) =>
      n.type === 'OCR_MINOR_MISMATCH' || n.type === 'OCR_MAJOR_BLOCKED'
    );
  }

  get majorCount(): number {
    return this.ocrNotifications.filter((n) => n.type === 'OCR_MAJOR_BLOCKED').length;
  }

  get minorCount(): number {
    return this.ocrNotifications.filter((n) => n.type === 'OCR_MINOR_MISMATCH').length;
  }

  loadNotifications(): void {
    this.loading = true;
    this.insuranceService.getAllNotificationsForAdmin().subscribe({
      next: (items) => {
        this.notifications = (items || []).slice().sort((a, b) =>
          new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        );
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to load OCR notifications';
        this.loading = false;
      }
    });
  }

  getTypeLabel(type: InsuranceNotification['type']): string {
    return type === 'OCR_MAJOR_BLOCKED' ? 'Major Mismatch' : 'Minor Mismatch';
  }

  getTypeClass(type: InsuranceNotification['type']): string {
    return type === 'OCR_MAJOR_BLOCKED' ? 'badge badge-danger' : 'badge badge-warning';
  }
}
