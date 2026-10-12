import { Injectable, computed, signal } from '@angular/core';
import { StoreProduct } from './product-store.service';

export interface ProductCartItem {
  productoId: number;
  slug: string;
  nombre: string;
  imagenUrl: string | null;
  precio: number;
  moneda: string;
  cantidad: number;
}

@Injectable({ providedIn: 'root' })
export class ProductCartService {
  private readonly tenantId = signal<number | null>(null);
  private readonly itemsState = signal<ProductCartItem[]>([]);
  readonly items = this.itemsState.asReadonly();
  readonly units = computed(() => this.itemsState().reduce((total, item) => total + item.cantidad, 0));
  readonly total = computed(() => this.itemsState().reduce((sum, item) => sum + item.precio * item.cantidad, 0));
  readonly currency = computed(() => this.itemsState()[0]?.moneda ?? 'MXN');

  connect(empresaId: number): void {
    if (this.tenantId() === empresaId) return;
    this.tenantId.set(empresaId);
    this.itemsState.set(this.read(empresaId));
  }

  add(product: StoreProduct): void {
    if (!product.disponible) return;
    this.itemsState.update(items => {
      const existing = items.find(item => item.productoId === product.id);
      if (existing) return items.map(item => item.productoId === product.id ? { ...item, cantidad: item.cantidad + 1 } : item);
      return [...items, {
        productoId: product.id, slug: product.slug, nombre: product.nombre, imagenUrl: product.imagenUrl,
        precio: product.precio, moneda: product.moneda, cantidad: 1
      }];
    });
    this.persist();
  }

  increase(productId: number): void {
    this.itemsState.update(items => items.map(item => item.productoId === productId
      ? { ...item, cantidad: item.cantidad + 1 } : item));
    this.persist();
  }

  decrease(productId: number): void {
    this.itemsState.update(items => items.flatMap(item => item.productoId !== productId
      ? [item]
      : item.cantidad > 1 ? [{ ...item, cantidad: item.cantidad - 1 }] : []));
    this.persist();
  }

  remove(productId: number): void {
    this.itemsState.update(items => items.filter(item => item.productoId !== productId));
    this.persist();
  }

  clear(): void { this.itemsState.set([]); this.persist(); }
  quantity(productId: number): number { return this.itemsState().find(item => item.productoId === productId)?.cantidad ?? 0; }

  reconcile(products: StoreProduct[]): void {
    const productMap = new Map(products.map(product => [product.id, product]));
    this.itemsState.update(items => items.flatMap(item => {
      const product = productMap.get(item.productoId);
      if (!product?.disponible) return [];
      return [{ ...item, nombre: product.nombre, slug: product.slug, imagenUrl: product.imagenUrl,
        precio: product.precio, moneda: product.moneda }];
    }));
    this.persist();
  }

  private read(empresaId: number): ProductCartItem[] {
    if (typeof localStorage === 'undefined') return [];
    try {
      const parsed = JSON.parse(localStorage.getItem(this.key(empresaId)) || '[]') as ProductCartItem[];
      return Array.isArray(parsed) ? parsed.filter(item => item.productoId && item.cantidad > 0) : [];
    } catch { return []; }
  }

  private persist(): void {
    const id = this.tenantId();
    if (id === null || typeof localStorage === 'undefined') return;
    localStorage.setItem(this.key(id), JSON.stringify(this.itemsState()));
  }

  private key(empresaId: number): string { return `fluora:product-cart:${empresaId}`; }
}
