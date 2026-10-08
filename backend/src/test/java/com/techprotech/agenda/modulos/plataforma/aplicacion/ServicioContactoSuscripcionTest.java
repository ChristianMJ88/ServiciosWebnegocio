package com.techprotech.agenda.modulos.plataforma.aplicacion;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.SuscripcionPlataformaEntidad;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.SuscripcionPlataformaRepositorio;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class ServicioContactoSuscripcionTest {
    @Test void modificaSoloLaSuscripcionDelTenantSolicitado(){
        var repo=mock(SuscripcionPlataformaRepositorio.class);var s=new SuscripcionPlataformaEntidad();s.setEmpresaId(3L);
        when(repo.findFirstByEmpresaIdOrderByCreadaEnDesc(3L)).thenReturn(Optional.of(s));
        new ServicioContactoSuscripcion(repo).guardar(3L," Billing@Example.Test ");
        assertThat(s.getCorreoFacturacion()).isEqualTo("billing@example.test");verify(repo).save(s);
        verify(repo,never()).findFirstByEmpresaIdOrderByCreadaEnDesc(4L);
    }
}
