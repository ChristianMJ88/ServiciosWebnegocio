import { TestBed } from '@angular/core/testing';import { provideRouter } from '@angular/router';import { of } from 'rxjs';import { vi } from 'vitest';
import { PlatformDomainsComponent } from './platform-domains.component';import { DomainsService,DomainView } from '../../core/domains/domains.service';import { PlatformAuthService } from '../../core/platform/platform-auth.service';
it('pagina y filtra la cola mediante backend sin paginación local',async()=>{
 const api={queue:vi.fn(()=>of({content:[],page:{number:1,size:20,totalElements:45,totalPages:3}}))};
 await TestBed.configureTestingModule({imports:[PlatformDomainsComponent],providers:[provideRouter([]),{provide:DomainsService,useValue:api},{provide:PlatformAuthService,useValue:{}}]}).compileComponents();const f=TestBed.createComponent(PlatformDomainsComponent);f.detectChanges();await f.whenStable();expect(f.nativeElement.textContent).toContain('45 solicitudes');f.componentInstance.page(1);expect(api.queue).toHaveBeenLastCalledWith('',1);f.componentInstance.estado='VERIFICADO';f.componentInstance.filter();expect(api.queue).toHaveBeenLastCalledWith('VERIFICADO',0);
});
