import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { MarketplaceAdminRoutingModule } from './marketplace-admin-routing.module';
import { MarketplaceAdminProductsComponent } from './products/marketplace-admin-products.component';
import { MarketplaceAdminProductFormComponent } from './product-form/marketplace-admin-product-form.component';
import { MarketplaceAdminOrdersComponent } from './orders/marketplace-admin-orders.component';

@NgModule({
  declarations: [
    MarketplaceAdminProductsComponent,
    MarketplaceAdminProductFormComponent,
    MarketplaceAdminOrdersComponent
  ],
  imports: [
    SharedModule,
    MarketplaceAdminRoutingModule
  ]
})
export class MarketplaceAdminModule {}
