package com.techprotech.agenda.compartido.whatsapp;

import org.springframework.stereotype.Service;

@Service
public class ServicioPlantillasWhatsappEmpresa {

    private final PlantillaWhatsappEmpresaRepositorio plantillaWhatsappEmpresaRepositorio;

    public ServicioPlantillasWhatsappEmpresa(PlantillaWhatsappEmpresaRepositorio plantillaWhatsappEmpresaRepositorio) {
        this.plantillaWhatsappEmpresaRepositorio = plantillaWhatsappEmpresaRepositorio;
    }

    public String resolverContentSid(Long empresaId, String uso) {
        if (empresaId == null || uso == null || uso.isBlank()) {
            return null;
        }

        return plantillaWhatsappEmpresaRepositorio
                .findFirstByEmpresaIdAndUsoIgnoreCaseAndActivaTrueOrderByIdDesc(empresaId, uso.trim())
                .map(PlantillaWhatsappEmpresaEntidad::getContentSid)
                .filter(contentSid -> contentSid != null && !contentSid.isBlank())
                .orElse(null);
    }
}
