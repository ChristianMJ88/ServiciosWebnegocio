package com.techprotech.agenda.compartido.whatsapp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlantillaWhatsappEmpresaRepositorio extends JpaRepository<PlantillaWhatsappEmpresaEntidad, Long> {

    List<PlantillaWhatsappEmpresaEntidad> findByEmpresaIdOrderByUsoAscNombreAsc(Long empresaId);

    Optional<PlantillaWhatsappEmpresaEntidad> findByIdAndEmpresaId(Long id, Long empresaId);

    Optional<PlantillaWhatsappEmpresaEntidad> findFirstByEmpresaIdAndUsoIgnoreCaseAndActivaTrueOrderByIdDesc(Long empresaId, String uso);

    boolean existsByEmpresaIdAndUsoIgnoreCaseAndContentSidIgnoreCase(Long empresaId, String uso, String contentSid);
}
