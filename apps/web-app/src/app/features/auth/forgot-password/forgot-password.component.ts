import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.scss']
})
export class ForgotPasswordComponent implements OnInit {
  forgotForm!: FormGroup;
  loading = false;
  errorMessage = '';
  infoMessage = '';

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.forgotForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });

    const stateMessage = history.state?.message;
    if (typeof stateMessage === 'string' && stateMessage.trim()) {
      this.infoMessage = stateMessage;
    }
  }

  onSubmit(): void {
    if (this.forgotForm.invalid) {
      this.forgotForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.infoMessage = '';

    const email = String(this.forgotForm.value.email ?? '').trim().toLowerCase();
    this.authService.requestPasswordOtp(email).subscribe({
      next: () => {
        sessionStorage.setItem('fp_email', email);
        this.loading = false;
        this.router.navigate(['/auth/forgot-password/otp']);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Could not start password recovery. Please try again.';
      }
    });
  }
}
