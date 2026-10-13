package com.techprotech.agenda.modulos.pagos.aplicacion;
import com.techprotech.agenda.modulos.pagos.api.dto.IncidenciaCheckoutResponse;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenRepositorio;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ServicioIncidenciasCheckout {
    private final CheckoutOrdenRepositorio ordenes;
    private final com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenDetalleRepositorio detalles;
    private final com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio citas;
    public ServicioIncidenciasCheckout(CheckoutOrdenRepositorio ordenes,
            com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenDetalleRepositorio detalles,
            com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio citas) {
        this.ordenes=ordenes; this.detalles=detalles; this.citas=citas;
    }
    private List<IncidenciaCheckoutResponse.Cita> miembros(Long empresa, Long orden) {
        var ids=new java.util.TreeSet<Long>();
        for (var d:detalles.findByEmpresaIdAndCheckoutOrdenIdOrderByIdAsc(empresa,orden)) {
            if (d.getCitaId()==null) continue;
            citas.buscarReferenciaReserva(d.getCitaId()).filter(r -> empresa.equals(r.getEmpresaId()))
                .ifPresent(r -> ids.addAll(citas.buscarMiembrosReserva(empresa,d.getCitaId(),r.getReservaGrupoId())));
        }
        return citas.findAllById(ids).stream().filter(c -> empresa.equals(c.getEmpresaId()))
            .map(c -> new IncidenciaCheckoutResponse.Cita(c.getId(),c.getInicio())).toList();
    }
    @Transactional(readOnly=true)
    public List<IncidenciaCheckoutResponse> listar(Long empresaId) {
        return ordenes.findTop100ByEmpresaIdAndRequiereConciliacionTrueOrderByIdDesc(empresaId).stream()
                .map(o -> new IncidenciaCheckoutResponse(o.getPublicId(),o.getSucursalId(),o.getMotivoIncidencia(),o.getTotal(),o.getMoneda(),o.getCreadaEn(),miembros(empresaId,o.getId()),java.util.List.of(com.techprotech.agenda.modulos.pagos.api.dto.ResolverIncidenciaRequest.Tipo.values()))).toList();
    }
}
