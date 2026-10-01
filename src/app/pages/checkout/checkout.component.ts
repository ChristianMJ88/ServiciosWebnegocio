import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, firstValueFrom, forkJoin } from 'rxjs';
import { TenantContextService } from '../../core/tenant/tenant-context.service';
import { BookingDataService, SucursalCatalogo } from '../../services/booking-data.service';
import { MercadoPagoBricksService, supportsMercadoPagoStatusScreen } from '../../services/mercado-pago-bricks.service';
import { ProductCartService } from '../../services/product-cart.service';
import { CardBrickData, CheckoutOrder, CheckoutPayment, DeliveryMode, PublicCheckoutConfig, PublicCheckoutService, ShippingQuoteOption } from '../../services/public-checkout.service';
import { MoneyDisplayPipe } from '../../shared/pipes/money-display.pipe';
import { AddressInput, AddressPlatformConfig, AddressValidationService } from '../../services/address-validation.service';
import { GoogleAddressAutocompleteComponent } from '../../shared/google-address-autocomplete/google-address-autocomplete.component';

interface BrickController { unmount(): void; }

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, MoneyDisplayPipe, GoogleAddressAutocompleteComponent],
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
  private readonly addressApi = inject(AddressValidationService);

  readonly loading = signal(true);
  readonly creatingOrder = signal(false);
  readonly brickReady = signal(false);
  readonly statusReady = signal(false);
  readonly statusMessage = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly config = signal<PublicCheckoutConfig | null>(null);
  readonly branches = signal<SucursalCatalogo[]>([]);
  readonly order = signal<CheckoutOrder | null>(null);
  readonly payment = signal<CheckoutPayment | null>(null);
  readonly validatingPostalCode = signal(false);
  readonly postalCodeMessage = signal<string | null>(null);
  readonly addressConfig = signal<AddressPlatformConfig | null>(null);
  readonly validatingAddress = signal(false);
  readonly addressValidationMessage = signal<string | null>(null);
  readonly quotingShipping = signal(false);
  readonly shippingQuotes = signal<ShippingQuoteOption[]>([]);
  readonly selectedShippingQuote = signal<ShippingQuoteOption | null>(null);
  readonly shippingQuoteMessage = signal<string | null>(null);
  private paymentController: BrickController | null = null;
  private statusController: BrickController | null = null;
  private orderKey = this.key('orden');
  private paymentKey = this.key('pago');

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(180)]],
    name: ['', [Validators.required, Validators.maxLength(180)]],
    phone: ['', [Validators.required, Validators.maxLength(30)]],
    branchId: [0, [Validators.required, Validators.min(1)]],
    deliveryMode: ['RECOGE_EN_SUCURSAL' as DeliveryMode, Validators.required],
    street: [''],
    exteriorNumber: [''],
    interiorNumber: [''],
    neighborhood: [''],
    state: [''],
    municipality: [''],
    postalCode: [''],
    countryCode: [''],
    references: ['']
  });

  ngOnInit(): void {
    const slug = this.tenant.slug();
    const empresaId = this.tenant.empresaId();
    if (!slug || !empresaId) { this.fail('No encontramos el negocio para iniciar el pago.'); return; }
    this.cart.connect(empresaId);
    forkJoin({ config: this.checkout.config(slug), branches: this.branchesApi.getBranches(empresaId), addressConfig: this.addressApi.configuration() })
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: ({ config, branches, addressConfig }) => {
          this.config.set(config);
          this.branches.set(branches);
          this.addressConfig.set(addressConfig);
          this.form.controls.countryCode.setValue(addressConfig.regionPredeterminada);
          if (branches.length === 1) this.form.controls.branchId.setValue(branches[0].id);
          this.selectInitialDelivery(config);
          this.loading.set(false);
        },
        error: () => this.fail('No pudimos preparar el pago. Intenta de nuevo más tarde.')
      });
  }

  ngOnDestroy(): void {
    this.paymentController?.unmount();
    this.statusController?.unmount();
  }

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
        nombreDestinatario: this.form.controls.name.value.trim(),
        telefonoDestinatario: this.form.controls.phone.value.trim(),
        modalidadEntrega: this.form.controls.deliveryMode.value,
        cotizacionEnvioId: this.selectedShippingQuote()?.id ?? null,
        direccion: this.requiresAddress() ? {
          calle: this.form.controls.street.value.trim(),
          numeroExterior: this.form.controls.exteriorNumber.value.trim(),
          numeroInterior: this.optional(this.form.controls.interiorNumber.value),
          colonia: this.form.controls.neighborhood.value.trim(),
          estado: this.form.controls.state.value.trim(),
          municipio: this.form.controls.municipality.value.trim(),
          codigoPostal: this.form.controls.postalCode.value.trim(),
          codigoPais: this.form.controls.countryCode.value.trim(),
          referencias: this.optional(this.form.controls.references.value)
        } : null,
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

  selectDelivery(mode: DeliveryMode): void {
    this.form.controls.deliveryMode.setValue(mode);
    const required = mode !== 'RECOGE_EN_SUCURSAL';
    const controls = [this.form.controls.street, this.form.controls.exteriorNumber,
      this.form.controls.neighborhood, this.form.controls.state, this.form.controls.municipality];
    controls.forEach(control => {
      control.setValidators(required ? [Validators.required] : []);
      control.updateValueAndValidity({ emitEvent: false });
    });
    this.form.controls.postalCode.setValidators(required ? [Validators.required, Validators.pattern(/^\d{5}$/)] : []);
    this.form.controls.postalCode.updateValueAndValidity({ emitEvent: false });
    this.clearShippingQuotes();
  }

  requiresAddress(): boolean { return this.form.controls.deliveryMode.value !== 'RECOGE_EN_SUCURSAL'; }

  applyGoogleAddress(address: Partial<AddressInput>): void {
    const controls = this.form.controls;
    if (address.calle) controls.street.setValue(address.calle);
    if (address.numeroExterior) controls.exteriorNumber.setValue(address.numeroExterior);
    if (address.numeroInterior) controls.interiorNumber.setValue(address.numeroInterior);
    if (address.colonia) controls.neighborhood.setValue(address.colonia);
    if (address.municipio) controls.municipality.setValue(address.municipio);
    if (address.estado) controls.state.setValue(address.estado);
    if (address.codigoPostal) controls.postalCode.setValue(address.codigoPostal);
    if (address.codigoPais) controls.countryCode.setValue(address.codigoPais);
    this.addressValidationMessage.set('Dirección seleccionada. Verifica el número y confirma los datos.');
    this.clearShippingQuotes();
  }

  validateAddress(): void {
    if (this.validatingAddress() || !this.requiresAddress()) return;
    const input = this.addressInput();
    if (!input) { this.addressValidationMessage.set('Completa los datos obligatorios para verificar la dirección.'); return; }
    this.validatingAddress.set(true);
    this.addressValidationMessage.set(null);
    this.addressApi.validate(input).pipe(
      finalize(() => this.validatingAddress.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: result => {
        if (result.valida) this.applyGoogleAddress(result.direccion);
        this.addressValidationMessage.set(result.mensaje);
      },
      error: error => this.addressValidationMessage.set(this.message(error, 'No pudimos verificar la dirección.'))
    });
  }

  clearAddressValidation(): void { this.addressValidationMessage.set(null); this.clearShippingQuotes(); }

  quoteShipping(): void {
    const slug = this.tenant.slug();
    const direccion = this.deliveryAddress();
    if (!slug || !direccion || this.form.controls.branchId.invalid || this.form.controls.name.invalid
      || this.form.controls.email.invalid || this.form.controls.phone.invalid || this.quotingShipping()) {
      this.form.markAllAsTouched();
      this.shippingQuoteMessage.set('Completa tus datos, sucursal y dirección para consultar las paqueterías.');
      return;
    }
    this.quotingShipping.set(true);
    this.shippingQuoteMessage.set(null);
    this.shippingQuotes.set([]);
    this.selectedShippingQuote.set(null);
    this.checkout.quoteShipping(slug, {
      sucursalId: this.form.controls.branchId.value,
      zonaPrecioClave: this.route.snapshot.queryParamMap.get('zona'),
      nombreDestinatario: this.form.controls.name.value.trim(),
      correoDestinatario: this.form.controls.email.value.trim().toLowerCase(),
      telefonoDestinatario: this.form.controls.phone.value.trim(),
      direccion,
      productos: this.cart.items().map(item => ({ productoId: item.productoId, cantidad: item.cantidad }))
    }).pipe(finalize(() => this.quotingShipping.set(false)), takeUntilDestroyed(this.destroyRef)).subscribe({
      next: response => {
        this.shippingQuotes.set(response.opciones);
        if (response.opciones.length === 1) this.selectShippingQuote(response.opciones[0]);
        this.shippingQuoteMessage.set(response.opciones.length
          ? 'Selecciona la opción que prefieras. La tarifa se confirmará en el servidor.'
          : 'No encontramos opciones disponibles para este destino.');
      },
      error: error => this.shippingQuoteMessage.set(this.message(error, 'No pudimos consultar las tarifas de paquetería.'))
    });
  }

  selectShippingQuote(option: ShippingQuoteOption): void {
    this.selectedShippingQuote.set(option);
    this.error.set(null);
  }

  clearShippingQuotes(): void {
    this.shippingQuotes.set([]);
    this.selectedShippingQuote.set(null);
    this.shippingQuoteMessage.set(null);
  }

  postPaymentMessage(): string {
    return switchDeliveryMessage(this.form.controls.deliveryMode.value, Boolean(this.selectedShippingQuote()));
  }

  deliveryCost(): number {
    const config = this.config();
    if (!config) return 0;
    if (this.form.controls.deliveryMode.value === 'ENTREGA_LOCAL') return config.costoEntregaLocal;
    if (this.form.controls.deliveryMode.value === 'ENVIO_NACIONAL') {
      return this.selectedShippingQuote()?.costo ?? config.costoEnvioNacional;
    }
    return 0;
  }

  canShowProviderStatus(paymentId: string | null): paymentId is string {
    return Boolean(this.config()?.mostrarEstadoPago) && supportsMercadoPagoStatusScreen(paymentId);
  }

  validatePostalCode(): void {
    const postalCode = this.form.controls.postalCode.value.trim();
    if (!/^\d{5}$/.test(postalCode) || this.validatingPostalCode()) return;
    this.validatingPostalCode.set(true);
    this.postalCodeMessage.set(null);
    this.checkout.validatePostalCode('MX', postalCode).pipe(
      finalize(() => this.validatingPostalCode.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: result => {
        if (!result.valido) {
          this.postalCodeMessage.set('No encontramos este código postal. Verifica el dato.');
          return;
        }
        if (!this.form.controls.municipality.value.trim() && result.municipio) this.form.controls.municipality.setValue(result.municipio);
        if (!this.form.controls.state.value.trim() && result.estado) this.form.controls.state.setValue(result.estado);
        this.postalCodeMessage.set('Código postal validado con Envia.com.');
      },
      error: () => this.postalCodeMessage.set('No pudimos validarlo ahora; puedes continuar capturando la dirección.')
    });
  }

  private selectInitialDelivery(config: PublicCheckoutConfig): void {
    const mode: DeliveryMode = config.permitirRecoleccion ? 'RECOGE_EN_SUCURSAL'
      : config.permitirEntregaLocal ? 'ENTREGA_LOCAL' : 'ENVIO_NACIONAL';
    this.selectDelivery(mode);
  }

  private optional(value: string): string | null { return value.trim() || null; }

  private addressInput(): AddressInput | null {
    const controls = this.form.controls;
    const values: AddressInput = {
      calle: controls.street.value.trim(), numeroExterior: controls.exteriorNumber.value.trim(),
      numeroInterior: this.optional(controls.interiorNumber.value), colonia: controls.neighborhood.value.trim(),
      municipio: controls.municipality.value.trim(), estado: controls.state.value.trim(),
      codigoPostal: controls.postalCode.value.trim(), codigoPais: controls.countryCode.value.trim()
    };
    return [values.calle, values.numeroExterior, values.colonia, values.municipio, values.estado,
      values.codigoPostal, values.codigoPais].every(Boolean) ? values : null;
  }

  private deliveryAddress() {
    const input = this.addressInput();
    if (!input) return null;
    return { ...input, referencias: this.optional(this.form.controls.references.value) };
  }

  private async mountBrick(config: PublicCheckoutConfig, order: CheckoutOrder): Promise<void> {
    if (!config.llavePublica) throw new Error('El negocio todavía no tiene una llave pública de pago disponible.');
    this.paymentController?.unmount();
    this.paymentController = await this.bricks.mountCard('cardPaymentBrick_container', config.llavePublica,
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
      if (result.accionUrl) {
        window.location.assign(result.accionUrl);
        return;
      }
      if (this.canShowProviderStatus(result.referenciaPagoExterna) && this.config()?.llavePublica) {
        await new Promise(resolve => setTimeout(resolve));
        try {
          await this.mountStatus(this.config()!.llavePublica!, result.referenciaPagoExterna);
        } catch {
          this.statusMessage.set('El pago fue recibido, pero el detalle interactivo no está disponible en este momento.');
        }
      }
    } catch (error: unknown) {
      this.error.set(this.message(error, 'El pago no pudo procesarse. Revisa los datos o intenta con otro método.'));
      throw error;
    }
  }

  private async mountStatus(publicKey: string, paymentId: string): Promise<void> {
    this.statusReady.set(false);
    this.statusMessage.set(null);
    this.statusController?.unmount();
    this.statusController = await this.bricks.mountStatus('statusScreenBrick_container', publicKey, paymentId,
      () => this.statusReady.set(true), message => {
        this.statusReady.set(true);
        this.statusMessage.set(message);
      });
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

function switchDeliveryMessage(mode: DeliveryMode, cotizacionIntegrada: boolean): string {
  if (mode === 'RECOGE_EN_SUCURSAL') return 'La sucursal preparará tu pedido y usará tus datos de contacto para avisarte cuando puedas recogerlo.';
  if (mode === 'ENTREGA_LOCAL') return 'El negocio recibió tu dirección y preparará la entrega local. Podrá contactarte para coordinar el horario.';
  return cotizacionIntegrada
    ? 'La tarifa seleccionada quedó asociada a tu pedido. El negocio preparará la guía y continuará el seguimiento del envío.'
    : 'El negocio recibió tu dirección y gestionará la guía con su paquetería. Podrá compartirte el seguimiento cuando esté disponible.';
}
