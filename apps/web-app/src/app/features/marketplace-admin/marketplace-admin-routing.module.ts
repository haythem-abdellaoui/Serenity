import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { MarketplaceAdminProductsComponent } from './products/marketplace-admin-products.component';
import { MarketplaceAdminProductFormComponent } from './product-form/marketplace-admin-product-form.component';
import { MarketplaceAdminOrdersComponent } from './orders/marketplace-admin-orders.component';

const routes: Routes = [
  { path: '', component: MarketplaceAdminProductsComponent },
  { path: 'products/new', component: MarketplaceAdminProductFormComponent },
  { path: 'products/:id/edit', component: MarketplaceAdminProductFormComponent },
  { path: 'orders', component: MarketplaceAdminOrdersComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class MarketplaceAdminRoutingModule {}
