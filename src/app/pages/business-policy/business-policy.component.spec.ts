import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { BusinessPolicyComponent } from './business-policy.component';
import { BusinessPolicyService } from '../../core/policies/business-policy.service';
import { TenantContextService } from '../../core/tenant/tenant-context.service';
import { vi } from 'vitest';

describe('Política pública del negocio', () => {
  it('consulta el tenant actual y presenta el texto como contenido sin ejecutar HTML', async () => {
    const get = vi.fn().mockReturnValue(of({nombreComercial:'Negocio',version:7,vigenteDesde:'2026-10-17T16:00:00Z',
      textoGenerado:'Reglas <script>alert(1)</script>',textoAdicional:'<img src=x onerror=alert(1)>', contactoReclamos:{correo:'n@example.com',telefono:null}}));
    await TestBed.configureTestingModule({imports:[BusinessPolicyComponent],providers:[provideRouter([]),
      {provide:BusinessPolicyService,useValue:{publicPolicy:get}},
      {provide:TenantContextService,useValue:{slug:()=> 'negocio-actual',nombreComercial:()=> 'Negocio',routeFor:()=> '/e/negocio-actual'}}
    ]}).compileComponents();
    const fixture = TestBed.createComponent(BusinessPolicyComponent); fixture.detectChanges();
    expect(get).toHaveBeenCalledWith('negocio-actual');
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('<script>alert(1)</script>');
    expect(el.querySelector('script')).toBeNull(); expect(el.querySelector('img')).toBeNull();
    expect(el.textContent).toContain('Versión 7');
  });
});
