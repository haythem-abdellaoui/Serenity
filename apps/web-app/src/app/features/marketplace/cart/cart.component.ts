import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import { CartItem } from '../../../shared/models/marketplace.model';

@Component({
  selector: 'app-cart',
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.scss']
})
export class CartComponent {
  readonly cart$: Observable<CartItem[]> = this.marketplaceService.cart$;
  emptyCartNotice = '';

  constructor(
    private readonly marketplaceService: MarketplaceService,
    private readonly router: Router
  ) {}

  updateQuantity(productId: number, quantityText: string): void {
    const quantity = Number(quantityText);
    if (!Number.isFinite(quantity)) {
      return;
    }
    this.marketplaceService.updateCartQuantity(productId, quantity);
  }

  removeItem(productId: number): void {
    this.marketplaceService.removeFromCart(productId);
  }

  getTotal(): number {
    return this.marketplaceService.getCartTotal();
  }

  checkout(): void {
    if (this.marketplaceService.getCartSnapshot().length === 0) {
      this.emptyCartNotice = 'Your cart is empty. Add at least one item before checkout.';
      return;
    }
    this.router.navigate(['/marketplace/checkout']);
  }

  dismissNotice(): void {
    this.emptyCartNotice = '';
  }
}
