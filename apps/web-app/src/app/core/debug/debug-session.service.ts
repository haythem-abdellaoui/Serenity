import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DebugCategory, DebugEvent, DebugSeverity } from './debug-event.model';

@Injectable({
  providedIn: 'root'
})
export class DebugSessionService {
  private static readonly SESSION_KEY = 'serenity_debug_mode';
  private static readonly EVENTS_KEY = 'serenity_debug_events';
  private static readonly MAX_EVENTS = 1000;

  private nextId = 1;
  private readonly eventsSubject = new BehaviorSubject<DebugEvent[]>([]);
  readonly events$ = this.eventsSubject.asObservable();

  private readonly enabledSubject = new BehaviorSubject<boolean>(false);
  readonly enabled$ = this.enabledSubject.asObservable();

  constructor() {
    this.bootstrapMode();
    this.bootstrapEvents();
  }

  isEnabled(): boolean {
    return this.enabledSubject.value;
  }

  enable(): void {
    if (!environment.debugModeAvailable) {
      return;
    }
    sessionStorage.setItem(DebugSessionService.SESSION_KEY, '1');
    this.enabledSubject.next(true);
    this.log('AUTH', 'Debug mode enabled', { source: 'session' });
  }

  disable(): void {
    sessionStorage.removeItem(DebugSessionService.SESSION_KEY);
    sessionStorage.removeItem(DebugSessionService.EVENTS_KEY);
    this.eventsSubject.next([]);
    this.enabledSubject.next(false);
  }

  log(
    category: DebugCategory,
    action: string,
    details?: Record<string, unknown>,
    severity: DebugSeverity = 'info'
  ): void {
    if (!this.isEnabled()) {
      return;
    }

    const route = window.location.pathname + window.location.search;
    const event: DebugEvent = {
      id: this.nextId++,
      timestamp: new Date().toISOString(),
      category,
      severity,
      action,
      route,
      details
    };

    const next = [...this.eventsSubject.value, event].slice(-DebugSessionService.MAX_EVENTS);
    this.eventsSubject.next(next);
    sessionStorage.setItem(DebugSessionService.EVENTS_KEY, JSON.stringify(next));
  }

  clear(): void {
    this.eventsSubject.next([]);
    sessionStorage.setItem(DebugSessionService.EVENTS_KEY, JSON.stringify([]));
  }

  exportJson(): string {
    return JSON.stringify(this.eventsSubject.value, null, 2);
  }

  exportMarkdown(): string {
    const lines = ['# Debug Session Report', ''];
    for (const event of this.eventsSubject.value) {
      lines.push(`- [${event.timestamp}] ${event.severity.toUpperCase()} ${event.category}: ${event.action}`);
    }
    return lines.join('\n');
  }

  private bootstrapMode(): void {
    const query = new URLSearchParams(window.location.search);
    const queryDebugEnabled = query.get('debugSession') === '1';
    const sessionEnabled = sessionStorage.getItem(DebugSessionService.SESSION_KEY) === '1';

    const enabled = environment.debugModeAvailable && (queryDebugEnabled || sessionEnabled);
    if (enabled) {
      sessionStorage.setItem(DebugSessionService.SESSION_KEY, '1');
    }
    this.enabledSubject.next(enabled);
  }

  private bootstrapEvents(): void {
    const raw = sessionStorage.getItem(DebugSessionService.EVENTS_KEY);
    if (!raw) {
      return;
    }

    try {
      const events = JSON.parse(raw) as DebugEvent[];
      this.eventsSubject.next(Array.isArray(events) ? events.slice(-DebugSessionService.MAX_EVENTS) : []);
      const maxId = this.eventsSubject.value.reduce((max, ev) => Math.max(max, ev.id), 0);
      this.nextId = maxId + 1;
    } catch {
      this.eventsSubject.next([]);
    }
  }
}
