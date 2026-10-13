package com.techprotech.agenda.modulos.servicios.aplicacion;
import com.techprotech.agenda.modulos.servicios.api.PoliticasCobroServiciosResponse;
import com.techprotech.agenda.modulos.servicios.api.PoliticasCobroServiciosResponse.*;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.EmpresaCheckoutConfigRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ServicioPoliticasCobroServicios {
    private final EmpresaCheckoutConfigRepositorio configuraciones;
    public ServicioPoliticasCobroServicios(EmpresaCheckoutConfigRepositorio configuraciones) { this.configuraciones=configuraciones; }
    @Transactional(readOnly=true)
    public PoliticasCobroServiciosResponse obtener(Long empresaId) {
        var config=configuraciones.findById(empresaId).orElse(null);
        Predeterminado predeterminado=new Predeterminado(false,null,null);
        if(config!=null && config.isHabilitado() && config.isPermitirPagoServicios()) {
            predeterminado=switch(config.getModalidadServicios()==null?"TOTAL":config.getModalidadServicios()) {
                case "ANTICIPO_PORCENTAJE" -> new Predeterminado(true,"PORCENTAJE",config.getAnticipoPorcentaje());
                case "ANTICIPO_IMPORTE" -> new Predeterminado(true,"FIJO",config.getAnticipoImporte());
                default -> new Predeterminado(true,"TOTAL",null);
            };
        }
        return new PoliticasCobroServiciosResponse(List.of(new Opcion("FIJO","Anticipo fijo"),new Opcion("PORCENTAJE","Anticipo porcentual"),new Opcion("TOTAL","Pago total")),predeterminado);
    }
}
