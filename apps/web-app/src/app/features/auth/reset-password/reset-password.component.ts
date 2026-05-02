import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-reset-password',
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.scss']
})
export class ResetPasswordComponent implements OnInit {
  resetForm!: FormGroup;
  loading = false;
  errorMessage = '';
  resetToken = '';

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.resetForm = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordsMatchValidator() });

    this.resetToken = sessionStorage.getItem('fp_reset_token') ?? '';
    if (!this.resetToken) {
      this.router.navigate(['/auth/forgot-password'], {
        state: { message: 'Please verify OTP before resetting your password.' }
      });
      return;
    }
  }

  onSubmit(): void {
    if (this.resetForm.invalid) {
      this.resetForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    const newPassword = String(this.resetForm.value.newPassword ?? '');
    this.authService.resetForgottenPassword(this.resetToken, newPassword).subscribe({
      next: () => {
        sessionStorage.removeItem('fp_email');
        sessionStorage.removeItem('fp_reset_token');
        this.loading = false;
        this.router.navigate(['/auth/login'], {
          state: { message: 'Password reset successful. You can now sign in.' }
        });
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Unable to reset password. Please try again.';
      }
    });
  }

  private passwordsMatchValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const newPassword = control.get('newPassword')?.value;
      const confirmPassword = control.get('confirmPassword')?.value;
      if (!newPassword || !confirmPassword) {
        return null;
      }
      return newPassword === confirmPassword ? null : { passwordMismatch: true };
    };
  }
}
