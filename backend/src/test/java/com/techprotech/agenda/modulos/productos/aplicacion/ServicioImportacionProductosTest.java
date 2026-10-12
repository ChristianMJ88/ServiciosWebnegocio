package com.techprotech.agenda.modulos.productos.aplicacion;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioCapacidadesEmpresa;
import jakarta.validation.Validation;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ServicioImportacionProductosTest {
    ServicioCatalogoProductos catalogo=mock(ServicioCatalogoProductos.class);
    ProductoRepositorio productos=mock(ProductoRepositorio.class);
    CategoriaProductoRepositorio categorias=mock(CategoriaProductoRepositorio.class);
    ProveedorProductoRepositorio proveedores=mock(ProveedorProductoRepositorio.class);
    ServicioCapacidadesEmpresa capacidades=mock(ServicioCapacidadesEmpresa.class);
    ServicioImportacionProductos servicio;
    @BeforeEach void iniciar(){
        servicio=new ServicioImportacionProductos(catalogo,productos,categorias,proveedores,capacidades,Validation.buildDefaultValidatorFactory().getValidator());
        ReflectionTestUtils.setField(servicio,"maxFilas",500);ReflectionTestUtils.setField(servicio,"maxBytes",5242880L);
    }
    MockMultipartFile archivo(String sku,String categoria) throws Exception {
        try(var libro=new XSSFWorkbook();var salida=new ByteArrayOutputStream()){
            var hoja=libro.createSheet("Productos");var h=hoja.createRow(0);
            String[] columnas={"sku","nombre","precioBase","moneda","tasaImpuesto","categoriaId","proveedorId","descripcion","codigoBarras","costo","controlaInventario","stockMinimo","visiblePublico","activo"};
            for(int i=0;i<columnas.length;i++)h.createCell(i).setCellValue(columnas[i]);
            String[] valores={sku,"Articulo", "80.00","MXN","0",categoria,"","","","","true","0","false","true"};
            var r=hoja.createRow(1);for(int i=0;i<valores.length;i++)r.createCell(i).setCellValue(valores[i]);
            libro.write(salida);return new MockMultipartFile("archivo","productos.xlsx","application/octet-stream",salida.toByteArray());
        }
    }
    @Test void revisionNoEscribeYUsaTenant() throws Exception {
        var revision=servicio.revisar(77L,archivo("SKU-1",""));assertTrue(revision.puedeImportar());
        verify(productos).existsByEmpresaIdAndSku(77L,"SKU-1");verify(capacidades).exigirLimite(77L,"PRODUCTOS_MAX",0);
        verify(catalogo).validarProductoImportacion(eq(77L),any());
        verify(catalogo,never()).crearProducto(any(),any());
    }
    @Test void rechazaDuplicadoSinCrear() throws Exception {
        when(productos.existsByEmpresaIdAndSku(77L,"SKU-1")).thenReturn(true);
        var file=archivo("SKU-1","");assertFalse(servicio.revisar(77L,file).puedeImportar());
        assertThrows(ResponseStatusException.class,()->servicio.importar(77L,file));verifyNoInteractions(catalogo);
    }
    @Test void importarRevalidaAntesDeCrear() throws Exception {
        assertEquals(1,servicio.importar(77L,archivo("SKU-1","")));
        verify(catalogo).validarProductoImportacion(eq(77L),any());
        verify(catalogo).crearProducto(eq(77L),any());
    }
    @Test void rechazaCategoriaAjena() throws Exception {
        when(categorias.findByIdAndEmpresaId(10L,77L)).thenReturn(Optional.empty());
        var r=servicio.revisar(77L,archivo("SKU-1","10"));assertFalse(r.puedeImportar());assertTrue(r.filas().getFirst().errores().stream().anyMatch(e->e.contains("Categoría")));
    }
}
