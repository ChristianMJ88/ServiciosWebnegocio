package com.techprotech.agenda.modulos.plataforma.aplicacion;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.plataforma.api.dto.AsignarSuscripcionPlataformaRequest;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.PlanPlataformaEntidad;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.SuscripcionPlataformaEntidad;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.LimitePlataformaRepositorio;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.ModuloPlataformaRepositorio;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.PlanLimitePlataformaRepositorio;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.PlanModuloPlataformaRepositorio;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.PlanPlataformaRepositorio;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.SuscripcionPlataformaRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicioSuscripcionesPlataformaTest {
    @Mock private ModuloPlataformaRepositorio modulos;
    @Mock private PlanPlataformaRepositorio planes;
    @Mock private PlanModuloPlataformaRepositorio planModulos;
    @Mock private LimitePlataformaRepositorio limites;
    @Mock private PlanLimitePlataformaRepositorio planLimites;
    @Mock private SuscripcionPlataformaRepositorio suscripciones;
    @Mock private EmpresaRepositorio empresas;
    @Mock private ServicioAuditoriaPlataforma auditoria;

    private ServicioSuscripcionesPlataforma servicio;
    private EmpresaEntidad empresa;
    private PlanPlataformaEntidad plan;

    @BeforeEach
    void preparar() {
        servicio = new ServicioSuscripcionesPlataforma(modulos, planes, planModulos, limites, planLimites, suscripciones, empresas, auditoria);
        empresa = new EmpresaEntidad();
        empresa.setId(7L);
        empresa.setNombre("Nail Art Studio");
        plan = new PlanPlataformaEntidad();
        plan.setId(3L);
        plan.setNombre("Fluora Pro");
        plan.setActivo(true);
        plan.setMoneda("MXN");
        plan.setPrecioMensual(new BigDecimal("499.00"));
        plan.setPrecioAnual(new BigDecimal("4990.00"));
    }

    @Test
    void reemplazaLaSuscripcionVigenteYConservaSuHistorial() {
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 3, 12, 0);
        SuscripcionPlataformaEntidad anterior = new SuscripcionPlataformaEntidad();
        anterior.setId(11L);
        anterior.setEmpresaId(empresa.getId());
        anterior.setEstado("ACTIVA");
        when(empresas.findById(empresa.getId())).thenReturn(Optional.of(empresa));
        when(planes.findById(plan.getId())).thenReturn(Optional.of(plan));
        when(suscripciones.findByEmpresaIdAndEstadoIn(empresa.getId(), List.of("ACTIVA", "PRUEBA")))
                .thenReturn(List.of(anterior));
        when(suscripciones.save(any())).thenAnswer(invocacion -> {
            SuscripcionPlataformaEntidad guardada = invocacion.getArgument(0);
            guardada.setId(12L);
            return guardada;
        });

        var respuesta = servicio.asignar(2L, new AsignarSuscripcionPlataformaRequest(
                empresa.getId(), plan.getId(), "MENSUAL", null, null, "ACTIVA", inicio, null, null));

        assertThat(anterior.getEstado()).isEqualTo("REEMPLAZADA");
        assertThat(anterior.getTerminaEn()).isNotNull();
        assertThat(respuesta.precioAcordado()).isEqualByComparingTo("499.00");
        assertThat(respuesta.moneda()).isEqualTo("MXN");
        verify(suscripciones).saveAll(List.of(anterior));
        verify(auditoria).registrar(eq(2L), eq("ASIGNAR"), eq("SUSCRIPCION_SAAS"), eq("12"), any());
    }

    @Test
    void rechazaUnPlanInactivo() {
        plan.setActivo(false);
        when(empresas.findById(empresa.getId())).thenReturn(Optional.of(empresa));
        when(planes.findById(plan.getId())).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> servicio.asignar(2L, new AsignarSuscripcionPlataformaRequest(
                empresa.getId(), plan.getId(), "MENSUAL", null, null, "ACTIVA",
                LocalDateTime.of(2026, 10, 3, 12, 0), null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no está activo");

        verifyNoInteractions(suscripciones);
    }

    @Test
    void exigeFechaFinalParaUnaPrueba() {
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 3, 12, 0);
        when(empresas.findById(empresa.getId())).thenReturn(Optional.of(empresa));
        when(planes.findById(plan.getId())).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> servicio.asignar(2L, new AsignarSuscripcionPlataformaRequest(
                empresa.getId(), plan.getId(), "MENSUAL", null, null, "PRUEBA", inicio, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("fecha final de prueba");

        verifyNoInteractions(suscripciones);
    }

    @Test
    void publicaSoloPlanesDisponiblesEnLaFechaActual() {
        LocalDateTime ahora = LocalDateTime.now(java.time.ZoneOffset.UTC);
        plan.setPublico(true);
        plan.setDestacado(true);
        plan.setVigenteDesde(ahora.minusDays(1));
        plan.setVigenteHasta(ahora.plusDays(1));
        PlanPlataformaEntidad expirado = nuevoPlan(4L, "EXPIRADO");
        expirado.setVigenteHasta(ahora.minusMinutes(1));
        PlanPlataformaEntidad futuro = nuevoPlan(5L, "FUTURO");
        futuro.setVigenteDesde(ahora.plusDays(1));
        PlanPlataformaEntidad privado = nuevoPlan(6L, "PRIVADO");
        privado.setPublico(false);
        when(planes.findAllByOrderByActivoDescNombreAsc()).thenReturn(List.of(expirado, futuro, privado, plan));
        when(modulos.findAll()).thenReturn(List.of());
        when(limites.findAll()).thenReturn(List.of());
        when(planModulos.findByPlanId(any())).thenReturn(List.of());
        when(planLimites.findByPlanId(any())).thenReturn(List.of());

        var publicados = servicio.planesPublicos();

        assertThat(publicados).extracting("id").containsExactly(plan.getId());
    }

    @Test
    void creaLaPruebaDeRegistroConLaDuracionConfiguradaEnElPlan() {
        plan.setPublico(true);
        plan.setDiasPrueba(9);
        when(planes.findById(plan.getId())).thenReturn(Optional.of(plan));
        when(suscripciones.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        SuscripcionPlataformaEntidad prueba = servicio.crearSuscripcionRegistro(empresa.getId(), plan.getId());

        assertThat(prueba.getEmpresaId()).isEqualTo(empresa.getId());
        assertThat(prueba.getEstado()).isEqualTo("PRUEBA");
        assertThat(prueba.getPrecioAcordado()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(prueba.getPruebaHasta()).isEqualTo(prueba.getIniciaEn().plusDays(9));
        assertThat(prueba.getReferenciaExterna()).isEqualTo("ONBOARDING");
    }

    @Test
    void dejaPendienteDePagoElRegistroCuandoElPlanNoOfrecePrueba() {
        plan.setPublico(true);
        plan.setDiasPrueba(0);
        when(planes.findById(plan.getId())).thenReturn(Optional.of(plan));
        when(suscripciones.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        SuscripcionPlataformaEntidad pendiente = servicio.crearSuscripcionRegistro(empresa.getId(), plan.getId());

        assertThat(pendiente.getEstado()).isEqualTo("PENDIENTE_PAGO");
        assertThat(pendiente.getPrecioAcordado()).isEqualByComparingTo(plan.getPrecioMensual());
        assertThat(pendiente.getPruebaHasta()).isNull();
    }

    private PlanPlataformaEntidad nuevoPlan(Long id, String codigo) {
        PlanPlataformaEntidad otro = new PlanPlataformaEntidad();
        otro.setId(id);
        otro.setCodigo(codigo);
        otro.setNombre(codigo);
        otro.setMoneda("MXN");
        otro.setPrecioMensual(new BigDecimal("99.00"));
        otro.setActivo(true);
        otro.setPublico(true);
        return otro;
    }
}
