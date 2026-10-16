package com.techprotech.agenda.modulos.plataforma.aplicacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.compartido.correo.*;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.SuscripcionPlataformaEntidad;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.PlanPlataformaRepositorio;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ServicioAvisosSuscripcion {
    private final java.time.Clock reloj;
    private final BandejaSalidaNotificacionRepositorio outbox;
    private final EmpresaRepositorio empresas;
    private final PlanPlataformaRepositorio planes;
    private final ObjectMapper mapper;
    public ServicioAvisosSuscripcion(BandejaSalidaNotificacionRepositorio outbox,EmpresaRepositorio empresas,
            PlanPlataformaRepositorio planes,ObjectMapper mapper, java.time.Clock reloj) {
        this.reloj = reloj;
        this.outbox=outbox;this.empresas=empresas;this.planes=planes;this.mapper=mapper;
    }
    public void programar(SuscripcionPlataformaEntidad s,String tipo,String referencia,BigDecimal pagado) {
        if(s.getCorreoFacturacion()==null||s.getCorreoFacturacion().isBlank())return;
        String clave="suscripcion:"+s.getId()+":"+tipo+":"+referencia;
        if(outbox.existsByClaveDeduplicacion(clave))return;
        var empresa=empresas.findById(s.getEmpresaId()).orElseThrow();
        var plan=planes.findById(s.getPlanId()).orElseThrow();
        String titulo=switch(tipo){
            case "PRUEBA"->"Tu período de prueba ha comenzado";
            case "PAGO"->"Recibimos el pago de tu suscripción";
            case "PLAN"->"Tu plan ha cambiado";
            case "FALLO"->"No pudimos cobrar tu suscripción";
            case "CANCELACION_PROGRAMADA"->"La cancelación de tu suscripción está programada";
            case "CANCELADA"->"Tu suscripción ha sido cancelada";
            case "VENCIMIENTO"->"Tu período de acceso ha terminado";
            default->throw new IllegalArgumentException("Aviso de suscripción no soportado");
        };
        String descripcion=switch(tipo){
            case "PRUEBA"->"Puedes utilizar las capacidades incluidas en tu plan hasta la fecha indicada.";
            case "PAGO"->"El servidor confirmó tu pago. Puedes consultar tu plan y facturación en Fluora.";
            case "PLAN"->"Los módulos y límites de tu negocio corresponden ahora al plan indicado.";
            case "FALLO"->"Revisa tu método de pago. Si tienes un período de gracia, su fecha límite aparece a continuación.";
            case "CANCELACION_PROGRAMADA"->"Conservarás el acceso hasta el final del período vigente.";
            case "CANCELADA"->"La cancelación fue confirmada. Puedes revisar las opciones de contratación en tu cuenta.";
            default->"El período de acceso terminó. Revisa tu suscripción para continuar utilizando el servicio.";
        };
        LocalDateTime limite="PRUEBA".equals(tipo)?s.getPruebaHasta():"FALLO".equals(tipo)?s.getGraciaHasta():s.getPeriodoActualHasta();
        if("VENCIMIENTO".equals(tipo))limite="PRUEBA".equals(s.getEstado())?s.getPruebaHasta():"GRACIA".equals(s.getEstado())?s.getGraciaHasta():s.getTerminaEn()!=null?s.getTerminaEn():s.getPeriodoActualHasta();
        String fecha=limite==null?null:limite.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.of(empresa.getZonaHoraria()))
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm z",Locale.forLanguageTag("es-MX")));
        var payload=new SuscripcionCorreoPayload(s.getCorreoFacturacion(),empresa.getNombre(),plan.getNombre(),titulo,descripcion,fecha,pagado,s.getMoneda(),"PAGO".equals(tipo)?referencia:null);
        try {
            var salida=new BandejaSalidaNotificacionEntidad();salida.setEmpresaId(s.getEmpresaId());salida.setTipoAgregado("SUSCRIPCION");
            salida.setAgregadoId(s.getId());salida.setTipoEvento("SUSCRIPCION_"+tipo+"_EMAIL");salida.setCanal("EMAIL");
            salida.setClaveDeduplicacion(clave);salida.setPayloadJson(mapper.writeValueAsString(payload));
            salida.setEstado("PENDIENTE");salida.setProgramadaEn(LocalDateTime.now(reloj));salida.setIntentos(0);outbox.save(salida);
        }catch(com.fasterxml.jackson.core.JsonProcessingException ex){throw new IllegalStateException("No se pudo preparar el aviso de suscripción",ex);}
    }
}
