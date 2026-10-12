import { AdminWorkspaceComponent } from '../shared/admin-workspace.component';
import { BranchPriceZoneComponent } from './branch-price-zone.component';

import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AddressValidationService, AddressPlatformConfig, AddressInput } from '../../../services/address-validation.service';
import { GoogleAddressAutocompleteComponent } from '../../../shared/google-address-autocomplete/google-address-autocomplete.component';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { GuardarSucursalPayload, SucursalAdmin } from '../../../core/admin/admin.service';

@Component({
  selector: 'app-admin-branches-section',
  standalone: true,
  imports: [AdminWorkspaceComponent, BranchPriceZoneComponent,
    FormsModule,
    GoogleAddressAutocompleteComponent,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule
],
  templateUrl: './admin-branches-section.component.html',
  styleUrl: './admin-branches-section.component.css'
})
export class AdminBranchesSectionComponent {
  readonly addressConfig = signal<AddressPlatformConfig|null>(null);
  constructor(){inject(AddressValidationService).configuration().pipe(takeUntilDestroyed()).subscribe({next:c=>this.addressConfig.set(c),error:()=>this.addressConfig.set(null)});}
  aplicarDireccion(address:Partial<AddressInput>&{direccionFormateada?:string}){
   Object.assign(this.formularioSucursal,{direccion:address.direccionFormateada ?? this.formularioSucursal.direccion,
    municipio:address.municipio ?? '',estadoRegion:address.estado ?? '',paisCodigo:address.codigoPais ?? '',codigoPostal:address.codigoPostal ?? ''});
  }
  @Input({ required: true }) sucursalEditandoId!: number | null;
  @Input({ required: true }) formularioSucursal!: GuardarSucursalPayload;
  @Input({ required: true }) guardandoSucursal!: boolean;
  @Input({ required: true }) sucursales!: SucursalAdmin[];

  @Output() clearEdit = new EventEmitter<void>();
  @Output() save = new EventEmitter<void>();
  @Output() editBranch = new EventEmitter<SucursalAdmin>();
}
