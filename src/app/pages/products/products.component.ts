import { A11yModule } from '@angular/cdk/a11y';
import { DOCUMENT } from '@angular/common';
import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { combineLatest, finalize } from 'rxjs';
import { TenantContextService } from '../../core/tenant/tenant-context.service';
import { SeoService } from '../../core/seo/seo.service';
import { ProductCartService } from '../../services/product-cart.service';
import { ProductStoreService, PublicProductCatalog, StoreCategory, StoreProduct } from '../../services/product-store.service';
import { MoneyDisplayPipe } from '../../shared/pipes/money-display.pipe';
import { PublicCheckoutService } from '../../services/public-checkout.service';

interface CategorySection {
  category: StoreCategory | null;
  children: Array<{ category: StoreCategory | null; products: StoreProduct[] }>;
  total: number;
}

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [A11yModule, CommonModule, FormsModule, RouterLink, MoneyDisplayPipe],
  templateUrl: './products.component.html',
  styleUrl: './products.component.css'
})
export class ProductsComponent implements OnInit {
  readonly tenantContext = inject(TenantContextService);
  readonly cart = inject(ProductCartService);
  private readonly store = inject(ProductStoreService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly seo = inject(SeoService);
  private readonly checkout = inject(PublicCheckoutService);
  private readonly destroyRef = inject(DestroyRef);

  readonly catalog = signal<PublicProductCatalog | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly search = signal('');
  readonly categoryId = signal<number | null>(null);
  readonly cartOpen = signal(false);
  readonly onlineCheckout = signal(false);
  readonly addedProduct = signal<string | null>(null);
  private readonly document = inject(DOCUMENT);
  private feedbackTimer: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    effect(onCleanup => {
      if (!this.cartOpen()) return;
      const body = this.document.body;
      const view = this.document.defaultView;
      const scrollY = view?.scrollY ?? 0;
      const previous = { overflow: body.style.overflow, position: body.style.position, top: body.style.top, width: body.style.width };
      Object.assign(body.style, { overflow: 'hidden', position: 'fixed', top: `-${scrollY}px`, width: '100%' });
      onCleanup(() => { Object.assign(body.style, previous); view?.scrollTo(0, scrollY); });
    });
    this.destroyRef.onDestroy(() => { clearTimeout(this.feedbackTimer); });
  }

  readonly productSlug = signal<string | null>(null);
  readonly detailProduct = computed(() => this.catalog()?.productos[0] ?? null);
  readonly isDetail = computed(() => Boolean(this.productSlug()));

  readonly filteredProducts = computed(() => {
    const term = this.search().trim().toLocaleLowerCase('es-MX');
    const categoryId = this.categoryId();
    const categories = this.catalog()?.categorias ?? [];
    const selectedIds = categoryId === null ? null : new Set([
      categoryId,
      ...categories.filter(item => item.categoriaPadreId === categoryId).map(item => item.id)
    ]);
    return (this.catalog()?.productos ?? []).filter(product =>
      (!selectedIds || (product.categoriaId !== null && selectedIds.has(product.categoriaId)))
      && (!term || `${product.nombre} ${product.descripcion || ''} ${product.categoriaNombre || ''}`.toLocaleLowerCase('es-MX').includes(term))
    );
  });

  readonly rootCategories = computed(() => (this.catalog()?.categorias ?? []).filter(item => item.categoriaPadreId === null));
  readonly sections = computed<CategorySection[]>(() => {
    const catalog = this.catalog();
    if (!catalog) return [];
    const products = this.filteredProducts();
    const roots = catalog.categorias.filter(item => item.categoriaPadreId === null);
    const sections: CategorySection[] = roots.map(root => {
      const children = catalog.categorias.filter(item => item.categoriaPadreId === root.id);
      const groups = [root, ...children].map(category => ({
        category,
        products: products.filter(product => product.categoriaId === category.id)
      })).filter(group => group.products.length > 0);
      return { category: root, children: groups, total: groups.reduce((total, group) => total + group.products.length, 0) };
    }).filter(section => section.total > 0);
    const uncategorized = products.filter(product => product.categoriaId === null);
    if (uncategorized.length) sections.push({ category: null, children: [{ category: null, products: uncategorized }], total: uncategorized.length });
    return sections;
  });

