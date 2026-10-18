package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.caja.aplicacion.PlanPagosVenta;
import com.techprotech.agenda.modulos.caja.api.dto.RegistrarVentaRequest;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class PagosPrevistosPoint {
    private final ObjectMapper mapper;
    public record Documento(int v,List<Parte> partes,String solicitudSha256) {}
    public record Parte(String metodoPago,String monto,String montoRecibido,String referencia) {}
    public String escribir(PlanPagosVenta plan) {return escribir(plan,null);}
    public String escribir(PlanPagosVenta plan,RegistrarVentaRequest solicitud) {
        try {return mapper.writeValueAsString(new Documento(1,plan.previstas().stream()
            .map(p -> new Parte(p.metodoPago(),p.monto().toPlainString(),p.montoRecibido()==null?null:p.montoRecibido().toPlainString(),p.referencia())).toList(),solicitud==null?null:huella(solicitud)));}
        catch(Exception e){throw new IllegalStateException("No se pudo conservar el reparto Point",e);}
    }
    public boolean mismaSolicitud(String json,RegistrarVentaRequest solicitud) {
        if(json==null)return false;
        try {return java.util.Objects.equals(mapper.readValue(json,Documento.class).solicitudSha256(),huella(solicitud));}
        catch(Exception e){return false;}
    }
    private String huella(RegistrarVentaRequest solicitud) {
        try {
            var canonica=new RegistrarVentaRequest(solicitud.sucursalId(),null,solicitud.clienteId(),solicitud.productos(),solicitud.metodoPago(),solicitud.referencia(),solicitud.observaciones(),solicitud.ventaPendienteId(),solicitud.citasNormalizadas(),solicitud.pagos());
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(canonica)));
        }catch(Exception e){throw new IllegalStateException("No se pudo identificar la solicitud Point",e);}
    }
    public List<PlanPagosVenta.Parte> leer(String json) {
        if(json==null) return List.of(); // órdenes históricas: toda la venta correspondía a Point.
        try {
            var d=mapper.readValue(json,Documento.class);
            if(d.v()!=1 || d.partes()==null || d.partes().size()>2) throw new IllegalArgumentException("Formato no soportado");
            var pagos=d.partes().stream().map(p -> new RegistrarVentaRequest.PagoVentaRequest(p.metodoPago(),new BigDecimal(p.monto()),p.montoRecibido()==null?null:new BigDecimal(p.montoRecibido()),p.referencia())).toList();
            if(pagos.isEmpty()) return List.of();
            var total=pagos.stream().map(RegistrarVentaRequest.PagoVentaRequest::monto).reduce(BigDecimal.ZERO,BigDecimal::add);
            return PlanPagosVenta.resolver(total,new RegistrarVentaRequest(null,null,null,List.of(),null,null,null,null,null,pagos),false).partes();
        } catch(Exception e) {log.error("Reparto Point inválido: requiere revisión, no se acredita parcialmente");throw new IllegalStateException("El reparto Point requiere revisión",e);}
    }
}
