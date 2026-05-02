import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import { AuthService } from '../../../core/services/auth.service';
import { MarketplaceProduct } from '../../../shared/models/marketplace.model';

type ArticleExperienceType = 'PODCAST' | 'BOOK' | 'EXERCISE' | 'VIDEO';

@Component({
  selector: 'app-product-detail',
  templateUrl: './product-detail.component.html',
  styleUrls: ['./product-detail.component.scss']
})
export class ProductDetailComponent implements OnInit {
  @ViewChild('previewAudio') previewAudio?: ElementRef<HTMLAudioElement>;
  @ViewChild('previewVideo') previewVideo?: ElementRef<HTMLVideoElement>;

  private static readonly AUDIO_PREVIEW_LIMIT_SECONDS = 90;
  private static readonly VIDEO_PREVIEW_LIMIT_SECONDS = 30;

  product: MarketplaceProduct | null = null;
  loading = false;
  quantity = 1;
  inWishlist = false;
  userId: number | null = null;
  showPaywall = false;
  paywallReason = 'Preview finished. Unlock to continue with the full content.';
  unlocking = false;
  unlockError = '';
  showMockPayment = false;
  mockPaymentProcessing = false;
  mediaError = '';

  bookPreviewPages: string[] = [];
  currentBookPage = 0;
  exerciseSteps: string[] = [];
  unlockedExerciseSteps = 2;
  completedSteps: boolean[] = [];

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly marketplaceService: MarketplaceService,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.userId = this.authService.getUserId();
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/marketplace']);
      return;
    }

    this.loading = true;
    this.marketplaceService.getProductById(id).subscribe({
      next: product => {
        this.product = product;
        this.bootstrapPreviewContent(product);
        this.loading = false;
        this.checkWishlistStatus(id);
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  private bootstrapPreviewContent(product: MarketplaceProduct): void {
    this.bookPreviewPages = this.buildBookPreviewPages(product);
    this.exerciseSteps = this.buildExerciseSteps(product);
    this.completedSteps = this.exerciseSteps.map(() => false);
    this.currentBookPage = 0;
  }

  checkWishlistStatus(productId: number): void {
    if (!this.userId) return;
    
    this.marketplaceService.isProductInWishlist(productId).subscribe({
      next: (inWishlist) => {
        this.inWishlist = inWishlist;
      }
    });
  }

  get isUnlocked(): boolean {
    return this.product ? this.marketplaceService.isArticleUnlocked(this.product.id) : false;
  }

  get isDigitalArticle(): boolean {
    return this.product?.type === 'DIGITAL';
  }

  get usesPreviewFlow(): boolean {
    return this.isDigitalArticle && Boolean(this.product?.previewable);
  }

  get experienceType(): ArticleExperienceType {
    if (this.product?.previewType === 'VIDEO') {
      return 'VIDEO';
    }
    if (this.product?.previewType === 'AUDIO') {
      return 'PODCAST';
    }
    if (this.product?.previewType === 'BOOK') {
      return 'BOOK';
    }

    const source = `${this.product?.name ?? ''} ${this.product?.description ?? ''}`.toLowerCase();
    if (source.includes('exercise') || source.includes('routine') || source.includes('breathing')) {
      return 'EXERCISE';
    }
    return 'BOOK';
  }

  get previewProgressPercent(): number {
    if (this.experienceType === 'VIDEO') {
      const video = this.previewVideo?.nativeElement;
      if (!video) {
        return 0;
      }
      return Math.min((video.currentTime / ProductDetailComponent.VIDEO_PREVIEW_LIMIT_SECONDS) * 100, 100);
    }

    const audio = this.previewAudio?.nativeElement;
    if (!audio) {
      return 0;
    }
    return Math.min((audio.currentTime / ProductDetailComponent.AUDIO_PREVIEW_LIMIT_SECONDS) * 100, 100);
  }

  get currentBookPreviewText(): string {
    if (this.bookPreviewPages.length === 0) {
      return 'Preview is being prepared. Please check back in a moment.';
    }
    return this.bookPreviewPages[this.currentBookPage] ?? this.bookPreviewPages[0];
  }

  get hasNextBookPage(): boolean {
    return this.currentBookPage < this.bookPreviewPages.length - 1;
  }

  get previewLimitSeconds(): number {
    return this.experienceType === 'VIDEO'
      ? ProductDetailComponent.VIDEO_PREVIEW_LIMIT_SECONDS
      : ProductDetailComponent.AUDIO_PREVIEW_LIMIT_SECONDS;
  }

  openPreview(): void {
    if (!this.product) {
      return;
    }
    if (!this.usesPreviewFlow || this.isUnlocked) {
      return;
    }
    this.mediaError = '';

    if (this.experienceType === 'PODCAST') {
      const audio = this.previewAudio?.nativeElement;
      if (audio && audio.currentTime >= ProductDetailComponent.AUDIO_PREVIEW_LIMIT_SECONDS) {
        audio.currentTime = 0;
      }
      audio?.play();
      return;
    }

    if (this.experienceType === 'VIDEO') {
      const video = this.previewVideo?.nativeElement;
      if (video && video.currentTime >= ProductDetailComponent.VIDEO_PREVIEW_LIMIT_SECONDS) {
        video.currentTime = 0;
      }
      video?.play();
    }
  }

  onAudioPlay(): void {
    if (!this.usesPreviewFlow) {
      return;
    }
  }

  onAudioTimeUpdate(): void {
    if (!this.usesPreviewFlow || this.isUnlocked) {
      return;
    }

    const audio = this.previewAudio?.nativeElement;
    if (!audio) {
      return;
    }

    if (audio.currentTime >= ProductDetailComponent.AUDIO_PREVIEW_LIMIT_SECONDS) {
      audio.currentTime = ProductDetailComponent.AUDIO_PREVIEW_LIMIT_SECONDS;
      this.pauseAudio();
      this.openPaywall('Your preview ended. Unlock for the full podcast session.');
    }
  }

  onAudioSeeking(): void {
    if (!this.usesPreviewFlow || this.isUnlocked) {
      return;
    }
    const audio = this.previewAudio?.nativeElement;
    if (!audio) {
      return;
    }
    if (audio.currentTime > ProductDetailComponent.AUDIO_PREVIEW_LIMIT_SECONDS) {
      audio.currentTime = ProductDetailComponent.AUDIO_PREVIEW_LIMIT_SECONDS;
      this.pauseAudio();
      this.openPaywall('Preview limit reached. Unlock to continue listening.');
    }
  }

  onAudioEnded(): void {
    if (!this.usesPreviewFlow || this.isUnlocked) {
      return;
    }
    this.openPaywall('Preview completed. Unlock to continue listening.');
  }

  onAudioError(): void {
    this.mediaError = 'Unable to load this audio preview right now. Please try again in a moment.';
  }

  onVideoTimeUpdate(): void {
    if (!this.usesPreviewFlow || this.isUnlocked) {
      return;
    }

    const video = this.previewVideo?.nativeElement;
    if (!video) {
      return;
    }

    if (video.currentTime >= ProductDetailComponent.VIDEO_PREVIEW_LIMIT_SECONDS) {
      video.currentTime = ProductDetailComponent.VIDEO_PREVIEW_LIMIT_SECONDS;
      video.pause();
      this.openPaywall('Your video preview ended. Unlock to continue watching.');
    }
  }

  onVideoSeeking(): void {
    if (!this.usesPreviewFlow || this.isUnlocked) {
      return;
    }

    const video = this.previewVideo?.nativeElement;
    if (!video) {
      return;
    }

    if (video.currentTime > ProductDetailComponent.VIDEO_PREVIEW_LIMIT_SECONDS) {
      video.currentTime = ProductDetailComponent.VIDEO_PREVIEW_LIMIT_SECONDS;
      video.pause();
      this.openPaywall('Preview limit reached. Unlock to continue watching.');
    }
  }

  onVideoEnded(): void {
    if (!this.usesPreviewFlow || this.isUnlocked) {
      return;
    }
    this.openPaywall('Video preview completed. Unlock to continue watching.');
  }

  onVideoError(): void {
    this.mediaError = 'Unable to load this video preview right now. Please try again in a moment.';
  }

  nextBookPage(): void {
    if (!this.usesPreviewFlow) {
      return;
    }

    if (this.isUnlocked) {
      if (this.hasNextBookPage) {
        this.currentBookPage += 1;
      }
      return;
    }

    if (this.hasNextBookPage) {
      this.currentBookPage += 1;
      return;
    }

    this.openPaywall('Preview pages completed. Unlock to continue reading.');
  }

  previousBookPage(): void {
    if (this.currentBookPage > 0) {
      this.currentBookPage -= 1;
    }
  }

  toggleExerciseStep(index: number): void {
    if (!this.usesPreviewFlow) {
      return;
    }

    if (this.isUnlocked || index < this.unlockedExerciseSteps) {
      this.completedSteps[index] = !this.completedSteps[index];
      return;
    }

    this.openPaywall('You completed the free exercise preview. Unlock to continue.');
  }

  requestUnlock(): void {
    if (!this.product) {
      return;
    }
    if (!this.usesPreviewFlow) {
      return;
    }
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/auth/login']);
      return;
    }

    this.showMockPayment = true;
    this.unlockError = '';
  }

  closeMockPayment(): void {
    if (this.mockPaymentProcessing) {
      return;
    }
    this.showMockPayment = false;
  }

  confirmMockPayment(): void {
    if (!this.product || this.mockPaymentProcessing || this.unlocking) {
      return;
    }

    this.mockPaymentProcessing = true;
    window.setTimeout(() => {
      this.mockPaymentProcessing = false;
      this.unlockNow();
    }, 900);
  }

  private unlockNow(): void {
    if (!this.product) {
      return;
    }

    this.unlocking = true;
    this.unlockError = '';
    this.marketplaceService.unlockArticle(this.product).subscribe({
      next: () => {
        this.marketplaceService.markArticleUnlocked(this.product!.id);
        this.unlocking = false;
        this.showPaywall = false;
        this.showMockPayment = false;
      },
      error: () => {
        this.unlocking = false;
        this.showMockPayment = false;
        this.unlockError = 'Unable to unlock right now. Please try again in a moment.';
      }
    });
  }

  closePaywall(): void {
    this.showPaywall = false;
    this.unlockError = '';
    this.resetPreviewState();
  }

  private openPaywall(reason: string): void {
    this.paywallReason = reason;
    this.showPaywall = true;
  }

  restartPreview(): void {
    this.resetPreviewState();
    this.openPreview();
  }

  private resetPreviewState(): void {
    this.pauseAudio();
    const audio = this.previewAudio?.nativeElement;
    if (audio) {
      audio.currentTime = 0;
    }

    const video = this.previewVideo?.nativeElement;
    if (video) {
      video.pause();
      video.currentTime = 0;
    }

    this.currentBookPage = 0;
    this.completedSteps = this.exerciseSteps.map(() => false);
    this.mediaError = '';
  }

  private pauseAudio(): void {
    const audio = this.previewAudio?.nativeElement;
    if (audio) {
      audio.pause();
    }
  }

  addToCart(): void {
    if (!this.product || !this.marketplaceService.isCartEligible(this.product)) {
      return;
    }

    this.marketplaceService.addToCart(this.product, this.quantity);
    this.router.navigate(['/marketplace/cart']);
  }

  getPodcastPreviewUrl(productId: number): string {
    if (this.product?.previewUrl) {
      return this.product.previewUrl;
    }

    const options = [
      'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3',
      'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3',
      'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3'
    ];
    return options[productId % options.length];
  }

  getVideoPreviewUrl(productId: number): string {
    if (this.product?.previewUrl) {
      return this.product.previewUrl;
    }

    const options = [
      'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
      'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4'
    ];
    return options[productId % options.length];
  }

  private buildBookPreviewPages(product: MarketplaceProduct): string[] {
    const base = product.description?.trim() || 'This article shares practical techniques to support emotional balance.';
    return [
      `${base} This opening preview helps you understand the tone and approach before unlocking full access.`,
      'In the full version, you will receive guided structure, reflective prompts, and calm step-by-step progression.'
    ];
  }

  private buildExerciseSteps(product: MarketplaceProduct): string[] {
    return [
      'Pause for 30 seconds and notice your breathing without judgment.',
      `Write one short intention inspired by \"${product.name}\".`,
      'Complete a full 5-minute guided routine (locked in preview mode).',
      'Record how your mood shifted after the full routine (locked in preview mode).'
    ];
  }

  toggleWishlist(): void {
    if (!this.product || !this.userId) {
      this.router.navigate(['/auth/login']);
      return;
    }

    if (this.inWishlist) {
      this.marketplaceService.removeFromWishlist(this.product.id).subscribe({
        next: () => {
          this.inWishlist = false;
        }
      });
    } else {
      this.marketplaceService.addToWishlist(this.product.id).subscribe({
        next: () => {
          this.inWishlist = true;
        }
      });
    }
  }
}
