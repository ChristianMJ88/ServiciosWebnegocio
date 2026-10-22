import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize, Observable } from 'rxjs';
import { DomainsService, DomainPage, DomainView, DnsRecord, domainStateLabel, domainErrorLabel } from '../../core/domains/domains.service';
import { PlatformAuthService } from '../../core/platform/platform-auth.service';
@Component({selector:'app-platform-domains',standalone:true,imports:[FormsModule,RouterLink],templateUrl:'./platform-domains.component.html',styleUrls:['../admin-dashboard/site/site-domains.component.css','./platform-domains.component.css']})
export class PlatformDomainsComponent implements OnInit {
 private readonly api=inject(DomainsService);readonly auth=inject(PlatformAuthService);
 readonly data=signal<DomainPage|null>(null);readonly busy=signal(false);readonly error=signal<string|null>(null);readonly selected=signal<DomainView|null>(null);readonly retiring=signal<number|null>(null);readonly stateLabel=domainStateLabel;readonly errorLabel=domainErrorLabel;
 estado='';pagina=0;records: DnsRecord[]=[];
 ngOnInit(){this.load();}
 load(){this.busy.set(true);this.api.queue(this.estado,this.pagina).pipe(finalize(()=>this.busy.set(false))).subscribe({next:d=>this.data.set(d),error:e=>this.fail(e)});}
 filter(){this.pagina=0;this.load();}
 page(step: number){this.pagina+=step;this.load();}
 edit(d: DomainView){this.selected.set(d);this.records=d.registros.length?d.registros.map(r=>({...r})):[{tipo:'A',nombre:d.hostname,valor:''}];}
 addRecord(){this.records.push({tipo:'TXT',nombre:this.selected()?.hostname??'',valor:''});}
 save(){const d=this.selected();if(d)this.run(this.api.connect(d,this.records));}
 verify(d: DomainView){this.run(this.api.verify(d,true));}
 retire(d: DomainView){this.run(this.api.retire(d,true));this.retiring.set(null);}
 private run(operation: Observable<DomainView>){this.error.set(null);this.busy.set(true);operation.pipe(finalize(()=>this.busy.set(false))).subscribe({next:()=>{this.selected.set(null);this.load();},error:e=>{this.fail(e);this.load();}});}
 private fail(e: {error?:{mensaje?:string;detail?:string};status?:number}){this.error.set(e.error?.mensaje||e.error?.detail||(e.status===403?'Tu cuenta no tiene permiso para gestionar dominios.':'No fue posible completar la operación. Actualiza la cola e inténtalo de nuevo.'));}
}
