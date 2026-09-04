package com.techprotech.agenda.modulos.parametros.aplicacion;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.parametros.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.parametros.infraestructura.repositorio.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.springframework.http.HttpStatus.*;

@Service
public class ServicioParametrosSistemaImpl implements ServicioParametrosSistema {
    private final ParametroSistemaDefinicionRepositorio definiciones;
    private final ParametroSistemaEmpresaRepositorio valores;
    private final ObjectMapper objectMapper;
    public ServicioParametrosSistemaImpl(ParametroSistemaDefinicionRepositorio definiciones,
                                         ParametroSistemaEmpresaRepositorio valores, ObjectMapper objectMapper) {
        this.definiciones = definiciones; this.valores = valores; this.objectMapper = objectMapper;
    }
    @Override @Transactional(readOnly = true)
    public List<ParametroSistema> listar(Long empresaId) {
        Map<String, ParametroSistemaEmpresaEntidad> porClave = new HashMap<>();
        valores.findByIdEmpresaId(empresaId).forEach(v -> porClave.put(v.getId().getClave(), v));
        return definiciones.findAllByOrderByCategoriaAscOrdenAsc().stream().map(d -> mapear(d, porClave.get(d.getClave()))).toList();
    }
    @Override @Transactional
    public ParametroSistema guardar(Long empresaId, Long usuarioId, String clave, String valor) {
        ParametroSistemaDefinicionEntidad d = definiciones.findById(clave).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El parámetro no existe"));
        if (!d.isEditable()) throw new ResponseStatusException(FORBIDDEN, "El parámetro no es editable");
        String limpio = validar(d, valor);
        ParametroSistemaEmpresaId id = new ParametroSistemaEmpresaId(empresaId, clave);
        ParametroSistemaEmpresaEntidad entidad = valores.findById(id).orElseGet(ParametroSistemaEmpresaEntidad::new);
        entidad.setId(id); entidad.setValor(limpio); entidad.setActualizadoPorUsuarioId(usuarioId);
        return mapear(d, valores.save(entidad));
    }
    @Override @Transactional
    public ParametroSistema restablecer(Long empresaId, String clave) {
        ParametroSistemaDefinicionEntidad d = definiciones.findById(clave).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "El parámetro no existe"));
        valores.deleteById(new ParametroSistemaEmpresaId(empresaId, clave));
        return mapear(d, null);
    }
    private String validar(ParametroSistemaDefinicionEntidad d, String valor) {
        String limpio = valor == null ? "" : valor.trim();
        if (limpio.isBlank() || limpio.length() > 500) throw new ResponseStatusException(BAD_REQUEST, "El valor del parámetro no es válido");
        if ("NUMERO".equals(d.getTipo())) {
            try { if (Integer.parseInt(limpio) < 0) throw new NumberFormatException(); } catch (NumberFormatException ex) { throw new ResponseStatusException(BAD_REQUEST, "El valor debe ser un número positivo"); }
        }
        List<String> opciones = opciones(d);
        if (!opciones.isEmpty() && !opciones.contains(limpio)) throw new ResponseStatusException(BAD_REQUEST, "El valor no pertenece al catálogo permitido");
        return limpio;
    }
    private ParametroSistema mapear(ParametroSistemaDefinicionEntidad d, ParametroSistemaEmpresaEntidad v) {
        return new ParametroSistema(d.getClave(), d.getNombre(), d.getDescripcion(), d.getTipo(),
                v == null ? d.getValorPredeterminado() : v.getValor(), d.getValorPredeterminado(), d.getCategoria(),
                opciones(d), v != null, d.isEditable(), v == null ? null : v.getActualizadoEn());
    }
    private List<String> opciones(ParametroSistemaDefinicionEntidad d) {
        if (d.getOpcionesJson() == null || d.getOpcionesJson().isBlank()) return List.of();
        try { return objectMapper.readValue(d.getOpcionesJson(), new TypeReference<>() {}); }
        catch (Exception ex) { throw new IllegalStateException("Catálogo de parámetro inválido: " + d.getClave(), ex); }
    }
}
