import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface AppNotification {
  type: 'success' | 'error' | 'info';
  message: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly state = new BehaviorSubject<AppNotification | null>(null);
  readonly message$ = this.state.asObservable();

  success(message: string): void {
    this.show({ type: 'success', message });
  }

  error(message: string): void {
    this.show({ type: 'error', message });
  }

  info(message: string): void {
    this.show({ type: 'info', message });
  }

  clear(): void {
    this.state.next(null);
  }

  private show(n: AppNotification): void {
    this.state.next(n);
    window.setTimeout(() => {
      if (this.state.value === n) {
        this.state.next(null);
      }
    }, 5000);
  }
}
