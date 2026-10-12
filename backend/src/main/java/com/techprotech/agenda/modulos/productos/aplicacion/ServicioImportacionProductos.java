package com.techprotech.agenda.modulos.productos.aplicacion;

import com.techprotech.agenda.modulos.productos.api.dto.*;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.*;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioCapacidadesEmpresa;
import jakarta.validation.Validator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.util.*;
import java.io.*;

@Service
public class ServicioImportacionProductos {
    private static final List<String> COLUMNAS = List.of("sku", "nombre", "precioBase", "moneda", "tasaImpuesto", "categoriaId", "proveedorId", "descripcion", "codigoBarras", "costo", "controlaInventario", "stockMinimo", "visiblePublico", "activo");
    private final ServicioCatalogoProductos catalogo;
    private final ProductoRepositorio productos;
    private final CategoriaProductoRepositorio categorias;
    private final ProveedorProductoRepositorio proveedores;
    private final ServicioCapacidadesEmpresa capacidades;
    private final Validator validator;
    @Value("${productos.importacion.max-filas:500}") private int maxFilas;
    @Value("${productos.importacion.max-bytes:5242880}") private long maxBytes;
    public ServicioImportacionProductos(ServicioCatalogoProductos catalogo, ProductoRepositorio productos,
            CategoriaProductoRepositorio categorias, ProveedorProductoRepositorio proveedores,
            ServicioCapacidadesEmpresa capacidades, Validator validator) {
        this.catalogo=catalogo; this.productos=productos; this.categorias=categorias; this.proveedores=proveedores; this.capacidades=capacidades; this.validator=validator;
    }
    public byte[] plantilla(Long empresaId) {
        var datos=catalogo.obtenerCatalogo(empresaId, org.springframework.data.domain.PageRequest.of(0,1), "");
        try(var libro=new XSSFWorkbook(); var salida=new ByteArrayOutputStream()) {
            var hoja=libro.createSheet("Productos"); var encabezado=hoja.createRow(0);
            for(int i=0;i<COLUMNAS.size();i++){encabezado.createCell(i).setCellValue(COLUMNAS.get(i)); hoja.setColumnWidth(i, 22*256);}
            hoja.createFreezePane(0,1);
            var instrucciones=libro.createSheet("Instrucciones");
            List<String> textos=List.of("Completa Productos desde la fila 2. No cambies los encabezados.", "SKU debe ser único en tu empresa. Sólo se crean productos nuevos; no se sobrescriben existentes.",
              "Obligatorios: sku, nombre, precioBase, moneda, tasaImpuesto, controlaInventario, stockMinimo, visiblePublico, activo.", "Booleanos: true o false. Importes: números sin símbolo monetario.",
              "Moneda del negocio: "+datos.monedaPredeterminada(), "Categoría y proveedor: usa los IDs de las hojas del mismo archivo o deja vacío.",
              "Imágenes, medidas y precios por zona se configuran después. Las existencias iniciales se registran en Inventario.");
            for(int i=0;i<textos.size();i++) instrucciones.createRow(i).createCell(0).setCellValue(textos.get(i));
            var cat=libro.createSheet("Categorias"); cat.createRow(0).createCell(0).setCellValue("ID / Nombre"); int n=1;
            for(var c:datos.categorias()){var r=cat.createRow(n++);r.createCell(0).setCellValue(c.id());r.createCell(1).setCellValue(c.nombre());}
            var prov=libro.createSheet("Proveedores"); prov.createRow(0).createCell(0).setCellValue("ID / Nombre");n=1;
            for(var v:datos.proveedores()){var r=prov.createRow(n++);r.createCell(0).setCellValue(v.id());r.createCell(1).setCellValue(v.nombre());}
            libro.write(salida);return salida.toByteArray();
        } catch(IOException e){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"No fue posible generar la plantilla");}
    }
    private record Lectura(List<ProductoRequest> productos, RevisionImportacionProductos revision) {}
    public RevisionImportacionProductos revisar(Long empresaId, MultipartFile archivo){return leer(empresaId,archivo).revision();}
    @Transactional
    public int importar(Long empresaId, MultipartFile archivo){
        var lectura=leer(empresaId,archivo);
        if(!lectura.revision().puedeImportar())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Corrige los errores del archivo antes de importar");
        for(var producto:lectura.productos())catalogo.crearProducto(empresaId,producto);
        return lectura.productos().size();
    }
    private Lectura leer(Long empresaId, MultipartFile archivo){
        if(archivo.isEmpty() || archivo.getSize()>maxBytes)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Archivo vacío o superior al límite configurado");
        List<RevisionImportacionProductos.Fila> filas=new ArrayList<>(); List<ProductoRequest> solicitudes=new ArrayList<>(); Set<String> skus=new HashSet<>(); Set<String> slugs=new HashSet<>();
        long uso=productos.countByEmpresaIdAndActivoTrue(empresaId);long activos=0;
        try(var libro=new XSSFWorkbook(archivo.getInputStream())){
            var hoja=libro.getSheet("Productos"); if(hoja==null)throw new IllegalArgumentException("Falta la hoja Productos");
            if(hoja.getLastRowNum()>maxFilas)throw new IllegalArgumentException("El archivo supera el límite de "+maxFilas+" filas");
            var formato=new DataFormatter(Locale.ROOT);
            for(int i=0;i<COLUMNAS.size();i++)if(!COLUMNAS.get(i).equals(valor(hoja.getRow(0),i,formato)))throw new IllegalArgumentException("Encabezados incorrectos; descarga la plantilla vigente");
            for(int i=1;i<=hoja.getLastRowNum();i++){
                var row=hoja.getRow(i); if(row==null)continue;
                String[] v=new String[COLUMNAS.size()]; boolean vacia=true;for(int j=0;j<v.length;j++){v[j]=valor(row,j,formato);if(!v[j].isBlank())vacia=false;}if(vacia)continue;
                List<String> errores=new ArrayList<>();
                try{
                    var request=new ProductoRequest(id(v[5]),id(v[6]),null,v[0],v[8],v[1],null,v[7],null,decimal(v[2]),v[9].isBlank()?null:decimal(v[9]),null,null,null,null,v[3],decimal(v[4]),booleano(v[10]),decimal(v[11]),booleano(v[12]),booleano(v[13]),List.of(),null,null);
                    validator.validate(request).forEach(e->errores.add(e.getPropertyPath()+": "+e.getMessage()));
                    if(!skus.add(v[0].toLowerCase(Locale.ROOT)))errores.add("SKU repetido en el archivo");
                    if(productos.existsByEmpresaIdAndSku(empresaId,v[0]))errores.add("SKU ya registrado en tu empresa");
                    if(request.categoriaId()!=null && categorias.findByIdAndEmpresaId(request.categoriaId(),empresaId).isEmpty())errores.add("Categoría no disponible en tu empresa");
                    if(request.proveedorId()!=null && proveedores.findByIdAndEmpresaId(request.proveedorId(),empresaId).isEmpty())errores.add("Proveedor no disponible en tu empresa");
                    if(request.activo()){capacidades.exigirLimite(empresaId,"PRODUCTOS_MAX",uso+activos);activos++;}
                    if(errores.isEmpty()) {
                        catalogo.validarProductoImportacion(empresaId,request);
                        if(!slugs.add(catalogo.slug(request.nombre()))) errores.add("Dos nombres generan la misma dirección pública; usa nombres diferentes");
                    }
                    solicitudes.add(request);
                }catch(IllegalArgumentException e){errores.add("Revisa números, IDs y booleanos (true/false)");}
                catch(ResponseStatusException e){errores.add(e.getReason());}
                filas.add(new RevisionImportacionProductos.Fila(i+1,v[0],v[1],errores));
            }
        }catch(IOException | IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No se pudo leer el Excel: "+e.getMessage());}
        return new Lectura(solicitudes,new RevisionImportacionProductos(filas,filas.size(),!filas.isEmpty()&&filas.stream().allMatch(f->f.errores().isEmpty())));
    }
    private String valor(Row row,int i,DataFormatter formato){if(row==null)return "";var celda=row.getCell(i);if(celda!=null&&celda.getCellType()==CellType.FORMULA)throw new IllegalArgumentException("No se permiten fórmulas; pega sólo valores");return formato.formatCellValue(celda).trim();}
    private Long id(String v){return v.isBlank()?null:Long.valueOf(v);}
    private BigDecimal decimal(String v){return new BigDecimal(v);}
    private boolean booleano(String v){if(!v.equalsIgnoreCase("true")&&!v.equalsIgnoreCase("false"))throw new IllegalArgumentException();return Boolean.parseBoolean(v);}
}
