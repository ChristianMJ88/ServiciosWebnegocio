package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.pagos.api.dto.ReporteLiberacionResponse;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.ConciliacionPagoEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.ProveedorPagoEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.ReporteLiberacionPagoEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago.ClienteReportesMercadoPago;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.ConciliacionPagoRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.ProveedorPagoRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.ReporteLiberacionPagoRepositorio;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.List;

@Service
public class ServicioReportesLiberacion {
    private final ClienteReportesMercadoPago cliente;
    private final ReporteLiberacionPagoRepositorio reportes;
    private final ConciliacionPagoRepositorio conciliaciones;
    private final ProveedorPagoRepositorio proveedores;

    public ServicioReportesLiberacion(ClienteReportesMercadoPago cliente, ReporteLiberacionPagoRepositorio reportes,
                                      ConciliacionPagoRepositorio conciliaciones, ProveedorPagoRepositorio proveedores) {
        this.cliente=cliente; this.reportes=reportes; this.conciliaciones=conciliaciones; this.proveedores=proveedores;
    }

    @Transactional
    public ReporteLiberacionResponse solicitar(Long empresaId, OffsetDateTime desde, OffsetDateTime hasta) {
        if (!desde.isBefore(hasta)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha inicial debe ser anterior a la final");
        if (Duration.between(desde, hasta).toDays() > 60) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El reporte no puede abarcar más de 60 días");
        ProveedorPagoEntidad proveedor = proveedor();
        var tarea = cliente.crear(empresaId, desde, hasta);
        if (tarea.tareaId() == null) throw new IllegalStateException("Mercado Pago no devolvió el identificador de la tarea");
        ReporteLiberacionPagoEntidad entidad = new ReporteLiberacionPagoEntidad();
        entidad.setEmpresaId(empresaId); entidad.setProveedorPagoId(proveedor.getId()); entidad.setTareaExternaId(tarea.tareaId());
        entidad.setReporteExternoId(tarea.reporteId()); entidad.setNombreArchivo(tarea.archivo());
        entidad.setEstado(normalizar(tarea.estado())); entidad.setFechaDesde(utc(desde)); entidad.setFechaHasta(utc(hasta));
        return respuesta(reportes.save(entidad));
    }

    @Transactional
    public ReporteLiberacionResponse sincronizar(Long empresaId, Long reporteId) {
        ReporteLiberacionPagoEntidad entidad = reportes.findByIdAndEmpresaId(reporteId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado"));
        try {
            var remota = cliente.consultar(empresaId, entidad.getTareaExternaId());
            entidad.setEstado(normalizar(remota.estado())); entidad.setReporteExternoId(remota.reporteId());
            if (remota.archivo()!=null) entidad.setNombreArchivo(remota.archivo());
            if ("LISTO".equals(entidad.getEstado()) && entidad.getNombreArchivo()!=null) {
                entidad.setFilasProcesadas(importar(empresaId, cliente.descargar(empresaId, entidad.getNombreArchivo())));
                entidad.setEstado("PROCESADO"); entidad.setProcesadoEn(LocalDateTime.now(ZoneOffset.UTC));
            }
            entidad.setErrorUltimo(null);
        } catch (Exception ex) {
            entidad.setEstado("ERROR"); entidad.setErrorUltimo(limitar(ex.getMessage(), 1000));
        }
        return respuesta(reportes.save(entidad));
    }

    @Transactional(readOnly=true)
    public List<ReporteLiberacionResponse> listar(Long empresaId) {
        return reportes.findTop50ByEmpresaIdOrderBySolicitadoEnDesc(empresaId).stream().map(this::respuesta).toList();
    }

    private int importar(Long empresaId, byte[] contenido) throws Exception {
        int actualizadas=0;
        try (var reader=new InputStreamReader(new ByteArrayInputStream(contenido), StandardCharsets.UTF_8);
             var parser=CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get().parse(reader)) {
            for (CSVRecord fila: parser) {
                String source=campo(fila,"SOURCE_ID");
                ConciliacionPagoEntidad conciliacion=source==null?null:conciliaciones.findByEmpresaIdAndReferenciaPagoExterna(empresaId,source).orElse(null);
                if (conciliacion==null) continue;
                conciliacion.setImporteBruto(decimal(fila,"GROSS_AMOUNT",conciliacion.getImporteBruto()));
                conciliacion.setComisionProveedorTotal(absoluto(fila,"MP_FEE_AMOUNT"));
                conciliacion.setImporteNeto(decimal(fila,"NET_CREDIT_AMOUNT",conciliacion.getImporteNeto()));
                conciliacion.setFuente("REPORTE_LIBERACIONES"); conciliacion.setLiquidacionDefinitiva(true);
                conciliacion.setEstado("CONCILIADA"); conciliacion.setLiquidadoEn(fecha(campo(fila,"DATE")));
                conciliaciones.save(conciliacion); actualizadas++;
            }
        }
        return actualizadas;
    }

    private ProveedorPagoEntidad proveedor(){return proveedores.findByCodigoAndActivoTrue("MERCADO_PAGO").orElseThrow(()->new IllegalStateException("Proveedor Mercado Pago no disponible"));}
    private String normalizar(String estado){if(estado==null)return "SOLICITADO";return switch(estado.toLowerCase()){case "processed","available"->"LISTO";case "failed"->"ERROR";default->"GENERANDO";};}
    private LocalDateTime utc(OffsetDateTime fecha){return fecha.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();}
    private String campo(CSVRecord fila,String nombre){if(!fila.isMapped(nombre))return null;String v=fila.get(nombre);return v==null||v.isBlank()?null:v.trim();}
    private BigDecimal decimal(CSVRecord fila,String nombre,BigDecimal respaldo){String v=campo(fila,nombre);if(v==null)return respaldo;try{return new BigDecimal(v).abs();}catch(Exception ex){return respaldo;}}
    private BigDecimal absoluto(CSVRecord fila,String nombre){return decimal(fila,nombre,null);}
    private LocalDateTime fecha(String valor){try{return valor==null?LocalDateTime.now(ZoneOffset.UTC):OffsetDateTime.parse(valor).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();}catch(Exception ex){return LocalDateTime.now(ZoneOffset.UTC);}}
    private String limitar(String valor,int max){if(valor==null)return "Error sin detalle";return valor.length()<=max?valor:valor.substring(0,max);}
    private ReporteLiberacionResponse respuesta(ReporteLiberacionPagoEntidad r){return new ReporteLiberacionResponse(r.getId(),r.getEstado(),r.getTareaExternaId(),r.getReporteExternoId(),r.getNombreArchivo(),r.getFechaDesde(),r.getFechaHasta(),r.getFilasProcesadas(),r.getErrorUltimo(),r.getSolicitadoEn(),r.getProcesadoEn());}
}
