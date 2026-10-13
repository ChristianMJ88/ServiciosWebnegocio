import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { environment } from '../../../../environments/environment';

interface ConfiguracionReservas { anticipacionMinimaMinutos:number; ventanaReservaDias:number; liberacionSinConfirmacionHoras:number; minutosApartadoPago:number; horasTopeProcesamientoPago:number; }
@Component({
  selector:'app-admin-reservation-settings', standalone:true,
  imports:[FormsModule,MatButtonModule,MatCardModule,MatFormFieldModule,MatInputModule],
  template:`<mat-card class="workspace-card">
    <details><summary>Política de reservas del negocio</summary>
      <p>Configura cuándo pueden reservar tus clientes y cuánto tiempo se conserva un horario apartado.</p>
      @if(configuracion(); as valores) {
        <form (ngSubmit)="guardar(valores)">
          <mat-form-field appearance="outline"><mat-label>Anticipación mínima (minutos)</mat-label><input matInput type="number" name="anticipacion" [(ngModel)]="valores.anticipacionMinimaMinutos" required></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Ventana de reserva (días)</mat-label><input matInput type="number" name="ventana" [(ngModel)]="valores.ventanaReservaDias" required></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Liberar sin confirmación (horas antes)</mat-label><input matInput type="number" name="liberacion" [(ngModel)]="valores.liberacionSinConfirmacionHoras" required></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Apartado para pago (minutos)</mat-label><input matInput type="number" name="apartado" [(ngModel)]="valores.minutosApartadoPago" required></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Tope de pago en proceso (horas)</mat-label><input matInput type="number" name="procesamiento" [(ngModel)]="valores.horasTopeProcesamientoPago" required></mat-form-field>
          <button mat-flat-button type="submit" [disabled]="guardando()">{{guardando()?'Guardando…':'Guardar política'}}</button>
        </form>
      }
      <p role="status">{{mensaje()}}</p>
    </details>
  </mat-card>`,
  styles:[`details{padding:1rem}summary{cursor:pointer;font-weight:700}form{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,240px),1fr));gap:1rem}button{justify-self:start}p{line-height:1.6}`]
})
export class AdminReservationSettingsComponent {
  private readonly http=inject(HttpClient);
  private readonly url=`${environment.apiBaseUrl}/admin/disponibilidad/configuracion-reservas`;
  readonly configuracion=signal<ConfiguracionReservas|null>(null);
  readonly guardando=signal(false); readonly mensaje=signal('');
  constructor() {
    this.http.get<ConfiguracionReservas>(this.url).subscribe({next:config=>this.configuracion.set(config),error:()=>this.mensaje.set('No fue posible cargar la política de reservas.')});
  }
  guardar(config:ConfiguracionReservas) {
    if(this.guardando()) return;
    this.guardando.set(true); this.mensaje.set('');
    this.http.patch<ConfiguracionReservas>(this.url,config).subscribe({
      next:actual=>{this.configuracion.set(actual);this.guardando.set(false);this.mensaje.set('Política guardada.');},
      error:err=>{this.guardando.set(false);this.mensaje.set(err?.error?.mensaje ?? err?.error?.message ?? 'Revisa los valores e intenta de nuevo.');}
    });
  }
}
