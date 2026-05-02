import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { MonitoringService } from '../../../../core/services/monitoring.service';
import { AuthService } from '../../../../core/services/auth.service';
import { CrisisAlertService } from '../../../../core/services/crisis-alert.service';
import {
  CrisisAlertPayload,
  DoctorRealtimeNotification,
  EmotionalTriggerRequest,
  EmotionalTriggerResponse,
  MoodEntryResponse
} from '../../../../shared/models/mood.model';

/** Sidebar row for doctor view — unique patients derived from mood entries. */
export interface DoctorPatientRow {
  id: number;
  firstName: string;
  lastName: string;
  /** From user_profiles.avatar (same as entry.patientAvatarUrl). */
  avatarUrl?: string;
  entryCount: number;
  hasCrisis: boolean;
}

@Component({
  selector: 'app-mood-list',
  templateUrl: './mood-list.component.html',
  styleUrls: ['./mood-list.component.scss']
})
export class MoodListComponent implements OnInit, OnDestroy {
  moodEntries: MoodEntryResponse[] = [];
  /** Doctor view: unique patients for sidebar (built from moodEntries). */
  patients: DoctorPatientRow[] = [];
  /** Doctor view: selected sidebar patient (null until user picks one). */
  selectedPatient: DoctorPatientRow | null = null;
  /** Doctor view: sidebar filter. */
  searchQuery = '';
  triggerMap: Record<number, EmotionalTriggerResponse[]> = {};
  triggerPanelOpen: Record<number, boolean> = {};
  triggerLoading: Record<number, boolean> = {};
  triggerSaving: Record<number, boolean> = {};
  triggerError: Record<number, string> = {};
  triggerForm: Record<number, EmotionalTriggerRequest> = {};
  editTriggerId: Record<number, number | null> = {};
  loading = true;
  errorMessage = '';
  emptyState = false;
  isDoctorView = false;
  exportingRecord = false;
  toastAlert: CrisisAlertPayload | null = null;

  private toastTimer: ReturnType<typeof setTimeout> | null = null;
  private alertSubscription: Subscription | null = null;

  // Mood score color mapping
  moodColors: { [key: number]: string } = {
    1: '#e74c3c',  // Red - very bad
    2: '#e67e22',  // Orange - bad
    3: '#f39c12',  // Yellow-orange - poor
    4: '#f1c40f',  // Yellow - below average
    5: '#f4d03f',  // Light yellow - average
    6: '#a3e048',  // Light green - good
    7: '#2ecc71',  // Green - very good
    8: '#1abc9c',  // Teal - excellent
    9: '#3498db',  // Blue - very excellent
    10: '#9b59b6'  // Purple - perfect
  };

  moodLabels: { [key: number]: string } = {
    1: 'Very Bad',
    2: 'Bad',
    3: 'Poor',
    4: 'Below Average',
    5: 'Average',
    6: 'Good',
    7: 'Very Good',
    8: 'Excellent',
    9: 'Very Excellent',
    10: 'Perfect'
  };

  private readonly highRiskTextPatterns: RegExp[] = [
    /\bkill\s+myself\b/i,
    /\bend\s+my\s+life\b/i,
    /\bwant\s+to\s+die\b/i,
    /\bsuicid(?:e|al)\b/i,
    /\bself\s*harm\b/i
  ];

