import { Component, OnInit } from '@angular/core';
import { UserService } from '../../../core/services/user.service';
import { BanDuration, UserResponse } from '../../../shared/models/user.model';

@Component({
  selector: 'app-user-list',
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.scss']
})
export class UserListComponent implements OnInit {
  users: UserResponse[] = [];
  loading = true;
  errorMessage = '';
  readonly banOptions: { label: string; value: BanDuration }[] = [
    { label: '1 day', value: 'ONE_DAY' },
    { label: '3 days', value: 'THREE_DAYS' },
    { label: '1 week', value: 'ONE_WEEK' },
    { label: '1 month', value: 'ONE_MONTH' },
    { label: 'Permanent', value: 'PERMANENT' }
  ];
  showBanModal = false;
  showBanDurationMenu = false;
  selectedBanDuration: BanDuration = 'ONE_DAY';
  banTargetUser: UserResponse | null = null;

  constructor(private readonly userService: UserService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.userService.getAllUsers().subscribe({
      next: (users) => {
        this.users = users;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load users';
        this.loading = false;
      }
    });
  }

  deactivateUser(id: number): void {
    if (!confirm('Are you sure you want to deactivate this user?')) return;

    this.userService.deactivateUser(id).subscribe({
      next: () => this.loadUsers(),
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to deactivate user';
      }
    });
  }

  activateUser(id: number): void {
    this.userService.activateUser(id).subscribe({
      next: () => this.loadUsers(),
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to activate user';
      }
    });
  }

  deleteUser(id: number): void {
    if (!confirm('Are you sure you want to permanently delete this user?')) return;

    this.userService.deleteUser(id).subscribe({
      next: () => this.loadUsers(),
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to delete user';
      }
    });
  }

  openBanModal(user: UserResponse): void {
    this.banTargetUser = user;
    this.selectedBanDuration = 'ONE_DAY';
    this.showBanDurationMenu = false;
    this.showBanModal = true;
  }

  closeBanModal(): void {
    this.showBanModal = false;
    this.showBanDurationMenu = false;
    this.banTargetUser = null;
    this.selectedBanDuration = 'ONE_DAY';
  }

  onModalBackdropKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape' || event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.closeBanModal();
    }
  }

  onBanModalKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      event.preventDefault();
      event.stopPropagation();
      this.closeBanModal();
    }
  }

  toggleBanDurationMenu(): void {
    this.showBanDurationMenu = !this.showBanDurationMenu;
  }

  selectBanDuration(value: BanDuration): void {
    this.selectedBanDuration = value;
    this.showBanDurationMenu = false;
  }

  getSelectedBanDurationLabel(): string {
    return this.banOptions.find(option => option.value === this.selectedBanDuration)?.label ?? '1 day';
  }

  confirmBan(): void {
    if (!this.banTargetUser) {
      return;
    }

    const duration = this.selectedBanDuration;
    const confirmMessage = duration === 'PERMANENT'
      ? 'Are you sure you want to permanently ban this user?'
      : 'Are you sure you want to ban this user?';
    if (!confirm(confirmMessage)) return;

    this.userService.banUser(this.banTargetUser.id, duration).subscribe({
      next: () => {
        this.closeBanModal();
        this.loadUsers();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to ban user';
      }
    });
  }

  unbanUser(id: number): void {
    this.userService.unbanUser(id).subscribe({
      next: () => this.loadUsers(),
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to unban user';
      }
    });
  }

  isCurrentlyBanned(user: UserResponse): boolean {
    if (user.isPermanentlyBanned) {
      return true;
    }
    if (!user.bannedUntil) {
      return false;
    }
    return new Date(user.bannedUntil).getTime() > Date.now();
  }

  getBanStatusText(user: UserResponse): string {
    if (user.isPermanentlyBanned) {
      return 'Permanently banned';
    }
    if (this.isCurrentlyBanned(user) && user.bannedUntil) {
      return `Banned until ${new Date(user.bannedUntil).toLocaleString()}`;
    }
    return user.isActive ? 'Active' : 'Inactive';
  }
}
