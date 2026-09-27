import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ElementRef, EventEmitter, Input, Output, ViewChild, inject, signal } from '@angular/core';
import { AddressInput, AddressPlatformConfig } from '../../services/address-validation.service';
import { GoogleMapsLoaderService } from '../../services/google-maps-loader.service';

interface GoogleAddressComponent {
  longText?: string;
  shortText?: string;
  types?: string[];
}

interface GooglePlace {
  addressComponents?: GoogleAddressComponent[];
  formattedAddress?: string;
  fetchFields(options: { fields: string[] }): Promise<void>;
}

interface GooglePlaceEvent extends Event { placePrediction: { toPlace(): GooglePlace }; }

@Component({
  selector: 'app-google-address-autocomplete',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="address-search" [class.is-error]="error()">
      <div class="address-search__heading"><i class="bi bi-google"></i><span><strong>Buscar dirección con Google Maps</strong><small>Selecciona un resultado para completar los campos.</small></span></div>
      <div #host class="address-search__host"></div>
      @if (loading()) { <p><span class="address-search__spinner"></span>Cargando búsqueda…</p> }
      @if (error()) { <p><i class="bi bi-exclamation-circle"></i>{{ error() }}</p> }
    </section>
  `,
  styles: [`
    :host { display:block; min-width:0; }
    .address-search { display:grid; gap:.6rem; padding:.8rem; border:1px solid #dce5ec; border-radius:14px; background:#fff; }
    .address-search.is-error { border-color:#fecaca; }
    .address-search__heading { display:flex; align-items:center; gap:.55rem; color:#334155; }
    .address-search__heading > i { width:30px; height:30px; display:grid; place-items:center; border-radius:9px; color:#2563eb; background:#eff6ff; }
    .address-search__heading span { display:grid; gap:.1rem; }.address-search__heading strong { font-size:.7rem; }.address-search__heading small { color:#64748b; font-size:.6rem; }
    .address-search__host { min-width:0; }
    .address-search__host > * { width:100%; }
    p { display:flex; align-items:center; gap:.4rem; margin:0; color:#64748b; font-size:.62rem; }
    .address-search__spinner { width:13px; height:13px; border:2px solid #dbe3ea; border-top-color:#2563eb; border-radius:50%; animation:spin .7s linear infinite; }
    @keyframes spin { to { transform:rotate(360deg); } }
  `]
})
export class GoogleAddressAutocompleteComponent implements AfterViewInit {
  private readonly loader = inject(GoogleMapsLoaderService);
  @ViewChild('host', { static: true }) private host!: ElementRef<HTMLElement>;
  @Input({ required: true }) config!: AddressPlatformConfig;
  @Output() addressSelected = new EventEmitter<Partial<AddressInput> & { direccionFormateada?: string }>();
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  async ngAfterViewInit(): Promise<void> {
    if (!this.config.browserApiKey) { this.loading.set(false); return; }
    try {
      await this.loader.load(this.config.browserApiKey);
      const googleScope = (window as unknown as { google: { maps: { places: { PlaceAutocompleteElement: new (options: object) => HTMLElement } } } }).google;
      const autocomplete = new googleScope.maps.places.PlaceAutocompleteElement({
        includedRegionCodes: this.config.regionesPermitidas
      });
      autocomplete.setAttribute('aria-label', 'Buscar dirección');
      autocomplete.addEventListener('gmp-select', event => void this.select(event as GooglePlaceEvent));
      this.host.nativeElement.replaceChildren(autocomplete);
    } catch {
      this.error.set('La búsqueda asistida no está disponible. Puedes capturar la dirección manualmente.');
    } finally {
      this.loading.set(false);
    }
  }

  private async select(event: GooglePlaceEvent): Promise<void> {
    const place = event.placePrediction.toPlace();
    await place.fetchFields({ fields: ['addressComponents', 'formattedAddress'] });
    const component = (type: string, short = false): string | undefined => {
      const found = place.addressComponents?.find(item => item.types?.includes(type));
      return short ? found?.shortText : found?.longText;
    };
    this.addressSelected.emit({
      calle: component('route'),
      numeroExterior: component('street_number'),
      numeroInterior: component('subpremise'),
      colonia: component('neighborhood') || component('sublocality_level_1'),
      municipio: component('locality') || component('administrative_area_level_2'),
      estado: component('administrative_area_level_1'),
      codigoPostal: component('postal_code'),
      codigoPais: component('country', true),
      direccionFormateada: place.formattedAddress
    });
  }
}
