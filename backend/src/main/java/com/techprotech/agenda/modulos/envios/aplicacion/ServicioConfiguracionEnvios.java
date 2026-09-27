package com.techprotech.agenda.modulos.envios.aplicacion;

import com.techprotech.agenda.modulos.envios.api.dto.*;
import com.techprotech.agenda.modulos.envios.infraestructura.entidad.EmpresaEnvioConfigEntidad;
import com.techprotech.agenda.modulos.envios.infraestructura.entidad.SucursalEnvioOrigenEntidad;
import com.techprotech.agenda.modulos.envios.infraestructura.repositorio.EmpresaEnvioConfigRepositorio;
import com.techprotech.agenda.modulos.envios.infraestructura.repositorio.SucursalEnvioOrigenRepositorio;
import com.techprotech.agenda.modulos.envios.infraestructura.seguridad.ProtectorSecretosEnvio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.entidad.SucursalEntidad;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServicioConfiguracionEnvios {
    private final EmpresaEnvioConfigRepositorio configuraciones;
    private final SucursalEnvioOrigenRepositorio origenes;
    private final SucursalRepositorio sucursales;
    private final ProtectorSecretosEnvio secretos;

    public ServicioConfiguracionEnvios(EmpresaEnvioConfigRepositorio configuraciones,
                                       SucursalEnvioOrigenRepositorio origenes,
                                       SucursalRepositorio sucursales,
                                       ProtectorSecretosEnvio secretos) {
        this.configuraciones = configuraciones;
        this.origenes = origenes;
        this.sucursales = sucursales;
        this.secretos = secretos;
    }

    @Transactional(readOnly = true)
    public ConfiguracionEnvioResponse obtener(Long empresaId) {
        return respuesta(empresaId, configuraciones.findById(empresaId).orElseGet(() -> predeterminada(empresaId)));
    }

    @Transactional
    public ConfiguracionEnvioResponse actualizar(Long empresaId, ConfiguracionEnvioRequest request) {
        EmpresaEnvioConfigEntidad config = configuraciones.findById(empresaId).orElseGet(() -> predeterminada(empresaId));
        String tokenNuevo = limpio(request.token());
        if (request.eliminarToken()) config.setTokenCifrado(null);
        if (tokenNuevo != null) config.setTokenCifrado(secretos.encriptar(tokenNuevo));

        Map<Long, SucursalEntidad> sucursalesEmpresa = sucursales.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .collect(Collectors.toMap(SucursalEntidad::getId, Function.identity()));
        Set<Long> ids = new HashSet<>();
        for (OrigenEnvioRequest origen : request.origenes()) {
            if (!ids.add(origen.sucursalId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada sucursal sólo puede tener un origen de envío");
            }
            if (!sucursalesEmpresa.containsKey(origen.sucursalId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La sucursal indicada no pertenece a la empresa");
            }
        }
        if (request.habilitado() && (config.getTokenCifrado() == null || config.getTokenCifrado().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Configura el token de Envia.com antes de habilitar envíos");
        }
        if (request.habilitado() && request.origenes().stream().noneMatch(OrigenEnvioRequest::activo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Configura al menos un origen activo antes de habilitar envíos");
        }

        config.setProveedor("ENVIA");
        config.setHabilitado(request.habilitado());
        config.setAmbiente(request.ambiente());
        config.setGenerarGuiaAutomaticamente(request.generarGuiaAutomaticamente());
        config.setPermitirSeleccionPaquetera(request.permitirSeleccionPaquetera());
        config.setTipoRecargo(request.tipoRecargo());
        config.setValorRecargo(request.valorRecargo().setScale(4, RoundingMode.HALF_UP));
        configuraciones.save(config);

        origenes.deleteByEmpresaId(empresaId);
        origenes.saveAll(request.origenes().stream().map(origen -> entidad(empresaId, origen)).toList());
        return respuesta(empresaId, config);
    }

    private ConfiguracionEnvioResponse respuesta(Long empresaId, EmpresaEnvioConfigEntidad config) {
        Map<Long, SucursalEnvioOrigenEntidad> configurados = origenes.findByEmpresaIdOrderBySucursalIdAsc(empresaId).stream()
                .collect(Collectors.toMap(SucursalEnvioOrigenEntidad::getSucursalId, Function.identity()));
        List<OrigenEnvioResponse> lista = sucursales.findByEmpresaIdOrderByNombreAsc(empresaId).stream()
                .map(sucursal -> respuestaOrigen(sucursal, configurados.get(sucursal.getId())))
                .toList();
        return new ConfiguracionEnvioResponse(config.getProveedor(), config.isHabilitado(), config.getAmbiente(),
                config.getTokenCifrado() != null && !config.getTokenCifrado().isBlank(),
                config.isGenerarGuiaAutomaticamente(), config.isPermitirSeleccionPaquetera(),
                config.getTipoRecargo(), config.getValorRecargo(), lista);
    }

    private EmpresaEnvioConfigEntidad predeterminada(Long empresaId) {
        EmpresaEnvioConfigEntidad entidad = new EmpresaEnvioConfigEntidad();
        entidad.setEmpresaId(empresaId);
        return entidad;
    }

    private SucursalEnvioOrigenEntidad entidad(Long empresaId, OrigenEnvioRequest request) {
        SucursalEnvioOrigenEntidad entidad = new SucursalEnvioOrigenEntidad();
        entidad.setEmpresaId(empresaId);
        entidad.setSucursalId(request.sucursalId());
        entidad.setActivo(request.activo());
        entidad.setNombreContacto(request.nombreContacto().trim());
        entidad.setTelefono(request.telefono().trim());
        entidad.setCorreo(limpio(request.correo()));
        entidad.setCalle(request.calle().trim());
        entidad.setNumeroExterior(request.numeroExterior().trim());
        entidad.setNumeroInterior(limpio(request.numeroInterior()));
        entidad.setColonia(request.colonia().trim());
        entidad.setMunicipio(request.municipio().trim());
        entidad.setEstado(request.estado().trim());
        entidad.setCodigoPostal(request.codigoPostal().trim().toUpperCase(Locale.ROOT));
        entidad.setCodigoPais(request.codigoPais().trim().toUpperCase(Locale.ROOT));
        entidad.setReferencias(limpio(request.referencias()));
        return entidad;
    }

    private OrigenEnvioResponse respuestaOrigen(SucursalEntidad sucursal, SucursalEnvioOrigenEntidad origen) {
        if (origen == null) {
            return new OrigenEnvioResponse(sucursal.getId(), sucursal.getNombre(), sucursal.isActiva(), false, false,
                    "", sucursal.getTelefono(), null, "", "", null, "", "", "", "", "MX", null);
        }
        return new OrigenEnvioResponse(sucursal.getId(), sucursal.getNombre(), sucursal.isActiva(), true, origen.isActivo(),
                origen.getNombreContacto(), origen.getTelefono(), origen.getCorreo(), origen.getCalle(), origen.getNumeroExterior(),
                origen.getNumeroInterior(), origen.getColonia(), origen.getMunicipio(), origen.getEstado(), origen.getCodigoPostal(),
                origen.getCodigoPais(), origen.getReferencias());
    }

    private String limpio(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
