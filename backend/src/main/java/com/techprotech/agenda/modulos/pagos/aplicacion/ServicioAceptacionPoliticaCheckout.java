package com.techprotech.agenda.modulos.pagos.aplicacion;

import com.techprotech.agenda.modulos.pagos.api.dto.CrearOrdenCheckoutRequest;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.CheckoutOrdenEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.CheckoutOrdenDetalleRepositorio;
import com.techprotech.agenda.modulos.politicas.aplicacion.ServicioPoliticaEmpresa;
import com.techprotech.agenda.modulos.politicas.infraestructura.repositorio.PoliticaEmpresaRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;

@Service
@lombok.extern.slf4j.Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServicioAceptacionPoliticaCheckout {
    private final ServicioPoliticaEmpresa politicas;
    private final PoliticaEmpresaRepositorio versiones;
    private final CheckoutOrdenDetalleRepositorio detalles;
    private final ServicioConfirmacionCitasCheckout confirmacion;
    private final Clock clock;
    private final com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio citas;

    public void aceptar(CheckoutOrdenEntidad orden, Long empresaId, CrearOrdenCheckoutRequest r, boolean servicios) {
        var actual = politicas.exigirPublicada(empresaId);
        if (!servicios) return; // Los productos solos no requieren casilla de aceptación.
        if (!Boolean.TRUE.equals(r.aceptaPolitica()) || !Objects.equals(actual.getVersion(), r.politicaVersion()))
            throw conflicto("Acepta la versión vigente de la política antes de continuar");
        orden.setPoliticaVersionId(actual.getId());
        orden.setPoliticaAceptadaEn(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
    }

    public void validarReintento(CheckoutOrdenEntidad orden, CrearOrdenCheckoutRequest r) {
        if (!incluyeServicios(orden)) return;
        Integer version = orden.getPoliticaVersionId() == null ? null : versiones
                .findByEmpresaIdAndId(orden.getEmpresaId(), orden.getPoliticaVersionId())
                .map(p -> p.getVersion()).orElseThrow(() -> conflicto("No se pudo verificar la política de la orden"));
        if (!Objects.equals(version, r.politicaVersion()) || (version != null && !Boolean.TRUE.equals(r.aceptaPolitica())))
            throw conflicto("La clave de la orden ya se utilizó con otra aceptación; actualiza la orden");
        if (version == null)
            throw conflicto("Crea una nueva orden y acepta la política vigente");
    }

    public void validarAntesDePagar(CheckoutOrdenEntidad orden) {
        var actual = politicas.exigirPublicada(orden.getEmpresaId());
        if (!incluyeServicios(orden)) return;
        if (orden.getPoliticaAceptadaEn() == null || !Objects.equals(actual.getId(), orden.getPoliticaVersionId()))
            throw conflicto("La política cambió o falta su aceptación. Actualiza la orden y acepta la versión vigente");
    }

    @Transactional
    public void acreditarAceptacion(CheckoutOrdenEntidad orden) {
        // Una aprobación antigua o tardía nunca se pierde por no tener aceptación histórica.
        if (orden.getPoliticaVersionId() == null || orden.getPoliticaAceptadaEn() == null) return;
        Set<Long> ids = new HashSet<>();
        detalles.findByEmpresaIdAndCheckoutOrdenIdOrderByIdAsc(orden.getEmpresaId(), orden.getId()).stream()
                .filter(d -> "SERVICIO".equals(d.getTipoConcepto()) && d.getCitaId() != null)
                .forEach(d -> ids.add(d.getCitaId()));
        if (ids.isEmpty()) return;
        var grupo = confirmacion.bloquearGrupoOrden(orden.getEmpresaId(), orden.getId());
        Set<Long> conPagoAnterior = new HashSet<>(citas.buscarCitasConPagoAnterior(orden.getEmpresaId(), ids, orden.getId()));
        // Las citas legadas con anticipo previo conservan su situación histórica (decisión del usuario).
        // Mantiene el orden de bloqueo: prestadores, después citas. Solo escribe citas de esta empresa.
        for (var cita : grupo) {
            if (orden.getEmpresaId().equals(cita.getEmpresaId()) && ids.contains(cita.getId()) && cita.getPoliticaVersionId() == null && !conPagoAnterior.contains(cita.getId())) {
                int escritas = citas.acreditarPoliticaSiAusente(orden.getEmpresaId(), cita.getId(), orden.getPoliticaVersionId(), orden.getPoliticaAceptadaEn());
                if (escritas == 1) {
                    cita.setPoliticaVersionId(orden.getPoliticaVersionId());
                    cita.setPoliticaAceptadaEn(orden.getPoliticaAceptadaEn());
                } else {
                    log.warn("Aceptación ya registrada o cita no disponible: empresa={}, cita={}, orden={}", orden.getEmpresaId(), cita.getId(), orden.getId());
                }
            } else if (orden.getEmpresaId().equals(cita.getEmpresaId()) && ids.contains(cita.getId()) && !Objects.equals(cita.getPoliticaVersionId(), orden.getPoliticaVersionId())) {
                log.warn("Pago posterior conserva política original: empresa={}, cita={}, orden={}", orden.getEmpresaId(), cita.getId(), orden.getId());
            }
        }
    }

    private boolean incluyeServicios(CheckoutOrdenEntidad orden) {
        return detalles.findByEmpresaIdAndCheckoutOrdenIdOrderByIdAsc(orden.getEmpresaId(), orden.getId())
                .stream().anyMatch(d -> "SERVICIO".equals(d.getTipoConcepto()));
    }
    private ResponseStatusException conflicto(String mensaje) { return new ResponseStatusException(HttpStatus.CONFLICT, mensaje); }
}
