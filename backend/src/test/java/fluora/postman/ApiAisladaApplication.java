package fluora.postman;

import com.techprotech.agenda.AgendaApplication;
import com.techprotech.agenda.soporte.MysqlAislado;
import com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago.ClienteMercadoPagoPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.sql.DataSource;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.Statement;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/** Solo classpath de pruebas. No se empaqueta ni existe en el backend desplegable. */
public class ApiAisladaApplication {
    public static void main(String[] args) throws Exception {
        if (!"aislado".equals(System.getenv("FLUORA_API_ENTORNO")))
            throw new IllegalStateException("Esta aplicación requiere el entorno aislado explícito");
        try (var c = java.sql.DriverManager.getConnection(System.getenv("SPRING_DATASOURCE_URL"),
                System.getenv("SPRING_DATASOURCE_USERNAME"),System.getenv("SPRING_DATASOURCE_PASSWORD"))) {
            MysqlAislado.comprobar(c);
            comprobarBase(c.getCatalog());
        }
        new SpringApplication(AgendaApplication.class, Config.class).run(args);
    }
    public static void comprobarBase(String nombre) {
        if(nombre==null || !nombre.matches("fluora_test_011_[a-z0-9]+"))
            throw new IllegalStateException("Esta aplicación solo admite una copia fluora_test_011_* efímera");
    }
    @TestConfiguration(proxyBeanMethods=false)
    @Import(Marcador.class)
    public static class Config {
        @Bean Fixtures fixtures(JdbcTemplate jdbc, PasswordEncoder encoder) { return new Fixtures(jdbc,encoder); }
        @Bean ApplicationRunner preparar(Fixtures fixtures,ObjectMapper mapper,com.techprotech.agenda.seguridad.jwt.PropiedadesJwt props) {
            return args -> {
                fixtures.crear();
                var expirado=new com.techprotech.agenda.seguridad.jwt.ServicioTokenJwt(props,Clock.offset(Clock.systemUTC(),Duration.ofDays(-2)));
                fixtures.datos.put("token_caducado",expirado.generarTokenAcceso((String)fixtures.datos.get("admin_a_correo"),(Long)fixtures.datos.get("admin_id_a"),(Long)fixtures.datos.get("empresa_a"),(String)fixtures.datos.get("slug_a"),List.of(),List.of(),List.of()));
                fixtures.datos.put("ayer",LocalDate.now(ZoneId.of("America/Mexico_City")).minusDays(1).toString());
                fixtures.datos.put("slug_inexistente","ausente-"+fixtures.nonce);
                Path destino=Path.of(System.getenv("FLUORA_API_ENV_PATH"));
                Path file=destino.resolveSibling(destino.getFileName()+".part");
                Files.createFile(file,PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
                var values=fixtures.datos.entrySet().stream().map(e->Map.of("key",e.getKey(),"value",e.getValue().toString(),"enabled",true)).toList();
                Files.writeString(file,mapper.writeValueAsString(Map.of("name","Fluora aislado efímero","values",values,"_postman_variable_scope","environment")));
                Files.move(file,destino,StandardCopyOption.ATOMIC_MOVE);
            };
        }
        @Bean @Primary ClienteMercadoPagoPoint pointSimulado() {
            var client=mock(ClienteMercadoPagoPoint.class);
            var orders=new ConcurrentHashMap<String,ClienteMercadoPagoPoint.OrdenRemota>();
            var keys=new ConcurrentHashMap<String,String>();
            when(client.listarTerminales(anyLong())).thenReturn(List.of());
            when(client.crearOrden(anyLong(),anyString(),anyString(),anyString(),any(),any(),anyString())).thenAnswer(i->{
                String id=keys.computeIfAbsent(i.getArgument(0)+":"+i.getArgument(1),k->"ORD_"+UUID.randomUUID().toString().replace("-",""));
                return orders.computeIfAbsent(id,k->new ClienteMercadoPagoPoint.OrdenRemota(id,i.<String>getArgument(1).startsWith("rechazo-")?"failed":"created",null,null,null,null,null,i.getArgument(3),i.getArgument(4),"MXN"));
            });
            when(client.consultar(anyLong(),anyString())).thenAnswer(i->{var o=orders.get(i.getArgument(1));if(o==null)throw new IllegalArgumentException("Orden simulada inexistente");return o;});
            doAnswer(i->{String id=i.getArgument(1);var o=orders.get(id);orders.put(id,new ClienteMercadoPagoPoint.OrdenRemota(id,"processed","accredited","PAY_"+id,"processed","accredited",null,o.externalReference(),o.importe(),o.moneda()));return null;}).when(client).simularAprobacion(anyLong(),anyString());
            when(client.cancelar(anyLong(),anyString(),anyString())).thenAnswer(i->{String id=i.getArgument(1);var o=orders.get(id);var n=new ClienteMercadoPagoPoint.OrdenRemota(id,"canceled",null,null,"canceled",null,null,o.externalReference(),o.importe(),o.moneda());orders.put(id,n);return n;});
            return client;
        }
    }
    @RestController
    public static class Marcador {
        private final JdbcTemplate jdbc; private final Fixtures fixtures;
        public Marcador(JdbcTemplate jdbc,Fixtures fixtures){this.jdbc=jdbc;this.fixtures=fixtures;}
        @GetMapping("/api/v1/publico/__pruebas/aislamiento")
        public Map<String,Object> obtener(@RequestHeader("X-Fluora-Prueba") String nonce) throws Exception {
            if (!fixtures.nonce.equals(nonce)) throw new ResponseStatusException(NOT_FOUND);
            try(var c=Objects.requireNonNull(jdbc.getDataSource()).getConnection()){MysqlAislado.comprobar(c);}
            Long empresa=(Long)fixtures.datos.get("empresa_a");
            return Map.of("nonce",nonce,"database",Objects.requireNonNull(jdbc.queryForObject("SELECT DATABASE()",String.class)),
                    "citas",jdbc.queryForObject("SELECT COUNT(*) FROM cita WHERE empresa_id=?",Long.class,empresa),
                    "ventas",jdbc.queryForObject("SELECT COUNT(*) FROM venta WHERE empresa_id=?",Long.class,empresa),
                    "usuarios",jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE empresa_id=?",Long.class,empresa),
                    "citasB",jdbc.queryForObject("SELECT COUNT(*) FROM cita WHERE empresa_id=?",Long.class,fixtures.datos.get("empresa_b")),
                    "usuariosB",jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE empresa_id=?",Long.class,fixtures.datos.get("empresa_b")));
        }
    }
    public static class Fixtures {
        final JdbcTemplate jdbc; final PasswordEncoder encoder;
        final String nonce=UUID.randomUUID().toString(), password=UUID.randomUUID()+"aA1!";
        final Map<String,Object> datos=new LinkedHashMap<>();
        Fixtures(JdbcTemplate jdbc,PasswordEncoder encoder){this.jdbc=jdbc;this.encoder=encoder;}
        long insert(String sql,Object...args) {
            var key=new GeneratedKeyHolder();jdbc.update(c->{var s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);for(int i=0;i<args.length;i++)s.setObject(i+1,args[i]);return s;},key);
            return Objects.requireNonNull(key.getKey()).longValue();
        }
        void exigirUna(int filas) { if(filas!=1) throw new IllegalStateException("Fixture sin rol o sucursal asignados"); }
        long usuario(long empresa,String alias,String role,long sucursal) {
            String correo=alias+"-"+nonce+"@example.test";
            long id=insert("INSERT INTO usuario(empresa_id,correo,contrasena_hash,correo_verificado_en) VALUES(?,?,?,UTC_TIMESTAMP())",empresa,correo,encoder.encode(password));
            exigirUna(jdbc.update("INSERT INTO usuario_rol(usuario_id,rol_id,empresa_id) SELECT ?,id,? FROM rol WHERE codigo=?",id,empresa,role));
            exigirUna(jdbc.update("INSERT INTO usuario_rol_empresa(usuario_id,rol_empresa_id) SELECT ?,id FROM rol_empresa WHERE empresa_id=? AND codigo=?",id,empresa,role));
            exigirUna(jdbc.update("INSERT INTO usuario_interno_sucursal(usuario_id,sucursal_id) VALUES(?,?)",id,sucursal));
            datos.put(alias+"_correo",correo);return id;
        }
        long servicio(long empresa,long sucursal,long staff,String alias,String precio,boolean anticipo) {
            long id=insert("INSERT INTO servicio(empresa_id,sucursal_id,nombre,slug,duracion_minutos,precio,moneda,visible_publico,requiere_anticipo,anticipo_tipo,anticipo_valor) VALUES(?,?,?,?,30,?,'MXN',1,?,'PORCENTAJE',50)",empresa,sucursal,"Servicio prueba "+alias,"prueba-"+alias,precio,anticipo);
            jdbc.update("INSERT INTO asignacion_servicio_prestador(prestador_id,servicio_id) VALUES(?,?)",staff,id);jdbc.update("INSERT INTO servicio_sucursal(empresa_id,servicio_id,sucursal_id,activo) VALUES(?,?,?,1)",empresa,id,sucursal);datos.put(alias,id);return id;
        }
        long cita(long empresa,long sucursal,long servicio,long staff,long cliente,String alias,boolean checkin,String moneda) {
            var inicio=LocalDate.now(ZoneId.of("America/Mexico_City")).minusDays(1).atTime(10,0);
            long id=insert("INSERT INTO cita(empresa_id,sucursal_id,servicio_id,prestador_id,cliente_id,estado,inicio,fin,precio,moneda,creada_por_usuario_id,check_in_en,folio) VALUES(?,?,?,?,?,'CONFIRMADA',?,?,500,?,?,?,?)",empresa,sucursal,servicio,staff,cliente,inicio,inicio.plusMinutes(30),moneda,staff,checkin?LocalDateTime.now(Clock.systemUTC()):null,"API-"+UUID.randomUUID().toString().substring(0,10));
            datos.put(alias,id);return id;
        }
        void crear() {
            datos.put("base","http://127.0.0.1:18080");datos.put("entorno","aislado");datos.put("nonce",nonce);datos.put("api_password",password);
            long plan=insert("INSERT INTO plataforma_plan(codigo,nombre,moneda,precio_mensual,activo,publico) VALUES(?,?,'MXN',100,1,0)","API-"+nonce,"Plan de prueba");
            jdbc.update("INSERT INTO plataforma_plan_modulo(plan_id,modulo_id) SELECT ?,id FROM plataforma_modulo WHERE activo=1",plan);
            for(String tenant:List.of("a","b","oculto")) {
                String slug="api-"+tenant+"-"+nonce;
                long empresa=insert("INSERT INTO empresa(nombre,slug) VALUES(?,?)","Negocio de prueba "+tenant,slug);
                jdbc.update("INSERT INTO rol_empresa(empresa_id,rol_base_id,codigo,nombre,activo,editable) SELECT ?,id,codigo,codigo,1,0 FROM rol WHERE codigo IN ('ADMIN','RECEPCIONISTA','CAJERO','STAFF','CLIENTE')",empresa);
                jdbc.update("INSERT INTO rol_empresa_permiso(rol_empresa_id,permiso_id) SELECT re.id,rp.permiso_id FROM rol_empresa re JOIN rol_permiso rp ON rp.rol_id=re.rol_base_id WHERE re.empresa_id=?",empresa);
                long sucursal=insert("INSERT INTO sucursal(empresa_id,nombre) VALUES(?,?)",empresa,"Sucursal prueba");
                jdbc.update("INSERT INTO empresa_sitio_config(empresa_id,slug,nombre_comercial,publicado) VALUES(?,?,?,?)",empresa,slug,"Negocio de prueba "+tenant,!tenant.equals("oculto"));
                jdbc.update("INSERT INTO plataforma_suscripcion(empresa_id,plan_id,estado,ciclo,precio_acordado,moneda,inicia_en,termina_en) VALUES(?,?,'ACTIVA','MENSUAL',100,'MXN',UTC_TIMESTAMP(),DATE_ADD(UTC_TIMESTAMP(),INTERVAL 30 DAY))",empresa,plan);
                long admin=usuario(empresa,"admin_"+tenant,"ADMIN",sucursal);datos.put("admin_id_"+tenant,admin);
                long staff=usuario(empresa,"staff_"+tenant,"STAFF",sucursal);
                jdbc.update("INSERT INTO prestador_servicio(usuario_id,sucursal_id,nombre_mostrar) VALUES(?,?,'Prestador prueba')",staff,sucursal);
                long cliente=usuario(empresa,"cliente_"+tenant,"CLIENTE",sucursal);
                jdbc.update("INSERT INTO cliente(usuario_id,nombre_completo,telefono,acepta_whatsapp) VALUES(?,'Cliente prueba','5550000000',0)",cliente);
                datos.put("empresa_"+tenant,empresa);datos.put("slug_"+tenant,slug);datos.put("sucursal_"+tenant,sucursal);datos.put("staff_"+tenant,staff);datos.put("cliente_"+tenant,cliente);
                long sin=servicio(empresa,sucursal,staff,"servicio_sin_"+tenant,"500",false);
                servicio(empresa,sucursal,staff,"servicio_anticipo_"+tenant,"500",true);
                for(int dia=1;dia<=7;dia++)for(var sujeto:Map.of("SUCURSAL",sucursal,"PRESTADOR",staff).entrySet())jdbc.update("INSERT INTO regla_disponibilidad(empresa_id,tipo_sujeto,sujeto_id,dia_semana,hora_inicio,hora_fin,intervalo_minutos) VALUES(?,?,?,?, '00:00','23:59',30)",empresa,sujeto.getKey(),sujeto.getValue(),dia);
                long proveedor=Objects.requireNonNull(jdbc.queryForObject("SELECT id FROM proveedor_pago WHERE codigo='MERCADO_PAGO'",Long.class));
                jdbc.update("INSERT INTO empresa_proveedor_pago(empresa_id,proveedor_pago_id,estado,ambiente,llave_publica) VALUES(?,?,'CONECTADO','PRUEBA','TEST-aislado-sin-red')",empresa,proveedor);
                jdbc.update("INSERT INTO empresa_checkout_config(empresa_id,proveedor_predeterminado_id,habilitado,aceptar_tarjeta,permitir_pago_productos,permitir_pago_servicios,permitir_recoleccion,moneda) VALUES(?,?,1,1,1,1,1,'MXN')",empresa,proveedor);
                cita(empresa,sucursal,sin,staff,cliente,"cita_"+tenant,true,"MXN");
                long venta=insert("INSERT INTO venta(empresa_id,sucursal_id,folio,estado,canal,moneda,subtotal,descuento_total,impuestos_total,total,creada_por_usuario_id,creada_en) VALUES(?,?,?,'POR_COBRAR','STAFF','MXN',50,0,0,50,?,UTC_TIMESTAMP())",empresa,sucursal,"API-"+tenant+"-"+nonce.substring(0,12),admin);
                datos.put("venta_"+tenant,venta);
                if(!tenant.equals("a")) continue;
                usuario(empresa,"recepcion","RECEPCIONISTA",sucursal);long cajero=usuario(empresa,"caja","CAJERO",sucursal);
                long otra=insert("INSERT INTO sucursal(empresa_id,nombre) VALUES(?,'Segunda sucursal prueba')",empresa);datos.put("otra_sucursal",otra);jdbc.update("INSERT INTO usuario_interno_sucursal(usuario_id,sucursal_id) VALUES(?,?)",cajero,otra);
                long otroCliente=usuario(empresa,"otro_cliente","CLIENTE",sucursal);jdbc.update("INSERT INTO cliente(usuario_id,nombre_completo,telefono) VALUES(?,'Otro cliente prueba','5550000001')",otroCliente);
                cita(empresa,sucursal,sin,staff,cliente,"cita_a2",true,"MXN");
                for(String alias:List.of("cita_parcial","cita_point","cita_point2","cita_cancelar","cita_rechazo"))cita(empresa,sucursal,sin,staff,cliente,alias,true,"MXN");
                cita(empresa,sucursal,sin,staff,otroCliente,"cita_otro_cliente",true,"MXN");
                cita(empresa,otra,sin,staff,cliente,"cita_otra_sucursal",true,"MXN");
                cita(empresa,sucursal,sin,staff,cliente,"cita_otra_moneda",true,"USD");
                cita(empresa,sucursal,sin,staff,cliente,"cita_sin_checkin",false,"MXN");
                long online=cita(empresa,sucursal,sin,staff,cliente,"cita_online",false,"MXN");
                jdbc.update("INSERT INTO pago_cita(empresa_id,cita_id,monto,metodo_pago,registrado_por_usuario_id,registrado_en) VALUES(?,?,500,'TARJETA',?,UTC_TIMESTAMP())",empresa,online,admin);
                jdbc.update("UPDATE cita SET precio=700 WHERE id=?",datos.get("cita_point"));
                long producto=insert("INSERT INTO producto(empresa_id,sku,nombre,slug,precio_base,costo,moneda,tasa_impuesto,controla_inventario,stock_minimo,visible_publico,activo) VALUES(?,'API','Producto prueba','producto-prueba',50,20,'MXN',0,0,0,1,1)",empresa);datos.put("producto",producto);
                long terminal=insert("INSERT INTO mercado_pago_terminal(empresa_id,sucursal_id,terminal_id,nombre,modo_operacion,activa) VALUES(?,?,'API-VIRTUAL','Terminal simulada','PDV',1)",empresa,sucursal);datos.put("terminal",terminal);
            }
            datos.put("cita_ausente",jdbc.queryForObject("SELECT COALESCE(MAX(id),0)+1000 FROM cita",Long.class));
            datos.put("fecha",LocalDate.now(ZoneId.of("America/Mexico_City")).plusDays(2).toString());
        }
    }
}
