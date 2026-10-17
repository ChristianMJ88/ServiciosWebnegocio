import {TestBed} from '@angular/core/testing';
import {of,throwError} from 'rxjs';
import {vi} from 'vitest';
import {AdminPolicyComponent} from './admin-policy.component';
import {BusinessPolicyService} from '../../../core/policies/business-policy.service';
describe('Editor de política: errores backend',()=>{
  it('presenta el 409 de carga y publicación sin reemplazar su mensaje',()=>{
    const api={admin:vi.fn(()=>throwError(()=>({status:409,error:{codigo:'ERROR_NEGOCIO',mensaje:'Configura el sitio antes de publicar'}}))),publish:vi.fn(()=>throwError(()=>({status:409,error:{codigo:'ERROR_NEGOCIO',mensaje:'La política cambió, actualiza'}})))};
    TestBed.configureTestingModule({providers:[{provide:BusinessPolicyService,useValue:api}]});
    const c=TestBed.runInInjectionContext(()=>new AdminPolicyComponent());c.ngOnInit();expect(c.error()).toBe('Configura el sitio antes de publicar');
    c.draft={} as any;c.preview.set('Texto revisado');c.publish();expect(c.error()).toBe('La política cambió, actualiza');
  });
});
