package com.techprotech.agenda.modulos.parametros.aplicacion;
import java.util.List;
public interface ServicioParametrosSistema {
    List<ParametroSistema> listar(Long empresaId);
    ParametroSistema guardar(Long empresaId, Long usuarioId, String clave, String valor);
    ParametroSistema restablecer(Long empresaId, String clave);
}
