import { Component, OnInit } from '@angular/core';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import { AuthService } from '../../../core/services/auth.service';
import { Router } from '@angular/router';
import { MarketplaceProduct } from '../../../shared/models/marketplace.model';

interface WishlistItemView {
  productId: number;
  productName: string;
  productPrice: number;
  productImageUrl?: string;
  productType?: 'PHYSICAL' | 'DIGITAL';
  productPreviewable?: boolean;
  addedAt: string;
}

@Component({
  selector: 'app-wishlist',
  templateUrl: './wishlist.component.html',
  styleUrls: ['./wishlist.component.scss']
})
export class WishlistComponent implements OnInit {
  wishlistItems: WishlistItemView[] = [];
  loading = false;
  errorMessage = '';
  userId: number | null = null;

  constructor(
    private marketplaceService: MarketplaceService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.userId = this.authService.getUserId();
    if (this.userId) {
      this.loadWishlist();
    }
  }

  loadWishlist(): void {
    if (!this.userId) return;
    
    this.loading = true;
    this.errorMessage = '';
    this.marketplaceService.getUserWishlist().subscribe({
      next: (items) => {
        this.wishlistItems = items;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load wishlist';
        this.loading = false;
      }
    });
  }

  removeFromWishlist(productId: number): void {
    if (!this.userId) return;

    this.marketplaceService.removeFromWishlist(productId).subscribe({
      next: () => {
        this.wishlistItems = this.wishlistItems.filter(item => item.productId !== productId);
      },
      error: () => {
        this.errorMessage = 'Failed to remove item from wishlist';
      }
    });
  }

  viewProduct(productId: number): void {
    this.router.navigate(['/marketplace/product', productId]);
  }

  addToCart(productId: number): void {
    const item = this.wishlistItems.find(w => w.productId === productId);
    if (item) {
      const product: MarketplaceProduct = {
        id: item.productId,
        name: item.productName,
        description: 'Saved wishlist item',
        category: 'SELF_CARE',
        type: item.productType ?? 'PHYSICAL',
        price: item.productPrice,
        active: true,
        imageUrl: item.productImageUrl,
        previewable: Boolean(item.productPreviewable),
        previewType: undefined,
        previewUrl: undefined,
        contentUrl: undefined
      };

      if (!this.marketplaceService.isCartEligible(product)) {
        this.viewProduct(productId);
        return;
      }

      this.marketplaceService.addToCart(product);
      this.router.navigate(['/marketplace/cart']);
    }
  }

  canAddToCart(item: WishlistItemView): boolean {
    const type = item.productType ?? 'PHYSICAL';
    return type === 'PHYSICAL' || !item.productPreviewable;
  }

  goToMarketplace(): void {
    this.router.navigate(['/marketplace']);
  }
}
