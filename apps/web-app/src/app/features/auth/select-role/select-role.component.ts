import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-select-role',
  templateUrl: './select-role.component.html',
  styleUrl: './select-role.component.scss'
})
export class SelectRoleComponent {
  selectedRole: string | null = null;
  loading = false;
  errorMessage = '';

  roles = [
    {
      id: 'patient',
      title: 'I am here seeking help for my medical conditions',
      icon: 'medical_services'
    },
    {
      id: 'doctor',
      title: 'I am a doctor wanting to join your team',
      icon: 'health_and_safety'
    },
    {
      id: 'pharmacist',
      title: 'I am a pharmacist wanting to collaborate with Serenity',
      icon: 'local_pharmacy'
    },
    {
      id: 'insurer',
      title: 'I am an Insurer wanting to work with you',
      icon: 'admin_panel_settings'
    }
  ];

  constructor(private router: Router, private authService: AuthService) {}

  ngOnInit() {
    /*
    const user = this.authService.getCurrentUser();
    console.log('Current user:', user);
    if (!user || user.role !== 'DOCTOR') {
      this.router.navigate(['/auth/login']);
      return;
    }*/
  }

  selectRole(roleId: string) {
    this.selectedRole = roleId;
  }

  continue() {
    if (!this.selectedRole || this.loading) return;

    if (this.selectedRole === 'pharmacist') {
      this.router.navigate(['/pharmacy/apply']);
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    const targetRole = this.selectedRole === 'insurer' ? 'MARKETPLACE_MANAGER' : this.selectedRole;

    this.authService.updateUserRole(targetRole).subscribe({
      next: (res) => {
        this.loading = false;

        switch (this.selectedRole) {
          case 'doctor':
            this.router.navigate(['/auth/doctor']);
            break;
          case 'pharmacist':
            this.router.navigate(['/']);
            break;
          case 'insurer':
            this.router.navigate(['/']);
            break;
          case 'patient':
            this.router.navigate(['/']);
            break;
          default:
            this.router.navigate(['/']);
        }
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Failed to update role.';
      }
    });
    
  }
  
}
