import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom, forkJoin } from 'rxjs';
import { TenantContextService } from '../../core/tenant/tenant-context.service';
import { BookingDataService, SucursalCatalogo } from '../../services/booking-data.service';
import { MercadoPagoBricksService } from '../../services/mercado-pago-bricks.service';
import { ProductCartService } from '../../services/product-cart.service';
import { CardBrickData, CheckoutOrder, CheckoutPayment, PublicCheckoutConfig, PublicCheckoutService } from '../../services/public-checkout.service';
import { MoneyDisplayPipe } from '../../shared/pipes/money-display.pipe';

interface BrickController { unmount(): void; }

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, MoneyDisplayPipe],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.css'
})
export class CheckoutComponent implements OnInit, OnDestroy {
  readonly tenant = inject(TenantContextService);
  readonly cart = inject(ProductCartService);
  private readonly checkout = inject(PublicCheckoutService);
  private readonly branchesApi = inject(BookingDataService);
  private readonly bricks = inject(MercadoPagoBricksService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly creatingOrder = signal(false);
  readonly brickReady = signal(false);
  readonly error = signal<string | null>(null);
  readonly config = signal<PublicCheckoutConfig | null>(null);
  readonly branches = signal<SucursalCatalogo[]>([]);
  readonly order = signal<CheckoutOrder | null>(null);
  readonly payment = signal<CheckoutPayment | null>(null);
  private controller: BrickController | null = null;
  private orderKey = this.key('orden');
  private paymentKey = this.key('pago');

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(180)]],
    branchId: [0, [Validators.required, Validators.min(1)]]
  });

  ngOnInit(): void {
    const slug = this.tenant.slug();
    const empresaId = this.tenant.empresaId();
    if (!slug || !empresaId) { this.fail('No encontramos el negocio para iniciar el pago.'); return; }
    this.cart.connect(empresaId);
    forkJoin({ config: this.checkout.config(slug), branches: this.branchesApi.getBranches(empresaId) })
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: ({ config, branches }) => {
          this.config.set(config);
          this.branches.set(branches);
          if (branches.length === 1) this.form.controls.branchId.setValue(branches[0].id);
          this.loading.set(false);
        },
        error: () => this.fail('No pudimos preparar el pago. Intenta de nuevo más tarde.')
      });
  }

  ngOnDestroy(): void { this.controller?.unmount(); }

  async confirmOrder(): Promise<void> {
    const slug = this.tenant.slug();
    const config = this.config();
    if (!slug || !config || this.form.invalid || !this.cart.items().length) {
      this.form.markAllAsTouched();
      return;
    }
    this.creatingOrder.set(true);
    this.error.set(null);
    try {
      const order = await firstValueFrom(this.checkout.createOrder(slug, {
        sucursalId: this.form.controls.branchId.value,
        zonaPrecioClave: this.route.snapshot.queryParamMap.get('zona'),
        correoCliente: this.form.controls.email.value.trim().toLowerCase(),
        productos: this.cart.items().map(item => ({ productoId: item.productoId, cantidad: item.cantidad })),
        citaIds: []
      }, this.orderKey));
      this.order.set(order);
      await new Promise(resolve => setTimeout(resolve));
      await this.mountBrick(config, order);
    } catch (error: unknown) {
      this.error.set(this.message(error, 'No pudimos confirmar la orden. Revisa disponibilidad y vuelve a intentar.'));
    } finally {
      this.creatingOrder.set(false);
    }
  }

  private async mountBrick(config: PublicCheckoutConfig, order: CheckoutOrder): Promise<void> {
    if (!config.llavePublica) throw new Error('El negocio todavía no tiene una llave pública de pago disponible.');
    this.controller?.unmount();
    this.controller = await this.bricks.mountCard('cardPaymentBrick_container', config.llavePublica,
      order.total, this.form.controls.email.value.trim().toLowerCase(),
      data => this.pay(data), () => this.brickReady.set(true), message => this.error.set(message));
  }

  private async pay(data: CardBrickData): Promise<void> {
    const slug = this.tenant.slug();
    const order = this.order();
    if (!slug || !order) throw new Error('La orden ya no está disponible.');
    this.error.set(null);
    try {
      const result = await firstValueFrom(this.checkout.payCard(slug, order.id, data,
        this.form.controls.email.value.trim().toLowerCase(), this.paymentKey));
      this.payment.set(result);
      if (result.estadoPago === 'APROBADO') this.cart.clear();
      if (result.accionUrl) window.location.assign(result.accionUrl);
    } catch (error: unknown) {
      this.error.set(this.message(error, 'El pago no pudo procesarse. Revisa los datos o intenta con otro método.'));
      throw error;
    }
  }

  private key(prefix: string): string {
    return `${prefix}-${crypto.randomUUID()}`;
  }

  private fail(message: string): void { this.error.set(message); this.loading.set(false); }
  private message(error: unknown, fallback: string): string {
    const candidate = error as { error?: { detail?: string; message?: string }; message?: string };
    return candidate?.error?.detail || candidate?.error?.message || candidate?.message || fallback;
  }
}
