import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { MonitoringService } from '../../../../core/services/monitoring.service';
import { AuthService } from '../../../../core/services/auth.service';
import { MotivationalQuote, ZenQuotesService } from '../../../../core/services/zen-quotes.service';

@Component({
  selector: 'app-mood-form',
  templateUrl: './mood-form.component.html',
  styleUrls: ['./mood-form.component.scss']
})
export class MoodFormComponent implements OnInit, OnDestroy {
  moodForm!: FormGroup;
  isEditMode = false;
  isPatient = false;
  editingId: number | null = null;
  loading = false;
  saving = false;
  errorMessage = '';

  showQuoteOverlay = false;
  currentQuote: MotivationalQuote | null = null;
  quoteLoading = false;

  private prefetchedQuote: MotivationalQuote | null = null;
  private quotePrefetchInFlight = false;
  private quotePrefetchSub: Subscription | null = null;
  private moodScoreSub: Subscription | null = null;
  private redirectTimer: ReturnType<typeof setTimeout> | null = null;
  private quoteResolveTimer: ReturnType<typeof setTimeout> | null = null;
  private redirectCountdownStarted = false;

  private readonly overlayRedirectDelayMs = 4000;
  private readonly quoteResolveTimeoutMs = 2500;

  private readonly fallbackQuotes: MotivationalQuote[] = [
    {
      text: 'Take a deep breath. You are not alone. Every day is a new beginning.',
      author: 'Serenity'
    },
    {
      text: 'You have survived difficult days before, and you will survive this one too.',
      author: 'Serenity'
    },
    {
      text: 'Small steps still move you forward. Be gentle with yourself today.',
      author: 'Serenity'
    },
    {
      text: 'Healing is not linear. Progress can be quiet and still be real.',
      author: 'Serenity'
    }
  ];

  moodScales = [
    { value: 1, label: 'Very Bad 😢', color: '#e74c3c' },
    { value: 2, label: 'Bad 😞', color: '#e67e22' },
    { value: 3, label: 'Poor 😕', color: '#f39c12' },
    { value: 4, label: 'Below Average 😐', color: '#f1c40f' },
    { value: 5, label: 'Average 😐', color: '#f4d03f' },
    { value: 6, label: 'Good 🙂', color: '#a3e048' },
    { value: 7, label: 'Very Good 😊', color: '#2ecc71' },
    { value: 8, label: 'Excellent 😄', color: '#1abc9c' },
    { value: 9, label: 'Very Excellent 😃', color: '#3498db' },
    { value: 10, label: 'Perfect 😄', color: '#9b59b6' }
  ];

  constructor(
    private readonly fb: FormBuilder,
    private readonly monitoringService: MonitoringService,
    private readonly authService: AuthService,
    private readonly zenQuotesService: ZenQuotesService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.isPatient = this.authService.isPatient();
    this.initializeForm();
    this.setupQuotePrefetch();
    this.checkEditMode();
  }

  ngOnDestroy(): void {
    this.quotePrefetchSub?.unsubscribe();
    this.moodScoreSub?.unsubscribe();
    if (this.redirectTimer) {
      clearTimeout(this.redirectTimer);
      this.redirectTimer = null;
    }
    if (this.quoteResolveTimer) {
      clearTimeout(this.quoteResolveTimer);
      this.quoteResolveTimer = null;
    }
  }

