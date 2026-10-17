package com.techprotech.agenda.modulos.caja.aplicacion;

import com.techprotech.agenda.compartido.tiempo.ZonaNegocio;
import com.techprotech.agenda.modulos.caja.infraestructura.config.ConfiguracionCaja;
import com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad;
import com.techprotech.agenda.modulos.citas.infraestructura.repositorio.CitaRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ServicioPendientesCitasCaja {
    private final CitaRepositorio citas;
    private final SucursalRepositorio sucursales;
    private final ZonaNegocio zonas;
    private final Clock reloj;
    private final ConfiguracionCaja configuracion;

    public ServicioPendientesCitasCaja(CitaRepositorio citas, SucursalRepositorio sucursales,
            ZonaNegocio zonas, Clock reloj, ConfiguracionCaja configuracion) {
        this.citas = citas; this.sucursales = sucursales; this.zonas = zonas;
        this.reloj = reloj; this.configuracion = configuracion;
    }

    public Lote consultar(Long empresaId, Collection<Long> permitidas, Long sucursalId) {
        var resultado = new ArrayList<CitaEntidad>();
        var dias = new HashMap<Long, LocalDate>();
        for (var rango : rangos(empresaId, permitidas, sucursalId)) {
            dias.put(rango.sucursalId(), rango.hoy());
            resultado.addAll(citas.buscarCobrablesEnRango(empresaId, rango.sucursalId(),
                    rango.desde(), rango.hasta(), ServicioSaldoCitas.ESTADOS_COBRABLES));
        }
        return new Lote(List.copyOf(resultado), Map.copyOf(dias));
    }

    public int contar(Long empresaId, Collection<Long> permitidas, Long sucursalId) {
        long total = 0;
        for (var rango : rangos(empresaId, permitidas, sucursalId))
            total += citas.contarCobrablesEnRango(empresaId, rango.sucursalId(),
                    rango.desde(), rango.hasta(), ServicioSaldoCitas.ESTADOS_COBRABLES);
        return Math.toIntExact(total);
    }

    private List<Rango> rangos(Long empresaId, Collection<Long> permitidas, Long sucursalId) {
        var rangos = new ArrayList<Rango>();
        // Exclusivo de la operación en Caja; los reportes por cobrar no usarán esta ventana.
        // El rango local puede ser distinto entre sucursales en el mismo instante.
        for (var sucursal : sucursales.findByEmpresaIdOrderByNombreAsc(empresaId)) {
            Long id = sucursal.getId();
            if (sucursalId != null && !sucursalId.equals(id)) continue;
            if (permitidas != null && !permitidas.isEmpty() && !permitidas.contains(id)) continue;
            var hoy = LocalDate.now(reloj.withZone(zonas.de(empresaId, id)));
            rangos.add(new Rango(id, hoy, hoy.minusDays(configuracion.getDiasPendientes()).atStartOfDay(),
                    hoy.plusDays(1).atStartOfDay()));
        }
        return List.copyOf(rangos);
    }

    private record Rango(Long sucursalId, LocalDate hoy, LocalDateTime desde, LocalDateTime hasta) {}

    public record Lote(List<CitaEntidad> citas, Map<Long, LocalDate> hoyPorSucursal) {
        public long diasAtraso(CitaEntidad cita) {
            return Math.max(0, ChronoUnit.DAYS.between(cita.getInicio().toLocalDate(), hoyPorSucursal.get(cita.getSucursalId())));
        }
    }
}
