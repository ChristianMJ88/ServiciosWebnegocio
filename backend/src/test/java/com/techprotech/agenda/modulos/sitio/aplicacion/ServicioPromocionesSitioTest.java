package com.techprotech.agenda.modulos.sitio.aplicacion;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.sitio.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.sitio.api.dto.*;
import com.techprotech.agenda.modulos.servicios.infraestructura.repositorio.ServicioRepositorio;
import com.techprotech.agenda.modulos.sucursales.infraestructura.repositorio.SucursalRepositorio;
import com.techprotech.agenda.modulos.admin.infraestructura.repositorio.AuditoriaConfiguracionEmpresaRepositorio;
import com.techprotech.agenda.compartido.media.aplicacion.ServicioMediaEmpresa;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class ServicioPromocionesSitioTest {
    final EmpresaSitioPromocionRepositorio promociones=mock(EmpresaSitioPromocionRepositorio.class);
    final EmpresaSitioPromocionServicioRepositorio relaciones=mock(EmpresaSitioPromocionServicioRepositorio.class);
    final EmpresaSitioPromocionSucursalRepositorio ramas=mock(EmpresaSitioPromocionSucursalRepositorio.class);
    final EmpresaSitioConfigRepositorio sitios=mock(EmpresaSitioConfigRepositorio.class);
    final AuditoriaConfiguracionEmpresaRepositorio auditoria=mock(AuditoriaConfiguracionEmpresaRepositorio.class);
    final Map<Long,EmpresaSitioPromocionEntidad> filas=new LinkedHashMap<>();
    final ServicioPromocionesSitio servicio=new ServicioPromocionesSitio(promociones,relaciones,ramas,sitios,
        mock(ServicioRepositorio.class),mock(SucursalRepositorio.class),auditoria,new ObjectMapper(),mock(ServicioMediaEmpresa.class));
    @BeforeEach void preparar() {
        when(sitios.existsById(1L)).thenReturn(true);
        when(promociones.findByEmpresaIdOrderByOrdenAsc(1L)).thenAnswer(i -> new ArrayList<>(filas.values()));
        when(promociones.findByIdAndEmpresaId(anyLong(),eq(1L))).thenAnswer(i -> Optional.ofNullable(filas.get(i.getArgument(0))));
        when(promociones.saveAndFlush(any())).thenAnswer(i -> { EmpresaSitioPromocionEntidad p=i.getArgument(0);if(p.getId()==null)p.setId((long)filas.size()+1);filas.put(p.getId(),p);return p; });
    }
    private PromocionSitioRequest item(Long id,String titulo) {
        return new PromocionSitioRequest(titulo,null,null,null,null,null,"PORCENTAJE",new BigDecimal("20"),"MXN","Reservar",true,0,List.of(),List.of(),id);
    }
    @Test void conservaIdsYHaceBorradoLogicoSinRomperCitas() {
        var primera=servicio.actualizar(1L,77L,new ActualizarPromocionesSitioRequest(List.of(item(null,"Uno"),item(null,"Dos"))));
        Long uno=primera.getFirst().id(), dos=primera.get(1).id();
        var cita=new com.techprotech.agenda.modulos.citas.infraestructura.entidad.CitaEntidad();
        cita.setPromocionId(dos);cita.setPrecio(new BigDecimal("200.00"));cita.setPromocionSnapshotJson("{\"id\":"+dos+",\"titulo\":\"Dos\"}");
        var segunda=servicio.actualizar(1L,77L,new ActualizarPromocionesSitioRequest(List.of(item(uno,"Uno editado"),item(dos,"Dos editado"))));
        assertThat(segunda).extracting(PromocionSitioAdminResponse::id).containsExactly(uno,dos);
        var tercera=servicio.actualizar(1L,77L,new ActualizarPromocionesSitioRequest(List.of(item(uno,"Uno"))));
        assertThat(tercera).hasSize(1); assertThat(filas.get(dos).getEliminadaEn()).isNotNull();
        assertThat(cita.getPromocionId()).isEqualTo(dos); assertThat(cita.getPrecio()).isEqualByComparingTo("200");
        assertThat(cita.getPromocionSnapshotJson()).contains("Dos").doesNotContain("editado");
        verify(auditoria,times(3)).save(any());
    }
    @Test void rechazaIdAjenoAntesDeModificarLaLista() {
        assertThatThrownBy(() -> servicio.actualizar(1L,77L,new ActualizarPromocionesSitioRequest(List.of(item(99L,"Ajena")))))
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("400");
        verify(promociones,never()).saveAndFlush(any());
    }
}
