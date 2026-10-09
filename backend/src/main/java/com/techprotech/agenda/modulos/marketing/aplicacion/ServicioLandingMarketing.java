package com.techprotech.agenda.modulos.marketing.aplicacion;
import com.techprotech.agenda.modulos.marketing.api.dto.LandingMarketingDto;
import java.util.List;
public interface ServicioLandingMarketing {
 LandingMarketingDto publica(String slug);
 List<LandingMarketingDto> listar();
 List<String> indice();
 LandingMarketingDto guardar(String slug,LandingMarketingDto solicitud,Long usuarioId);
}