  private readonly mediumRiskTextPatterns: RegExp[] = [
    /\b(feel(?:ing)?\s+)?tired\b/i,
    /\bno\s+energy\b/i,
    /\bexhausted\b/i,
    /\bdon[' ]?t\s+want\s+to\s+do\s+(?:anything|nothing)\b/i,
    /\bhopeless\b/i,
    /\bvery\s+sad\b/i,
    /\bstress\b/i,
    /\bbullying\b/i,
    /\bfight\b/i,
    /\btrauma\b/i
  ];

  readonly triggerTypes: string[] = [
    'WORK_STRESS',
    'SLEEP_DEPRIVATION',
    'FAMILY_CONFLICT',
    'SOCIAL_ISOLATION',
    'TRAUMA',
    'FINANCIAL_STRESS',
    'COGNITIVE_OVERLOAD',
    'OTHER'
  ];

  constructor(
    private readonly monitoringService: MonitoringService,
    private readonly authService: AuthService,
    private readonly crisisAlertService: CrisisAlertService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    const currentUser = this.authService.getCurrentUser();
    if (this.authService.isDoctor() && currentUser?.userId) {
      this.alertSubscription = this.crisisAlertService.newAlert$.subscribe((alert) => {
        if (this.isCrisisNotification(alert)) {
          this.showCrisisToast({
            doctorId: alert.doctorId,
            patientId: alert.patientId!,
            patientFullName: alert.patientFullName || 'Patient',
            moodLevel: alert.moodLevel!,
            message: alert.message,
            timestamp: alert.timestamp
          });
        }
      });
    }

    this.loadMoodEntries();
  }

  ngOnDestroy(): void {
    this.alertSubscription?.unsubscribe();
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
      this.toastTimer = null;
    }
  }

  loadMoodEntries(): void {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.userId) {
      this.errorMessage = 'User not logged in or userId not available';
      this.loading = false;
      console.error('Current user:', currentUser);
      return;
    }

    this.isDoctorView = this.authService.isDoctor();
    const request$ = this.isDoctorView
      ? this.monitoringService.getMoodEntriesForDoctor(currentUser.userId)
      : this.monitoringService.getMoodEntries(currentUser.userId);

