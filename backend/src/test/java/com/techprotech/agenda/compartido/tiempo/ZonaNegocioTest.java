package com.techprotech.agenda.compartido.tiempo;

import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ZonaNegocioTest {
    final EmpresaRepositorio empresas = mock(EmpresaRepositorio.class);
    final SucursalRepositorio sucursales = mock(SucursalRepositorio.class);
    final ZonaNegocio zonas = new ZonaNegocio(empresas, sucursales);

    @Test void sinEmpresaDevuelveRespaldoSinConsultarRepositorios() {
        assertThat(zonas.de((Long) null, 3L)).isEqualTo(ZoneId.of("America/Mexico_City"));
        verifyNoInteractions(empresas, sucursales);
    }

    @Test void priorizaLaSucursalSinConsultarOtraEmpresa() {
        var sede = new SucursalEntidad(); sede.setZonaHoraria("America/Tijuana");
        when(sucursales.findByIdAndEmpresaId(3L, 7L)).thenReturn(Optional.of(sede));
        assertThat(zonas.de(7L, 3L)).isEqualTo(ZoneId.of("America/Tijuana"));
        verifyNoInteractions(empresas);
    }

    @Test void zonaAusenteOInvalidaRecurreALaEmpresa() {
        var sede = new SucursalEntidad(); sede.setEmpresaId(7L); sede.setZonaHoraria("zona-invalida");
        var empresa = new EmpresaEntidad(); empresa.setId(7L); empresa.setZonaHoraria("America/Bogota");
        when(sucursales.findByIdAndEmpresaId(3L, 7L)).thenReturn(Optional.of(sede));
        when(empresas.findById(7L)).thenReturn(Optional.of(empresa));
        assertThat(zonas.de(7L, 3L)).isEqualTo(ZoneId.of("America/Bogota"));
        sede.setZonaHoraria(null);
        assertThat(zonas.de(7L, 3L)).isEqualTo(ZoneId.of("America/Bogota"));
    }

    @Test void noUsaUnaSucursalDeOtroTenantYRespaldaSoloAlFinal() {
        var empresa = new EmpresaEntidad(); empresa.setId(7L); empresa.setZonaHoraria("America/Bogota");
        var ajena = new SucursalEntidad(); ajena.setEmpresaId(8L); ajena.setZonaHoraria("Europe/Madrid");
        assertThat(zonas.de(empresa, ajena)).isEqualTo(ZoneId.of("America/Bogota"));
        assertThat(zonas.de(7L, 3L)).isEqualTo(ZoneId.of("America/Mexico_City"));
        verify(sucursales).findByIdAndEmpresaId(3L, 7L);
        verify(sucursales, never()).findById(any());
    }
}
