package com.techprotech.agenda.modulos.plataforma.aplicacion;

import com.techprotech.agenda.modulos.plataforma.api.dto.CapacidadLimiteEmpresaResponse;
import com.techprotech.agenda.modulos.plataforma.api.dto.CapacidadModuloEmpresaResponse;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.PlanPlataformaEntidad;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.SuscripcionPlataformaEntidad;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioCapacidadesEmpresaTest {
    @Mock private SuscripcionPlataformaRepositorio suscripciones;
    @Mock private PlanPlataformaRepositorio planes;
    @Mock private PlanModuloPlataformaRepositorio planModulos;
    @Mock private PlanLimitePlataformaRepositorio planLimites;
    private ServicioCapacidadesEmpresa servicio;
    private SuscripcionPlataformaEntidad suscripcion;
    private PlanPlataformaEntidad plan;

    @BeforeEach
    void preparar() {
        servicio=new ServicioCapacidadesEmpresa(suscripciones,planes,planModulos,planLimites);
        suscripcion=new SuscripcionPlataformaEntidad();suscripcion.setId(8L);suscripcion.setEmpresaId(3L);
        suscripcion.setPlanId(4L);suscripcion.setEstado("PRUEBA");
        suscripcion.setPruebaHasta(LocalDateTime.now(ZoneOffset.UTC).plusDays(5));
        plan=new PlanPlataformaEntidad();plan.setId(4L);plan.setCodigo("INICIAL");plan.setNombre("Inicial");
    }

    @Test
    void publicaModulosYLimitesDelPlanVigente() {
        prepararSuscripcionVigente();
        when(planModulos.findCapacidadesByPlanId(4L)).thenReturn(List.of(new CapacidadModuloEmpresaResponse("AGENDA","Agenda",null)));
        when(planLimites.findCapacidadesByPlanId(4L)).thenReturn(List.of(new CapacidadLimiteEmpresaResponse("SERVICIOS_MAX","Servicios","servicios",20L)));

        var respuesta=servicio.obtener(3L);

        assertThat(respuesta.suscripcionVigente()).isTrue();
        assertThat(respuesta.modulos()).extracting("codigo").containsExactly("AGENDA");
        assertThat(respuesta.limites().getFirst().valor()).isEqualTo(20L);
    }

    @Test
    void rechazaUnModuloQueElPlanNoIncluye() {
        prepararSuscripcionVigente();
        when(planModulos.findCapacidadesByPlanId(4L)).thenReturn(List.of());
        when(planLimites.findCapacidadesByPlanId(4L)).thenReturn(List.of());

        assertThatThrownBy(()->servicio.exigirModulo(3L,"CAJA"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("no incluye");
    }

    @Test
    void ignoraUnaPruebaExpirada() {
        suscripcion.setPruebaHasta(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1));
        when(suscripciones.findByEmpresaIdAndEstadoInOrderByCreadaEnDesc(3L,List.of("ACTIVA","PRUEBA","GRACIA")))
                .thenReturn(List.of(suscripcion));

        assertThat(servicio.obtener(3L).suscripcionVigente()).isFalse();
    }

    @Test
    void rechazaCrearCuandoElUsoAlcanzoElLimite() {
        prepararSuscripcionVigente();
        when(planModulos.findCapacidadesByPlanId(4L)).thenReturn(List.of());
        when(planLimites.findCapacidadesByPlanId(4L)).thenReturn(List.of(
                new CapacidadLimiteEmpresaResponse("SERVICIOS_MAX","Servicios","servicios",20L)));

        assertThatThrownBy(()->servicio.exigirLimite(3L,"SERVICIOS_MAX",20L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("20 servicios");
    }

    private void prepararSuscripcionVigente() {
        when(suscripciones.findByEmpresaIdAndEstadoInOrderByCreadaEnDesc(3L,List.of("ACTIVA","PRUEBA","GRACIA")))
                .thenReturn(List.of(suscripcion));
        when(planes.findById(4L)).thenReturn(Optional.of(plan));
    }
}
