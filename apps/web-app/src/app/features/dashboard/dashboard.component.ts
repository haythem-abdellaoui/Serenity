import { Component, OnDestroy, OnInit } from '@angular/core';
import { AppointmentService } from '../../core/services/appointment.service';
import { AuthService } from '../../core/services/auth.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { PharmacyService } from '../../core/services/pharmacy.service';
import { UserService } from '../../core/services/user.service';
import { AppointmentResponse, appointmentParticipantDisplayName } from '../../shared/models/appointment.model';
import { UserResponse } from '../../shared/models/user.model';
import { StockItemResponse } from '../../shared/models/pharmacy.model';

type DashboardRole = 'PATIENT' | 'DOCTOR' | 'PHARMACIST';

interface DashboardPerson {
  name: string;
  role: string;
  initials: string;
}

interface DashboardTimelineRow {
  title: string;
  when: string;
}

interface DashboardListRow {
  name: string;
  schedule: string;
}

interface DashboardMetric {
  label: string;
  value: number;
  unit: string;
  alt?: boolean;
}

interface DashboardPreset {
  greeting: string;
  subGreeting: string;
  focusTitle: string;
  focusSubtitle: string;
  focusTask: string;
  focusChecklist: string;
  focusNote: string;
  metricsTitle: string;
  metricRows: [DashboardMetric, DashboardMetric];
  circleTitle: string;
  circleRows: DashboardPerson[];
  contactLabel: string;
  timelineTitle: string;
  timelineSubtitle: string;
  timelineRows: DashboardTimelineRow[];
  listTitle: string;
  listSubtitle: string;
  listRows: DashboardListRow[];
}

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  private user: UserResponse | null = null;
  loading = true;
  private dynamicPreset: DashboardPreset | null = null;
  readonly patientPreset: DashboardPreset = {
    greeting: 'Let’s personalize your care journey.',
    subGreeting: 'Daily mindfulness',
    focusTitle: 'Daily Focus',
    focusSubtitle: 'Daily mindfulness',
    focusTask: "Today’s Task: Deep Breathing (10 min)",
    focusChecklist: 'Guided Meditation',
    focusNote: 'Add a note for your current mood and comfort level.',
    metricsTitle: 'My Vitals',
    metricRows: [
      { label: 'Heart Rate (last 24h)', value: 72, unit: 'bpm' },
      { label: 'Sleep Quality', value: 8.5, unit: 'hrs', alt: true }
    ],
    circleTitle: 'Care Circle',
    circleRows: [
      { name: 'Dr. Elara Vance', role: 'Primary', initials: 'EV' },
      { name: 'Nurse Chen', role: 'Coordinator', initials: 'NC' }
    ],
    contactLabel: 'Contact Care Circle',
    timelineTitle: 'Upcoming Appointments',
    timelineSubtitle: 'Next appts',
    timelineRows: [
      { title: 'First Session - Dr. X', when: 'April 2, 10:00 AM' },
      { title: 'Follow-up - Dr. X', when: 'April 12, 7:00 AM' },
      { title: 'Next Checkup - Dr. X', when: 'April 22, 8:00 AM' }
    ],
    listTitle: 'Current Medications',
    listSubtitle: 'Prescribed meds',
    listRows: [
      { name: 'Xanax 5mg', schedule: 'Daily' },
      { name: 'Advil 10mg', schedule: 'Morning' },
      { name: 'Ibuprofen 10mg', schedule: 'Daily' },
      { name: 'Naproxen 10mg', schedule: 'Morning' }
    ]
  };

  readonly doctorPreset: DashboardPreset = {
    greeting: 'Here is your clinical snapshot for today.',
    subGreeting: 'Clinical priorities',
    focusTitle: 'Daily Focus',
    focusSubtitle: 'Clinical priorities',
    focusTask: "Today’s Task: Review all high-risk patients before 12:00 PM",
    focusChecklist: 'Check crisis alerts and pending follow-ups',
    focusNote: 'Capture short care notes to keep the team aligned.',
    metricsTitle: 'Doctor Metrics',
    metricRows: [
      { label: 'Patients Today', value: 14, unit: 'pts' },
      { label: 'Avg Consultation Time', value: 28, unit: 'min', alt: true }
    ],
    circleTitle: 'Care Team',
    circleRows: [
      { name: 'Nurse Amira', role: 'Coordinator', initials: 'NA' },
      { name: 'Pharm. Youssef', role: 'Pharmacy', initials: 'PY' }
    ],
    contactLabel: 'Contact Team',
    timelineTitle: 'Upcoming Consultations',
    timelineSubtitle: 'Today and next sessions',
    timelineRows: [
      { title: 'Patient Follow-up - Sara L.', when: '10:30 AM' },
      { title: 'Initial Assessment - Adam H.', when: '1:00 PM' },
      { title: 'Medication Review - Lina R.', when: '4:15 PM' }
    ],
    listTitle: 'Patients Requiring Attention',
    listSubtitle: 'Priority watchlist',
    listRows: [
      { name: 'Sara L.', schedule: 'Mood dip trend - check today' },
      { name: 'Nour B.', schedule: 'Missed last appointment' },
      { name: 'Adam H.', schedule: 'First consultation pending notes' },
      { name: 'Lina R.', schedule: 'Medication adjustment review' }
    ]
  };

  readonly pharmacistPreset: DashboardPreset = {
    greeting: 'Keep prescriptions flowing and stock ready.',
    subGreeting: 'Pharmacy operations',
    focusTitle: 'Daily Focus',
    focusSubtitle: 'Pharmacy operations',
    focusTask: "Today’s Task: Clear pending prescriptions in the first hour",
    focusChecklist: 'Prioritize low-stock medicine requests',
    focusNote: 'Log substitutions early to avoid pickup delays.',
    metricsTitle: 'Pharmacy Metrics',
    metricRows: [
      { label: 'Pending Prescriptions', value: 18, unit: 'rx' },
      { label: 'Low Stock Medicines', value: 6, unit: 'items', alt: true }
    ],
    circleTitle: 'Pharmacy Team',
    circleRows: [
      { name: 'Dr. Maha S.', role: 'Prescriber', initials: 'MS' },
      { name: 'Ops Karim', role: 'Inventory', initials: 'OK' }
    ],
    contactLabel: 'Contact Prescribers',
    timelineTitle: 'Prescription Queue',
    timelineSubtitle: 'Next workflow tasks',
    timelineRows: [
      { title: 'Ready for pickup confirmations', when: 'Within 30 min' },
      { title: 'Pending validation batch', when: 'Before 2:00 PM' },
      { title: 'Restock critical SKUs', when: 'By end of day' }
    ],
    listTitle: 'Inventory Attention List',
    listSubtitle: 'Critical stock levels',
    listRows: [
      { name: 'Sertraline 50mg', schedule: 'Only 4 units left' },
      { name: 'Melatonin 3mg', schedule: 'Only 3 units left' },
      { name: 'Omega-3 Softgel', schedule: 'Only 5 units left' },
      { name: 'Vitamin D 1000 IU', schedule: 'Only 2 units left' }
    ]
  };

  constructor(
    public readonly authService: AuthService,
    private readonly userService: UserService,
    private readonly dashboardService: DashboardService,
    private readonly appointmentService: AppointmentService,
    private readonly pharmacyService: PharmacyService
  ) {}

  // ── Sticky notes (Daily Focus) ───────────────────────────────────────────────
  private readonly stickyPrefix = 'serenity:dashboard:sticky';
  stickyNotes: string[] = [''];
  stickyColors: string[] = ['yellow'];
  stickyIndex = 0;
  private stickyResetTimer: number | null = null;
  stickySavedPulse = false;
  private stickySavedPulseTimer: number | null = null;

  // Pinned sticky note (optional, per-day)
  pinnedSticky: { pageIndex: number; x: number; y: number; open: boolean } | null = null;
  private draggingPinned = false;
  private dragStart: { mouseX: number; mouseY: number; x: number; y: number } | null = null;

  ngOnInit(): void {
    this.loading = true;
    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        this.user = user;
        this.loadStickyNotes();
        this.startStickyResetWatcher();
        this.loadDynamicDashboard();
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  ngOnDestroy(): void {
    if (this.stickyResetTimer != null) {
      window.clearInterval(this.stickyResetTimer);
      this.stickyResetTimer = null;
    }
    if (this.stickySavedPulseTimer != null) {
      window.clearTimeout(this.stickySavedPulseTimer);
      this.stickySavedPulseTimer = null;
    }
    this.stopPinnedDrag();
  }

  getDisplayName(): string {
    if (this.user?.profile?.isAnonymous) return 'Anonymous';
    if (this.user?.firstName) return this.user.firstName;
    return (this.authService.getCurrentUser()?.email || '').split('@')[0];
  }

  get activePreset(): DashboardPreset {
    return this.dynamicPreset ?? this.basePresetForRole(this.currentRole);
  }

  get currentRole(): DashboardRole {
    if (this.authService.hasRole('DOCTOR')) return 'DOCTOR';
    if (this.authService.hasRole('PHARMACIST')) return 'PHARMACIST';
    return 'PATIENT';
  }

  onContactCareCircle(): void {
    // Hook up to messaging flow when available.
  }

  // ── Sticky notes helpers ────────────────────────────────────────────────────
  private stickyStorageKey(): string {
    const userId = this.user?.id ?? this.authService.getUserId() ?? 'anon';
    return `${this.stickyPrefix}:${userId}`;
  }

  private todayKey(d = new Date()): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  private loadStickyNotes(): void {
    const key = this.stickyStorageKey();
    try {
      const raw = localStorage.getItem(key);
      if (!raw) {
        this.resetStickyNotes();
        return;
      }
      const parsed = JSON.parse(raw) as {
        date?: string;
        pages?: unknown;
        colors?: unknown;
        index?: unknown;
        pinned?: unknown;
      };
      if (!parsed?.date || parsed.date !== this.todayKey()) {
        this.resetStickyNotes();
        return;
      }
      const pages = Array.isArray(parsed.pages) ? parsed.pages.filter((p) => typeof p === 'string') as string[] : [];
      this.stickyNotes = pages.length > 0 ? pages : [''];
      const colors = Array.isArray(parsed.colors) ? parsed.colors.filter((c) => typeof c === 'string') as string[] : [];
      this.stickyColors = (colors.length === this.stickyNotes.length)
        ? colors
        : this.stickyNotes.map(() => this.randomStickyColor());
      const idx = typeof parsed.index === 'number' ? parsed.index : 0;
      this.stickyIndex = Math.max(0, Math.min(idx, this.stickyNotes.length - 1));

      const p = parsed.pinned as any;
      if (p && typeof p === 'object' && p.open === true) {
        const pageIndex = typeof p.pageIndex === 'number' ? p.pageIndex : this.stickyIndex;
        this.pinnedSticky = {
          open: true,
          pageIndex: Math.max(0, Math.min(pageIndex, this.stickyNotes.length - 1)),
          x: typeof p.x === 'number' ? p.x : 24,
          y: typeof p.y === 'number' ? p.y : 120
        };
      } else {
        this.pinnedSticky = null;
      }
    } catch {
      this.resetStickyNotes();
    }
  }

  private readStickyPayloadRaw(): any | null {
    try {
      const raw = localStorage.getItem(this.stickyStorageKey());
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }

  private persistStickyNotes(opts: { pulse?: boolean; pinnedOverride?: any } = {}): void {
    const key = this.stickyStorageKey();

    // IMPORTANT: keep pinned state source-of-truth in storage.
    // Otherwise, if another component (Layout overlay) unpins, this component could
    // accidentally "re-pin" on the next save.
    const existing = this.readStickyPayloadRaw();
    const pinned =
      Object.prototype.hasOwnProperty.call(opts, 'pinnedOverride')
        ? opts.pinnedOverride
        : (existing?.pinned ?? null);
    this.pinnedSticky = pinned;

    const payload = JSON.stringify({
      date: this.todayKey(),
      pages: this.stickyNotes,
      colors: this.stickyColors,
      index: this.stickyIndex,
      pinned
    });
    try {
      localStorage.setItem(key, payload);
    } catch {
      // ignore (storage full / disabled)
    }

    if (opts.pulse) {
      // Visual confirmation (“Saved”) only for actual typing edits.
      this.stickySavedPulse = true;
      if (this.stickySavedPulseTimer != null) {
        window.clearTimeout(this.stickySavedPulseTimer);
      }
      this.stickySavedPulseTimer = window.setTimeout(() => {
        this.stickySavedPulse = false;
        this.stickySavedPulseTimer = null;
      }, 700);
    }
  }

  private resetStickyNotes(): void {
    this.stickyNotes = [''];
    this.stickyColors = [this.randomStickyColor()];
    this.stickyIndex = 0;
    this.pinnedSticky = null;
    this.persistStickyNotes();
  }

  private startStickyResetWatcher(): void {
    // Cheap “midnight reset”: check date change every minute while app is open.
    if (this.stickyResetTimer != null) return;
    const initial = this.todayKey();
    this.stickyResetTimer = window.setInterval(() => {
      if (this.todayKey() !== initial) {
        this.resetStickyNotes();
      }
    }, 60_000);
  }

  get stickyPageCount(): number {
    return this.stickyNotes.length;
  }

  get stickyCurrentText(): string {
    return this.stickyNotes[this.stickyIndex] ?? '';
  }

  onStickyTextChange(v: string): void {
    this.stickyNotes[this.stickyIndex] = v;
    this.persistStickyNotes({ pulse: true });
  }

  stickyPrev(): void {
    this.stickyIndex = Math.max(0, this.stickyIndex - 1);
    this.persistStickyNotes();
  }

  stickyNext(): void {
    this.stickyIndex = Math.min(this.stickyNotes.length - 1, this.stickyIndex + 1);
    this.persistStickyNotes();
  }

  stickyAddPage(): void {
    this.stickyNotes.push('');
    this.stickyColors.push(this.randomStickyColor());
    this.stickyIndex = this.stickyNotes.length - 1;
    this.persistStickyNotes();
  }

  stickyDeletePage(): void {
    if (this.stickyNotes.length <= 1) {
      this.resetStickyNotes();
      return;
    }
    this.stickyNotes.splice(this.stickyIndex, 1);
    this.stickyColors.splice(this.stickyIndex, 1);
    this.stickyIndex = Math.max(0, Math.min(this.stickyIndex, this.stickyNotes.length - 1));
    this.persistStickyNotes();
  }

  private randomStickyColor(): string {
    const palette = ['yellow', 'pink', 'green', 'blue', 'lavender', 'peach'] as const;
    return palette[Math.floor(Math.random() * palette.length)];
  }

  get stickyColor(): string {
    return this.stickyColors[this.stickyIndex] ?? 'yellow';
  }

  // ── Pin / Drag sticky ───────────────────────────────────────────────────────
  get canPinCurrent(): boolean {
    return this.pinnedSticky?.open === true && this.pinnedSticky.pageIndex === this.stickyIndex;
  }

  pinCurrentSticky(): void {
    // Pin the current page as a floating note.
    const pinned = {
      open: true,
      pageIndex: this.stickyIndex,
      x: (this.pinnedSticky as any)?.x ?? 24,
      y: (this.pinnedSticky as any)?.y ?? 120
    };
    this.pinnedSticky = pinned;
    this.persistStickyNotes({ pinnedOverride: pinned });
  }

  unpinSticky(): void {
    this.pinnedSticky = null;
    this.persistStickyNotes({ pinnedOverride: null });
  }

  get pinnedStyle(): { [k: string]: string } {
    const x = this.pinnedSticky?.x ?? 24;
    const y = this.pinnedSticky?.y ?? 120;
    return { left: `${x}px`, top: `${y}px` };
  }

  get pinnedText(): string {
    const idx = this.pinnedSticky?.pageIndex ?? 0;
    return this.stickyNotes[idx] ?? '';
  }

  get pinnedColor(): string {
    const idx = this.pinnedSticky?.pageIndex ?? 0;
    return this.stickyColors[idx] ?? 'yellow';
  }

  onPinnedTextChange(v: string): void {
    if (!this.pinnedSticky) return;
    const idx = this.pinnedSticky.pageIndex;
    this.stickyNotes[idx] = v;
    this.persistStickyNotes({ pulse: true });
  }

  onPinnedMouseDown(ev: MouseEvent): void {
    if (!this.pinnedSticky) return;
    this.draggingPinned = true;
    this.dragStart = {
      mouseX: ev.clientX,
      mouseY: ev.clientY,
      x: this.pinnedSticky.x,
      y: this.pinnedSticky.y
    };
    window.addEventListener('mousemove', this.onPinnedMouseMove);
    window.addEventListener('mouseup', this.onPinnedMouseUp);
    ev.preventDefault();
  }

  private onPinnedMouseMove = (ev: MouseEvent): void => {
    if (!this.draggingPinned || !this.dragStart || !this.pinnedSticky) return;
    const dx = ev.clientX - this.dragStart.mouseX;
    const dy = ev.clientY - this.dragStart.mouseY;
    this.pinnedSticky.x = Math.max(8, this.dragStart.x + dx);
    this.pinnedSticky.y = Math.max(8, this.dragStart.y + dy);
  };

  private onPinnedMouseUp = (): void => {
    if (this.draggingPinned) {
      this.draggingPinned = false;
      this.dragStart = null;
      this.persistStickyNotes();
    }
    this.stopPinnedDrag();
  };

  private stopPinnedDrag(): void {
    window.removeEventListener('mousemove', this.onPinnedMouseMove);
    window.removeEventListener('mouseup', this.onPinnedMouseUp);
  }

  private basePresetForRole(role: DashboardRole): DashboardPreset {
    switch (role) {
      case 'DOCTOR':
        return this.doctorPreset;
      case 'PHARMACIST':
        return this.pharmacistPreset;
      default:
        return this.patientPreset;
    }
  }

  private clonePreset(p: DashboardPreset): DashboardPreset {
    return {
      ...p,
      metricRows: [{ ...p.metricRows[0] }, { ...p.metricRows[1] }],
      circleRows: p.circleRows.map((x) => ({ ...x })),
      timelineRows: p.timelineRows.map((x) => ({ ...x })),
      listRows: p.listRows.map((x) => ({ ...x }))
    };
  }

  private loadDynamicDashboard(): void {
    const role = this.currentRole;
    const base = this.clonePreset(this.basePresetForRole(role));

    // Always attempt to make the timeline real using appointments.
    this.appointmentService.getMine().subscribe({
      next: (appts) => {
        const rows = (appts ?? [])
          .slice(0, 5)
          .map((a: AppointmentResponse) => {
            const otherParty =
              role === 'DOCTOR'
                ? appointmentParticipantDisplayName(a, 'patient')
                : appointmentParticipantDisplayName(a, 'doctor');
            const typeLabel = a.type === 'TELECONSULTATION' ? 'Teleconsultation' : 'In-person';
            return {
              title: `${typeLabel} - ${otherParty}`.trim(),
              when: `${a.appointmentDate} ${a.timeSlot}`.trim()
            };
          });
        if (rows.length > 0) {
          base.timelineRows = rows;
        }

        // Patient-only extras: real care circle + “upcoming” count.
        if (role === 'PATIENT') {
          base.metricRows[1] = { label: 'Upcoming Appointments', value: (appts ?? []).length, unit: 'appts', alt: true };

          const uniqueDoctors: { name: string; initials: string }[] = [];
          for (const a of appts ?? []) {
            const name = appointmentParticipantDisplayName(a, 'doctor');
            if (!name || name === '—') continue;
            if (uniqueDoctors.some((d) => d.name === name)) continue;
            const initials = name
              .split(' ')
              .filter(Boolean)
              .slice(0, 2)
              .map((p) => p[0]?.toUpperCase())
              .join('');
            uniqueDoctors.push({ name, initials: initials || 'DR' });
          }
          if (uniqueDoctors.length > 0) {
            base.circleRows = uniqueDoctors.slice(0, 3).map((d) => ({
              name: d.name,
              role: 'Doctor',
              initials: d.initials
            }));
          }
        }
      },
      error: () => {}
    });

    if (role === 'DOCTOR') {
      this.dashboardService.getStats().subscribe({
        next: (stats) => {
          base.metricRows[0] = { label: 'Active Records', value: stats.activeRecords ?? 0, unit: 'records' };
          base.metricRows[1] = { label: 'Active Prescriptions', value: stats.activePrescriptions ?? 0, unit: 'rx', alt: true };
          base.listRows = [
            { name: 'Severity LOW', schedule: String(stats.severityLow ?? 0) },
            { name: 'Severity MEDIUM', schedule: String(stats.severityMedium ?? 0) },
            { name: 'Severity HIGH', schedule: String(stats.severityHigh ?? 0) }
          ];
        },
        error: () => {},
        complete: () => {
          this.dynamicPreset = base;
          this.loading = false;
        }
      });
      return;
    }

    if (role === 'PHARMACIST') {
      // Keep inbox as first metric, but make the "inventory attention list" real (low stock).
      this.pharmacyService.getInbox().subscribe({
        next: (rows) => {
          base.metricRows[0] = { label: 'Inbox Prescriptions', value: (rows ?? []).length, unit: 'rx' };
        },
        error: () => {}
      });

      this.pharmacyService.listStock(undefined, false).subscribe({
        next: (stock) => {
          const rows = (stock ?? []) as StockItemResponse[];
          const lowStock = rows
            .filter((s) => (s.state === 'OUT_OF_STOCK') || (typeof s.quantity === 'number' && s.quantity <= 2))
            .sort((a, b) => (a.quantity ?? 0) - (b.quantity ?? 0));

          base.metricRows[1] = { label: 'Low Stock Medicines', value: lowStock.length, unit: 'items', alt: true };
          if (lowStock.length > 0) {
            base.listTitle = 'Inventory Attention List';
            base.listSubtitle = 'Low / out of stock';
            base.listRows = lowStock.slice(0, 6).map((s) => ({
              name: s.medicineName,
              schedule: s.state === 'OUT_OF_STOCK' ? 'Out of stock' : `Only ${s.quantity} units left`
            }));
          } else {
            base.listRows = [{ name: 'No low-stock items', schedule: 'All good' }];
          }
        },
        error: () => {
          base.metricRows[1] = { label: 'Low Stock Medicines', value: 0, unit: 'items', alt: true };
        },
        complete: () => {
          this.dynamicPreset = base;
          this.loading = false;
        }
      });
      return;
    }

    // PATIENT: avoid doctor/admin-only endpoints; use only patient-authorized APIs.
    this.appointmentService.getAppointmentNotificationsUnreadCount().subscribe({
      next: (c) => {
        base.metricRows[0] = { label: 'Unread Notifications', value: c?.unreadCount ?? 0, unit: '' };
      },
      error: () => {}
    });

    this.dynamicPreset = base;
    this.loading = false;
  }
}