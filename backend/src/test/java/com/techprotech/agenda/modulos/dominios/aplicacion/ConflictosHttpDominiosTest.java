package com.techprotech.agenda.modulos.dominios.aplicacion;
import com.techprotech.agenda.modulos.dominios.api.*;
import com.techprotech.agenda.modulos.dominios.infraestructura.entidad.EmpresaDominioEntidad;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import static org.mockito.Mockito.*;import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class ConflictosHttpDominiosTest {
 @Test void conflictosDelRepositorioSon409SinDetallesInternos()throws Exception{
  for(RuntimeException ex:new RuntimeException[]{new ObjectOptimisticLockingFailureException(EmpresaDominioEntidad.class,7L),new PessimisticLockingFailureException("private SQL detail")}){
   var servicio=mock(ServicioDominios.class);when(servicio.cola(null,0,20)).thenThrow(ex);var mvc=MockMvcBuilders.standaloneSetup(new ControladorDominiosPlataforma(servicio)).setControllerAdvice(new ManejadorConflictosDominio()).build();
   mvc.perform(get("/api/v1/plataforma/dominios")).andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value("La solicitud cambió durante otra operación. Actualiza e inténtalo de nuevo."));
  }
 }
}
