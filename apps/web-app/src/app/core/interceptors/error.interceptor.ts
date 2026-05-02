import { Injectable } from '@angular/core';
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponseDTO } from '../../models/api-response.model';
import { NotificationService } from '../../shared/services/notification.service';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(private readonly notification: NotificationService) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(req).pipe(
      catchError((err: HttpErrorResponse) => {
        if (!req.url.startsWith(environment.medicalApiUrl)) {
          return throwError(() => err);
        }
        if (err.status === 401) {
          return throwError(() => err);
        }
        const msg = this.extractMessage(err);
        console.error('[Medical API HTTP]', err.status, req.url, err.error?.message ?? err.message);
        this.notification.error(msg);
        return throwError(() => err);
      })
    );
  }

  private extractMessage(err: HttpErrorResponse): string {
    const body = err.error as ApiResponseDTO<Record<string, string> | string | null> | undefined;
    if (body && typeof body === 'object' && 'message' in body && typeof body.message === 'string') {
      const msg = body.message;
      if (body.data && typeof body.data === 'object' && !Array.isArray(body.data)) {
        const fields = Object.values(body.data as Record<string, string>).filter(Boolean);
        if (fields.length) {
          return `${msg} : ${fields.join(' · ')}`;
        }
      }
      return msg;
    }
    if (typeof err.error === 'string') {
      return err.error;
    }
    return err.message || 'Erreur réseau';
  }
}
