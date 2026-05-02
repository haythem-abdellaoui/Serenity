import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import {
  MARKETPLACE_CATEGORIES,
  MARKETPLACE_TYPES,
  MarketplaceProduct,
  MarketplaceProductCategory,
  MarketplaceProductType,
  ProductRecommendationItem
} from '../../../shared/models/marketplace.model';

@Component({
  selector: 'app-product-list',
  templateUrl: './product-list.component.html',
  styleUrls: ['./product-list.component.scss']
})
export class ProductListComponent implements OnInit, OnDestroy {
  products: MarketplaceProduct[] = [];
  loading = false;
  query = '';
  selectedCategory: MarketplaceProductCategory | '' = '';
  selectedType: MarketplaceProductType | '' = '';
  resetAnimating = false;
  showQuiz = false;
  quizSubmitting = false;
  quizError = '';
  quizReasoning = '';
  quizRecommendations: ProductRecommendationItem[] = [];

  anxietyLevel = 3;
  stressLevel = 3;
  sleepNeed = 3;

  private searchDebounceId: number | null = null;
  private readonly searchDebounceMs = 220;

  readonly categories = MARKETPLACE_CATEGORIES;
  readonly types = MARKETPLACE_TYPES;

  constructor(
    private readonly marketplaceService: MarketplaceService,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  ngOnDestroy(): void {
    if (this.searchDebounceId !== null) {
      window.clearTimeout(this.searchDebounceId);
      this.searchDebounceId = null;
    }
  }

  loadProducts(): void {
    this.loading = true;
    this.marketplaceService.getProducts({
      query: this.query,
      category: this.selectedCategory,
      type: this.selectedType
    }).subscribe({
      next: products => {
        this.products = products;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  clearFilters(): void {
    this.query = '';
    this.selectedCategory = '';
    this.selectedType = '';
    this.loadProducts();
  }

  clearFiltersSmooth(): void {
    this.resetAnimating = true;
    this.clearFilters();
    window.setTimeout(() => {
      this.resetAnimating = false;
    }, 320);
  }

  onQueryInputChange(): void {
    if (this.searchDebounceId !== null) {
      window.clearTimeout(this.searchDebounceId);
    }

    this.searchDebounceId = window.setTimeout(() => {
      this.loadProducts();
    }, this.searchDebounceMs);
  }

  toggleQuiz(): void {
    this.showQuiz = !this.showQuiz;
    this.quizError = '';
  }

  submitQuiz(): void {
    this.quizSubmitting = true;
    this.quizError = '';
    this.quizReasoning = '';

    this.marketplaceService.getQuizRecommendations({
      anxietyLevel: this.anxietyLevel,
      stressLevel: this.stressLevel,
      sleepNeed: this.sleepNeed
    }).subscribe({
      next: response => {
        this.quizRecommendations = response.recommendations || [];
        this.quizReasoning = response.reasoning || '';
        this.quizSubmitting = false;
      },
      error: err => {
        this.quizError = err?.error?.message || 'Unable to generate recommendations right now.';
        this.quizSubmitting = false;
      }
    });
  }

  resetQuiz(): void {
    this.anxietyLevel = 3;
    this.stressLevel = 3;
    this.sleepNeed = 3;
    this.quizReasoning = '';
    this.quizError = '';
    this.quizRecommendations = [];
  }

  openDetails(productId: number): void {
    this.router.navigate(['/marketplace/product', productId]);
  }

  addToCart(product: MarketplaceProduct): void {
    this.marketplaceService.addToCart(product, 1);
    this.router.navigate(['/marketplace/cart']);
  }

  isManager(): boolean {
    return this.authService.hasRole('MARKETPLACE_MANAGER') || this.authService.isAdmin();
  }

  isDigital(product: MarketplaceProduct): boolean {
    return product.type === 'DIGITAL';
  }

  canUseCart(product: MarketplaceProduct): boolean {
    return this.marketplaceService.isCartEligible(product);
  }

  get hasResults(): boolean {
    return this.products.length > 0;
  }
}
