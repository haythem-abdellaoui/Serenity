import { Injectable } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class SelectRoleGuard implements CanActivate {

  constructor(private router: Router) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean {
    // Vérifie si on a un indicateur "passedRegister" dans le sessionStorage
    const cameFromRegister = sessionStorage.getItem('passedRegister');
    if (cameFromRegister === 'true') {
      return true;
    } else {
      // Redirige vers la page register
      this.router.navigate(['/auth/register']);
      return false;
    }
  }
}