package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.modulos.disponibilidad.aplicacion.ServicioConfiguracionReservas;
import com.techprotech.agenda.modulos.pagos.aplicacion.ServicioConfiguracionCheckout;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServicioPoliticaReservaPago {
    private final java.time.Clock reloj;
    private final ServicioConfiguracionCheckout checkout;
    private final ServicioConfiguracionReservas configuracion;
    public ServicioPoliticaReservaPago(ServicioConfiguracionCheckout checkout,ServicioConfiguracionReservas configuracion,java.time.Clock reloj) {
        this.reloj=reloj;this.checkout=checkout;this.configuracion=configuracion;
    }
    public boolean puedeCobrarOnline(Long empresaId) {
        try { var config=checkout.obtener(empresaId); return config.habilitado() && config.conectado() && config.permitirPagoServicios() && config.aceptarTarjeta(); }
        catch(ResponseStatusException ex) { if(ex.getStatusCode().value()==404) return false; throw ex; }
    }
public boolean puedeCobrarProductosOnline(Long empresaId) {
    try { var config=checkout.obtener(empresaId); return config.habilitado() && config.conectado() && config.permitirPagoProductos() && (config.aceptarTarjeta() || config.aceptarTransferencia() || config.aceptarEfectivoReferenciado()); }
    catch(ResponseStatusException ex) { if(ex.getStatusCode().value()==404) return false; throw ex; }
}
    public PlanReserva plan(Long empresaId,boolean publico,List<BigDecimal> importes) {
        boolean requierePago=publico && importes.stream().anyMatch(i -> i.signum()>0) && puedeCobrarOnline(empresaId);
        return new PlanReserva(java.util.UUID.randomUUID().toString(),requierePago,
                requierePago?LocalDateTime.now(reloj).plusMinutes(configuracion.obtener(empresaId).minutosApartadoPago()):null);
    }
public java.time.OffsetDateTime limiteConfirmacion(CitaEntidad cita,java.time.ZoneId zona) {
    if(!"PENDIENTE".equals(cita.getEstado())) return null;
    return PlazoConfirmacionCita.limite(cita.getInicio(),zona,configuracion.obtener(cita.getEmpresaId()).liberacionSinConfirmacionHoras(),cita.getCreadaEn()).atOffset(java.time.ZoneOffset.UTC);
}
    public void aplicar(CitaEntidad cita,PlanReserva plan,PoliticaCobroServicio politica) {
        cita.setReservaGrupoId(plan.grupo()); cita.setEstado(plan.requierePago()?"PENDIENTE_PAGO":"PENDIENTE");
        cita.setPagoExpiraEn(plan.expiraEn()); cita.setPoliticaCobro(politica.politica()); cita.setImporteRequerido(politica.importeRequerido());
    }
    public record PlanReserva(String grupo,boolean requierePago,LocalDateTime expiraEn) {}
}
