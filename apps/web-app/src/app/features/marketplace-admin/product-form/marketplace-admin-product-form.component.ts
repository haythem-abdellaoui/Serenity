import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MarketplaceService } from '../../../core/services/marketplace.service';
import {
  MARKETPLACE_CATEGORIES,
  MARKETPLACE_TYPES,
  PreviewContentType,
  MarketplaceProduct,
  MarketplaceProductUpsertRequest
} from '../../../shared/models/marketplace.model';

@Component({
  selector: 'app-marketplace-admin-product-form',
  templateUrl: './marketplace-admin-product-form.component.html',
  styleUrls: ['./marketplace-admin-product-form.component.scss']
})
export class MarketplaceAdminProductFormComponent implements OnInit {
  readonly categories = MARKETPLACE_CATEGORIES;
  readonly types = MARKETPLACE_TYPES;
  readonly previewTypes: PreviewContentType[] = ['VIDEO', 'BOOK', 'AUDIO'];

  saving = false;
  loading = false;
  error = '';
  productId: number | null = null;

  readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    description: ['', [Validators.required, Validators.maxLength(2000)]],
    category: ['', Validators.required],
    type: ['', Validators.required],
    price: [null as number | null, [Validators.required, Validators.min(0.10)]],
    imageUrl: [''],
    previewable: [false, Validators.required],
    previewType: ['' as PreviewContentType | ''],
    previewUrl: [''],
    contentUrl: [''],
    active: [true, Validators.required]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly marketplaceService: MarketplaceService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      return;
    }

    this.productId = id;
    this.loading = true;

    this.marketplaceService.getProductById(id).subscribe({
      next: (product) => {
        this.patchProduct(product);
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load product.';
        this.loading = false;
      }
    });
  }

  submit(): void {
    this.error = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    const raw = this.form.getRawValue();
    const isDigital = raw.type === 'DIGITAL';
    const previewable = isDigital && Boolean(raw.previewable);
    const payload: MarketplaceProductUpsertRequest = {
      name: raw.name ?? '',
      description: raw.description ?? '',
      category: (raw.category ?? 'SELF_CARE') as MarketplaceProductUpsertRequest['category'],
      type: (raw.type ?? 'PHYSICAL') as MarketplaceProductUpsertRequest['type'],
      price: Number(raw.price) || 0,
      imageUrl: raw.imageUrl ?? undefined,
      previewable,
      previewType: previewable && raw.previewType ? raw.previewType : undefined,
      previewUrl: previewable ? (raw.previewUrl ?? undefined) : undefined,
      contentUrl: isDigital ? (raw.contentUrl ?? undefined) : undefined,
      active: raw.active ?? true
    };

    const request$ = this.productId
      ? this.marketplaceService.updateProduct(this.productId, payload)
      : this.marketplaceService.createProduct(payload);

    request$.subscribe({
      next: () => {
        this.saving = false;
        this.router.navigate(['/admin/marketplace']);
      },
      error: (err) => {
        this.error = err.error?.message || 'Failed to save product.';
        this.saving = false;
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/marketplace']);
  }

  get isDigitalSelected(): boolean {
    return this.form.controls.type.value === 'DIGITAL';
  }

  onTypeChanged(): void {
    if (!this.isDigitalSelected) {
      this.form.patchValue({
        previewable: false,
        previewType: '',
        previewUrl: '',
        contentUrl: ''
      });
    }
  }

  private patchProduct(product: MarketplaceProduct): void {
    this.form.patchValue({
      name: product.name,
      description: product.description,
      category: product.category,
      type: product.type,
      price: product.price,
      imageUrl: product.imageUrl ?? '',
      previewable: product.previewable,
      previewType: product.previewType ?? '',
      previewUrl: product.previewUrl ?? '',
      contentUrl: product.contentUrl ?? '',
      active: product.active
    });
  }
}
