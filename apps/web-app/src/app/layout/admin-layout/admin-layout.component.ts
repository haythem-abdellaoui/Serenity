import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';
import { UserResponse } from '../../shared/models/user.model';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-admin-layout',
  templateUrl: './admin-layout.component.html',
  styleUrls: ['./admin-layout.component.scss']
})
export class AdminLayoutComponent implements OnInit {
  sidebarCollapsed = false;
  currentYear = new Date().getFullYear();
  user: UserResponse | null = null;
  currentRoute = '';

  navItems = [
    { label: 'Dashboard', icon: 'grid', route: '/admin' },
    { label: 'Users', icon: 'users', route: '/admin/users' },
    { label: 'Doctors', icon: 'user-md', route: '/admin/doctors' },
    { label: 'Pharmacy Verification', icon: 'shield', route: '/admin/pharmacy-applications' },
    { label: 'Insurance Claims', icon: 'shield', route: '/admin/insurance' },
    { label: 'Appointments', icon: 'calendar', route: '/admin/appointments' },
    { label: 'Edit User', icon: 'edit', route: '/admin/users/edit', hidden: true }
  ];

  constructor(
    public readonly authService: AuthService,
    private readonly userService: UserService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.currentRoute = this.router.url;
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd)
    ).subscribe((e: any) => {
      this.currentRoute = e.urlAfterRedirects || e.url;
    });

    this.userService.getCurrentUser().subscribe({
      next: (user) => this.user = user
    });
  }

  get visibleNavItems() {
    return this.navItems.filter(item => !item.hidden);
  }

  getDisplayName(): string {
    if (this.user?.firstName && this.user?.lastName) {
      return `${this.user.firstName} ${this.user.lastName}`;
    }
    if (this.user?.firstName) return this.user.firstName;
    return (this.authService.getCurrentUser()?.email || '').split('@')[0];
  }

  getInitials(): string {
    if (this.user?.firstName && this.user?.lastName) {
      return this.user.firstName[0] + this.user.lastName[0];
    }
    return (this.authService.getCurrentUser()?.email || 'A')[0].toUpperCase();
  }

  isActive(route: string): boolean {
    if (route === '/admin') return this.currentRoute === '/admin';
    return this.currentRoute.startsWith(route);
  }

  toggleSidebar(): void {
    this.sidebarCollapsed = !this.sidebarCollapsed;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
