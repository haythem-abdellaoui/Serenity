import { Component, HostListener, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin, of, Subscription } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../core/services/auth.service';
import { CrisisAlertService } from '../core/services/crisis-alert.service';
import { UserService } from '../core/services/user.service';
import { InsuranceService } from '../core/services/insurance.service';
import { AppointmentService } from '../core/services/appointment.service';
import { InsuranceNotification } from '../shared/models/insurance.model';
import { AppointmentNotification, NavbarNotification } from '../shared/models/appointment.model';
import { DoctorRealtimeNotification } from '../shared/models/mood.model';
import { UserResponse } from '../shared/models/user.model';
import { WebSocketService } from '../core/services/web-socket.service';

@Component({
  selector: 'app-layout',
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.scss']
})
export class LayoutComponent implements OnInit, OnDestroy {
  currentYear = new Date().getFullYear();
  characterVisible = false;
  user: UserResponse | null = null;
  notifications: NavbarNotification[] = [];
  unreadNotificationCount = 0;
  notificationsOpen = false;
  notificationsLoading = false;
  private readonly locallyReadNotificationIds = new Set<string>();
  alerts: DoctorRealtimeNotification[] = [];
  notificationPanelOpen = false;
  private peekInterval: ReturnType<typeof setInterval> | undefined;
  private userSub!: Subscription;
  private wsSub!: Subscription;
  messageNotifications: Array<{
    id: number;
    text: string;
    senderName: string;
    conversationId: number;
    time: Date;
    read: boolean;
  }> = [];
  unreadMessageCount = 0;
  notifDropdownVisible = false;
  private alertsSub!: Subscription;
  private authSub!: Subscription;
  private notificationRefreshInterval: ReturnType<typeof setInterval> | undefined;

  // ── Sticky notes: pinned overlay state (persists across navigation) ──────────
  pinnedSticky: { pageIndex: number; x: number; y: number; open: boolean } | null = null;
  private readonly stickyPrefix = 'serenity:dashboard:sticky';
  private stickyPollInterval: ReturnType<typeof setInterval> | undefined;
  private draggingPinned = false;
  private dragStart: { mouseX: number; mouseY: number; x: number; y: number } | null = null;

  constructor(
    public readonly authService: AuthService,
    private readonly crisisAlertService: CrisisAlertService,
    private readonly userService: UserService,
    private readonly router: Router,
    private readonly webSocketService: WebSocketService,
    private readonly insuranceService: InsuranceService,
    private readonly appointmentService: AppointmentService
  ) {}

  ngOnInit(): void {
    document.addEventListener('click', () => {
      this.notifDropdownVisible = false;
    });
    if (this.authService.isLoggedIn()) {
      this.userService.getCurrentUser().subscribe();

      this.userSub = this.userService.currentUser$.subscribe((user) => {
        this.user = user;
        if (user && !this.peekInterval) {
          this.startPeekAnimation();
        }
      });

      this.webSocketService.connect();

      this.wsSub = this.webSocketService.newMessage$.subscribe((msg: any) => {
        const currentUserId = this.authService.getCurrentUser()?.userId;
        if (msg.senderId !== currentUserId && !msg.deletedMessageId) {
          this.userService.getUsersNamesById([msg.senderId]).subscribe({
            next: (users) => {
              const sender = users[0];
              const senderName = sender ? `${sender.firstName} ${sender.lastName}` : 'Unknown';

              this.messageNotifications.unshift({
                id: msg.id,
                text: msg.content,
                senderName,
                conversationId: msg.conversationId,
                time: new Date(),
                read: false
              });
              this.unreadMessageCount++;
            },
            error: () => {
              this.messageNotifications.unshift({
                id: msg.id,
                text: msg.content,
                senderName: 'Unknown',
                conversationId: msg.conversationId,
                time: new Date(),
                read: false
              });
              this.unreadMessageCount++;
            }
          });
        }
      });

      this.refreshNotifications();
      this.notificationRefreshInterval = setInterval(() => this.refreshNotifications(), 20000);

      this.alertsSub = this.crisisAlertService.alerts$.subscribe((alerts) => {
        this.alerts = alerts;
      });
    }

    this.authSub = this.authService.currentUser$.subscribe((authUser) => {
      if (authUser && this.authService.isDoctor() && authUser.userId) {
        this.crisisAlertService.connect(authUser.userId);
        return;
      }
      this.crisisAlertService.disconnect();
    });

    // Poll localStorage for pinned note changes (same-tab writes don’t trigger `storage` events).
    this.refreshPinnedSticky();
    this.stickyPollInterval = setInterval(() => this.refreshPinnedSticky(), 750);
  }

