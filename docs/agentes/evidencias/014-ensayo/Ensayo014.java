import com.techprotech.agenda.AgendaApplication;
import com.techprotech.agenda.soporte.MysqlAislado;
import com.techprotech.agenda.soporte.EsquemaMigraciones;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ProcesadorConciliacionSuscripciones;
import com.fasterxml.jackson.databind.*;
import org.flywaydb.core.Flyway;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.sql.*;import java.time.*;import java.nio.file.*;import java.net.*;import java.net.http.*;import java.util.*;

public class Ensayo014 {
 static final ObjectMapper JSON=new ObjectMapper().findAndRegisterModules();
 static final Map<String,Object> proof=new LinkedHashMap<>();
 static final List<Map<String,Object>> checks=new ArrayList<>();
 static final String password=UUID.randomUUID()+"Az1!";
 static JdbcTemplate jdbc;static String origin;
 static HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
 record Tenant(long id,long branch,long admin,long staff,long client,long service,long deposit,String slug,String email){}
 static List<Tenant> tenants=new ArrayList<>();
 static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
 static long insert(String sql,Object...args){var k=new GeneratedKeyHolder();jdbc.update(c->{var s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);for(int i=0;i<args.length;i++)s.setObject(i+1,args[i]);return s;},k);return Objects.requireNonNull(k.getKey()).longValue();}
 static long user(long tenant,long branch,String role,String mail,String hash){
  long id=insert("INSERT INTO usuario(empresa_id,correo,contrasena_hash,correo_verificado_en) VALUES(?,?,?,UTC_TIMESTAMP())",tenant,mail,hash);
  jdbc.update("INSERT INTO usuario_rol(usuario_id,rol_id,empresa_id) SELECT ?,id,? FROM rol WHERE codigo=?",id,tenant,role);
  jdbc.update("INSERT INTO usuario_rol_empresa(usuario_id,rol_empresa_id) SELECT ?,id FROM rol_empresa WHERE empresa_id=? AND codigo=?",id,tenant,role);
  if(role.equals("ADMIN"))jdbc.update("INSERT INTO usuario_interno_sucursal(usuario_id,sucursal_id) VALUES(?,?)",id,branch);
  return id;
 }
 static JsonNode call(String label,String method,String path,Object body,String token,String slug,int expected)throws Exception{
  var b=HttpRequest.newBuilder(URI.create(origin+path)).timeout(Duration.ofSeconds(25)).header("Accept","application/json");
  if(token!=null)b.header("Authorization","Bearer "+token);if(slug!=null)b.header("X-Tenant-Slug",slug);
  if(body!=null)b.header("Content-Type","application/json");
  b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
  var r=http.send(b.build(),HttpResponse.BodyHandlers.ofString());checks.add(Map.of("check",label,"status",r.statusCode(),"expected",expected));
  check(r.statusCode()==expected,label+" HTTP "+r.statusCode()+" expected "+expected);
  return r.body().isBlank()?JSON.nullNode():JSON.readTree(r.body());
 }
 static String login(Tenant t)throws Exception{return call("tenant login "+t.id,"POST","/api/v1/auth/app-login",Map.of("correo",t.email,"contrasena",password,"empresaId",t.id),null,null,200).path("sesion").path("tokenAcceso").asText();}
 static int jdbcCountSchema(Connection connection,String schema)throws Exception {
  try(var query=connection.prepareStatement("SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME=?")) {
   query.setString(1,schema);try(var rows=query.executeQuery()){rows.next();return rows.getInt(1);}
  }
 }
 public static void main(String[] args)throws Exception{
  if(org.springframework.util.ClassUtils.isPresent("org.springframework.boot.devtools.restart.Restarter",null))
   throw new IllegalStateException("DevTools rejected before database access");
  Set<String> allowed=Set.of("PATH","HOME","TMPDIR","LANG","LC_ALL","JAVA_HOME","__CF_USER_TEXT_ENCODING");
  for(String key:System.getenv().keySet())
   if(!allowed.contains(key) && !key.startsWith("FLUORA_REGRESION_MYSQL_") && !key.startsWith("FLUORA_014_") && !key.matches("JAVA_MAIN_CLASS_[0-9]+"))
    throw new IllegalStateException("Inherited provider/profile environment rejected before database access: "+key);
  proof.put("success",false);proof.put("http_checks",checks);proof.put("devtools_on_classpath",false);
  proof.put("environment_names",System.getenv().keySet().stream().sorted().toList());
  Map<String,String> hashes=new LinkedHashMap<>();
  for(String file:List.of("V143__alinear_columnas_varchar.sql","V144__alinear_calificacion_resena_integer.sql"))
   try(var stream=Ensayo014.class.getClassLoader().getResourceAsStream("db/migration/"+file)) {
    check(stream!=null,"Migration present on classpath");
    hashes.put(file,HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(stream.readAllBytes())));
   }
  proof.put("migration_sha256",hashes);
  String base=System.getenv("FLUORA_REGRESION_MYSQL_URL"),username=System.getenv("FLUORA_REGRESION_MYSQL_USER"),secret=System.getenv("FLUORA_REGRESION_MYSQL_PASSWORD");
  check(base!=null && base.matches("jdbc:mysql://(127\\.0\\.0\\.1|localhost):[0-9]+/fluora_test_[a-z0-9_]+(?:[?].*)?"),"Only local MySQL URLs are allowed");
  String db="fluora_test_014_"+UUID.randomUUID().toString().replace("-","");
  String url=MysqlAislado.urlHija(base,db),ref=db+"_ref";
  String collation=System.getenv("FLUORA_014_COLLATION");
  check(collation!=null && collation.matches("utf8mb4_[a-z0-9_]+"),"Production collation must be supplied explicitly");
  Path result=Path.of(System.getenv("FLUORA_014_RESULT"));boolean created=false,referenceCreated=false;Throwable primaryFailure=null;
  try(var parent=DriverManager.getConnection(base,username,secret)){
   try{
    MysqlAislado.comprobar(parent);
    try(var st=parent.createStatement()){st.execute("CREATE DATABASE `"+db+"` CHARACTER SET utf8mb4 COLLATE "+collation);created=true;}
    var ds=new DriverManagerDataSource(url,username,secret);jdbc=new JdbcTemplate(ds);
    try(var c=ds.getConnection()){MysqlAislado.comprobar(c);}
    EsquemaMigraciones.vacio(ds);
    var start=System.nanoTime();EsquemaMigraciones.migrar(ds,"58");
    long expected58=EsquemaMigraciones.versiones().stream().filter(v->Integer.parseInt(v)<=58).count();
    check(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE version IS NOT NULL AND success=1",Long.class)==expected58,"All classpath migrations through58 applied");
    proof.put("v1_to_58_seconds",(System.nanoTime()-start)/1e9);proof.put("collation",collation);
    String hash=new BCryptPasswordEncoder().encode(password);
    jdbc.update("UPDATE empresa SET nombre='Ensayo sintético 1',slug='ensayo-014-1' WHERE id=1");
    jdbc.update("UPDATE empresa_sitio_config SET slug='ensayo-014-1',nombre_comercial='Ensayo sintético 1',dominio_principal=NULL WHERE empresa_id=1");
    for(int i=1;i<=8;i++){
     long tenant=i==1?1:insert("INSERT INTO empresa(nombre,slug) VALUES(?,?)","Ensayo sintético "+i,"ensayo-014-"+i);
     String slug="ensayo-014-"+i;long branch=insert("INSERT INTO sucursal(empresa_id,nombre) VALUES(?,'Sucursal sintética')",tenant);
     if(i!=1){jdbc.update("INSERT INTO empresa_sitio_config(empresa_id,slug,nombre_comercial,publicado) VALUES(?,?,?,1)",tenant,slug,"Ensayo sintético "+i);jdbc.update("INSERT INTO rol_empresa(empresa_id,rol_base_id,codigo,nombre,activo,editable) SELECT ?,id,codigo,codigo,1,0 FROM rol",tenant);jdbc.update("INSERT INTO rol_empresa_permiso(rol_empresa_id,permiso_id) SELECT re.id,rp.permiso_id FROM rol_empresa re JOIN rol_permiso rp ON rp.rol_id=re.rol_base_id WHERE re.empresa_id=?",tenant);}
     else jdbc.update("UPDATE empresa_sitio_config SET publicado=1 WHERE empresa_id=1");
     String email="admin014-"+i+"@example.test";
     long admin=user(tenant,branch,"ADMIN",email,hash),staff=user(tenant,branch,"STAFF","staff014-"+i+"@example.test",hash),client=user(tenant,branch,"CLIENTE","client014-"+i+"@example.test",hash);
     jdbc.update("INSERT INTO prestador_servicio(usuario_id,sucursal_id,nombre_mostrar) VALUES(?,?,'Prestador sintético')",staff,branch);
     jdbc.update("INSERT INTO cliente(usuario_id,nombre_completo,telefono,acepta_whatsapp) VALUES(?,'Cliente sintético','5550000000',0)",client);
     long service=insert("INSERT INTO servicio(empresa_id,sucursal_id,nombre,slug,duracion_minutos,precio,moneda,visible_publico,requiere_anticipo) VALUES(?,?,'Servicio sintético','simple',30,100,'MXN',1,0)",tenant,branch);
     long deposit=insert("INSERT INTO servicio(empresa_id,sucursal_id,nombre,slug,duracion_minutos,precio,moneda,visible_publico,requiere_anticipo,anticipo_tipo,anticipo_valor) VALUES(?,?,'Servicio anticipo sintético','anticipo',30,100,'MXN',1,1,'PORCENTAJE',50)",tenant,branch);
     for(long s:List.of(service,deposit)){jdbc.update("INSERT INTO asignacion_servicio_prestador(prestador_id,servicio_id) VALUES(?,?)",staff,s);jdbc.update("INSERT INTO servicio_sucursal(empresa_id,servicio_id,sucursal_id,activo) VALUES(?,?,?,1)",tenant,s,branch);}
     for(int day=1;day<=7;day++)for(var subject:Map.of("SUCURSAL",branch,"PRESTADOR",staff).entrySet())jdbc.update("INSERT INTO regla_disponibilidad(empresa_id,tipo_sujeto,sujeto_id,dia_semana,hora_inicio,hora_fin,intervalo_minutos) VALUES(?,?,?,?, '09:00','19:00',30)",tenant,subject.getKey(),subject.getValue(),day);
     var when=LocalDate.now().minusDays(i*20).atTime(10,0);
     long appointment=insert("INSERT INTO cita(empresa_id,sucursal_id,servicio_id,prestador_id,cliente_id,estado,inicio,fin,precio,moneda,creada_por_usuario_id) VALUES(?,?,?,?,?,'CONFIRMADA',?,?,100,'MXN',?)",tenant,branch,service,staff,client,when,when.plusMinutes(30),admin);
     if(i%2==0)jdbc.update("INSERT INTO pago_cita(empresa_id,cita_id,monto,metodo_pago,registrado_por_usuario_id,registrado_en) VALUES(?,?,25,'EFECTIVO',?,UTC_TIMESTAMP())",tenant,appointment,admin);
     if(i==2)jdbc.update("UPDATE empresa_sitio_config SET dominio_principal='dominio-invalido:443' WHERE empresa_id=?",tenant);
     if(i==3)jdbc.update("UPDATE empresa_sitio_config SET dominio_principal='negocio.example.test' WHERE empresa_id=?",tenant);
     tenants.add(new Tenant(tenant,branch,admin,staff,client,service,deposit,slug,email));
    }
    check(jdbc.queryForObject("SELECT COUNT(*) FROM empresa",Integer.class)==8,"Exactly eight synthetic tenants");
    var appointmentsBefore=jdbc.queryForList("SELECT empresa_id,inicio,fin,precio FROM cita ORDER BY id");var paidBefore=jdbc.queryForObject("SELECT SUM(monto) FROM pago_cita",java.math.BigDecimal.class);
    proof.put("synthetic_tenants",8);proof.put("legacy_appointments",appointmentsBefore.size());
    var timings=new ArrayList<EsquemaMigraciones.MedicionV143>();
    long migrationStart=System.nanoTime();
    EsquemaMigraciones.migrar(EsquemaMigraciones.medirV143(ds,timings),null);
    check(timings.size()==2,"Both V143 statements timed");
    proof.put("v143_statements",timings);proof.put("upgrade_migration_seconds",(System.nanoTime()-migrationStart)/1e9);
    try(var st=parent.createStatement()){st.execute("CREATE DATABASE `"+ref+"` CHARACTER SET utf8mb4 COLLATE "+collation);referenceCreated=true;}
    var reference=new DriverManagerDataSource(MysqlAislado.urlHija(base,ref),username,secret);
    EsquemaMigraciones.vacio(reference);EsquemaMigraciones.hibernate(reference,"create");
    var inventory=EsquemaMigraciones.comparar(ds,reference);
    proof.put("schema_inventory",inventory);
    check(inventory.diferencias().isEmpty(),"All type differences before startup: "+inventory.diferencias());
    start=System.nanoTime();
    String[] options={"--spring.profiles.active=prod","--spring.datasource.url="+url,"--spring.datasource.username="+username,"--spring.datasource.password="+secret,"--server.address=127.0.0.1","--server.port=0","--spring.devtools.restart.enabled=false","--spring.jpa.hibernate.ddl-auto=validate","--aplicacion.tareas-programadas.habilitadas=false","--aplicacion.correo.habilitado=false","--aplicacion.correo.plataforma.habilitado=false","--aplicacion.whatsapp.habilitado=false","--aplicacion.cloudinary.habilitado=false","--aplicacion.pagos.mercado-pago.habilitado=false","--aplicacion.plataforma.stripe.habilitado=false","--aplicacion.envios.envia.plataforma-habilitada=false","--aplicacion.direcciones.google-maps.habilitado=false","--aplicacion.dominios.verificacion-automatica=false","--aplicacion.limite-solicitudes.habilitado=false","--aplicacion.invitaciones.url-aceptacion=http://127.0.0.1/invitacion","--aplicacion.recuperacion-contrasena.url=http://127.0.0.1/recuperacion","--aplicacion.jwt.secreto="+UUID.randomUUID()+UUID.randomUUID(),"--management.health.mail.enabled=false"};
    try(var context=new SpringApplicationBuilder(AgendaApplication.class).profiles("prod").run(options)){
     proof.put("startup_seconds",(System.nanoTime()-start)/1e9);
     origin="http://127.0.0.1:"+((ServletWebServerApplicationContext)context).getWebServer().getPort();
     check(jdbc.queryForObject("SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history",Integer.class)==Integer.parseInt(EsquemaMigraciones.versiones().getLast()),"Flyway latest classpath version");
     check(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success=0",Integer.class)==0,"No Flyway failures");
     check(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE version IS NOT NULL",Integer.class)==EsquemaMigraciones.versiones().size(),"All classpath migrations");
     check(appointmentsBefore.equals(jdbc.queryForList("SELECT empresa_id,inicio,fin,precio FROM cita ORDER BY id")),"Legacy appointments preserved");
     check(paidBefore.equals(jdbc.queryForObject("SELECT SUM(monto) FROM pago_cita",java.math.BigDecimal.class)),"Legacy payments preserved");
     check(jdbc.queryForObject("SELECT COUNT(*) FROM cita WHERE folio IS NULL OR folio=''",Integer.class)==0,"Legacy appointment folios populated");
     check(jdbc.queryForObject("SELECT COUNT(*) FROM empresa_sitio_config WHERE dominio_principal IS NOT NULL",Integer.class)==0,"Legacy domains removed by callback and migration");
     proof.put("migrations_after_v58",jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE CAST(version AS UNSIGNED)>58 AND success=1",Integer.class));proof.put("migration_target",EsquemaMigraciones.versiones().getLast());proof.put("flyway_failed",0);proof.put("legacy_values_preserved",true);
     check(jdbc.queryForObject("SELECT COUNT(*) FROM plataforma_suscripcion",Integer.class)==0,"No subscriptions seeded by migrations");
     check(jdbc.queryForObject("SELECT COUNT(*) FROM plataforma_plan WHERE activo=1",Integer.class)==0,"No active plan seeded");
     var health=call("health","GET","/actuator/health",null,null,null,200);check(health.path("status").asText().equals("UP"),"Health UP");
     for(String p:List.of("/api/v1/publico/sitio/indice","/api/v1/publico/plataforma/soporte","/api/v1/publico/plataforma/planes","/api/v1/publico/marketing/landings/indice"))call("public read before assignment","GET",p,null,null,null,200);
     List<String> tokens=new ArrayList<>();
     for(var t:tenants){String token=login(t);check(!token.isBlank(),"Real tenant login token");tokens.add(token);call("no subscription blocks Caja tenant "+t.id,"GET","/api/v1/caja/catalogo?sucursalId="+t.branch,null,token,null,402);var cap=call("no subscription capability tenant "+t.id,"GET","/api/v1/admin/suscripcion/capacidades",null,token,null,200);check(!cap.path("suscripcionVigente").asBoolean(),"No implicit subscription");}
     long actor=insert("INSERT INTO plataforma_usuario(correo,nombre,contrasena_hash) VALUES('plataforma014@example.test','Administrador sintético',?)",hash);
     check(jdbc.update("INSERT INTO plataforma_usuario_rol(usuario_id,rol_id) SELECT ?,id FROM plataforma_rol WHERE codigo='ADMINISTRADOR_PLATAFORMA'",actor)==1,"Catalog platform role exists");
     String platform=call("platform login","POST","/api/v1/plataforma/auth/iniciar-sesion",Map.of("correo","plataforma014@example.test","contrasena",password),null,null,200).path("tokenAcceso").asText();
     long assignmentStart=System.nanoTime();
     var modules=call("editor module catalog","GET","/api/v1/plataforma/suscripciones/modulos",null,platform,null,200);
     List<Object> included=new ArrayList<>();for(var m:modules)if(!m.has("activo")||m.path("activo").asBoolean())included.add(Map.of("moduloId",m.path("id").asLong()));check(!included.isEmpty(),"Catalog modules available");
     var planBody=new LinkedHashMap<String,Object>();planBody.put("codigo","ENSAYO014_"+UUID.randomUUID().toString().replace("-",""));planBody.put("nombre","Plan sintético de ensayo");planBody.put("moneda","MXN");planBody.put("precioMensual",100);planBody.put("activo",true);planBody.put("publico",false);planBody.put("diasPrueba",0);planBody.put("diasGracia",0);planBody.put("modulos",included);planBody.put("limites",List.of());
     long plan=call("create plan through editor API","POST","/api/v1/plataforma/suscripciones/planes",planBody,platform,null,200).path("id").asLong();check(plan>0,"Plan created dynamically");
     for(var t:tenants){call("manual assignment through editor API tenant "+t.id,"POST","/api/v1/plataforma/suscripciones",Map.of("empresaId",t.id,"planId",plan,"estado","ACTIVA","ciclo","MENSUAL","precioAcordado",100,"moneda","MXN","iniciaEn",LocalDateTime.now(Clock.systemUTC()).minusMinutes(1).toString(),"terminaEn",LocalDateTime.now(Clock.systemUTC()).plusDays(30).toString()),platform,null,200);}
     proof.put("automated_editor_api_assignment_seconds",(System.nanoTime()-assignmentStart)/1e9);
     for(int i=0;i<tenants.size();i++){var t=tenants.get(i);String token=tokens.get(i);var cap=call("subscription restored tenant "+t.id,"GET","/api/v1/admin/suscripcion/capacidades",null,token,null,200);check(cap.path("suscripcionVigente").asBoolean(),"Assigned subscription live");call("Caja available tenant "+t.id,"GET","/api/v1/caja/catalogo?sucursalId="+t.branch,null,token,null,200);var state=call("subscription screen state tenant "+t.id,"GET","/api/v1/admin/suscripcion",null,token,null,200);check(state.path("tieneSuscripcion").asBoolean(),"Subscription screen state");var checkout=call("checkout without tenant setup "+t.id,"GET","/api/v1/publico/empresas/"+t.slug+"/checkout/configuracion",null,null,null,200);check(!checkout.path("habilitado").asBoolean(),"No checkout without tenant connection/configuration/policy");}
     // Fixture configurada: observar el contrato real, sin atribuirlo al flag global.
     var configured=tenants.get(1);
     long provider=jdbc.queryForObject("SELECT id FROM proveedor_pago WHERE codigo='MERCADO_PAGO'",Long.class);
     String paymentEnvironment=context.getEnvironment().getProperty("aplicacion.pagos.mercado-pago.ambiente","PRODUCCION");
     paymentEnvironment="PRUEBA".equalsIgnoreCase(paymentEnvironment)?"PRUEBA":"PRODUCCION";
     jdbc.update("INSERT INTO empresa_proveedor_pago(empresa_id,proveedor_pago_id,estado,ambiente,llave_publica) VALUES(?,?,'CONECTADO',?,'TEST-sintetica-sin-red')",configured.id,provider,paymentEnvironment);
     jdbc.update("INSERT INTO empresa_checkout_config(empresa_id,proveedor_predeterminado_id,habilitado,aceptar_tarjeta,permitir_pago_productos,permitir_pago_servicios,moneda) VALUES(?,?,1,1,1,1,'MXN')",configured.id,provider);
     jdbc.update("INSERT INTO politica_empresa(empresa_id,version,modo_no_show,dias_aclaracion,contacto_reclamos_correo,plantilla_texto,texto_generado,texto_sha256,publicada_en,publicada_por_usuario_id) VALUES(?,1,'RETENER',7,'fixture@example.test','ANTICIPOS_V1','Política sintética de ensayo',SHA2('Política sintética de ensayo',256),UTC_TIMESTAMP(),?)",configured.id,configured.admin);
     var configuredCheckout=call("configured tenant with global MP flag disabled","GET","/api/v1/publico/empresas/"+configured.slug+"/checkout/configuracion",null,null,null,200);
     check(configuredCheckout.path("habilitado").isBoolean(),"Configured checkout exposes boolean capability");
     proof.put("configured_checkout_enabled_with_global_mp_disabled",configuredCheckout.path("habilitado").asBoolean());
     var one=tenants.getFirst();String token=tokens.getFirst();
     var at=ZonedDateTime.now(ZoneId.of("America/Mexico_City")).plusDays(3).withHour(12).withMinute(0).withSecond(0).withNano(0).toOffsetDateTime();
     var noDeposit=call("public reservation quote no deposit","POST","/api/v1/publico/citas/cotizar",Map.of("sucursalId",one.branch,"items",List.of(Map.of("servicioId",one.service,"inicio",at.toString()))),null,one.slug,200);
     var depositQuote=call("public reservation quote deposit unavailable","POST","/api/v1/publico/citas/cotizar",Map.of("sucursalId",one.branch,"items",List.of(Map.of("servicioId",one.deposit,"inicio",at.toString()))),null,one.slug,200);
     check(noDeposit.path("reservableEnLinea").asBoolean() && !noDeposit.path("requierePago").asBoolean(),"Free reservation contract");
     check(noDeposit.path("importeRequeridoTotal").decimalValue().signum()==0 && noDeposit.path("lineas").get(0).path("politicaCobro").asText().equals("SIN_COBRO"),"No-deposit amounts");
     String unavailable=com.techprotech.agenda.modulos.citas.aplicacion.AnticipoNoDisponibleException.MENSAJE;
     check(!depositQuote.path("reservableEnLinea").asBoolean() && !depositQuote.path("requierePago").asBoolean(),"Deposit unavailable contract");
     check(depositQuote.path("motivoNoReservable").asText().equals(unavailable),"Deposit unavailable explanation");
     var depositLine=depositQuote.path("lineas").get(0);
     check(depositLine.path("politicaCobro").asText().equals("ANTICIPO_PORCENTAJE") && depositLine.path("importeRequerido").decimalValue().compareTo(new java.math.BigDecimal("50"))==0 && depositLine.path("saldoEnNegocio").decimalValue().compareTo(new java.math.BigDecimal("50"))==0,"Deposit and remaining balance");
     var booking=new LinkedHashMap<String,Object>();booking.put("sucursalId",one.branch);booking.put("prestadorId",one.staff);booking.put("inicio",at.toString());booking.put("nombreCliente","Cliente sintético");booking.put("correoCliente","client014-1@example.test");booking.put("telefonoCliente","5550000000");booking.put("servicioId",one.deposit);
     var rejected=call("booking deposit rejected without payment availability","POST","/api/v1/publico/citas",booking,null,one.slug,409);
     check(rejected.path("codigo").asText().equals("ANTICIPO_NO_DISPONIBLE") && rejected.path("mensaje").asText().equals(unavailable),"Deposit create rejection explanation");
     booking.put("servicioId",one.service);
     var createdBooking=call("booking without deposit succeeds","POST","/api/v1/publico/citas",booking,null,one.slug,201);
     check(createdBooking.path("estado").asText().equals("PENDIENTE") && createdBooking.path("pagoExpiraEn").isNull(),"Unpaid free booking does not expire as pending payment");
     check(!createdBooking.path("requierePago").asBoolean(),"Free booking does not request payment");
     check(jdbc.queryForObject("SELECT COUNT(*) FROM cita WHERE id=? AND empresa_id=? AND estado='PENDIENTE' AND pago_expira_en IS NULL",Integer.class,createdBooking.path("id").asLong(),one.id)==1,"Free booking persisted without payment expiry");
     call("Stripe checkout disabled","POST","/api/v1/admin/suscripcion/checkout",Map.of("planId",plan,"ciclo","MENSUAL"),token,null,503);
     call("Stripe portal disabled","POST","/api/v1/admin/suscripcion/portal",Map.of(),token,null,503);
     var terminals=call("Point terminal list without configured devices","GET","/api/v1/caja/point/terminales?sucursalId="+one.branch,null,token,null,200);
     check(terminals.isArray() && terminals.isEmpty(),"Point list is empty without devices");
     check(Boolean.TRUE.equals(context.getEnvironment().getProperty("aplicacion.plataforma.stripe.conciliacion.habilitado",Boolean.class)),"Stripe reconciliation default true");
     var reconciled=tenants.getLast();
     check(jdbc.update("UPDATE plataforma_suscripcion SET stripe_suscripcion_id=?,estado='GRACIA',periodo_actual_hasta=DATE_SUB(UTC_TIMESTAMP(),INTERVAL 1 HOUR) WHERE empresa_id=?","sub_ensayo014_"+UUID.randomUUID().toString().replace("-",""),reconciled.id)==1,"Exactly one subscription made eligible");
     check(!context.getBean(com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.SuscripcionPlataformaRepositorio.class).findParaConciliar(LocalDateTime.now(Clock.systemUTC()),0L,org.springframework.data.domain.PageRequest.of(0,20)).isEmpty(),"Stripe reconciliation has an eligible fixture");
     var subscriptionsBefore=jdbc.queryForList("SELECT id,estado,periodo_actual_hasta,termina_en FROM plataforma_suscripcion ORDER BY id");
     context.getBean(ProcesadorConciliacionSuscripciones.class).procesar();
     check(subscriptionsBefore.equals(jdbc.queryForList("SELECT id,estado,periodo_actual_hasta,termina_en FROM plataforma_suscripcion ORDER BY id")),"Stripe disabled reconciliation preserves manual subscriptions");
     proof.put("stripe_reconciliation_default_true_handles_eligible_fixture",true);proof.put("payment_providers_disabled",true);proof.put("http_checks",checks);proof.put("success",true);
    }
   }catch(Throwable failure){
    primaryFailure=failure;proof.put("success",false);proof.put("failureClass",failure.getClass().getName());
    if(failure instanceof AssertionError)proof.put("assertion",failure.getMessage());
    throw failure;
   }finally{
    Exception cleanup=null;
    if(referenceCreated)try{MysqlAislado.comprobarNombre(ref);try(var st=parent.createStatement()){st.execute("DROP DATABASE `"+ref+"`");}if(jdbcCountSchema(parent,ref)!=0)throw new IllegalStateException("Reference schema not removed");proof.put("reference_database_removed",true);}catch(Exception e){cleanup=e;}
    if(created)try{MysqlAislado.comprobarNombre(db);try(var st=parent.createStatement()){st.execute("DROP DATABASE `"+db+"`");}if(jdbcCountSchema(parent,db)!=0)throw new IllegalStateException("Isolated schema not removed");proof.put("isolated_database_removed",true);}catch(Exception e){if(cleanup==null)cleanup=e;else cleanup.addSuppressed(e);}
    if(cleanup!=null){proof.put("success",false);proof.put("cleanupFailureClass",cleanup.getClass().getName());}
    Files.writeString(result,JSON.writeValueAsString(proof));
    if(cleanup!=null){if(primaryFailure!=null)primaryFailure.addSuppressed(cleanup);else throw cleanup;}
   }
  }
  System.out.println("014_REHEARSAL_SUCCESS "+result);
 }
}