  private initializeForm(): void {
    this.moodForm = this.fb.group({
      moodScore: [5, [Validators.required, Validators.min(1), Validators.max(10)]],
      moodDescription: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(500)]],
      triggers: ['', [Validators.maxLength(500)]]
    });
  }

  private checkEditMode(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.editingId = parseInt(id, 10);
        this.loadMoodEntry(this.editingId);
      }
    });
  }

  private loadMoodEntry(id: number): void {
    this.loading = true;
    this.monitoringService.getMoodEntryById(id).subscribe({
      next: (entry) => {
        this.moodForm.patchValue({
          moodScore: entry.moodScore,
          moodDescription: entry.moodDescription,
          triggers: entry.triggers || ''
        });
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load mood entry';
        this.loading = false;
      }
    });
  }

  getMoodColor(): string {
    const score = this.moodForm.get('moodScore')?.value || 5;
    const scale = this.moodScales.find(s => s.value === score);
    return scale?.color || '#95a5a6';
  }

  getMoodLabel(): string {
    const score = this.moodForm.get('moodScore')?.value || 5;
    const scale = this.moodScales.find(s => s.value === score);
    return scale?.label || 'Average';
  }

  onSubmit(): void {
    if (!this.moodForm.valid) {
      this.errorMessage = 'Please fill in all required fields correctly';
      return;
    }

    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.userId) {
      this.errorMessage = 'User not logged in';
      return;
    }

    this.saving = true;
    this.errorMessage = '';

    const request = {
      patientId: currentUser.userId,
      ...this.moodForm.value
    };
    const submittedMoodScore = Number(request.moodScore ?? 0);

    // Start quote request in parallel so success overlay can render immediately.
    if (this.isPatient && submittedMoodScore <= 5) {
      this.prefetchQuote(true);
    }

    if (this.isEditMode && this.editingId) {
      this.monitoringService.updateMoodEntry(this.editingId, request).subscribe({
        next: () => {
          this.handleSuccessfulSubmit(submittedMoodScore);
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Failed to update mood entry';
          this.saving = false;
        }
      });
    } else {
      this.monitoringService.createMoodEntry(request).subscribe({
        next: () => {
          this.handleSuccessfulSubmit(submittedMoodScore);
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Failed to create mood entry';
          this.saving = false;
        }
      });
    }
  }

  onCancel(): void {
    this.router.navigate(['/monitoring']);
  }

  getConditionChars(): number {
    return this.moodForm.get('moodDescription')?.value?.length || 0;
  }

  getTriggersChars(): number {
    return this.moodForm.get('triggers')?.value?.length || 0;
  }

  private setupQuotePrefetch(): void {
    const moodScoreControl = this.moodForm.get('moodScore');
    if (!moodScoreControl || !this.isPatient) {
      return;
    }

    this.prefetchQuoteForScore(Number(moodScoreControl.value ?? 0));
    this.moodScoreSub = moodScoreControl.valueChanges.subscribe((value) => {
      this.prefetchQuoteForScore(Number(value ?? 0));
    });
  }

  private prefetchQuoteForScore(moodScore: number): void {
    if (moodScore <= 5) {
      this.prefetchQuote();
    }
  }

  private prefetchQuote(forceRefresh = false): void {
    if (forceRefresh) {
      this.prefetchedQuote = null;
    }

    if (!forceRefresh && (this.prefetchedQuote || this.quotePrefetchInFlight)) {
      return;
    }

    this.quotePrefetchInFlight = true;
    this.quotePrefetchSub?.unsubscribe();
    this.quotePrefetchSub = this.zenQuotesService.getRandomQuote().subscribe({
      next: (quote) => {
        this.prefetchedQuote = quote;
        this.quotePrefetchInFlight = false;
        if (this.showQuoteOverlay) {
          this.currentQuote = quote;
          this.quoteLoading = false;
          this.startRedirectCountdown();
        }
      },
      error: () => {
        this.prefetchedQuote = this.getRandomFallbackQuote();
        this.quotePrefetchInFlight = false;
        if (this.showQuoteOverlay) {
          this.currentQuote = this.prefetchedQuote;
          this.quoteLoading = false;
          this.startRedirectCountdown();
        }
      }
    });
  }

  private handleSuccessfulSubmit(moodScore: number): void {
    this.saving = false;

    if (!this.isPatient || moodScore > 5) {
      this.navigateToMoodList();
      return;
    }

    this.showQuoteOverlay = true;
    this.redirectCountdownStarted = false;
    this.currentQuote = this.prefetchedQuote;
    this.quoteLoading = !this.currentQuote;

    if (this.currentQuote) {
      this.startRedirectCountdown();
      return;
    }

    if (!this.currentQuote) {
      this.prefetchQuote();
    }

    if (this.quoteResolveTimer) {
      clearTimeout(this.quoteResolveTimer);
    }
    this.quoteResolveTimer = setTimeout(() => {
      if (!this.currentQuote) {
        this.currentQuote = this.getRandomFallbackQuote();
        this.quoteLoading = false;
      }
      this.startRedirectCountdown();
    }, this.quoteResolveTimeoutMs);
  }

  private getRandomFallbackQuote(): MotivationalQuote {
    const randomIndex = Math.floor(Math.random() * this.fallbackQuotes.length);
    return this.fallbackQuotes[randomIndex];
  }

  private startRedirectCountdown(): void {
    if (this.redirectCountdownStarted) {
      return;
    }
    this.redirectCountdownStarted = true;

    if (this.quoteResolveTimer) {
      clearTimeout(this.quoteResolveTimer);
      this.quoteResolveTimer = null;
    }
    if (this.redirectTimer) {
      clearTimeout(this.redirectTimer);
    }
    this.redirectTimer = setTimeout(() => this.navigateToMoodList(), this.overlayRedirectDelayMs);
  }

  private navigateToMoodList(): void {
    this.router.navigate(['/monitoring']);
  }
}
