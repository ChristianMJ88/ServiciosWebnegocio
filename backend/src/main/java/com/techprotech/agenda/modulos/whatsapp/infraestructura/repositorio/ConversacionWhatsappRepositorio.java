package com.techprotech.agenda.modulos.whatsapp.infraestructura.repositorio;

import com.techprotech.agenda.modulos.whatsapp.infraestructura.entidad.ConversacionWhatsappEntidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversacionWhatsappRepositorio extends JpaRepository<ConversacionWhatsappEntidad, Long> {

    Optional<ConversacionWhatsappEntidad> findByEmpresaIdAndTelefonoNormalizado(Long empresaId, String telefonoNormalizado);
}
