package com.techprotech.agenda.modulos.admin.aplicacion;

import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.admin.api.ControladorAdminCitas;
import com.techprotech.agenda.compartido.tiempo.ZonaNegocio;
import com.techprotech.agenda.seguridad.jwt.UsuarioAutenticado;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumenAdminFinanzasPermisosTest {
    @Mock CitaRepositorio citaRepositorio;
    @Mock ServicioReporteCobroCitas reporteCobros;
    @Mock ZonaNegocio zonasNegocio;
    @Mock Clock reloj;
    @InjectMocks ServicioAdminCitas servicio;

    @ParameterizedTest @ValueSource(strings={"EMPRESA","PLATAFORMA"})
    void elPanelSinReportesConservaConteosSinConsultarImportes(String ambito) {
        Long empresa="PLATAFORMA".equals(ambito)?null:7L;
        var usuario=new UsuarioAutenticado(null,11L,empresa,ambito,List.of("ADMIN"),List.of("PANEL_ADMIN_ACCESO"),List.of());
        var r=new ControladorAdminCitas(servicio).resumen(usuario,null,null);
        assertThat(r.totalCitas()).isZero();assertThat(r.finanzasCitas()).isEmpty();assertThat(r.puedeVerFinanzas()).isFalse();
        verifyNoInteractions(reporteCobros);
    }

    @Test void permisoEfectivoConsultaReportesSinDependerDelNombreDelRol() {
        var usuario=new UsuarioAutenticado(null,11L,7L,"EMPRESA",List.of("ROL_PERSONALIZADO"),List.of("REPORTES_ADMIN_VER"),List.of(3L));
        var r=servicio.resumen(usuario,null,null);
        assertThat(r.puedeVerFinanzas()).isTrue();verify(reporteCobros).consultar(usuario,null,null);
    }

    @Test void elResumenOperativoConservaConteosYSucursalSinPermisoFinanciero() {
        var usuario=new UsuarioAutenticado(null,11L,7L,"EMPRESA",List.of(),List.of("PANEL_ADMIN_ACCESO"),List.of(3L));
        var c=new CitaEntidad();c.setSucursalId(3L);c.setEstado("FINALIZADA");c.setInicio(LocalDateTime.parse("2026-10-16T10:00"));c.setPrecio(BigDecimal.TEN);
        var ajena=new CitaEntidad();ajena.setSucursalId(4L);
        when(citaRepositorio.findByEmpresaIdOrderByInicioDesc(7L)).thenReturn(List.of(c,ajena));
        when(zonasNegocio.de(7L,3L)).thenReturn(ZoneOffset.UTC);
        when(reloj.withZone(ZoneOffset.UTC)).thenReturn(Clock.fixed(Instant.parse("2026-10-16T16:00:00Z"),ZoneOffset.UTC));
        var r=servicio.resumen(usuario,null,null);
        assertThat(r.totalCitas()).isEqualTo(1);assertThat(r.finalizadas()).isEqualTo(1);assertThat(r.citasHoy()).isEqualTo(1);
        assertThat(r.finanzasCitas()).isEmpty();verifyNoInteractions(reporteCobros);
    }
}
