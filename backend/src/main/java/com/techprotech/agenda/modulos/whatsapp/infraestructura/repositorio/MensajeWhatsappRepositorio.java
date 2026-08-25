package com.techprotech.agenda.modulos.whatsapp.infraestructura.repositorio;

import com.techprotech.agenda.modulos.whatsapp.infraestructura.entidad.MensajeWhatsappEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensajeWhatsappRepositorio extends JpaRepository<MensajeWhatsappEntidad, Long> {

    List<MensajeWhatsappEntidad> findTop200ByEmpresaIdOrderByCreadoEnDesc(Long empresaId);
}
