package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.ConciliacionPagoEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.ReporteLiberacionPagoEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago.ClienteReportesMercadoPago;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServicioReportesLiberacionTest {
    @Test
    void completaColumnasSinEliminarLasQueYaConfiguroElTenant() {
        ClienteReportesMercadoPago cliente=mock(ClienteReportesMercadoPago.class);
        var actual=new ClienteReportesMercadoPago.ConfiguracionReporte("reporte-negocio", "GMT-06", false, "es",
                List.of("DATE", "CUSTOM_COLUMN"), "daily", 2, "0", true, false);
        when(cliente.consultarConfiguracion(7L)).thenReturn(actual);
        when(cliente.guardarConfiguracion(eq(7L), any(), eq(false))).thenAnswer(invocacion -> invocacion.getArgument(1));
        ServicioReportesLiberacion servicio=new ServicioReportesLiberacion(cliente,
                mock(ReporteLiberacionPagoRepositorio.class), mock(ConciliacionPagoRepositorio.class),
                mock(ProveedorPagoRepositorio.class));
        var request=new com.techprotech.agenda.modulos.pagos.api.dto.GuardarConfiguracionReporteLiberacionRequest(
                "reporte-negocio", "GMT-06", "daily", 2, "0", true, false);

        var respuesta=servicio.guardarConfiguracion(7L,request,false);

        assertThat(respuesta.listaParaConciliar()).isTrue();
        assertThat(respuesta.columnas()).contains("CUSTOM_COLUMN", "SOURCE_ID", "MP_FEE_AMOUNT", "TAXES_AMOUNT");
    }

    @Test
    void impideProgramarUnaConfiguracionIncompleta() {
        ClienteReportesMercadoPago cliente=mock(ClienteReportesMercadoPago.class);
        when(cliente.consultarConfiguracion(7L)).thenReturn(new ClienteReportesMercadoPago.ConfiguracionReporte(
                "reporte-negocio", "GMT-06", false, "es", List.of("DATE"), "daily", 2, "0", true, false));
        ServicioReportesLiberacion servicio=new ServicioReportesLiberacion(cliente,
                mock(ReporteLiberacionPagoRepositorio.class), mock(ConciliacionPagoRepositorio.class),
                mock(ProveedorPagoRepositorio.class));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> servicio.cambiarProgramacion(7L,true))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verify(cliente,never()).cambiarProgramacion(anyLong(),anyBoolean());
    }

    @Test
    void informaLasColumnasFaltantesDeLaConfiguracionDelTenant() {
        ClienteReportesMercadoPago cliente=mock(ClienteReportesMercadoPago.class);
        when(cliente.consultarConfiguracion(7L)).thenReturn(new ClienteReportesMercadoPago.ConfiguracionReporte(
                "release-report-7", "GMT-06", false, "es",
                java.util.List.of("DATE", "SOURCE_ID", "GROSS_AMOUNT", "NET_CREDIT_AMOUNT"),
                "monthly", 2, "1", true, false));
        ServicioReportesLiberacion servicio=new ServicioReportesLiberacion(cliente,
                mock(ReporteLiberacionPagoRepositorio.class), mock(ConciliacionPagoRepositorio.class),
                mock(ProveedorPagoRepositorio.class));

        var respuesta=servicio.consultarConfiguracion(7L);

        assertThat(respuesta.listaParaConciliar()).isFalse();
        assertThat(respuesta.programado()).isFalse();
        assertThat(respuesta.columnasFaltantes()).containsExactly(
                "DESCRIPTION", "EXTERNAL_REFERENCE", "MP_FEE_AMOUNT", "RECORD_TYPE", "TAXES_AMOUNT");
    }

    @Test
    void convierteEnDefinitivaLaConciliacionAlImportarElReporte() {
        ClienteReportesMercadoPago cliente=mock(ClienteReportesMercadoPago.class);
        ReporteLiberacionPagoRepositorio reportes=mock(ReporteLiberacionPagoRepositorio.class);
        ConciliacionPagoRepositorio conciliaciones=mock(ConciliacionPagoRepositorio.class);
        ReporteLiberacionPagoEntidad reporte=new ReporteLiberacionPagoEntidad();
        reporte.setId(3L); reporte.setEmpresaId(7L); reporte.setTareaExternaId("99"); reporte.setEstado("GENERANDO");
        reporte.setFechaDesde(LocalDateTime.now().minusDays(1)); reporte.setFechaHasta(LocalDateTime.now());
        when(reportes.findByIdAndEmpresaId(3L,7L)).thenReturn(Optional.of(reporte));
        when(reportes.save(any())).thenAnswer(i->i.getArgument(0));
        when(cliente.consultar(7L,"99")).thenReturn(new ClienteReportesMercadoPago.TareaReporte("99","1701","processed","fluora.csv"));
        String csv="DATE,SOURCE_ID,EXTERNAL_REFERENCE,RECORD_TYPE,DESCRIPTION,NET_CREDIT_AMOUNT,GROSS_AMOUNT,MP_FEE_AMOUNT,TAXES_AMOUNT\n"
                +"2026-10-03T10:00:00-06:00,PAY-1,orden-1,release,payment,471.44,500.00,-23.56,-5.00\n";
        when(cliente.descargar(7L,"fluora.csv")).thenReturn(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        ConciliacionPagoEntidad conciliacion=new ConciliacionPagoEntidad(); conciliacion.setEmpresaId(7L);
        when(conciliaciones.findByEmpresaIdAndReferenciaPagoExterna(7L,"PAY-1")).thenReturn(Optional.of(conciliacion));
        ServicioReportesLiberacion servicio=new ServicioReportesLiberacion(cliente,reportes,conciliaciones,mock(ProveedorPagoRepositorio.class));

        var respuesta=servicio.sincronizar(7L,3L);

        assertThat(respuesta.estado()).isEqualTo("PROCESADO");
        assertThat(respuesta.filasProcesadas()).isEqualTo(1);
        assertThat(conciliacion.isLiquidacionDefinitiva()).isTrue();
        assertThat(conciliacion.getFuente()).isEqualTo("REPORTE_LIBERACIONES");
        assertThat(conciliacion.getComisionProveedorTotal()).isEqualByComparingTo(new BigDecimal("23.56"));
        assertThat(conciliacion.getRetenciones()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(conciliacion.getImporteNeto()).isEqualByComparingTo(new BigDecimal("471.44"));
    }
}
