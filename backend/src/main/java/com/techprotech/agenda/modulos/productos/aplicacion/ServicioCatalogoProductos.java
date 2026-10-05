package com.techprotech.agenda.modulos.productos.aplicacion;

import com.techprotech.agenda.compartido.media.aplicacion.ServicioMediaEmpresa;
import com.techprotech.agenda.compartido.media.dominio.CategoriaMedia;
import com.techprotech.agenda.compartido.media.infraestructura.EmpresaArchivoMediaEntidad;
import com.techprotech.agenda.modulos.productos.api.dto.*;
import com.techprotech.agenda.modulos.parametros.aplicacion.ServicioParametrosSistema;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioCapacidadesEmpresa;
import com.techprotech.agenda.modulos.productos.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.productos.infraestructura.repositorio.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServicioCatalogoProductos {
    private final CategoriaProductoRepositorio categorias;
    private final ProveedorProductoRepositorio proveedores;
    private final ZonaPrecioProductoRepositorio zonas;
    private final ProductoRepositorio productos;
    private final PrecioZonaProductoRepositorio precios;
    private final ServicioMediaEmpresa servicioMedia;
    private final ServicioParametrosSistema parametros;
    private final ServicioCapacidadesEmpresa capacidadesEmpresa;

    public ServicioCatalogoProductos(
            CategoriaProductoRepositorio categorias,
            ProveedorProductoRepositorio proveedores,
            ZonaPrecioProductoRepositorio zonas,
            ProductoRepositorio productos,
            PrecioZonaProductoRepositorio precios,
            ServicioMediaEmpresa servicioMedia,
            ServicioParametrosSistema parametros,
            ServicioCapacidadesEmpresa capacidadesEmpresa
    ) {
        this.categorias = categorias;
        this.proveedores = proveedores;
        this.zonas = zonas;
        this.productos = productos;
        this.precios = precios;
        this.servicioMedia = servicioMedia;
        this.parametros = parametros;
        this.capacidadesEmpresa = capacidadesEmpresa;
    }

    @Transactional(readOnly = true)
    public CatalogoProductosAdminResponse obtenerCatalogo(Long empresaId) {
        List<CategoriaProductoEntidad> listaCategorias = categorias.findByEmpresaIdOrderByOrdenAscNombreAsc(empresaId);
        List<ProveedorProductoEntidad> listaProveedores = proveedores.findByEmpresaIdOrderByNombreAsc(empresaId);
        List<ZonaPrecioProductoEntidad> listaZonas = zonas.findByEmpresaIdOrderByPrioridadAscNombreAsc(empresaId);
        List<ProductoEntidad> listaProductos = productos.findByEmpresaIdOrderByNombreAsc(empresaId);
        Map<Long, CategoriaProductoEntidad> categoriaPorId = listaCategorias.stream().collect(Collectors.toMap(CategoriaProductoEntidad::getId, Function.identity()));
        Map<Long, ProveedorProductoEntidad> proveedorPorId = listaProveedores.stream().collect(Collectors.toMap(ProveedorProductoEntidad::getId, Function.identity()));
        Map<Long, ZonaPrecioProductoEntidad> zonaPorId = listaZonas.stream().collect(Collectors.toMap(ZonaPrecioProductoEntidad::getId, Function.identity()));
        Map<Long, List<PrecioZonaProductoEntidad>> preciosPorProducto = listaProductos.isEmpty()
                ? Map.of()
                : precios.findByEmpresaIdAndProductoIdIn(empresaId, listaProductos.stream().map(ProductoEntidad::getId).toList())
                .stream().collect(Collectors.groupingBy(PrecioZonaProductoEntidad::getProductoId));

        return new CatalogoProductosAdminResponse(
                parametros.listar(empresaId).stream()
                        .filter(parametro -> "MONEDA_PREDETERMINADA".equals(parametro.clave()))
                        .map(parametro -> parametro.valor())
                        .findFirst().orElseThrow(() -> new IllegalStateException("Falta el parámetro MONEDA_PREDETERMINADA")),
                listaCategorias.stream().map(this::mapearCategoria).toList(),
                listaProveedores.stream().map(this::mapearProveedor).toList(),
                listaZonas.stream().map(this::mapearZona).toList(),
                listaProductos.stream().map(producto -> mapearProducto(
                        producto, categoriaPorId, proveedorPorId, zonaPorId,
                        preciosPorProducto.getOrDefault(producto.getId(), List.of())
                )).toList()
        );
    }

    @Transactional
    public CategoriaProductoResponse crearCategoria(Long empresaId, CategoriaProductoRequest request) {
        CategoriaProductoEntidad entidad = new CategoriaProductoEntidad();
        entidad.setEmpresaId(empresaId);
        aplicarCategoria(empresaId, entidad, request);
        return mapearCategoria(categorias.save(entidad));
    }

    @Transactional
    public CategoriaProductoResponse actualizarCategoria(Long empresaId, Long id, CategoriaProductoRequest request) {
        CategoriaProductoEntidad entidad = categoria(empresaId, id);
        validarJerarquiaCategoria(empresaId, id, request.categoriaPadreId());
        aplicarCategoria(empresaId, entidad, request);
        return mapearCategoria(categorias.save(entidad));
    }

    @Transactional
    public ProveedorProductoResponse crearProveedor(Long empresaId, ProveedorProductoRequest request) {
        ProveedorProductoEntidad entidad = new ProveedorProductoEntidad();
        entidad.setEmpresaId(empresaId);
        aplicarProveedor(empresaId, entidad, request);
        return mapearProveedor(proveedores.save(entidad));
    }

    @Transactional
    public ProveedorProductoResponse actualizarProveedor(Long empresaId, Long id, ProveedorProductoRequest request) {
        ProveedorProductoEntidad entidad = proveedor(empresaId, id);
        aplicarProveedor(empresaId, entidad, request);
        return mapearProveedor(proveedores.save(entidad));
    }

    @Transactional
    public ZonaPrecioProductoResponse crearZona(Long empresaId, ZonaPrecioProductoRequest request) {
        ZonaPrecioProductoEntidad entidad = new ZonaPrecioProductoEntidad();
        entidad.setEmpresaId(empresaId);
        aplicarZona(empresaId, entidad, request);
        return mapearZona(zonas.save(entidad));
    }

    @Transactional
    public ZonaPrecioProductoResponse actualizarZona(Long empresaId, Long id, ZonaPrecioProductoRequest request) {
        ZonaPrecioProductoEntidad entidad = zona(empresaId, id);
        aplicarZona(empresaId, entidad, request);
        return mapearZona(zonas.save(entidad));
    }

    @Transactional
    public ProductoResponse crearProducto(Long empresaId, ProductoRequest request) {
        if (request.activo()) {
            capacidadesEmpresa.exigirLimite(empresaId, "PRODUCTOS_MAX", productos.countByEmpresaIdAndActivoTrue(empresaId));
        }
        ProductoEntidad entidad = new ProductoEntidad();
        entidad.setEmpresaId(empresaId);
        aplicarProducto(empresaId, entidad, request);
        entidad = productos.save(entidad);
        guardarPrecios(empresaId, entidad.getId(), request.preciosZona());
        return obtenerProducto(empresaId, entidad.getId());
    }

    @Transactional
    public ProductoResponse actualizarProducto(Long empresaId, Long id, ProductoRequest request) {
        ProductoEntidad entidad = producto(empresaId, id);
        if (request.activo() && !entidad.isActivo()) {
            capacidadesEmpresa.exigirLimite(empresaId, "PRODUCTOS_MAX", productos.countByEmpresaIdAndActivoTrue(empresaId));
        }
        aplicarProducto(empresaId, entidad, request);
        productos.save(entidad);
        precios.deleteByEmpresaIdAndProductoId(empresaId, id);
        precios.flush();
        guardarPrecios(empresaId, id, request.preciosZona());
        return obtenerProducto(empresaId, id);
    }

    private ProductoResponse obtenerProducto(Long empresaId, Long id) {
        return obtenerCatalogo(empresaId).productos().stream().filter(item -> item.id().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    private void aplicarCategoria(Long empresaId, CategoriaProductoEntidad entidad, CategoriaProductoRequest request) {
        if (request.categoriaPadreId() != null) categoria(empresaId, request.categoriaPadreId());
        String slug = slug(request.slug() == null || request.slug().isBlank() ? request.nombre() : request.slug());
        boolean repetido = entidad.getId() == null
                ? categorias.existsByEmpresaIdAndSlug(empresaId, slug)
                : categorias.existsByEmpresaIdAndSlugAndIdNot(empresaId, slug, entidad.getId());
        if (repetido) throw error("Ya existe una categoría con ese slug");
        entidad.setCategoriaPadreId(request.categoriaPadreId());
        entidad.setNombre(request.nombre().trim());
        entidad.setSlug(slug);
        entidad.setDescripcion(texto(request.descripcion()));
        entidad.setOrden(request.orden());
        entidad.setActiva(request.activa());
    }

    private void aplicarProveedor(Long empresaId, ProveedorProductoEntidad entidad, ProveedorProductoRequest request) {
        String clave = clave(request.clave());
        if (clave != null) {
            boolean repetida = entidad.getId() == null
                    ? proveedores.existsByEmpresaIdAndClave(empresaId, clave)
                    : proveedores.existsByEmpresaIdAndClaveAndIdNot(empresaId, clave, entidad.getId());
            if (repetida) throw error("Ya existe un proveedor con esa clave");
        }
        entidad.setNombre(request.nombre().trim());
        entidad.setClave(clave);
        entidad.setContacto(texto(request.contacto()));
        entidad.setCorreo(texto(request.correo()));
        entidad.setTelefono(texto(request.telefono()));
        entidad.setSitioWeb(texto(request.sitioWeb()));
        entidad.setNotas(texto(request.notas()));
        entidad.setActivo(request.activo());
    }

    private void aplicarZona(Long empresaId, ZonaPrecioProductoEntidad entidad, ZonaPrecioProductoRequest request) {
        String clave = Optional.ofNullable(clave(request.clave())).orElseGet(() -> clave(request.nombre()));
        boolean repetida = entidad.getId() == null
                ? zonas.existsByEmpresaIdAndClave(empresaId, clave)
                : zonas.existsByEmpresaIdAndClaveAndIdNot(empresaId, clave, entidad.getId());
        if (repetida) throw error("Ya existe una zona de precio con esa clave");
        entidad.setNombre(request.nombre().trim());
        entidad.setClave(clave);
        entidad.setDescripcion(texto(request.descripcion()));
        entidad.setPrioridad(request.prioridad());
        entidad.setActiva(request.activa());
    }

    private void aplicarProducto(Long empresaId, ProductoEntidad entidad, ProductoRequest request) {
        if (request.categoriaId() != null) categoria(empresaId, request.categoriaId());
        if (request.proveedorId() != null) proveedor(empresaId, request.proveedorId());
        String sku = clave(request.sku());
        String slug = slug(request.slug() == null || request.slug().isBlank() ? request.nombre() : request.slug());
        boolean skuRepetido = entidad.getId() == null ? productos.existsByEmpresaIdAndSku(empresaId, sku)
                : productos.existsByEmpresaIdAndSkuAndIdNot(empresaId, sku, entidad.getId());
        boolean slugRepetido = entidad.getId() == null ? productos.existsByEmpresaIdAndSlug(empresaId, slug)
                : productos.existsByEmpresaIdAndSlugAndIdNot(empresaId, slug, entidad.getId());
        if (skuRepetido) throw error("Ya existe un producto con ese SKU");
        if (slugRepetido) throw error("Ya existe un producto con ese slug");

        String imagenUrl = texto(request.imagenUrl());
        if (request.mediaId() != null) {
            EmpresaArchivoMediaEntidad media = servicioMedia.validarPropiedad(empresaId, request.mediaId(), CategoriaMedia.PRODUCTOS);
            if (!Objects.equals(media.getSecureUrl(), imagenUrl)) throw error("La URL no corresponde a la imagen cargada");
            imagenUrl = media.getSecureUrl();
        }
        entidad.setCategoriaId(request.categoriaId());
        entidad.setProveedorId(request.proveedorId());
        entidad.setMediaId(request.mediaId());
        entidad.setSku(sku);
        entidad.setCodigoBarras(texto(request.codigoBarras()));
        entidad.setNombre(request.nombre().trim());
        entidad.setSlug(slug);
        entidad.setDescripcion(texto(request.descripcion()));
        entidad.setImagenUrl(imagenUrl);
        entidad.setPrecioBase(request.precioBase());
        entidad.setCosto(request.costo());
        entidad.setPesoKg(request.pesoKg());
        entidad.setLargoCm(request.largoCm());
        entidad.setAnchoCm(request.anchoCm());
        entidad.setAltoCm(request.altoCm());
        entidad.setMoneda(request.moneda().trim().toUpperCase(Locale.ROOT));
        entidad.setTasaImpuesto(request.tasaImpuesto());
        entidad.setControlaInventario(request.controlaInventario());
        entidad.setStockMinimo(request.stockMinimo());
        entidad.setVisiblePublico(request.visiblePublico());
        entidad.setActivo(request.activo());
    }

    private void guardarPrecios(Long empresaId, Long productoId, List<PrecioZonaProductoRequest> solicitudes) {
        if (solicitudes == null || solicitudes.isEmpty()) return;
        Set<Long> ids = new HashSet<>();
        for (PrecioZonaProductoRequest request : solicitudes) {
            if (!ids.add(request.zonaPrecioId())) throw error("Una zona de precio está repetida");
            zona(empresaId, request.zonaPrecioId());
            PrecioZonaProductoEntidad entidad = new PrecioZonaProductoEntidad();
            entidad.setEmpresaId(empresaId);
            entidad.setProductoId(productoId);
            entidad.setZonaPrecioId(request.zonaPrecioId());
            entidad.setPrecio(request.precio());
            entidad.setMoneda(request.moneda().trim().toUpperCase(Locale.ROOT));
            entidad.setActivo(request.activo());
            precios.save(entidad);
        }
    }

    private void validarJerarquiaCategoria(Long empresaId, Long categoriaId, Long padreId) {
        Set<Long> visitados = new HashSet<>();
        Long actual = padreId;
        while (actual != null) {
            if (Objects.equals(actual, categoriaId) || !visitados.add(actual)) {
                throw error("La jerarquía de categorías contiene un ciclo");
            }
            actual = categoria(empresaId, actual).getCategoriaPadreId();
        }
    }

    private CategoriaProductoEntidad categoria(Long empresaId, Long id) {
        return categorias.findByIdAndEmpresaId(id, empresaId).orElseThrow(() -> error("Categoría no encontrada para este tenant"));
    }
    private ProveedorProductoEntidad proveedor(Long empresaId, Long id) {
        return proveedores.findByIdAndEmpresaId(id, empresaId).orElseThrow(() -> error("Proveedor no encontrado para este tenant"));
    }
    private ZonaPrecioProductoEntidad zona(Long empresaId, Long id) {
        return zonas.findByIdAndEmpresaId(id, empresaId).orElseThrow(() -> error("Zona de precio no encontrada para este tenant"));
    }
    private ProductoEntidad producto(Long empresaId, Long id) {
        return productos.findByIdAndEmpresaId(id, empresaId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    private CategoriaProductoResponse mapearCategoria(CategoriaProductoEntidad e) {
        return new CategoriaProductoResponse(e.getId(), e.getCategoriaPadreId(), e.getNombre(), e.getSlug(), e.getDescripcion(), e.getOrden(), e.isActiva());
    }
    private ProveedorProductoResponse mapearProveedor(ProveedorProductoEntidad e) {
        return new ProveedorProductoResponse(e.getId(), e.getNombre(), e.getClave(), e.getContacto(), e.getCorreo(), e.getTelefono(), e.getSitioWeb(), e.getNotas(), e.isActivo());
    }
    private ZonaPrecioProductoResponse mapearZona(ZonaPrecioProductoEntidad e) {
        return new ZonaPrecioProductoResponse(e.getId(), e.getNombre(), e.getClave(), e.getDescripcion(), e.getPrioridad(), e.isActiva());
    }
    private ProductoResponse mapearProducto(ProductoEntidad e, Map<Long, CategoriaProductoEntidad> categoriaPorId,
                                             Map<Long, ProveedorProductoEntidad> proveedorPorId,
                                             Map<Long, ZonaPrecioProductoEntidad> zonaPorId,
                                             List<PrecioZonaProductoEntidad> listaPrecios) {
        CategoriaProductoEntidad categoria = categoriaPorId.get(e.getCategoriaId());
        ProveedorProductoEntidad proveedor = proveedorPorId.get(e.getProveedorId());
        List<PrecioZonaProductoResponse> preciosZona = listaPrecios.stream()
                .sorted(Comparator.comparingInt(precio -> Optional.ofNullable(zonaPorId.get(precio.getZonaPrecioId())).map(ZonaPrecioProductoEntidad::getPrioridad).orElse(Integer.MAX_VALUE)))
                .map(precio -> new PrecioZonaProductoResponse(precio.getId(), precio.getZonaPrecioId(),
                        Optional.ofNullable(zonaPorId.get(precio.getZonaPrecioId())).map(ZonaPrecioProductoEntidad::getNombre).orElse(null),
                        precio.getPrecio(), precio.getMoneda(), precio.isActivo()))
                .toList();
        return new ProductoResponse(e.getId(), e.getCategoriaId(), categoria == null ? null : categoria.getNombre(),
                e.getProveedorId(), proveedor == null ? null : proveedor.getNombre(), e.getMediaId(), e.getSku(),
                e.getCodigoBarras(), e.getNombre(), e.getSlug(), e.getDescripcion(), e.getImagenUrl(), e.getPrecioBase(),
                e.getCosto(), e.getPesoKg(), e.getLargoCm(), e.getAnchoCm(), e.getAltoCm(), e.getMoneda(),
                e.getTasaImpuesto(), e.isControlaInventario(), e.getStockMinimo(),
                e.isVisiblePublico(), e.isActivo(), preciosZona);
    }

    private String texto(String valor) { return valor == null || valor.isBlank() ? null : valor.trim(); }
    private String clave(String valor) {
        String normalizado = texto(valor);
        return normalizado == null ? null : normalizado.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_-]", "-").replaceAll("-+", "-");
    }
    private String slug(String valor) {
        String sinAcentos = Normalizer.normalize(valor.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String resultado = sinAcentos.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (resultado.isBlank()) throw error("No fue posible generar un slug válido");
        return resultado;
    }
    private ResponseStatusException error(String mensaje) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje); }
}
