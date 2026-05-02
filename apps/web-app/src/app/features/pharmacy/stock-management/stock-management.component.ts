import { Component, OnInit } from '@angular/core';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import { StockItemResponse } from '../../../shared/models/pharmacy.model';

@Component({
  selector: 'app-stock-management',
  templateUrl: './stock-management.component.html',
  styleUrls: ['./stock-management.component.scss']
})
export class StockManagementComponent implements OnInit {
  loading = true;
  errorMessage = '';
  successMessage = '';

  query = '';
  includeArchived = false;
  incrementValue: Record<number, number> = {};
  selectedImageItem: StockItemResponse | null = null;
  renamingItemId: number | null = null;
  renameDraft = '';

  items: StockItemResponse[] = [];

  constructor(private readonly pharmacyService: PharmacyService) {}

  ngOnInit(): void {
    this.loadStock();
  }

  loadStock(): void {
    this.loading = true;
    this.errorMessage = '';
    this.cancelRename();

    this.pharmacyService.listStock(this.query, this.includeArchived).subscribe({
      next: (items) => {
        this.items = items;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load stock items';
        this.loading = false;
      }
    });
  }

  increment(item: StockItemResponse): void {
    const incrementBy = this.incrementValue[item.id] || 1;
    if (incrementBy < 1) {
      this.errorMessage = 'Quantity increment must be at least 1.';
      return;
    }

    if (!window.confirm(`Add ${incrementBy} units to "${item.medicineName}"?`)) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.pharmacyService.incrementStockItem(item.id, { incrementBy }).subscribe({
      next: (updated) => {
        this.successMessage = `${updated.medicineName} quantity updated`;
        this.replaceItem(updated);
        this.incrementValue[item.id] = 1;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to increment stock quantity';
      }
    });
  }

  startRename(item: StockItemResponse): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.renamingItemId = item.id;
    this.renameDraft = item.medicineName;
  }

  cancelRename(): void {
    this.renamingItemId = null;
    this.renameDraft = '';
  }

  saveRename(item: StockItemResponse): void {
    const normalizedName = this.renameDraft.trim();
    if (normalizedName.length < 2 || normalizedName.length > 80) {
      this.errorMessage = 'Medicine name must be between 2 and 80 characters.';
      return;
    }

    if (!window.confirm(`Rename "${item.medicineName}" to "${normalizedName}"?`)) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.pharmacyService.renameStockItem(item.id, { medicineName: normalizedName }).subscribe({
      next: (updated) => {
        this.successMessage = `Medicine renamed to ${updated.medicineName}`;
        this.replaceItem(updated);
        this.cancelRename();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to rename medicine';
      }
    });
  }

  onRenameKeydown(event: KeyboardEvent, item: StockItemResponse): void {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.saveRename(item);
      return;
    }
    if (event.key === 'Escape') {
      event.preventDefault();
      this.cancelRename();
    }
  }

  markOutOfStock(item: StockItemResponse): void {
    if (!window.confirm(`Mark "${item.medicineName}" as out of stock?`)) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.pharmacyService.markOutOfStock(item.id).subscribe({
      next: (updated) => {
        this.successMessage = `${updated.medicineName} marked as out of stock`;
        this.replaceItem(updated);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to mark as out of stock';
      }
    });
  }

  archive(item: StockItemResponse): void {
    if (!window.confirm(`Archive "${item.medicineName}"?`)) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.pharmacyService.archiveStockItem(item.id).subscribe({
      next: () => {
        this.successMessage = `${item.medicineName} archived`;
        if (!this.includeArchived) {
          this.items = this.items.filter(x => x.id !== item.id);
          return;
        }
        this.replaceItem({ ...item, archived: true });
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to archive medicine';
      }
    });
  }

  restore(item: StockItemResponse): void {
    if (!window.confirm(`Restore "${item.medicineName}" from archive?`)) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.pharmacyService.restoreStockItem(item.id).subscribe({
      next: (updated) => {
        this.successMessage = `${updated.medicineName} restored`;
        this.replaceItem(updated);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to restore medicine';
      }
    });
  }

  deletePermanently(item: StockItemResponse): void {
    if (!item.archived) {
      this.errorMessage = 'Only archived medicines can be deleted permanently.';
      return;
    }

    if (!window.confirm(`Delete "${item.medicineName}" permanently? This cannot be undone.`)) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.pharmacyService.deleteArchivedStockItem(item.id).subscribe({
      next: () => {
        this.successMessage = `${item.medicineName} deleted permanently`;
        this.items = this.items.filter(x => x.id !== item.id);
        if (this.renamingItemId === item.id) {
          this.cancelRename();
        }
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to delete medicine permanently';
      }
    });
  }

  trackById(_: number, item: StockItemResponse): number {
    return item.id;
  }

  openImageModal(item: StockItemResponse): void {
    this.selectedImageItem = item;
  }

  closeImageModal(): void {
    this.selectedImageItem = null;
  }

  formatUpdated(updatedAt?: string): string {
    if (!updatedAt) {
      return '-';
    }
    const date = new Date(updatedAt);
    if (Number.isNaN(date.getTime())) {
      return updatedAt;
    }
    return date.toLocaleString();
  }

  private replaceItem(updated: StockItemResponse): void {
    this.items = this.items.map(item => (item.id === updated.id ? updated : item));
  }
}
