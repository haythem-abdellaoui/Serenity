import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import { MarketplaceOrder, MarketplaceOrderStatus } from '../../../shared/models/marketplace.model';

@Component({
  selector: 'app-order-history',
  templateUrl: './order-history.component.html',
  styleUrls: ['./order-history.component.scss']
})
export class OrderHistoryComponent implements OnInit {
  loading = false;
  orders: MarketplaceOrder[] = [];
  errorMessage = '';
  expandedOrderId: number | null = null;
  selectedFilter: 'ALL' | MarketplaceOrderStatus = 'ALL';

  readonly filters: Array<'ALL' | MarketplaceOrderStatus> = ['ALL', 'PAID', 'CREATED', 'CANCELLED'];

  constructor(
    private readonly marketplaceService: MarketplaceService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loading = true;
    this.marketplaceService.getMyOrders().subscribe({
      next: orders => {
        this.orders = [...orders].sort((a, b) =>
          new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        );
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Unable to load order history right now. Please try again.';
        this.loading = false;
      }
    });
  }

  get filteredOrders(): MarketplaceOrder[] {
    if (this.selectedFilter === 'ALL') {
      return this.orders;
    }
    return this.orders.filter(order => order.status === this.selectedFilter);
  }

  get totalSpent(): number {
    return this.orders
      .filter(order => order.status === 'PAID')
      .reduce((sum, order) => sum + order.totalAmount, 0);
  }

  get latestOrderDate(): string | null {
    if (this.orders.length === 0) {
      return null;
    }
    return this.orders[0].createdAt;
  }

  setFilter(filter: 'ALL' | MarketplaceOrderStatus): void {
    this.selectedFilter = filter;
  }

  toggleOrder(orderId: number): void {
    this.expandedOrderId = this.expandedOrderId === orderId ? null : orderId;
  }

  openOrder(orderId: number, event?: Event): void {
    event?.stopPropagation();
    this.expandedOrderId = orderId;
  }

  collapseOrder(orderId: number, event?: Event): void {
    event?.stopPropagation();
    if (this.expandedOrderId === orderId) {
      this.expandedOrderId = null;
    }
  }

  statusTone(status: MarketplaceOrderStatus): 'success' | 'pending' | 'cancelled' {
    if (status === 'PAID') {
      return 'success';
    }
    if (status === 'CANCELLED') {
      return 'cancelled';
    }
    return 'pending';
  }

  openArticle(productId: number): void {
    this.router.navigate(['/marketplace/product', productId]);
  }
}
