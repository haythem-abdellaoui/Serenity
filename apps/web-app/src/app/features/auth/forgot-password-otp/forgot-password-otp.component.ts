import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password-otp',
  templateUrl: './forgot-password-otp.component.html',
  styleUrls: ['./forgot-password-otp.component.scss']
})
export class ForgotPasswordOtpComponent implements OnInit {
  otpForm!: FormGroup;
  email = '';
  loading = false;
  errorMessage = '';

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.otpForm = this.fb.group({
      otp: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
    });

    this.email = sessionStorage.getItem('fp_email') ?? '';
    if (!this.email) {
      this.router.navigate(['/auth/forgot-password'], {
        state: { message: 'Please submit your email first.' }
      });
      return;
    }
  }

  onSubmit(): void {
    if (this.otpForm.invalid) {
      this.otpForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    const otp = String(this.otpForm.value.otp ?? '').trim();
    this.authService.verifyPasswordOtp(this.email, otp).subscribe({
      next: (response) => {
        sessionStorage.setItem('fp_reset_token', response.resetToken);
        this.loading = false;
        this.router.navigate(['/auth/reset-password']);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'OTP verification failed. Please try again.';
      }
    });
  }

  backToEmailStep(): void {
    this.router.navigate(['/auth/forgot-password']);
  }
}
