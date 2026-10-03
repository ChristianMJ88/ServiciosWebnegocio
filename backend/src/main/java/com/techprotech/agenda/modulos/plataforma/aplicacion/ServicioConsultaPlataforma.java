package com.techprotech.agenda.modulos.plataforma.aplicacion;

import com.techprotech.agenda.modulos.plataforma.api.dto.ResumenPlataformaResponse;
import com.techprotech.agenda.modulos.plataforma.api.dto.TenantPlataformaResponse;
import com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.ConsultaPlataformaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ServicioConsultaPlataforma {
    private final ConsultaPlataformaRepositorio consultas;

    public ServicioConsultaPlataforma(ConsultaPlataformaRepositorio consultas) {
        this.consultas = consultas;
    }

    public ResumenPlataformaResponse resumen() { return consultas.resumen(); }
    public List<TenantPlataformaResponse> tenants() { return consultas.tenants(); }
}
