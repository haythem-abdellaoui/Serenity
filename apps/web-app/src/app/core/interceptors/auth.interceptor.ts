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
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.authService.getToken();
    const shouldAttachToken = this.shouldAttachToken(request.url);
    const currentUser = this.authService.getCurrentUser();
    const userId = currentUser?.userId;

    if (token && shouldAttachToken) {
      const headers: Record<string, string> = {
        Authorization: `Bearer ${token}`
      };
      if (userId != null) {
        headers['X-Doctor-Id'] = String(userId);
        headers['userId'] = String(userId);
      }
      request = request.clone({
        setHeaders: headers
      });
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          // Only force logout if the error is from auth endpoints
          // For other services (e.g., monitoring), let the service handle the error gracefully
          const isAuthError = request.url.includes('/auth/') || 
                            request.url.includes('/login') || 
                            request.url.includes('/register');
          
          if (isAuthError) {
            this.authService.logout();
            this.router.navigate(['/auth/login']);
          }
        }
        return throwError(() => error);
      })
    );
  }

  private shouldAttachToken(url: string): boolean {
    // Keep auth headers on Serenity APIs only; skip third-party URLs (quotes/proxies) to avoid CORS preflight failures.
    if (!/^https?:\/\//i.test(url)) {
      return true;
    }

    return url.startsWith('http://localhost:8082') ||
      url.startsWith('http://localhost:8085') ||
      url.startsWith('http://localhost:8099') ||
      url.includes('/api/');
  }
}