  ngOnDestroy(): void {
    if (this.peekInterval) {
      clearInterval(this.peekInterval);
    }
    if (this.userSub) {
      this.userSub.unsubscribe();
    }
    if (this.wsSub) {
      this.wsSub.unsubscribe();
    }
    if (this.alertsSub) {
      this.alertsSub.unsubscribe();
    }
    if (this.authSub) {
      this.authSub.unsubscribe();
    }
    if (this.notificationRefreshInterval) {
      clearInterval(this.notificationRefreshInterval);
    }
    if (this.stickyPollInterval) {
      clearInterval(this.stickyPollInterval);
    }
    this.stopPinnedDrag();
    if (this.authService.isDoctor()) {
      this.crisisAlertService.disconnect();
    }
  }

  // ── Pinned sticky helpers ───────────────────────────────────────────────────
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

  private readStickyPayload(): any | null {
    try {
      const raw = localStorage.getItem(this.stickyStorageKey());
      if (!raw) return null;
      return JSON.parse(raw);
    } catch {
      return null;
    }
  }

  private writeStickyPayload(payload: any): void {
    try {
      localStorage.setItem(this.stickyStorageKey(), JSON.stringify(payload));
    } catch {
      // ignore
    }
  }

  private refreshPinnedSticky(): void {
    const payload = this.readStickyPayload();
    if (!payload || payload.date !== this.todayKey()) {
      this.pinnedSticky = null;
      return;
    }
    const p = payload.pinned;
    if (p && typeof p === 'object' && p.open === true) {
      this.pinnedSticky = {
        open: true,
        pageIndex: typeof p.pageIndex === 'number' ? p.pageIndex : 0,
        x: typeof p.x === 'number' ? p.x : 24,
        y: typeof p.y === 'number' ? p.y : 120
      };
    } else {
      this.pinnedSticky = null;
    }
  }

  unpinSticky(): void {
    const payload = this.readStickyPayload();
    if (!payload) {
      this.pinnedSticky = null;
      return;
    }
    payload.pinned = null;
    this.writeStickyPayload(payload);
    this.pinnedSticky = null;
  }

  get pinnedStyle(): { [k: string]: string } {
    const x = this.pinnedSticky?.x ?? 24;
    const y = this.pinnedSticky?.y ?? 120;
    return { left: `${x}px`, top: `${y}px` };
  }

  get pinnedText(): string {
    const payload = this.readStickyPayload();
    const pages: string[] = Array.isArray(payload?.pages) ? payload.pages : [];
    const idx = this.pinnedSticky?.pageIndex ?? 0;
    return pages[idx] ?? '';
  }

  get pinnedColor(): string {
    const payload = this.readStickyPayload();
    const colors: string[] = Array.isArray(payload?.colors) ? payload.colors : [];
    const idx = this.pinnedSticky?.pageIndex ?? 0;
    return colors[idx] ?? 'yellow';
  }

