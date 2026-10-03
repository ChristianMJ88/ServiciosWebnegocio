package com.techprotech.agenda.modulos.plataforma.aplicacion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.pagos.aplicacion.ServicioComisionPlataforma;
import com.techprotech.agenda.modulos.pagos.dominio.TipoComisionPlataforma;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.ProveedorPagoEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.entidad.ReglaComisionPlataformaEntidad;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.ProveedorPagoRepositorio;
import com.techprotech.agenda.modulos.pagos.infraestructura.repositorio.ReglaComisionPlataformaRepositorio;
import com.techprotech.agenda.modulos.plataforma.api.dto.*;
import com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.AuditoriaPlataformaEntidad;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.AuditoriaPlataformaRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class ServicioGestionComisionesPlataforma {
    private final ReglaComisionPlataformaRepositorio reglas;
    private final ProveedorPagoRepositorio proveedores;
    private final EmpresaRepositorio empresas;
    private final AuditoriaPlataformaRepositorio auditoria;
    private final ServicioComisionPlataforma calculadora;
    private final ObjectMapper json;

    public ServicioGestionComisionesPlataforma(ReglaComisionPlataformaRepositorio reglas,
            ProveedorPagoRepositorio proveedores, EmpresaRepositorio empresas,
            AuditoriaPlataformaRepositorio auditoria, ServicioComisionPlataforma calculadora,
            ObjectMapper json) {
        this.reglas = reglas; this.proveedores = proveedores; this.empresas = empresas;
        this.auditoria = auditoria; this.calculadora = calculadora; this.json = json;
    }

    @Transactional(readOnly = true)
    public List<ProveedorPagoPlataformaResponse> proveedores() {
        return proveedores.findByActivoTrueOrderByNombreAsc().stream()
                .map(p -> new ProveedorPagoPlataformaResponse(p.getId(), p.getCodigo(), p.getNombre())).toList();
    }

    @Transactional(readOnly = true)
    public List<ReglaComisionPlataformaResponse> reglas() {
        List<ReglaComisionPlataformaEntidad> listado = reglas.findAllByOrderByActivaDescPrioridadDescIdDesc();
        Map<Long, ProveedorPagoEntidad> mapaProveedores = mapa(proveedores.findAll());
        Set<Long> idsEmpresa = new HashSet<>();
        listado.stream().map(ReglaComisionPlataformaEntidad::getEmpresaId).filter(Objects::nonNull).forEach(idsEmpresa::add);
        Map<Long, EmpresaEntidad> mapaEmpresas = mapa(empresas.findAllById(idsEmpresa));
        return listado.stream().map(r -> respuesta(r, mapaProveedores.get(r.getProveedorPagoId()),
                r.getEmpresaId() == null ? null : mapaEmpresas.get(r.getEmpresaId()))).toList();
    }

    @Transactional
    public ReglaComisionPlataformaResponse crear(Long usuarioId, GuardarReglaComisionPlataformaRequest solicitud) {
        ReglaComisionPlataformaEntidad regla = guardar(new ReglaComisionPlataformaEntidad(), solicitud);
        registrar(usuarioId, "CREAR", regla);
        return respuestaCompleta(regla);
    }

    @Transactional
    public ReglaComisionPlataformaResponse actualizar(Long usuarioId, Long id, GuardarReglaComisionPlataformaRequest solicitud) {
        ReglaComisionPlataformaEntidad regla = reglas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La regla de comisión no existe"));
        guardar(regla, solicitud);
        registrar(usuarioId, "ACTUALIZAR", regla);
        return respuestaCompleta(regla);
    }

    @Transactional
    public ReglaComisionPlataformaResponse cambiarEstado(Long usuarioId, Long id, boolean activa) {
        ReglaComisionPlataformaEntidad regla = reglas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La regla de comisión no existe"));
        regla.setActiva(activa); reglas.save(regla); registrar(usuarioId, activa ? "ACTIVAR" : "DESACTIVAR", regla);
        return respuestaCompleta(regla);
    }

    @Transactional(readOnly = true)
    public SimulacionComisionPlataformaResponse simular(SimularComisionPlataformaRequest s) {
        validarValores(s.tipo(), s.valor(), s.importeMinimo(), s.importeMaximo());
        try {
            BigDecimal comision = calculadora.calcularConfiguracion(s.tipo(), s.valor(), s.importeMinimo(), s.importeMaximo(), s.importe());
            return new SimulacionComisionPlataformaResponse(s.importe().setScale(2, RoundingMode.HALF_UP), comision,
                    s.importe().subtract(comision).setScale(2, RoundingMode.HALF_UP), normalizarMoneda(s.moneda()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    private ReglaComisionPlataformaEntidad guardar(ReglaComisionPlataformaEntidad regla, GuardarReglaComisionPlataformaRequest s) {
        ProveedorPagoEntidad proveedor = proveedores.findById(s.proveedorId()).filter(ProveedorPagoEntidad::isActivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El proveedor de pago no está disponible"));
        if (s.empresaId() != null && !empresas.existsById(s.empresaId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El negocio seleccionado no existe");
        validarValores(s.tipo(), s.valor(), s.importeMinimo(), s.importeMaximo());
        if (s.vigenteHasta() != null && !s.vigenteHasta().isAfter(s.vigenteDesde()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha final debe ser posterior a la fecha inicial");
        regla.setProveedorPagoId(proveedor.getId()); regla.setEmpresaId(s.empresaId()); regla.setTipo(s.tipo());
        regla.setValor(s.valor()); regla.setImporteMinimo(s.importeMinimo()); regla.setImporteMaximo(s.importeMaximo());
        regla.setMoneda(normalizarMonedaOpcional(s.moneda())); regla.setPrioridad(s.prioridad());
        regla.setVigenteDesde(s.vigenteDesde()); regla.setVigenteHasta(s.vigenteHasta()); regla.setActiva(s.activa());
        return reglas.save(regla);
    }

    private void validarValores(TipoComisionPlataforma tipo, BigDecimal valor, BigDecimal minimo, BigDecimal maximo) {
        if (tipo == null || valor == null || valor.signum() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo y valor mayor a cero son obligatorios");
        if (tipo == TipoComisionPlataforma.PORCENTAJE && valor.compareTo(new BigDecimal("100")) > 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El porcentaje no puede ser mayor a 100");
        if (minimo != null && maximo != null && minimo.compareTo(maximo) > 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El importe mínimo no puede superar al máximo");
    }

    private ReglaComisionPlataformaResponse respuestaCompleta(ReglaComisionPlataformaEntidad r) {
        ProveedorPagoEntidad p = proveedores.findById(r.getProveedorPagoId()).orElse(null);
        EmpresaEntidad e = r.getEmpresaId() == null ? null : empresas.findById(r.getEmpresaId()).orElse(null);
        return respuesta(r, p, e);
    }

    private ReglaComisionPlataformaResponse respuesta(ReglaComisionPlataformaEntidad r, ProveedorPagoEntidad p, EmpresaEntidad e) {
        return new ReglaComisionPlataformaResponse(r.getId(), r.getProveedorPagoId(), p == null ? null : p.getNombre(),
                r.getEmpresaId(), e == null ? null : e.getNombre(), r.getTipo(), r.getValor(), r.getImporteMinimo(),
                r.getImporteMaximo(), r.getMoneda(), r.getPrioridad(), r.getVigenteDesde(), r.getVigenteHasta(), r.isActiva());
    }

    private void registrar(Long usuarioId, String accion, ReglaComisionPlataformaEntidad regla) {
        AuditoriaPlataformaEntidad evento = new AuditoriaPlataformaEntidad(); evento.setUsuarioId(usuarioId);
        evento.setAccion(accion); evento.setRecurso("REGLA_COMISION"); evento.setRecursoId(String.valueOf(regla.getId()));
        try { evento.setDetalle(json.writeValueAsString(Map.of("activa", regla.isActiva(), "proveedorId", regla.getProveedorPagoId(),
                "alcance", regla.getEmpresaId() == null ? "GLOBAL" : "TENANT", "tipo", regla.getTipo().name(), "valor", regla.getValor()))); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("No fue posible registrar la auditoría", ex); }
        auditoria.save(evento);
    }

    private static String normalizarMoneda(String valor) { return valor.trim().toUpperCase(Locale.ROOT); }
    private static String normalizarMonedaOpcional(String valor) { return valor == null || valor.isBlank() ? null : normalizarMoneda(valor); }
    private static <T> Map<Long,T> mapa(Collection<T> items) {
        Map<Long,T> mapa = new HashMap<>();
        for (T item : items) {
            Long id = item instanceof ProveedorPagoEntidad p ? p.getId() : ((EmpresaEntidad)item).getId(); mapa.put(id, item);
        }
        return mapa;
    }
}
