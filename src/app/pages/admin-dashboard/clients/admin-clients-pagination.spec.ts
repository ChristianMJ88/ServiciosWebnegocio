import { TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { ClientsService, PaginaClientesAdmin } from '../../../core/admin/clients.service';
import { AdminClientsSectionComponent } from './admin-clients-section.component';

describe('Paginación de clientes',()=>{
  it('usa el tamaño devuelto por backend y reinicia la página al buscar',()=>{
    const respuesta:PaginaClientesAdmin={contenido:[{id:1,nombreCompleto:'Cliente',correo:'',telefono:'',aceptaWhatsapp:false,notas:null,totalCitas:0,ultimaCita:null}],pagina:1,tamano:10,totalElementos:11,totalPaginas:2};
    const listar=vi.fn(()=>of(respuesta));
    TestBed.configureTestingModule({providers:[{provide:ClientsService,useValue:{listar}}]});
    const c=TestBed.runInInjectionContext(()=>new AdminClientsSectionComponent());
    c.cargar(1); expect(c.inicioPagina()).toBe(11); expect(c.finPagina()).toBe(11);
    c.paginaSiguiente(); expect(listar).toHaveBeenCalledTimes(1);
    c.filtro='Cliente'; c.buscar(); expect(listar).toHaveBeenLastCalledWith('Cliente',0,undefined,{});
  });
  it('impide peticiones simultáneas al cambiar de página',()=>{
    const pendiente=new Subject<PaginaClientesAdmin>(); const listar=vi.fn(()=>pendiente);
    TestBed.configureTestingModule({providers:[{provide:ClientsService,useValue:{listar}}]});
    const c=TestBed.runInInjectionContext(()=>new AdminClientsSectionComponent());
    c.totalPaginas.set(3); c.cargar(); c.paginaSiguiente(); expect(listar).toHaveBeenCalledTimes(1);
  });
});
