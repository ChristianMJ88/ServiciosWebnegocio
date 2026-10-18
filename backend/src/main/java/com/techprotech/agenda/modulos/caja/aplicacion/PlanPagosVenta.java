package com.techprotech.agenda.modulos.caja.aplicacion;

import com.techprotech.agenda.modulos.caja.api.dto.RegistrarVentaRequest;
import java.math.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static com.techprotech.agenda.modulos.caja.aplicacion.CatalogosCaja.*;

/** Única validación del reparto. Los importes se resuelven sobre el total definitivo del servidor. */
public record PlanPagosVenta(List<Parte> partes) {
    public record Parte(String metodoPago,BigDecimal monto,BigDecimal montoRecibido,BigDecimal cambio,String referencia) {}
    public List<Parte> previstas() {return partes.stream().filter(p -> !METODO_PAGO_POINT.equals(p.metodoPago())).toList();}
    public BigDecimal importePoint() {return partes.stream().filter(p -> METODO_PAGO_POINT.equals(p.metodoPago())).map(Parte::monto).reduce(BigDecimal.ZERO,BigDecimal::add);}
    public static PlanPagosVenta resolver(BigDecimal total,RegistrarVentaRequest solicitud,boolean point) {
        var enviadas=solicitud.pagos();
        if (enviadas!=null && solicitud.metodoPago()!=null) throw invalido("Usa metodoPago o pagos, no ambos");
        if (enviadas==null) {
            String metodo=point?METODO_PAGO_POINT:solicitud.metodoPago();
            if(metodo==null || metodo.isBlank()) throw invalido("Selecciona un método de pago");
            enviadas=List.of(new RegistrarVentaRequest.PagoVentaRequest(metodo,point?null:total,null,solicitud.referencia()));
        }
        if(enviadas.isEmpty() || enviadas.size()>3 || enviadas.stream().anyMatch(Objects::isNull)) throw invalido("Indica entre uno y tres pagos");
        var partes=new ArrayList<Parte>();BigDecimal suma=BigDecimal.ZERO;int points=0;
        for(var p:enviadas) {
            String metodo=p.metodoPago()==null?"":p.metodoPago().trim().toUpperCase(Locale.ROOT);
            if(METODO_PAGO_POINT.equals(metodo)) {
                if(!point || ++points>1 || p.monto()!=null || p.montoRecibido()!=null) throw invalido("Point admite una sola línea sin importe editable");
                continue;
            }
            if(!contiene(METODOS_PAGO,metodo)) throw invalido("El método de pago no es válido");
            BigDecimal monto=dinero(p.monto());
            if(monto.signum()<=0) throw invalido("Cada pago debe ser mayor que cero");
            BigDecimal recibido=null,cambio=null;
            if(p.montoRecibido()!=null) {
                if(!METODO_PAGO_EFECTIVO.equals(metodo)) throw invalido("Recibido solo se admite para efectivo");
                recibido=dinero(p.montoRecibido());if(recibido.compareTo(monto)<0) throw invalido("El efectivo recibido no alcanza");cambio=recibido.subtract(monto);
            }
            String referencia=p.referencia()==null?null:p.referencia().trim();
            if(referencia!=null && referencia.length()>120) throw invalido("La referencia supera 120 caracteres");
            if(METODOS_PAGO.stream().filter(m -> m.codigo().equals(metodo)).anyMatch(Opcion::requiereReferencia) && (referencia==null || referencia.isBlank())) throw invalido("Captura la referencia del pago");
            partes.add(new Parte(metodo,monto,recibido,cambio,referencia));suma=suma.add(monto);
        }
        if(point && points==0) { // Compatibilidad Point legado, nunca añade una segunda línea implícita a pagos[].
            if(solicitud.pagos()!=null) throw invalido("Selecciona una línea Point");points=1;
        }
        if(points==1) {
            if(suma.compareTo(total)>=0) throw invalido("La parte de Point debe ser mayor que cero");
            partes.add(new Parte(METODO_PAGO_POINT,total.subtract(suma),null,null,null));
        } else if(suma.compareTo(total)!=0) throw invalido("La suma de pagos debe coincidir con el total de la cuenta");
        return new PlanPagosVenta(List.copyOf(partes));
    }
    private static BigDecimal dinero(BigDecimal n) {
        if(n==null || n.scale()>2 || n.abs().compareTo(new BigDecimal("9999999999.99"))>0) throw invalido("Indica importes válidos con hasta dos decimales");
        return n.setScale(2,RoundingMode.UNNECESSARY);
    }
    private static ResponseStatusException invalido(String mensaje) {return new ResponseStatusException(HttpStatus.BAD_REQUEST,mensaje);}
}
