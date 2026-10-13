package com.techprotech.agenda.modulos.disponibilidad.aplicacion;
import com.techprotech.agenda.modulos.disponibilidad.api.dto.ConfiguracionReservasResponse;
import com.techprotech.agenda.modulos.disponibilidad.infraestructura.entidad.EmpresaConfiguracionReservasEntidad;
import com.techprotech.agenda.modulos.disponibilidad.infraestructura.repositorio.EmpresaConfiguracionReservasRepositorio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioConfiguracionReservas {
    private final EmpresaConfiguracionReservasRepositorio repositorio;
    private final ConfiguracionReservasResponse respaldo;
    public ServicioConfiguracionReservas(EmpresaConfiguracionReservasRepositorio repositorio,
            @Value("${aplicacion.reservas.anticipacion-minima-minutos:0}") int anticipacion,
            @Value("${aplicacion.reservas.ventana-reserva-dias:30}") int ventana,
            @Value("${aplicacion.whatsapp.liberacion-sin-confirmacion-horas-antes:2}") int liberacion,
            @Value("${aplicacion.pagos.checkout.minutos-vigencia-orden:15}") int apartado,
            @Value("${aplicacion.reservas.horas-tope-procesamiento-pago:24}") int procesamiento) {
        this.repositorio=repositorio;
        this.respaldo=new ConfiguracionReservasResponse(anticipacion,ventana,liberacion,apartado,procesamiento);
    }
    @Transactional(readOnly=true)
    public ConfiguracionReservasResponse obtener(Long empresaId) {
        if(empresaId==null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"La empresa es obligatoria");
        return repositorio.findById(empresaId).map(e -> new ConfiguracionReservasResponse(e.getAnticipacionMinimaMinutos(),e.getVentanaReservaDias(),e.getLiberacionSinConfirmacionHoras(),e.getMinutosApartadoPago(),e.getHorasTopeProcesamientoPago()==null?respaldo.horasTopeProcesamientoPago():e.getHorasTopeProcesamientoPago())).orElse(respaldo);
    }
    @Transactional
    public ConfiguracionReservasResponse guardar(Long empresaId,ConfiguracionReservasResponse request) {
        var actual = obtener(empresaId);
        var entidad=repositorio.findById(empresaId).orElseGet(EmpresaConfiguracionReservasEntidad::new);
        entidad.setEmpresaId(empresaId); entidad.setAnticipacionMinimaMinutos(request.anticipacionMinimaMinutos());
        entidad.setVentanaReservaDias(request.ventanaReservaDias()); entidad.setLiberacionSinConfirmacionHoras(request.liberacionSinConfirmacionHoras());
        entidad.setMinutosApartadoPago(request.minutosApartadoPago());
        entidad.setHorasTopeProcesamientoPago(request.horasTopeProcesamientoPago()==null?actual.horasTopeProcesamientoPago():request.horasTopeProcesamientoPago());
        repositorio.save(entidad);
        return obtener(empresaId);
    }
}