    request$.subscribe({
      next: (entries) => {
        this.moodEntries = entries;
        this.emptyState = entries.length === 0;
        this.loading = false;
        if (this.isDoctorView) {
          this.buildPatientList();
          // Pre-select first patient so the two-panel layout is obviously different from the old flat grid.
          this.selectedPatient = this.patients.length > 0 ? this.patients[0] : null;
        } else {
          this.patients = [];
          this.selectedPatient = null;
          this.searchQuery = '';
        }
      },
      error: (err) => {
        console.error('Error loading mood entries:', err);
        this.errorMessage = err.error?.message || err.message || 'Failed to load mood entries';
        this.loading = false;
      }
    });
  }

  getMoodColor(score: number): string {
    return this.moodColors[score] || '#95a5a6';
  }

  getMoodLabel(score: number): string {
    return this.moodLabels[score] || 'Unknown';
  }

  getMoodEmoji(score: number): string {
    if (score <= 2) return '😢';
    if (score <= 4) return '😕';
    if (score <= 6) return '😐';
    if (score <= 8) return '🙂';
    return '😄';
  }

  getEntryRiskLevel(entry: MoodEntryResponse): 'HIGH_RISK' | 'MEDIUM_RISK' | 'LOW_RISK' {
    const level = (entry.aiRiskLevel || '').trim().toUpperCase();
    if (level === 'HIGH_RISK' || level === 'MEDIUM_RISK' || level === 'LOW_RISK') {
      return level;
    }

    const text = `${entry.moodDescription || ''} ${entry.triggers || ''}`.trim();
    if (this.matchesAny(text, this.highRiskTextPatterns)) {
      return 'HIGH_RISK';
    }
    if (this.matchesAny(text, this.mediumRiskTextPatterns)) {
      return 'MEDIUM_RISK';
    }

    // Fallback keeps doctor UX informative if AI service is temporarily unavailable.
    if (entry.moodScore <= 3) return 'HIGH_RISK';
    if (entry.moodScore <= 6) return 'MEDIUM_RISK';
    return 'LOW_RISK';
  }

  getEntryRiskLabel(entry: MoodEntryResponse): string {
    const level = this.getEntryRiskLevel(entry);
    if (level === 'HIGH_RISK') return 'High risk';
    if (level === 'MEDIUM_RISK') return 'Medium risk';
    return 'Low risk';
  }

  getEntryRiskClass(entry: MoodEntryResponse): string {
    const level = this.getEntryRiskLevel(entry);
    if (level === 'HIGH_RISK') return 'risk-chip--high';
    if (level === 'MEDIUM_RISK') return 'risk-chip--medium';
    return 'risk-chip--low';
  }

  getEntryRiskConfidence(entry: MoodEntryResponse): string {
    const confidence = entry.aiRiskConfidence;
    if (typeof confidence !== 'number' || Number.isNaN(confidence)) {
      const hasAiLabel = !!(entry.aiRiskLevel && entry.aiRiskLevel.trim());
      if (!hasAiLabel) {
        const fallbackLevel = this.getEntryRiskLevel(entry);
        if (fallbackLevel === 'HIGH_RISK' || fallbackLevel === 'MEDIUM_RISK') {
          return 'LOCAL';
        }
      }
      return 'N/A';
    }
    return `${Math.round(confidence * 100)}%`;
  }

  getEntryRiskTypeLabel(entry: MoodEntryResponse): string | null {
    const level = this.getEntryRiskLevel(entry);
    if (level === 'LOW_RISK') {
      return 'Normal';
    }

    const raw = (entry.aiRiskType || entry.aiMediumRiskType || '').trim();
    if (!raw) {
      return null;
    }

    const mappedStatus = this.mapSubtypeToStatus(raw);
    if (mappedStatus) {
      return mappedStatus;
    }

    return raw
      .split('_')
      .map((part) => part.charAt(0) + part.slice(1).toLowerCase())
      .join(' ');
  }

  private mapSubtypeToStatus(subtype: string): string | null {
    const normalized = subtype.trim().toUpperCase();
    if (!normalized) {
      return null;
    }

    const map: Record<string, string> = {
      SUICIDAL_CRISIS: 'Suicidal',
      BIPOLAR_EPISODE: 'Bipolar',
      ANXIETY_DISTRESS: 'Anxiety',
      DEPRESSIVE_MOOD: 'Depression',
      STRESS_OVERLOAD: 'Stress',
      PERSONALITY_DYSREGULATION: 'Personality disorder'
    };

    return map[normalized] || null;
  }

  private matchesAny(text: string, patterns: RegExp[]): boolean {
    if (!text) {
      return false;
    }
    return patterns.some((pattern) => pattern.test(text));
  }

  createNewEntry(): void {
    if (this.isDoctorView) {
      return;
    }
    this.router.navigate(['/monitoring/new']);
  }

  editEntry(id: number): void {
    if (this.isDoctorView) {
      return;
    }
    this.router.navigate(['/monitoring/edit', id]);
  }

  deleteEntry(id: number): void {
    if (this.isDoctorView) {
      return;
    }
    if (confirm('Are you sure you want to delete this mood entry?')) {
      this.monitoringService.deleteMoodEntry(id).subscribe({
        next: () => {
          this.moodEntries = this.moodEntries.filter(entry => entry.id !== id);
          this.emptyState = this.moodEntries.length === 0;
          if (this.isDoctorView) {
            this.buildPatientList();
            this.syncDoctorSelectionAfterDataChange();
          }
        },
        error: (err) => {
          this.errorMessage = this.resolveDeleteMoodError(err);
        }
      });
    }
  }

  formatDate(date: string): string {
    const d = new Date(date);
    return d.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getHeaderTitle(): string {
    return this.isDoctorView ? 'Patient Mood Monitoring' : 'Mood Tracking';
  }

  getHeaderSubtitle(): string {
    return this.isDoctorView
      ? 'Track mood entries from patients assigned to you'
      : 'Monitor your emotional wellness over time';
  }

  getPatientDisplayName(entry: MoodEntryResponse): string {
    return entry.patientName?.trim() || 'Unknown Patient';
  }

  getDoctorDisplayName(entry: MoodEntryResponse): string {
    return entry.doctorName?.trim() || 'Not assigned yet';
  }

  /**
   * Builds unique patient rows for the doctor sidebar from loaded mood entries.
   */
  buildPatientList(): void {
    const map = new Map<number, DoctorPatientRow>();
    for (const e of this.moodEntries) {
      if (!map.has(e.patientId)) {
        const { firstName, lastName } = this.splitPatientName(e.patientName);
        map.set(e.patientId, {
          id: e.patientId,
          firstName,
          lastName,
          avatarUrl: e.patientAvatarUrl?.trim() || undefined,
          entryCount: 0,
          hasCrisis: false
        });
      }
      const p = map.get(e.patientId)!;
      p.entryCount++;
      if (e.moodScore <= 3) {
        p.hasCrisis = true;
      }
      if (e.patientAvatarUrl?.trim()) {
        p.avatarUrl = e.patientAvatarUrl.trim();
      }
    }
    this.patients = Array.from(map.values()).sort((a, b) =>
      `${a.firstName} ${a.lastName}`.localeCompare(`${b.firstName} ${b.lastName}`, undefined, {
        sensitivity: 'base'
      })
    );
  }

  get filteredPatients(): DoctorPatientRow[] {
    if (!this.searchQuery?.trim()) {
      return this.patients;
    }
    const q = this.searchQuery.toLowerCase().trim();
    return this.patients.filter((p) =>
      `${p.firstName} ${p.lastName}`.toLowerCase().includes(q)
    );
  }

  selectPatient(patient: DoctorPatientRow): void {
    this.selectedPatient = patient;
  }

  exportSelectedPatientRecord(): void {
    if (!this.isDoctorView || !this.selectedPatient || this.exportingRecord) {
      return;
    }

    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.userId) {
      this.errorMessage = 'Doctor account not found. Please log in again.';
      return;
    }

    this.exportingRecord = true;
    this.errorMessage = '';

    this.monitoringService.exportPatientRecordPdf(currentUser.userId, this.selectedPatient.id).subscribe({
      next: (pdfBlob) => {
        const fullName = `${this.selectedPatient?.firstName || 'patient'}-${this.selectedPatient?.lastName || ''}`
          .trim()
          .replace(/\s+/g, '-');
        const filename = `mental-health-record-${fullName}-${Date.now()}.pdf`;

        const objectUrl = URL.createObjectURL(pdfBlob);
        const anchor = document.createElement('a');
        anchor.href = objectUrl;
        anchor.download = filename;
        document.body.appendChild(anchor);
        anchor.click();
        document.body.removeChild(anchor);
        URL.revokeObjectURL(objectUrl);

        this.exportingRecord = false;
      },
      error: (err) => {
        this.exportingRecord = false;
        this.errorMessage = err?.error?.message || err?.message || 'Failed to export patient record PDF';
      }
    });
  }

  openDoctorDashboard(): void {
    if (!this.isDoctorView) {
      return;
    }

    this.router.navigate(['/monitoring/dashboard'], {
      queryParams: this.selectedPatient ? { patientId: this.selectedPatient.id } : undefined
    });
  }

  get selectedPatientEntries(): MoodEntryResponse[] {
    if (!this.selectedPatient) {
      return [];
    }
    return this.moodEntries.filter((e) => e.patientId === this.selectedPatient!.id);
  }

  get averageMood(): number {
    const list = this.selectedPatientEntries;
    if (!list.length) {
      return 0;
    }
    const sum = list.reduce((acc, e) => acc + e.moodScore, 0);
    return Math.round(sum / list.length);
  }

  private splitPatientName(patientName?: string): { firstName: string; lastName: string } {
    const raw = patientName?.trim() || 'Unknown Patient';
    const parts = raw.split(/\s+/).filter(Boolean);
    if (parts.length === 0) {
      return { firstName: 'Unknown', lastName: 'Patient' };
    }
    if (parts.length === 1) {
      return { firstName: parts[0], lastName: '' };
    }
    return { firstName: parts[0], lastName: parts.slice(1).join(' ') };
  }

  private syncDoctorSelectionAfterDataChange(): void {
    if (!this.isDoctorView) {
      return;
    }
    if (!this.selectedPatient) {
      return;
    }
    const still = this.patients.find((p) => p.id === this.selectedPatient!.id);
    if (!still) {
      this.selectedPatient = this.patients.length > 0 ? this.patients[0] : null;
    } else {
      this.selectedPatient = still;
    }
  }

  toggleTriggerPanel(moodEntryId: number): void {
    const isOpen = this.triggerPanelOpen[moodEntryId];
    this.triggerPanelOpen[moodEntryId] = !isOpen;

    if (!isOpen) {
      this.loadTriggers(moodEntryId);
      this.resetTriggerForm(moodEntryId);
    }
  }

  loadTriggers(moodEntryId: number): void {
    this.triggerLoading[moodEntryId] = true;
    this.triggerError[moodEntryId] = '';

    this.monitoringService.getTriggersByMoodEntryId(moodEntryId).subscribe({
      next: (triggers) => {
        this.triggerMap[moodEntryId] = triggers || [];
        this.triggerLoading[moodEntryId] = false;
      },
      error: (err) => {
        this.triggerError[moodEntryId] = err.error?.message || 'Failed to load clinical triggers';
        this.triggerLoading[moodEntryId] = false;
      }
    });
  }

  submitTrigger(moodEntryId: number): void {
    const form = this.triggerForm[moodEntryId];
    if (!form || !form.triggerType || !form.description || !form.intensity) {
      this.triggerError[moodEntryId] = 'Please fill all trigger fields correctly.';
      return;
    }
    if (form.description.trim().length < 10) {
      this.triggerError[moodEntryId] = 'Description must be at least 10 characters.';
      return;
    }

    this.triggerSaving[moodEntryId] = true;
    this.triggerError[moodEntryId] = '';

    const request: EmotionalTriggerRequest = {
      moodEntryId,
      triggerType: form.triggerType,
      description: form.description.trim(),
      intensity: Number(form.intensity)
    };

    const currentEditId = this.editTriggerId[moodEntryId];
    const save$ = currentEditId
      ? this.monitoringService.updateTrigger(currentEditId, request)
      : this.monitoringService.createTrigger(moodEntryId, request);

    save$.subscribe({
      next: () => {
        this.triggerSaving[moodEntryId] = false;
        this.resetTriggerForm(moodEntryId);
        this.loadTriggers(moodEntryId);
      },
      error: (err) => {
        this.triggerSaving[moodEntryId] = false;
        this.triggerError[moodEntryId] = err.error?.message || 'Failed to save clinical trigger';
      }
    });
  }

  startEditTrigger(moodEntryId: number, trigger: EmotionalTriggerResponse): void {
    this.editTriggerId[moodEntryId] = trigger.id;
    this.triggerForm[moodEntryId] = {
      moodEntryId,
      triggerType: trigger.triggerType,
      description: trigger.description,
      intensity: trigger.intensity
    };
  }

  cancelEditTrigger(moodEntryId: number): void {
    this.resetTriggerForm(moodEntryId);
  }

  deleteTrigger(moodEntryId: number, triggerId: number): void {
    if (!confirm('Delete this clinical trigger?')) return;

    this.monitoringService.deleteTrigger(triggerId).subscribe({
      next: () => this.loadTriggers(moodEntryId),
      error: (err) => {
        this.triggerError[moodEntryId] = err.error?.message || 'Failed to delete trigger';
      }
    });
  }

  private resetTriggerForm(moodEntryId: number): void {
    this.editTriggerId[moodEntryId] = null;
    this.triggerForm[moodEntryId] = {
      moodEntryId,
      triggerType: '',
      description: '',
      intensity: 5
    };
  }

  dismissToast(): void {
    this.toastAlert = null;
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
      this.toastTimer = null;
    }
  }

  private showCrisisToast(alert: CrisisAlertPayload): void {
    this.toastAlert = alert;
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
    }
    this.toastTimer = setTimeout(() => {
      this.toastAlert = null;
      this.toastTimer = null;
    }, 8000);
  }

  private isCrisisNotification(alert: DoctorRealtimeNotification): boolean {
    return alert.type === 'CRISIS'
      && typeof alert.patientId === 'number'
      && typeof alert.moodLevel === 'number';
  }

  private resolveDeleteMoodError(err: any): string {
    const backendMessage = (err?.error?.message || err?.message || '').toString();
    const normalized = backendMessage.toLowerCase();

    if (normalized.includes('clinical') || normalized.includes('trigger') || normalized.includes('related records')) {
      return 'You cannot delete a mood entry that has a clinical record.';
    }

    // Keep UX stable even if backend still returns generic security text.
    if (normalized.includes('full authentication is required')) {
      return 'You cannot delete a mood entry that has a clinical record.';
    }

    return backendMessage || 'Failed to delete mood entry';
  }
}


