import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CartItem,
  CheckoutRequest,
  MarketplaceOrder,
  MarketplaceProduct,
  MarketplaceProductCategory,
  MarketplaceProductType,
  MarketplaceProductUpsertRequest,
  MarketplaceOrderStatus,
  OrderStatusUpdateRequest,
  QuizRecommendationRequest,
  RecommendationResponse
} from '../../shared/models/marketplace.model';

@Injectable({
  providedIn: 'root'
})
export class MarketplaceService {

  private static readonly UNLOCKED_ARTICLES_KEY = 'unlocked_article_ids';
  private readonly API_URL = `${environment.marketplaceServiceApiUrl}/api/articles`;
  private readonly cartSubject = new BehaviorSubject<CartItem[]>([]);
  readonly cart$ = this.cartSubject.asObservable();

  constructor(private readonly http: HttpClient) {}

  getProducts(filters?: {
    query?: string;
    category?: MarketplaceProductCategory | '';
    type?: MarketplaceProductType | '';
  }): Observable<MarketplaceProduct[]> {
    let params = new HttpParams();

    if (filters?.query) {
      params = params.set('query', filters.query);
    }
    if (filters?.category) {
      params = params.set('category', filters.category);
    }
    if (filters?.type) {
      params = params.set('type', filters.type);
    }

    return this.http.get<MarketplaceProduct[]>(`${this.API_URL}/products`, { params });
  }

  getProductById(id: number): Observable<MarketplaceProduct> {
    return this.http.get<MarketplaceProduct>(`${this.API_URL}/products/${id}`);
  }

  getAllProductsForAdmin(): Observable<MarketplaceProduct[]> {
    return this.http.get<MarketplaceProduct[]>(`${this.API_URL}/products/admin/all`);
  }

  createProduct(request: MarketplaceProductUpsertRequest): Observable<MarketplaceProduct> {
    return this.http.post<MarketplaceProduct>(`${this.API_URL}/products`, request);
  }

  updateProduct(id: number, request: MarketplaceProductUpsertRequest): Observable<MarketplaceProduct> {
    return this.http.put<MarketplaceProduct>(`${this.API_URL}/products/${id}`, request);
  }

  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/products/${id}`);
  }

  getMyOrders(): Observable<MarketplaceOrder[]> {
    return this.http.get<MarketplaceOrder[]>(`${this.API_URL}/orders/me`);
  }

  getAllOrdersForAdmin(): Observable<MarketplaceOrder[]> {
    return this.http.get<MarketplaceOrder[]>(`${this.API_URL}/orders`);
  }

  getOrderByIdForAdmin(orderId: number): Observable<MarketplaceOrder> {
    return this.http.get<MarketplaceOrder>(`${this.API_URL}/orders/${orderId}`);
  }

  unlockArticle(product: MarketplaceProduct): Observable<MarketplaceOrder> {
    const request: CheckoutRequest = {
      items: [{ productId: product.id, quantity: 1 }],
      shippingAddress: 'Digital delivery',
      customerNote: `Unlock article: ${product.name}`
    };

    return this.http.post<MarketplaceOrder>(`${this.API_URL}/orders/checkout`, request);
  }

  isArticleUnlocked(productId: number): boolean {
    return this.getUnlockedArticleIds().includes(productId);
  }

  markArticleUnlocked(productId: number): void {
    const current = new Set(this.getUnlockedArticleIds());
    current.add(productId);
    localStorage.setItem(
      MarketplaceService.UNLOCKED_ARTICLES_KEY,
      JSON.stringify(Array.from(current))
    );
  }

  getUnlockedArticleIds(): number[] {
    const raw = localStorage.getItem(MarketplaceService.UNLOCKED_ARTICLES_KEY);
    if (!raw) {
      return [];
    }

    try {
      const parsed = JSON.parse(raw);
      if (!Array.isArray(parsed)) {
        return [];
      }
      return parsed
        .map((id) => Number(id))
        .filter((id) => Number.isFinite(id));
    } catch {
      return [];
    }
  }

  updateOrderStatus(orderId: number, status: MarketplaceOrderStatus): Observable<MarketplaceOrder> {
    const request: OrderStatusUpdateRequest = { status };
    return this.http.patch<MarketplaceOrder>(`${this.API_URL}/orders/${orderId}/status`, request);
  }

  cancelOrderForAdmin(orderId: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/orders/${orderId}`);
  }

  checkout(shippingAddress: string, customerNote?: string): Observable<MarketplaceOrder> {
    const request: CheckoutRequest = {
      items: this.cartSubject.value.map(item => ({
        productId: item.product.id,
        quantity: item.quantity
      })),
      shippingAddress,
      customerNote
    };

    return this.http.post<MarketplaceOrder>(`${this.API_URL}/orders/checkout`, request).pipe(
      tap(() => {
        this.clearCart();
      })
    );
  }

  addToCart(product: MarketplaceProduct, quantity = 1): void {
    if (!this.isCartEligible(product)) {
      return;
    }

    const current = [...this.cartSubject.value];
    const existing = current.find(item => item.product.id === product.id);

    if (existing) {
      existing.quantity += quantity;
    } else {
      current.push({ product, quantity });
    }

    this.cartSubject.next(current);
  }

  isCartEligible(product: MarketplaceProduct): boolean {
    return product.type === 'PHYSICAL' || !product.previewable;
  }

  updateCartQuantity(productId: number, quantity: number): void {
    const current = this.cartSubject.value
      .map(item => item.product.id === productId ? { ...item, quantity } : item)
      .filter(item => item.quantity > 0);
    this.cartSubject.next(current);
  }

  removeFromCart(productId: number): void {
    this.cartSubject.next(this.cartSubject.value.filter(item => item.product.id !== productId));
  }

  clearCart(): void {
    this.cartSubject.next([]);
  }

  getCartSnapshot(): CartItem[] {
    return this.cartSubject.value;
  }

  getCartTotal(): number {
    return this.cartSubject.value.reduce(
      (sum, item) => sum + (item.product.price * item.quantity),
      0
    );
  }

  // ===== WISHLIST OPERATIONS =====
  addToWishlist(productId: number): Observable<any> {
    return this.http.post(`${this.API_URL}/wishlist/${productId}`, {});
  }

  removeFromWishlist(productId: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/wishlist/${productId}`);
  }

  getUserWishlist(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/wishlist/me`);
  }

  isProductInWishlist(productId: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.API_URL}/wishlist/check/${productId}`);
  }

  // ===== REVIEWS OPERATIONS =====
  createOrUpdateReview(productId: number, rating: number, reviewText: string): Observable<any> {
    const request = { productId, rating, reviewText };
    return this.http.post(`${this.API_URL}/reviews`, request);
  }

  getProductReviews(productId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/reviews/product/${productId}`);
  }

  getAverageRating(productId: number): Observable<number> {
    return this.http.get<number>(`${this.API_URL}/reviews/product/${productId}/average`);
  }

  getQuizRecommendations(request: QuizRecommendationRequest): Observable<RecommendationResponse> {
    return this.http.post<RecommendationResponse>(`${this.API_URL}/recommendations/quiz`, request);
  }

  getUserReviews(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/reviews/me`);
  }

  deleteReview(reviewId: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/reviews/${reviewId}`);
  }

}
