package com.techprotech.agenda.modulos.marketplace.aplicacion;
import com.techprotech.agenda.modulos.marketplace.api.dto.*;
public interface ServicioMarketplace {
 DirectorioResponse buscar(String texto,long cursor);
 DirectorioResponse buscar(String texto,String ubicacion,String categoria,long cursor);
 ParticipacionMarketplaceResponse obtener(Long empresaId);
 ParticipacionMarketplaceResponse actualizar(Long empresaId,boolean participante);
}
