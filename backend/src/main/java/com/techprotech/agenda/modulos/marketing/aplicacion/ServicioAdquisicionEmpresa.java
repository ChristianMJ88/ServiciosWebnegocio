package com.techprotech.agenda.modulos.marketing.aplicacion;
import com.techprotech.agenda.modulos.marketing.api.dto.AtribucionAdquisicionRequest;
public interface ServicioAdquisicionEmpresa {
 void registrar(Long empresaId,AtribucionAdquisicionRequest atribucion);
}