  ngOnInit(): void {
    combineLatest([this.route.paramMap, this.route.queryParamMap]).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(([params, query]) => {
      this.productSlug.set(params.get('productoSlug'));
      this.load(query.get('zona'),query.get('sucursalId'));
    });
  }

  selectZone(zone: string): void {
    void this.router.navigate([], { relativeTo: this.route, queryParams: { zona: zone || null }, queryParamsHandling: 'merge' });
  }

  selectBranch(branch: string): void { void this.router.navigate([], {relativeTo:this.route,queryParams:{sucursalId:branch || null},queryParamsHandling:'merge'}); }
  storeQuery(): {zona:string|null;sucursalId:number|null} { return {zona:this.catalog()?.zonaSeleccionada?.clave ?? null,sucursalId:this.catalog()?.sucursalSeleccionadaId ?? null}; }
  selectCategory(id: number | null): void { this.categoryId.set(id); }
  add(product: StoreProduct): void {
    if (!product.disponible) return;
    this.cart.add(product);
    this.addedProduct.set(product.nombre);
    clearTimeout(this.feedbackTimer);
    this.feedbackTimer = setTimeout(() => this.addedProduct.set(null), 3500);
  }
  increase(productId: number): void {
    this.cart.increase(productId);
  }
  decrease(productId: number): void { this.cart.decrease(productId); }
  remove(productId: number): void { this.cart.remove(productId); }

  productRoute(product: StoreProduct): string { return this.tenantContext.routeFor(`productos/${product.slug}`); }
  storeRoute(): string { return this.tenantContext.routeFor('productos'); }
  checkoutRoute(): string { return this.tenantContext.routeFor('checkout'); }
  image(product: StoreProduct): string { return product.imagenUrl || this.tenantContext.logoUrl() || '/fluora-mark.svg'; }

  whatsappOrder(): string {
    const lines = this.cart.items().map(item => `${item.cantidad} × ${item.nombre} — ${this.money(item.precio * item.cantidad, item.moneda)}`);
    const zone = this.catalog()?.zonaSeleccionada?.nombre;
    const message = [
      `Hola, quiero solicitar estos productos de ${this.tenantContext.nombreComercial()}:`,
      '', ...lines, '', `Total estimado: ${this.money(this.cart.total(), this.cart.currency())}`,
      zone ? `Zona de precio: ${zone}` : null
    ].filter((line): line is string => Boolean(line)).join('\n');
    return this.tenantContext.whatsappHref(message);
  }

  private load(zone: string | null, branch: string | null): void {
    const slug = this.tenantContext.slug();
    if (!slug) return;
    this.loading.set(true);
    this.error.set(null);
    this.store.catalog(slug, zone, this.productSlug(),branch).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: catalog => {
        this.catalog.set(catalog);
        this.cart.connect(catalog.empresaId);
        if (!this.isDetail()) this.cart.reconcile(catalog.productos);
        const tenant = this.tenantContext.actual();
        if (tenant) this.seo.applyProductCatalog(tenant, catalog, this.isDetail() ? this.detailProduct() : null);
      },
      error: error => this.error.set(error?.status === 404 ? 'Esta tienda todavía no está disponible.' : 'No pudimos cargar los productos. Intenta de nuevo.')
    });
    this.checkout.config(slug).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: config => this.onlineCheckout.set(config.habilitado && config.aceptarTarjeta && config.permitirPagoProductos),
      error: () => this.onlineCheckout.set(false)
    });
  }

  private money(value: number, currency: string): string {
    return new Intl.NumberFormat('es-MX', { style: 'currency', currency, currencyDisplay: currency === 'MXN' ? 'narrowSymbol' : 'symbol', minimumFractionDigits: 2 }).format(value);
  }
}
