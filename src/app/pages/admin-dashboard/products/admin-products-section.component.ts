import { CommonModule, getCurrencySymbol } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { Observable, finalize, forkJoin } from 'rxjs';
import {
  CatalogoProductosAdmin, CategoriaProductoAdmin, CategoriaProductoPayload, ConfiguracionTiendaAdmin, ProductoAdmin, ProductoPayload,
  ProductsAdminService, ProveedorProductoAdmin, ProveedorProductoPayload, ZonaPrecioProductoAdmin, ZonaPrecioProductoPayload
} from './products-admin.service';
import { AdminProductDiscountsComponent } from './admin-product-discounts.component';

type VistaProductos = 'productos' | 'categorias' | 'proveedores' | 'zonas' | 'descuentos' | 'tienda';
type CategoriaForm = CategoriaProductoPayload & { id: number | null };
type ProveedorForm = ProveedorProductoPayload & { id: number | null };
type ZonaForm = ZonaPrecioProductoPayload & { id: number | null };
type ProductoForm = ProductoPayload & { id: number | null };

@Component({
  selector: 'app-admin-products-section',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatSlideToggleModule, AdminProductDiscountsComponent],
  providers: [ProductsAdminService],
  templateUrl: './admin-products-section.component.html',
  styleUrl: './admin-products-section.component.css'
})
export class AdminProductsSectionComponent implements OnInit {
  private readonly service = inject(ProductsAdminService);
  readonly catalogo = signal<CatalogoProductosAdmin | null>(null);
  readonly configuracionTienda = signal<ConfiguracionTiendaAdmin | null>(null);
  readonly vista = signal<VistaProductos>('productos');
  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly subiendoImagen = signal(false);
  readonly error = signal<string | null>(null);
  readonly mensaje = signal<string | null>(null);
  readonly busqueda = signal('');
  readonly formularioProducto = signal<ProductoForm | null>(null);
  readonly formularioCategoria = signal<CategoriaForm | null>(null);
  readonly formularioProveedor = signal<ProveedorForm | null>(null);
  readonly formularioZona = signal<ZonaForm | null>(null);

  readonly productosFiltrados = computed(() => {
    const termino = this.busqueda().trim().toLocaleLowerCase('es-MX');
    const productos = this.catalogo()?.productos ?? [];
    if (!termino) return productos;
    return productos.filter(item => [item.nombre, item.sku, item.categoriaNombre, item.proveedorNombre]
      .some(valor => valor?.toLocaleLowerCase('es-MX').includes(termino)));
  });
  readonly totalActivos = computed(() => this.catalogo()?.productos.filter(item => item.activo).length ?? 0);
  readonly totalPublicados = computed(() => this.catalogo()?.productos.filter(item => item.activo && item.visiblePublico).length ?? 0);

  ngOnInit(): void { this.cargar(); }

  seleccionarVista(vista: VistaProductos): void {
    this.vista.set(vista);
    this.cancelarFormularios();
  }

  nuevoProducto(): void {
    const moneda = this.catalogo()?.monedaPredeterminada ?? '';
    this.formularioProducto.set({
      id: null, categoriaId: null, proveedorId: null, mediaId: null, sku: '', codigoBarras: null, nombre: '', slug: '',
      descripcion: null, imagenUrl: null, precioBase: 0, costo: null, moneda, tasaImpuesto: 0, controlaInventario: true,
      pesoKg: null, largoCm: null, anchoCm: null, altoCm: null,
      stockMinimo: 0, visiblePublico: false, activo: true, preciosZona: (this.catalogo()?.zonasPrecio ?? []).filter(zona => zona.activa)
        .map(zona => ({ zonaPrecioId: zona.id, zonaNombre: zona.nombre, precio: 0, moneda, activo: false }))
    });
  }

  editarProducto(item: ProductoAdmin): void {
    const moneda = item.moneda || this.catalogo()?.monedaPredeterminada || '';
    const existentes = new Map(item.preciosZona.map(precio => [precio.zonaPrecioId, precio]));
    this.formularioProducto.set({
      id: item.id, categoriaId: item.categoriaId, proveedorId: item.proveedorId, mediaId: item.mediaId, sku: item.sku,
      codigoBarras: item.codigoBarras, nombre: item.nombre, slug: item.slug, descripcion: item.descripcion,
      imagenUrl: item.imagenUrl, precioBase: item.precioBase, costo: item.costo, moneda, tasaImpuesto: item.tasaImpuesto,
      pesoKg: item.pesoKg, largoCm: item.largoCm, anchoCm: item.anchoCm, altoCm: item.altoCm,
      controlaInventario: item.controlaInventario, stockMinimo: item.stockMinimo, visiblePublico: item.visiblePublico,
      activo: item.activo, preciosZona: (this.catalogo()?.zonasPrecio ?? []).filter(zona => zona.activa).map(zona => ({
        ...(existentes.get(zona.id) ?? { zonaPrecioId: zona.id, precio: 0, moneda, activo: false }), zonaNombre: zona.nombre
      }))
    });
  }

