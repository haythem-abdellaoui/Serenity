import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AppointmentService } from '../../../core/services/appointment.service';

@Component({
  selector: 'app-google-calendar-oauth-callback',
  template:
    '<div class="container oauth-cal-page"><p>Finishing Google Calendar connection…</p></div>',
  styles: [
    `
      .oauth-cal-page {
        padding: 2rem 1rem;
      }
    `
  ]
})
export class GoogleCalendarOAuthCallbackComponent implements OnInit {
  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly appointmentService: AppointmentService
  ) {}

  ngOnInit(): void {
    const returnList = this.decodeReturnList(this.route.snapshot.queryParamMap.get('state'));
    const err = this.route.snapshot.queryParamMap.get('error');
    if (err) {
      void this.router.navigateByUrl(`${returnList}?calendarError=${encodeURIComponent(err)}`);
      return;
    }
    const code = this.route.snapshot.queryParamMap.get('code');
    if (!code) {
      void this.router.navigateByUrl(`${returnList}?calendarError=missing_code`);
      return;
    }
    this.appointmentService.completeGoogleCalendarOAuth(code).subscribe({
      next: () => void this.router.navigateByUrl(`${returnList}?calendarLinked=1`),
      error: () => void this.router.navigateByUrl(`${returnList}?calendarError=token_exchange`)
    });
  }

  private decodeReturnList(state: string | null): string {
    if (!state) {
      return '/appointments/list';
    }
    try {
      const raw = atob(decodeURIComponent(state));
      if (raw === '/appointments' || raw === '/appointments/list' || raw === '/admin/appointments/list') {
        return raw;
      }
    } catch {
      /* ignore */
    }
    return '/appointments/list';
  }
}
