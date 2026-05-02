import { Component } from '@angular/core';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-toast',
  templateUrl: './toast.component.html',
  styleUrls: ['./toast.component.scss']
})
export class ToastComponent {
  readonly message$ = this.notification.message$;

  constructor(private readonly notification: NotificationService) {}

  dismiss(): void {
    this.notification.clear();
  }
}