  guardarProducto(): void {
    const form = this.formularioProducto();
    if (!form || !form.nombre.trim() || !form.sku.trim() || !form.moneda.trim() || this.guardando()) return;
    const { id, ...resto } = form;
    const payload: ProductoPayload = {
      ...resto,
      preciosZona: resto.preciosZona.filter(precio => precio.activo).map(precio => ({
        zonaPrecioId: precio.zonaPrecioId,
        precio: precio.precio,
        moneda: precio.moneda,
        activo: precio.activo
      }))
    };
    this.ejecutar(id ? this.service.actualizarProducto(id, payload) : this.service.crearProducto(payload), 'Producto guardado.');
  }

  seleccionarImagen(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const archivo = input.files?.[0];
    input.value = '';
    if (!archivo || this.subiendoImagen()) return;
    this.subiendoImagen.set(true);
    this.limpiarMensajes();
    this.service.subirImagen(archivo).pipe(finalize(() => this.subiendoImagen.set(false))).subscribe({
      next: media => this.formularioProducto.update(form => form ? { ...form, mediaId: media.id, imagenUrl: media.secureUrl } : null),
      error: err => this.error.set(err?.error?.message || err?.error?.detail || 'No fue posible cargar la imagen.')
    });
  }

  nuevaCategoria(): void { this.formularioCategoria.set({ id: null, categoriaPadreId: null, nombre: '', slug: '', descripcion: null, orden: this.catalogo()?.categorias.length ?? 0, activa: true }); }
  editarCategoria(item: CategoriaProductoAdmin): void { this.formularioCategoria.set({ ...item }); }
  guardarCategoria(): void {
    const form = this.formularioCategoria(); if (!form || !form.nombre.trim() || this.guardando()) return;
    const { id, ...payload } = form;
    this.ejecutar(id ? this.service.actualizarCategoria(id, payload) : this.service.crearCategoria(payload), 'Categoría guardada.');
  }

  nuevoProveedor(): void { this.formularioProveedor.set({ id: null, nombre: '', clave: null, contacto: null, correo: null, telefono: null, sitioWeb: null, notas: null, activo: true }); }
  editarProveedor(item: ProveedorProductoAdmin): void { this.formularioProveedor.set({ ...item }); }
  guardarProveedor(): void {
    const form = this.formularioProveedor(); if (!form || !form.nombre.trim() || this.guardando()) return;
    const { id, ...payload } = form;
    this.ejecutar(id ? this.service.actualizarProveedor(id, payload) : this.service.crearProveedor(payload), 'Proveedor guardado.');
  }

  nuevaZona(): void { this.formularioZona.set({ id: null, nombre: '', clave: '', descripcion: null, prioridad: this.catalogo()?.zonasPrecio.length ?? 0, activa: true }); }
  editarZona(item: ZonaPrecioProductoAdmin): void { this.formularioZona.set({ ...item }); }
  guardarZona(): void {
    const form = this.formularioZona(); if (!form || !form.nombre.trim() || this.guardando()) return;
    const { id, ...payload } = form;
    this.ejecutar(id ? this.service.actualizarZona(id, payload) : this.service.crearZona(payload), 'Zona de precio guardada.');
  }

  guardarTienda(): void {
    const configuracion = this.configuracionTienda();
    if (!configuracion || this.guardando()) return;
    this.ejecutar(this.service.actualizarConfiguracionTienda({
      ...configuracion,
      titulo: this.texto(configuracion.titulo),
      descripcion: this.texto(configuracion.descripcion),
      seoTitulo: this.texto(configuracion.seoTitulo),
      seoDescripcion: this.texto(configuracion.seoDescripcion),
      textoAgregar: this.texto(configuracion.textoAgregar)
    }), 'Configuración de la tienda guardada.');
  }

  cancelarFormularios(): void {
    this.formularioProducto.set(null); this.formularioCategoria.set(null); this.formularioProveedor.set(null); this.formularioZona.set(null);
  }

  simboloMoneda(moneda: string | null | undefined): string {
    const codigo = moneda?.trim().toUpperCase();
    return codigo ? getCurrencySymbol(codigo, 'narrow') : '';
  }

  private cargar(mensaje?: string): void {
    this.cargando.set(true);
    forkJoin({
      catalogo: this.service.obtenerCatalogo(),
      tienda: this.service.obtenerConfiguracionTienda()
    }).pipe(finalize(() => this.cargando.set(false))).subscribe({
      next: ({ catalogo, tienda }) => {
        this.catalogo.set(catalogo);
        this.configuracionTienda.set(tienda);
        this.cancelarFormularios();
        if (mensaje) this.mensaje.set(mensaje);
      },
      error: err => this.error.set(err?.error?.message || 'No fue posible cargar el catálogo de productos.')
    });
  }

  private ejecutar(peticion: Observable<unknown>, mensaje: string): void {
    this.guardando.set(true); this.limpiarMensajes();
    peticion.pipe(finalize(() => this.guardando.set(false))).subscribe({
      next: () => this.cargar(mensaje),
      error: err => this.error.set(err?.error?.message || err?.error?.detail || 'No fue posible guardar los cambios.')
    });
  }

  private limpiarMensajes(): void { this.error.set(null); this.mensaje.set(null); }
  private texto(valor: string | null): string | null { return valor?.trim() || null; }
}
