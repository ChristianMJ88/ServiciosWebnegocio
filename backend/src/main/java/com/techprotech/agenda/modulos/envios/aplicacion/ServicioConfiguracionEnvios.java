package com.techprotech.agenda.modulos.envios.aplicacion;

import com.techprotech.agenda.compartido.direcciones.aplicacion.ServicioValidacionDirecciones;
import com.techprotech.agenda.compartido.direcciones.dominio.Direccion;
import com.techprotech.agenda.compartido.direcciones.dominio.ResultadoValidacionDireccion;
import com.techprotech.agenda.modulos.envios.api.dto.*;
import com.techprotech.agenda.modulos.envios.infraestructura.entidad.EmpresaEnvioConfigEntidad;
import com.techprotech.agenda.modulos.envios.infraestructura.entidad.SucursalEnvioOrigenEntidad;
import com.techprotech.agenda.modulos.envios.infraestructura.envia.PropiedadesEnvia;
import com.techprotech.agenda.modulos.envios.infraestructura.envia.ClienteConsultasEnvia;
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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServicioConfiguracionEnvios {
    private final EmpresaEnvioConfigRepositorio configuraciones;
    private final SucursalEnvioOrigenRepositorio origenes;
    private final SucursalRepositorio sucursales;
    private final ProtectorSecretosEnvio secretos;
    private final PropiedadesEnvia propiedadesEnvia;
    private final ClienteConsultasEnvia clienteConsultas;
    private final ServicioValidacionDirecciones direcciones;
    private final ResolutorCredencialEnvio resolutorCredencial;

    public ServicioConfiguracionEnvios(EmpresaEnvioConfigRepositorio configuraciones,
                                       SucursalEnvioOrigenRepositorio origenes,
                                       SucursalRepositorio sucursales,
                                       ProtectorSecretosEnvio secretos,
                                       PropiedadesEnvia propiedadesEnvia,
                                       ClienteConsultasEnvia clienteConsultas,
                                       ServicioValidacionDirecciones direcciones,
                                       ResolutorCredencialEnvio resolutorCredencial) {
        this.configuraciones = configuraciones;
        this.origenes = origenes;
        this.sucursales = sucursales;
        this.secretos = secretos;
        this.propiedadesEnvia = propiedadesEnvia;
        this.clienteConsultas = clienteConsultas;
        this.direcciones = direcciones;
        this.resolutorCredencial = resolutorCredencial;
    }

    @Transactional(readOnly = true)
    public PruebaConexionEnvioResponse probarConexion(Long empresaId) {
        EmpresaEnvioConfigEntidad config = configuraciones.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Configura Envia.com antes de probar la conexión"));
        SucursalEnvioOrigenEntidad origen = origenes.findByEmpresaIdOrderBySucursalIdAsc(empresaId).stream()
                .filter(SucursalEnvioOrigenEntidad::isActivo)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Configura un origen activo antes de probar la conexión"));
        String token = resolutorCredencial.resolver(config);
        int paqueterias = clienteConsultas.contarPaqueterias(token, config.getAmbiente(), origen.getCodigoPais());
        return new PruebaConexionEnvioResponse(true, config.getProveedor(), config.getAmbiente(),
                origen.getCodigoPais(), paqueterias,
                paqueterias > 0
                        ? "Conexión verificada con Envia.com"
                        : "La credencial respondió, pero no devolvió paqueterías para el origen configurado");
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
        if (tokenNuevo != null) config.setTokenCifrado(secretos.encriptar(normalizarToken(tokenNuevo)));

        if ("PLATAFORMA".equals(request.modoCredencial()) && !plataformaDisponible(request.ambiente())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los envíos administrados por la plataforma no están disponibles");
        }

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
        if (request.habilitado() && !credencialDisponible(config, request.modoCredencial(), request.ambiente())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Configura una credencial de Envia.com antes de habilitar envíos");
        }
        if (request.habilitado() && request.origenes().stream().noneMatch(OrigenEnvioRequest::activo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Configura al menos un origen activo antes de habilitar envíos");
        }

        config.setProveedor("ENVIA");
        config.setHabilitado(request.habilitado());
        config.setAmbiente(request.ambiente());
        config.setModoCredencial(request.modoCredencial());
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
        boolean tokenConfigurado = config.getTokenCifrado() != null && !config.getTokenCifrado().isBlank();
        return new ConfiguracionEnvioResponse(config.getProveedor(), config.isHabilitado(), config.getAmbiente(),
                config.getModoCredencial(), tokenConfigurado,
                credencialDisponible(config, config.getModoCredencial(), config.getAmbiente()),
                config.isGenerarGuiaAutomaticamente(), config.isPermitirSeleccionPaquetera(),
                config.getTipoRecargo(), config.getValorRecargo(), lista, onboarding(config.getAmbiente()));
    }

    private OnboardingEnvioResponse onboarding(String ambiente) {
        boolean plataformaDisponible = plataformaDisponible(ambiente);
        boolean produccion = "PRODUCCION".equals(ambiente);
        return new OnboardingEnvioResponse(
                plataformaDisponible,
                produccion ? propiedadesEnvia.registroProduccionUrl() : propiedadesEnvia.registroPruebasUrl(),
                produccion ? propiedadesEnvia.credencialesProduccionUrl() : propiedadesEnvia.credencialesPruebasUrl(),
                List.of(
                        new ModoCredencialEnvioResponse("CUENTA_PROPIA", "Cuenta propia", "El negocio usa su saldo y facturación de Envia.com.", true),
                        new ModoCredencialEnvioResponse("PLATAFORMA", "Administrado por Fluora", "Fluora genera las guías y registra el cargo para el negocio.", plataformaDisponible)
                )
        );
    }

    private boolean credencialDisponible(EmpresaEnvioConfigEntidad config, String modoCredencial, String ambiente) {
        if ("PLATAFORMA".equals(modoCredencial)) return plataformaDisponible(ambiente);
        return config.getTokenCifrado() != null && !config.getTokenCifrado().isBlank();
    }

    private boolean plataformaDisponible(String ambiente) {
        if (!propiedadesEnvia.plataformaHabilitada()) return false;
        String token = "PRODUCCION".equals(ambiente)
                ? propiedadesEnvia.tokenPlataformaProduccion()
                : propiedadesEnvia.tokenPlataformaPruebas();
        return token != null && !token.isBlank();
    }

    private String normalizarToken(String token) {
        String valor = token.trim();
        return valor.regionMatches(true, 0, "Bearer ", 0, 7) ? valor.substring(7).trim() : valor;
    }

    private EmpresaEnvioConfigEntidad predeterminada(Long empresaId) {
        EmpresaEnvioConfigEntidad entidad = new EmpresaEnvioConfigEntidad();
        entidad.setEmpresaId(empresaId);
        return entidad;
    }

    private SucursalEnvioOrigenEntidad entidad(Long empresaId, OrigenEnvioRequest request) {
        ResultadoValidacionDireccion validacion = direcciones.validarParaPersistencia(new Direccion(
                request.calle(), request.numeroExterior(), request.numeroInterior(), request.colonia(), request.municipio(),
                request.estado(), request.codigoPostal(), request.codigoPais()));
        Direccion direccion = validacion.direccion();
        SucursalEnvioOrigenEntidad entidad = new SucursalEnvioOrigenEntidad();
        entidad.setEmpresaId(empresaId);
        entidad.setSucursalId(request.sucursalId());
        entidad.setActivo(request.activo());
        entidad.setNombreContacto(request.nombreContacto().trim());
        entidad.setTelefono(request.telefono().trim());
        entidad.setCorreo(limpio(request.correo()));
        entidad.setCalle(direccion.calle());
        entidad.setNumeroExterior(direccion.numeroExterior());
        entidad.setNumeroInterior(direccion.numeroInterior());
        entidad.setColonia(direccion.colonia());
        entidad.setMunicipio(direccion.municipio());
        entidad.setEstado(direccion.estado());
        entidad.setCodigoPostal(direccion.codigoPostal());
        entidad.setCodigoPais(direccion.codigoPais());
        entidad.setReferencias(limpio(request.referencias()));
        entidad.setDireccionFormateada(validacion.direccionFormateada());
        entidad.setDireccionValidacionEstado(validacion.estadoValidacion());
        entidad.setGooglePlaceId(validacion.placeId());
        entidad.setLatitud(validacion.latitud());
        entidad.setLongitud(validacion.longitud());
        entidad.setDireccionValidadaEn(validacion.valida() ? LocalDateTime.now(ZoneOffset.UTC) : null);
        return entidad;
    }

    private OrigenEnvioResponse respuestaOrigen(SucursalEntidad sucursal, SucursalEnvioOrigenEntidad origen) {
        if (origen == null) {
            return new OrigenEnvioResponse(sucursal.getId(), sucursal.getNombre(), sucursal.isActiva(), false, false,
                    "", sucursal.getTelefono(), null, "", "", null, "", "", "", "",
                    direcciones.configuracion().regionPredeterminada(), null, null, "NO_CONFIGURADA");
        }
        return new OrigenEnvioResponse(sucursal.getId(), sucursal.getNombre(), sucursal.isActiva(), true, origen.isActivo(),
                origen.getNombreContacto(), origen.getTelefono(), origen.getCorreo(), origen.getCalle(), origen.getNumeroExterior(),
                origen.getNumeroInterior(), origen.getColonia(), origen.getMunicipio(), origen.getEstado(), origen.getCodigoPostal(),
                origen.getCodigoPais(), origen.getReferencias(), origen.getDireccionFormateada(), origen.getDireccionValidacionEstado());
    }

    private String limpio(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