  onPinnedTextChange(v: string): void {
    const payload = this.readStickyPayload();
    if (!payload || !this.pinnedSticky) return;
    const pages: string[] = Array.isArray(payload.pages) ? payload.pages : [];
    const idx = this.pinnedSticky.pageIndex;
    while (pages.length <= idx) {
      pages.push('');
    }
    pages[idx] = v;
    payload.pages = pages;
    this.writeStickyPayload(payload);
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
      // Persist final position
      const payload = this.readStickyPayload();
      if (payload && this.pinnedSticky) {
        payload.pinned = this.pinnedSticky;
        this.writeStickyPayload(payload);
      }
    }
    this.stopPinnedDrag();
  };

  private stopPinnedDrag(): void {
    window.removeEventListener('mousemove', this.onPinnedMouseMove);
    window.removeEventListener('mouseup', this.onPinnedMouseUp);
  }

  toggleNotifDropdown(): void {
    this.notifDropdownVisible = !this.notifDropdownVisible;
    if (this.notifDropdownVisible) {
      this.unreadMessageCount = 0;
      this.messageNotifications = this.messageNotifications.map((n) => ({ ...n, read: true }));
    }
  }

  goToConversation(_notif: { conversationId: number }): void {
    this.notifDropdownVisible = false;
    this.router.navigate(['/messagerie']);
  }

  clearNotifications(): void {
    this.messageNotifications = [];
    this.unreadMessageCount = 0;
  }

  get unreadCount(): number {
    return this.alerts.length + this.unreadMessageCount;
  }

  get showAlertPanel(): boolean {
    return this.notificationPanelOpen;
  }

  set showAlertPanel(value: boolean) {
    this.notificationPanelOpen = value;
  }

  toggleAlertPanel(): void {
    this.toggleNotificationPanel();
  }

  toggleNotificationPanel(): void {
    if (!this.authService.isDoctor()) {
      return;
    }
    this.notificationPanelOpen = !this.notificationPanelOpen;
  }

  clearAllAlerts(): void {
    this.crisisAlertService.clearAlerts();
    this.notificationPanelOpen = false;
  }

  getAlertTitle(alert: DoctorRealtimeNotification): string {
    return alert.type === 'WEEKLY_DIGEST' ? 'Weekly Digest' : 'Crisis Alert';
  }

  getAlertIcon(alert: DoctorRealtimeNotification): string {
    return alert.type === 'WEEKLY_DIGEST' ? '📊' : '🚨';
  }

  hasMoodBadge(alert: DoctorRealtimeNotification): boolean {
    return alert.type === 'CRISIS' && typeof alert.moodLevel === 'number';
  }

  getMoodBadgeText(alert: DoctorRealtimeNotification): string {
    if (!this.hasMoodBadge(alert)) {
      return '';
    }
    return `${alert.moodLevel}/10`;
  }

  getDigestMeta(alert: DoctorRealtimeNotification): string {
    if (alert.type !== 'WEEKLY_DIGEST') {
      return '';
    }
    return `Crisis: ${alert.crisisCount ?? 0} • Worsening: ${alert.worseningPatients ?? 0} • No check-in: ${alert.noCheckinPatients ?? 0}`;
  }

  getDisplayName(): string {
    if (this.user?.profile?.isAnonymous) {
      return 'Anonymous';
    }
    if (this.user?.firstName) {
      return this.user.firstName;
    }
    return (this.authService.getCurrentUser()?.email || '').split('@')[0];
  }

  getCharacterEmoji(): string {
    if (this.user?.profile?.isAnonymous) {
      return '🥷';
    }
    switch (this.user?.role) {
      case 'ADMIN':
        return '🛡️';
      case 'DOCTOR':
        return '👨‍⚕️';
      case 'PHARMACIST':
        return '💊';
      case 'MARKETPLACE_MANAGER':
        return '🛒';
      default:
        return '🧑';
    }
  }

  private startPeekAnimation(): void {
    this.peekInterval = setInterval(() => {
      this.characterVisible = true;
      setTimeout(() => {
        this.characterVisible = false;
      }, 3000);
    }, 8000 + Math.random() * 7000);
  }

  logout(): void {
    this.authService.logout();
    this.notificationPanelOpen = false;
    this.router.navigate(['/auth/login']);
  }

  toggleNotifications(event: MouseEvent): void {
    event.stopPropagation();
    this.notificationsOpen = !this.notificationsOpen;
    if (this.notificationsOpen) {
      this.loadNotifications();
    }
  }

  onNotificationClick(notification: NavbarNotification, event: MouseEvent): void {
    event.stopPropagation();
    const wasUnread = !notification.isRead;

    if (wasUnread) {
      this.locallyReadNotificationIds.add(`${notification.source}-${notification.id}`);
      notification.isRead = true;
      const req$ =
        notification.source === 'insurance'
          ? this.insuranceService.markNotificationAsRead(notification.id)
          : this.appointmentService.markAppointmentNotificationRead(notification.id);
      req$.subscribe({
        next: () => {
          this.unreadNotificationCount = Math.max(0, this.unreadNotificationCount - 1);
          this.refreshNotifications();
        }
      });
    }

    this.notificationsOpen = false;
    if (notification.source === 'insurance' && notification.claimId != null) {
      this.router.navigate(['/insurance', notification.claimId]);
    } else if (notification.source === 'appointment' && notification.appointmentId != null) {
      const base = this.router.url.split('?')[0].includes('/admin/')
        ? '/admin/appointments'
        : '/appointments';
      this.router.navigate([base, notification.appointmentId]);
    }
  }

  markAllAsRead(event: MouseEvent): void {
    event.stopPropagation();
    for (const notification of this.notifications) {
      this.locallyReadNotificationIds.add(`${notification.source}-${notification.id}`);
    }
    forkJoin([
      this.insuranceService.markAllNotificationsAsRead().pipe(catchError(() => of(undefined))),
      this.appointmentService.markAllAppointmentNotificationsRead().pipe(catchError(() => of(undefined)))
    ]).subscribe({
      next: () => {
        this.notifications = this.notifications.map((n) => ({ ...n, isRead: true }));
        this.unreadNotificationCount = 0;
        this.refreshNotifications();
      }
    });
  }

  formatNotificationDate(isoDate: string): string {
    const parsed = new Date(isoDate);
    return Number.isNaN(parsed.getTime()) ? '' : parsed.toLocaleString();
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.notificationsOpen = false;
  }

  private refreshNotifications(): void {
    forkJoin({
      insurance: this.insuranceService.getUnreadNotificationsCount().pipe(
        catchError(() => of({ unreadCount: 0 }))
      ),
      appointment: this.appointmentService.getAppointmentNotificationsUnreadCount().pipe(
        catchError(() => of({ unreadCount: 0 }))
      )
    }).subscribe({
      next: ({ insurance, appointment }) => {
        this.unreadNotificationCount =
          (insurance.unreadCount || 0) + (appointment.unreadCount || 0);
      }
    });
  }

  private loadNotifications(): void {
    this.notificationsLoading = true;
    let insuranceRows: InsuranceNotification[] = [];
    let appointmentRows: AppointmentNotification[] = [];
    let pending = 2;

    const finish = (): void => {
      const merged: NavbarNotification[] = [
        ...insuranceRows.map((n) => ({
          source: 'insurance' as const,
          id: n.id,
          title: n.title,
          message: n.message,
          isRead: n.isRead || this.locallyReadNotificationIds.has(`insurance-${n.id}`),
          createdAt: this.normalizeNotificationDate(n.createdAt),
          claimId: n.claimId,
          appointmentId: null
        })),
        ...appointmentRows.map((n) => ({
          source: 'appointment' as const,
          id: n.id,
          title: n.title,
          message: n.message,
          isRead: n.isRead || this.locallyReadNotificationIds.has(`appointment-${n.id}`),
          createdAt: this.normalizeNotificationDate(n.createdAt),
          claimId: null,
          appointmentId: n.appointmentId
        }))
      ];
      merged.sort((a, b) => this.notificationSortKey(b.createdAt) - this.notificationSortKey(a.createdAt));
      this.notifications = merged;
      this.notificationsLoading = false;
    };

    const step = (): void => {
      pending -= 1;
      if (pending === 0) {
        finish();
      }
    };

    this.insuranceService.getMyNotifications().pipe(catchError(() => of([] as InsuranceNotification[]))).subscribe({
      next: (rows) => {
        insuranceRows = rows || [];
        step();
      },
      error: () => step()
    });

    this.appointmentService
      .getAppointmentNotifications()
      .pipe(catchError(() => of([] as AppointmentNotification[])))
      .subscribe({
        next: (rows) => {
          appointmentRows = rows || [];
          step();
        },
        error: () => step()
      });
  }

  private normalizeNotificationDate(value: string | unknown): string {
    if (typeof value === 'string') {
      return value;
    }
    return new Date().toISOString();
  }

  private notificationSortKey(iso: string): number {
    const t = new Date(iso).getTime();
    return Number.isNaN(t) ? 0 : t;
  }
}
