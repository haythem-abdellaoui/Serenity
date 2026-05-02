import { Component, OnInit, NgZone, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { environment } from '../../../../environments/environment';

declare const google: any;
declare const FB: any;

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit, AfterViewInit {
  @ViewChild('googleBtn') googleBtn!: ElementRef;

  loginForm!: FormGroup;
  errorMessage = '';
  infoMessage = '';
  loading = false;
  socialLoading = false;
  googleReady = false;
  facebookReady = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly ngZone: NgZone
  ) {}

  ngOnInit(): void {
    if (this.authService.isLoggedIn()) {
      this.redirectAfterLogin();
    }

    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]]
    });

    const stateMessage = history.state?.message;
    if (typeof stateMessage === 'string' && stateMessage.trim()) {
      this.infoMessage = stateMessage;
    }

    this.initFacebookSdk();
  }

  ngAfterViewInit(): void {
    this.initGoogleSignIn();
  }

  onSubmit(): void {
    if (this.loginForm.invalid) return;

    this.loading = true;
    this.errorMessage = '';

    this.authService.login(this.loginForm.value).subscribe({
      next: () => this.redirectAfterLogin(),
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Invalid email or password';
      }
    });
  }

  signInWithGoogle(): void {
    if (!environment.googleClientId) {
      this.errorMessage = 'Google Sign-In is not configured for this environment.';
      return;
    }
    if (!this.googleReady) {
      this.errorMessage = 'Google Sign-In is still loading. Please try again in a moment.';
      return;
    }
    const btnEl = this.googleBtn?.nativeElement?.querySelector('[role="button"]');
    if (btnEl) {
      btnEl.click();
    } else {
      this.errorMessage = 'Google Sign-In is not available. Please try again later.';
    }
  }

  signInWithFacebook(): void {
    if (!environment.facebookAppId || environment.facebookAppId === 'YOUR_FACEBOOK_APP_ID') {
      this.errorMessage = 'Facebook Login is not configured for this environment.';
      return;
    }
    if (!this.facebookReady) {
      this.errorMessage = 'Facebook Login is still loading. Please try again in a moment.';
      return;
    }

    this.socialLoading = true;
    this.errorMessage = '';

    try {
      FB.login((response: any) => {
        this.ngZone.run(() => {
          if (response.authResponse) {
            this.authService.loginWithFacebook(response.authResponse.accessToken).subscribe({
              next: () => this.redirectAfterLogin(),
              error: (err) => {
                this.socialLoading = false;
                this.errorMessage = err.error?.message || 'Facebook login failed';
              }
            });
          } else {
            this.socialLoading = false;
          }
        });
      }, { scope: 'email,public_profile' });
    } catch {
      this.socialLoading = false;
      this.errorMessage = 'Facebook Login requires HTTPS. Please try again later.';
    }
  }

  private initGoogleSignIn(): void {
    if (!environment.googleClientId) {
      return;
    }
    const checkGoogle = setInterval(() => {
      if (google !== undefined && google.accounts) {
        clearInterval(checkGoogle);
        try {
          google.accounts.id.initialize({
            client_id: environment.googleClientId,
            callback: (response: any) => this.handleGoogleResponse(response),
            use_fedcm_for_prompt: false
          });
          google.accounts.id.renderButton(this.googleBtn.nativeElement, {
            type: 'icon',
            shape: 'circle',
            size: 'small'
          });
          this.googleReady = true;
        } catch {
          console.warn('Google Sign-In initialization failed');
        }
      }
    }, 300);
    setTimeout(() => clearInterval(checkGoogle), 10000);
  }

  private handleGoogleResponse(response: any): void {
    this.ngZone.run(() => {
      this.socialLoading = true;
      this.errorMessage = '';

      this.authService.loginWithGoogle(response.credential).subscribe({
        next: () => this.redirectAfterLogin(),
        error: (err) => {
          this.socialLoading = false;
          this.errorMessage = err.error?.message || 'Google login failed';
        }
      });
    });
  }

  private redirectAfterLogin(): void {
    const destination = this.authService.isAdmin() ? '/admin' : '/';
    this.router.navigate([destination]);
  }

  private initFacebookSdk(): void {
    if (!environment.facebookAppId || environment.facebookAppId === 'YOUR_FACEBOOK_APP_ID') {
      return;
    }
    const checkFB = setInterval(() => {
      if (FB !== undefined) {
        clearInterval(checkFB);
        try {
          FB.init({
            appId: environment.facebookAppId,
            cookie: true,
            xfbml: true,
            version: 'v19.0'
          });
          this.facebookReady = true;
        } catch {
          console.warn('Facebook SDK initialization failed');
        }
      }
    }, 300);
    setTimeout(() => clearInterval(checkFB), 10000);
  }
}
