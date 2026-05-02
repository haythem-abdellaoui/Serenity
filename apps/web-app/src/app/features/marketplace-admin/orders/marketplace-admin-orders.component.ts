import { Component, OnInit } from '@angular/core';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import { MarketplaceOrder, MarketplaceOrderStatus } from '../../../shared/models/marketplace.model';

@Component({
  selector: 'app-marketplace-admin-orders',
  templateUrl: './marketplace-admin-orders.component.html',
  styleUrls: ['./marketplace-admin-orders.component.scss']
})
export class MarketplaceAdminOrdersComponent implements OnInit {
  loading = false;
  orders: MarketplaceOrder[] = [];
  error = '';
  successMessage = '';
  expandedOrderId: number | null = null;
  deletingOrderId: number | null = null;

  readonly statuses: MarketplaceOrderStatus[] = ['CREATED', 'PAID', 'CANCELLED'];

  constructor(private readonly marketplaceService: MarketplaceService) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading = true;
    this.error = '';
    this.successMessage = '';

    this.marketplaceService.getAllOrdersForAdmin().subscribe({
      next: orders => {
        this.orders = orders;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load orders.';
        this.loading = false;
      }
    });
  }

  toggleExpandOrder(orderId: number): void {
    this.expandedOrderId = this.expandedOrderId === orderId ? null : orderId;
  }

  updateStatus(orderId: number, status: MarketplaceOrderStatus): void {
    this.marketplaceService.updateOrderStatus(orderId, status).subscribe({
      next: updatedOrder => {
        this.orders = this.orders.map(order => order.id === updatedOrder.id ? updatedOrder : order);
        this.successMessage = `Order #${orderId} status updated to ${status}`;
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: () => {
        this.error = 'Failed to update order status.';
      }
    });
  }

  deleteOrder(orderId: number): void {
    if (!confirm('Are you sure you want to delete this order? This action cannot be undone.')) {
      return;
    }

    this.deletingOrderId = orderId;
    this.marketplaceService.cancelOrderForAdmin(orderId).subscribe({
      next: () => {
        this.orders = this.orders.filter(order => order.id !== orderId);
        this.deletingOrderId = null;
        this.successMessage = `Order #${orderId} deleted successfully`;
        this.expandedOrderId = null;
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: () => {
        this.error = 'Failed to delete order.';
        this.deletingOrderId = null;
      }
    });
  }

  getStatusClass(status: MarketplaceOrderStatus): string {
    switch (status) {
      case 'PAID':
        return 'status-paid';
      case 'CREATED':
        return 'status-created';
      case 'CANCELLED':
        return 'status-cancelled';
      default:
        return '';
    }
  }

  getTotalItems(order: MarketplaceOrder): number {
    return order.items?.reduce((sum, item) => sum + (item.quantity || 0), 0) || 0;
  }
}
