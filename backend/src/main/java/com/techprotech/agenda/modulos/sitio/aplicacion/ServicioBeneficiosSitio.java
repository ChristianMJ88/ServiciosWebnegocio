package com.techprotech.agenda.modulos.sitio.aplicacion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.admin.infraestructura.entidad.AuditoriaConfiguracionEmpresaEntidad;
import com.techprotech.agenda.modulos.admin.infraestructura.repositorio.AuditoriaConfiguracionEmpresaRepositorio;
import com.techprotech.agenda.modulos.sitio.api.dto.ActualizarBeneficiosSitioRequest;
import com.techprotech.agenda.modulos.sitio.api.dto.BeneficioSitioAdminResponse;
import com.techprotech.agenda.modulos.sitio.api.dto.BeneficioSitioRequest;
import com.techprotech.agenda.modulos.sitio.dominio.IconoBeneficioSitio;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.EmpresaSitioBeneficioEntidad;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioBeneficioRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class ServicioBeneficiosSitio {
    private final EmpresaSitioBeneficioRepositorio beneficioRepositorio;
    private final EmpresaSitioConfigRepositorio sitioRepositorio;
    private final AuditoriaConfiguracionEmpresaRepositorio auditoriaRepositorio;
    private final ObjectMapper objectMapper;

    public ServicioBeneficiosSitio(
            EmpresaSitioBeneficioRepositorio beneficioRepositorio,
            EmpresaSitioConfigRepositorio sitioRepositorio,
            AuditoriaConfiguracionEmpresaRepositorio auditoriaRepositorio,
            ObjectMapper objectMapper
    ) {
        this.beneficioRepositorio = beneficioRepositorio;
        this.sitioRepositorio = sitioRepositorio;
        this.auditoriaRepositorio = auditoriaRepositorio;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<BeneficioSitioAdminResponse> obtenerParaAdministracion(Long empresaId) {
        validarSitio(empresaId);
        return mapearAdmin(empresaId);
    }

    @Transactional
    public List<BeneficioSitioAdminResponse> actualizar(
            Long empresaId,
            Long usuarioActorId,
            ActualizarBeneficiosSitioRequest request
    ) {
        validarSitio(empresaId);
        List<BeneficioSitioRequest> ordenados = request.beneficios().stream()
                .sorted(Comparator.comparingInt(BeneficioSitioRequest::orden))
                .toList();
        String antes = serializar(mapearAdmin(empresaId));

        beneficioRepositorio.deleteByEmpresaId(empresaId);
        beneficioRepositorio.flush();
        for (int orden = 0; orden < ordenados.size(); orden++) {
            BeneficioSitioRequest item = ordenados.get(orden);
            EmpresaSitioBeneficioEntidad entidad = new EmpresaSitioBeneficioEntidad();
            entidad.setEmpresaId(empresaId);
            entidad.setTitulo(item.titulo().trim());
            entidad.setDescripcion(item.descripcion().trim());
            entidad.setIcono(parsearIcono(item.icono()));
            entidad.setActivo(item.activo());
            entidad.setOrden(orden);
            beneficioRepositorio.save(entidad);
        }

        List<BeneficioSitioAdminResponse> respuesta = mapearAdmin(empresaId);
        registrarAuditoria(empresaId, usuarioActorId, antes, serializar(respuesta));
        return respuesta;
    }

    public List<BeneficioSitioPublicoResponse> obtenerParaSitioPublico(Long empresaId) {
        return beneficioRepositorio.findByEmpresaIdOrderByOrdenAsc(empresaId).stream()
                .filter(EmpresaSitioBeneficioEntidad::isActivo)
                .map(item -> new BeneficioSitioPublicoResponse(
                        item.getId(), item.getTitulo(), item.getDescripcion(), item.getIcono().name()
                ))
                .toList();
    }

    private List<BeneficioSitioAdminResponse> mapearAdmin(Long empresaId) {
        return beneficioRepositorio.findByEmpresaIdOrderByOrdenAsc(empresaId).stream()
                .map(item -> new BeneficioSitioAdminResponse(
                        item.getId(), item.getTitulo(), item.getDescripcion(), item.getIcono().name(),
                        item.isActivo(), item.getOrden()
                ))
                .toList();
    }

    private IconoBeneficioSitio parsearIcono(String valor) {
        try {
            return IconoBeneficioSitio.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException excepcion) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Icono de beneficio no reconocido");
        }
    }

    private void validarSitio(Long empresaId) {
        if (!sitioRepositorio.existsById(empresaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El tenant no tiene configuración de sitio");
        }
    }

    private void registrarAuditoria(Long empresaId, Long usuarioActorId, String antes, String despues) {
        AuditoriaConfiguracionEmpresaEntidad auditoria = new AuditoriaConfiguracionEmpresaEntidad();
        auditoria.setEmpresaId(empresaId);
        auditoria.setUsuarioActorId(usuarioActorId);
        auditoria.setModulo("SITIO");
        auditoria.setAccion("BENEFICIOS_SITIO_ACTUALIZADOS");
        auditoria.setResumen("Se actualizaron los beneficios del sitio");
        auditoria.setDetalleAntesJson(antes);
        auditoria.setDetalleDespuesJson(despues);
        auditoriaRepositorio.save(auditoria);
    }

    private String serializar(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException excepcion) {
            throw new IllegalStateException("No fue posible serializar los beneficios", excepcion);
        }
    }
}
