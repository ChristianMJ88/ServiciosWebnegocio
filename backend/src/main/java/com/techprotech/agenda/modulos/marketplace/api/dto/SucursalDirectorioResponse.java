package com.techprotech.agenda.modulos.marketplace.api.dto;
import java.util.List;
import com.techprotech.agenda.modulos.sucursales.aplicacion.HorarioComercial;
public record SucursalDirectorioResponse(Long id,String nombre,String direccion,String municipio,String estadoRegion,
 String paisCodigo,String codigoPostal,String zonaHoraria,List<HorarioComercial> horariosComerciales) {}
