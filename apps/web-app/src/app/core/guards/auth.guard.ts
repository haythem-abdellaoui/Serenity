import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  canActivate(route: ActivatedRouteSnapshot): boolean | UrlTree {
    if (!this.authService.isLoggedIn()) {
      return this.router.createUrlTree(['/auth/login']);
    }

    const isAdminRoute = route.url.some(seg => seg.path === 'admin')
      || route.routeConfig?.path === 'admin';

    if (this.authService.isAdmin() && !isAdminRoute) {
      return this.router.createUrlTree(['/admin']);
    }

    return true;
  }
}
