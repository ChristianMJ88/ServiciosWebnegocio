package com.techprotech.agenda.modulos.citas.aplicacion;

import com.techprotech.agenda.modulos.citas.api.dto.CotizarCitasRequest.ItemCotizacion;
import com.techprotech.agenda.modulos.citas.api.dto.ResultadoCotizacionCitas;
import com.techprotech.agenda.modulos.citas.api.dto.ResultadoCotizacionCitas.*;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioPromocionEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.EmpresaCheckoutConfigRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ServicioCotizacionCitas {
    private final SucursalRepositorio sucursales;
    private final ServicioRepositorio servicios;
    private final ServicioSucursalRepositorio asignaciones;
    private final EmpresaSitioPromocionRepositorio promociones;
    private final EmpresaSitioPromocionServicioRepositorio serviciosPromocion;
    private final EmpresaSitioPromocionSucursalRepositorio sucursalesPromocion;
    private final EmpresaCheckoutConfigRepositorio checkout;
    private final Clock reloj;
    private ServicioPoliticaReservaPago politicaPago;
    @Autowired
    public ServicioCotizacionCitas(SucursalRepositorio sucursales, ServicioRepositorio servicios,
            ServicioSucursalRepositorio asignaciones, EmpresaSitioPromocionRepositorio promociones,
            EmpresaSitioPromocionServicioRepositorio serviciosPromocion,
            EmpresaSitioPromocionSucursalRepositorio sucursalesPromocion, EmpresaCheckoutConfigRepositorio checkout, ServicioPoliticaReservaPago politicaPago, Clock reloj) {
        this(sucursales,servicios,asignaciones,promociones,serviciosPromocion,sucursalesPromocion,checkout,reloj);
        this.politicaPago=politicaPago;
    }
    ServicioCotizacionCitas(SucursalRepositorio sucursales, ServicioRepositorio servicios,
            ServicioSucursalRepositorio asignaciones, EmpresaSitioPromocionRepositorio promociones,
            EmpresaSitioPromocionServicioRepositorio serviciosPromocion,
            EmpresaSitioPromocionSucursalRepositorio sucursalesPromocion, EmpresaCheckoutConfigRepositorio checkout, Clock reloj) {
        this.sucursales=sucursales; this.servicios=servicios; this.asignaciones=asignaciones;
        this.promociones=promociones; this.serviciosPromocion=serviciosPromocion;
        this.sucursalesPromocion=sucursalesPromocion; this.checkout=checkout; this.reloj=reloj;
    }
    @Transactional(readOnly=true)
    public ResultadoCotizacionCitas cotizar(Long empresaId, Long sucursalId, Long promocionId, List<ItemCotizacion> items) {
        return cotizarInterno(empresaId, sucursalId, promocionId, items, true);
    }
    /** Crear la cita congela precios; no depende de la configuración del cobro. */
    @Transactional(readOnly=true)
    public ResultadoCotizacionCitas cotizarPrecio(Long empresaId, Long sucursalId, Long promocionId, List<ItemCotizacion> items) {
        return cotizarInterno(empresaId, sucursalId, promocionId, items, false);
    }
    private ResultadoCotizacionCitas cotizarInterno(Long empresaId, Long sucursalId, Long promocionId, List<ItemCotizacion> items, boolean calcularAnticipo) {
        if (items == null || items.isEmpty() || items.size()>20) throw error("Indica entre uno y veinte servicios");
        var sucursal = sucursales.findByIdAndEmpresaIdAndActivaTrue(sucursalId,empresaId)
                .orElseThrow(() -> error("La sucursal no pertenece al negocio o está inactiva"));
        ZoneId zona = ZoneId.of(sucursal.getZonaHoraria());
        LocalDate hoy = LocalDate.now(reloj.withZone(zona));
        Set<Long> ids = items.stream().map(ItemCotizacion::servicioId).collect(Collectors.toSet());
        var catalogo = servicios.findByEmpresaIdAndIdIn(empresaId,ids).stream().filter(s -> s.isActivo())
                .collect(Collectors.toMap(s -> s.getId(),s -> s));
        Set<Long> disponibles = asignaciones.findByEmpresaIdAndIdSucursalIdAndActivoTrue(empresaId,sucursalId)
                .stream().map(a -> a.getId().getServicioId()).collect(Collectors.toSet());
        if (!catalogo.keySet().containsAll(ids) || !disponibles.containsAll(ids)) throw error("Uno de los servicios no está disponible en esta sucursal");
        String moneda = catalogo.get(items.getFirst().servicioId()).getMoneda();
        if (catalogo.values().stream().anyMatch(s -> !moneda.equalsIgnoreCase(s.getMoneda()))) throw error("Todos los servicios deben usar la misma moneda");
        EmpresaSitioPromocionEntidad promocion = promocionId == null ? null : promociones.findByIdAndEmpresaId(promocionId,empresaId).orElse(null);
        String motivo = null;
        Set<Long> incluidos = Set.of();
        if (promocionId != null) {
            if (promocion == null || promocion.getEliminadaEn()!=null || !promocion.isActiva()) motivo="La promoción ya no está disponible para este negocio";
            else if (!vigente(promocion,hoy)) motivo="La promoción no está vigente hoy";
            else {
                var ramas = sucursalesPromocion.findByEmpresaIdAndPromocionId(empresaId,promocionId);
                if (!ramas.isEmpty() && ramas.stream().noneMatch(r -> sucursalId.equals(r.getSucursalId()))) motivo="La promoción no aplica en esta sucursal";
                incluidos = serviciosPromocion.findByEmpresaIdAndPromocionId(empresaId,promocionId).stream().map(r -> r.getServicioId()).collect(Collectors.toSet());
            }
        }
        boolean online=calcularAnticipo && politicaPago!=null && politicaPago.puedeCobrarOnline(empresaId);
        List<LineaCotizacionCita> lineas = new ArrayList<>();
        Set<String> motivosLinea = new TreeSet<>();
        for (var item : items) {
            var servicio = catalogo.get(item.servicioId());
            LocalDate fecha = item.inicio()!=null ? item.inicio().atZoneSameInstant(zona).toLocalDate()
                    : item.fecha()!=null ? item.fecha() : hoy;
            boolean aplica = promocion != null && motivo==null && incluidos.contains(servicio.getId())
                    && moneda.equalsIgnoreCase(promocion.getMoneda()) && vigente(promocion,fecha);
            if (promocion!=null && motivo==null && !aplica) motivosLinea.add( !incluidos.contains(servicio.getId())
                    ? "La promoción no incluye los servicios seleccionados" : !moneda.equalsIgnoreCase(promocion.getMoneda())
                    ? "La moneda de la promoción no coincide con la del servicio" : "La fecha de la cita queda fuera de la vigencia de la promoción");
            var precio = CalculadorPromocionServicios.calcular(servicio.getPrecio(),aplica?promocion.getTipoDescuento():null,aplica?promocion.getValorDescuento():null);
PoliticaCobroServicio politica;
try { politica=PoliticaCobroServicio.resolver(precio.precioNeto(),servicio); }
catch(ResponseStatusException ex) {
    if(ex.getStatusCode()!=HttpStatus.CONFLICT) throw ex;
    politica=new PoliticaCobroServicio("SIN_COBRO",BigDecimal.ZERO.setScale(2),precio.precioNeto());
}
BigDecimal anticipo=politica.importeRequerido();
            lineas.add(new LineaCotizacionCita(servicio.getId(),servicio.getNombre(),precio.precioLista(),precio.descuento(),precio.precioNeto(),anticipo,aplica,politica.politica(),politica.importeRequerido(),politica.saldoEnNegocio()));
        }
        boolean aplicada = lineas.stream().anyMatch(LineaCotizacionCita::promocionAplicada);
        return new ResultadoCotizacionCitas(moneda,suma(lineas.stream().map(LineaCotizacionCita::precioLista).toList()),
                suma(lineas.stream().map(LineaCotizacionCita::descuento).toList()),suma(lineas.stream().map(LineaCotizacionCita::precioNeto).toList()),
                suma(lineas.stream().map(LineaCotizacionCita::anticipo).toList()),
                aplicada ? new PromocionAplicada(promocion.getId(),promocion.getTitulo(),promocion.getTipoDescuento().name(),promocion.getValorDescuento(),promocion.getMoneda()) : null,
                aplicada?null:motivo!=null?motivo:motivosLinea.isEmpty()?null:String.join(". ",motivosLinea),List.copyOf(lineas),
                online && lineas.stream().anyMatch(l -> l.importeRequerido().signum()>0), suma(lineas.stream().map(LineaCotizacionCita::importeRequerido).toList()));
    }
    private static boolean vigente(EmpresaSitioPromocionEntidad p,LocalDate fecha) {
        return (p.getFechaInicio()==null || !fecha.isBefore(p.getFechaInicio())) && (p.getFechaFin()==null || !fecha.isAfter(p.getFechaFin()));
    }
    private static BigDecimal suma(List<BigDecimal> valores) { return valores.stream().reduce(BigDecimal.ZERO,BigDecimal::add).setScale(2,java.math.RoundingMode.HALF_UP); }
    private static ResponseStatusException error(String mensaje) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,mensaje); }
}
