import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, Observable } from 'rxjs';
import { DomainsService, DomainPanel, DomainView, domainStateLabel, domainErrorLabel } from '../../../core/domains/domains.service';
@Component({selector:'app-site-domains',standalone:true,imports:[FormsModule],templateUrl:'./site-domains.component.html',styleUrl:'./site-domains.component.css'})
export class SiteDomainsComponent implements OnInit {
 private readonly api=inject(DomainsService);
 readonly panel=signal<DomainPanel|null>(null);readonly busy=signal(false);readonly error=signal<string|null>(null);readonly message=signal<string|null>(null);readonly retiring=signal<number|null>(null);
 hostname='';readonly stateLabel=domainStateLabel;readonly errorLabel=domainErrorLabel;
 ngOnInit(){this.load();}
 load(){this.busy.set(true);this.api.panel().pipe(finalize(()=>this.busy.set(false))).subscribe({next:p=>this.panel.set(p),error:e=>this.fail(e)});}
 request(){if(this.busy()||!this.hostname.trim()||!this.panel()?.puedeAgregar)return;this.run(this.api.request(this.hostname.trim()),'Solicitud creada. Copia el TXT en tu proveedor de DNS.');}
 verify(d: DomainView){this.run(this.api.verify(d),'Comprobación terminada. Revisa el estado actualizado.');}
 retire(d: DomainView){this.run(this.api.retire(d),'Dominio retirado del sitio público.');this.retiring.set(null);}
 private run(operation: Observable<DomainView>,message: string){this.busy.set(true);this.error.set(null);this.message.set(null);operation.pipe(finalize(()=>this.busy.set(false))).subscribe({next:()=>{this.message.set(message);this.hostname='';this.load();},error:e=>{this.fail(e);this.load();}});}
 async copy(value: string){try{await navigator.clipboard.writeText(value);this.message.set('Copiado al portapapeles.');}catch{this.error.set('Selecciona y copia el valor del registro.');}}
 private fail(e: {error?:{mensaje?:string;detail?:string};status?:number}){this.error.set(e.error?.mensaje||e.error?.detail||(e.status===429?'Espera un momento antes de volver a verificar.':'No fue posible completar la solicitud. Actualiza e inténtalo de nuevo.'));}
}
