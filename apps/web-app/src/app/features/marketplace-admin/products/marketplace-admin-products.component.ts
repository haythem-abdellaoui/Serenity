import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import { MarketplaceProduct } from '../../../shared/models/marketplace.model';

@Component({
  selector: 'app-marketplace-admin-products',
  templateUrl: './marketplace-admin-products.component.html',
  styleUrls: ['./marketplace-admin-products.component.scss']
})
export class MarketplaceAdminProductsComponent implements OnInit {
  loading = false;
  products: MarketplaceProduct[] = [];
  error = '';
  successMessage = '';
  deletingProductId: number | null = null;

  constructor(
    private readonly marketplaceService: MarketplaceService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading = true;
    this.error = '';
    this.successMessage = '';

    this.marketplaceService.getAllProductsForAdmin().subscribe({
      next: products => {
        this.products = products;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load products.';
        this.loading = false;
      }
    });
  }

  createProduct(): void {
    this.router.navigate(['/admin/marketplace/products/new']);
  }

  editProduct(productId: number): void {
    this.router.navigate(['/admin/marketplace/products', productId, 'edit']);
  }

  deleteProduct(productId: number): void {
    const product = this.products.find(p => p.id === productId);
    if (!confirm(`Are you sure you want to delete "${product?.name}"? This action cannot be undone.`)) {
      return;
    }

    this.deletingProductId = productId;
    this.marketplaceService.deleteProduct(productId).subscribe({
      next: () => {
        this.products = this.products.filter(p => p.id !== productId);
        this.deletingProductId = null;
        this.successMessage = `Product deleted successfully`;
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (error) => {
        this.deletingProductId = null;
        
        // Provide detailed error message based on status
        if (error.status === 403) {
          this.error = 'You do not have permission to delete products. Only administrators can delete products.';
        } else if (error.status === 404) {
          this.error = 'Product not found. It may have been deleted already.';
        } else if (error.status === 0) {
          this.error = 'Network error. Please check your connection and try again.';
        } else {
          this.error = error.error?.message || 'Failed to delete product.';
        }
      }
    });
  }

  toggleStatus(productId: number): void {
    const product = this.products.find(p => p.id === productId);
    if (!product) return;

    const updatedProduct = { ...product, active: !product.active };
    this.marketplaceService.updateProduct(productId, {
      name: updatedProduct.name,
      description: updatedProduct.description,
      price: updatedProduct.price,
      category: updatedProduct.category,
      type: updatedProduct.type,
      active: updatedProduct.active,
      imageUrl: updatedProduct.imageUrl,
      previewable: updatedProduct.previewable,
      previewType: updatedProduct.previewType,
      previewUrl: updatedProduct.previewUrl,
      contentUrl: updatedProduct.contentUrl
    }).subscribe({
      next: (updated) => {
        const idx = this.products.findIndex(p => p.id === productId);
        if (idx !== -1) {
          this.products[idx] = updated;
        }
        this.successMessage = `Product ${updated.active ? 'activated' : 'deactivated'}`;
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: () => {
        this.error = 'Failed to update product status.';
      }
    });
  }
}
