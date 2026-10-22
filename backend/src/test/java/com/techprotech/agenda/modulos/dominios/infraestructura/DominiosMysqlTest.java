package com.techprotech.agenda.modulos.dominios.infraestructura;
import com.techprotech.agenda.soporte.MysqlAislado;
import com.techprotech.agenda.modulos.dominios.aplicacion.*;
import com.techprotech.agenda.modulos.dominios.infraestructura.entidad.*;
import com.techprotech.agenda.modulos.dominios.infraestructura.repositorio.EmpresaDominioRepositorio;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.entidad.EmpresaEntidad;
import com.techprotech.agenda.modulos.autenticacion.infraestructura.repositorio.EmpresaRepositorio;
import com.techprotech.agenda.modulos.sitio.infraestructura.repositorio.EmpresaSitioConfigRepositorio;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioCapacidadesEmpresa;
import com.techprotech.agenda.modulos.plataforma.api.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.dao.support.PersistenceExceptionTranslationInterceptor;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.jdbc.core.JdbcTemplate;
import java.sql.*;import java.time.*;import java.util.*;import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;import static org.mockito.Mockito.*;
@EnabledIfEnvironmentVariable(named="FLUORA_REGRESION_MYSQL_URL",matches=".+")
class DominiosMysqlTest {
 @Test void migracionYArbitrajeDeDosPropietariosSinTomarElHostAjeno()throws Exception{
  String base=System.getenv("FLUORA_REGRESION_MYSQL_URL"),user=System.getenv("FLUORA_REGRESION_MYSQL_USER"),password=System.getenv("FLUORA_REGRESION_MYSQL_PASSWORD");
  String db="fluora_test_012_"+UUID.randomUUID().toString().replace("-","");String hija=MysqlAislado.urlHija(base,db);
  try(var origen=DriverManager.getConnection(base,user,password)){MysqlAislado.comprobar(origen);try(var st=origen.createStatement()){st.execute("CREATE DATABASE `"+db+"`");}
   try{
    var ds=new DriverManagerDataSource(hija,user,password);var jdbc=new JdbcTemplate(ds);
    try(var c=ds.getConnection();var st=c.createStatement()){
     MysqlAislado.comprobar(c);
     st.execute("CREATE TABLE empresa(id BIGINT PRIMARY KEY,nombre VARCHAR(150) NOT NULL,slug VARCHAR(100) NOT NULL,zona_horaria VARCHAR(60) NOT NULL,estado VARCHAR(20) NOT NULL)");st.execute("INSERT INTO empresa VALUES(1,'Fixture A','fixture-a','America/Mexico_City','ACTIVA'),(2,'Fixture B','fixture-b','America/Mexico_City','ACTIVA')");
     st.execute("CREATE TABLE empresa_sitio_config(empresa_id BIGINT PRIMARY KEY,dominio_principal VARCHAR(255) UNIQUE) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");st.execute("INSERT INTO empresa_sitio_config VALUES(1,'anterior-a.example.mx'),(2,'anterior-b.example.mx')");
     st.execute("INSERT INTO empresa VALUES(3,'Fixture C','fixture-c','UTC','ACTIVA'),(4,'Fixture D','fixture-d','UTC','ACTIVA'),(5,'Fixture E','fixture-e','UTC','ACTIVA'),(6,'Fixture F','fixture-f','UTC','ACTIVA')");st.execute("INSERT INTO empresa_sitio_config VALUES(3,'Marca.MX'),(4,'ñandú.mx'),(5,'x.mx.'),(6,'x.mx:443')");
     st.execute("CREATE TABLE plataforma_modulo(id BIGINT AUTO_INCREMENT PRIMARY KEY,codigo VARCHAR(80) UNIQUE,nombre VARCHAR(120),descripcion VARCHAR(255),orden INT)");
     st.execute("CREATE TABLE plataforma_limite(id BIGINT AUTO_INCREMENT PRIMARY KEY,codigo VARCHAR(80) UNIQUE,nombre VARCHAR(120),descripcion VARCHAR(255),unidad VARCHAR(30),orden INT)");
     st.execute("CREATE TABLE plataforma_permiso(id BIGINT AUTO_INCREMENT PRIMARY KEY,codigo VARCHAR(100) UNIQUE,nombre VARCHAR(140),descripcion VARCHAR(255))");
     st.execute("CREATE TABLE plataforma_rol(id BIGINT PRIMARY KEY,codigo VARCHAR(80))");st.execute("INSERT INTO plataforma_rol VALUES(1,'ADMINISTRADOR_PLATAFORMA')");st.execute("CREATE TABLE plataforma_rol_permiso(rol_id BIGINT,permiso_id BIGINT,PRIMARY KEY(rol_id,permiso_id))");

    }
    for (int id=7;id<=9;id++) jdbc.update("INSERT INTO empresa VALUES(?,?,?,'UTC','ACTIVA')",id,"Fixture "+id,"fixture-"+id);
    jdbc.update("INSERT INTO empresa_sitio_config VALUES(7,?),(8,?),(9,?)","ſ.mx","x.mx\n"," x.mx");
    new org.springframework.boot.test.context.runner.ApplicationContextRunner()
     .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.class))
     .withBean(javax.sql.DataSource.class,()->ds).withBean(PrepararLegadoDominiosFlyway.class)
     .withPropertyValues("spring.flyway.locations=filesystem:src/main/resources/db/migration","spring.flyway.baseline-on-migrate=true","spring.flyway.baseline-version=140","spring.flyway.target=142")
     .run(context->{assertThat(context).hasNotFailed();assertThat(context.getBean(org.flywaydb.core.Flyway.class).getConfiguration().getCallbacks()).contains(context.getBean(PrepararLegadoDominiosFlyway.class));});
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM empresa_dominio WHERE estado='PENDIENTE_VERIFICACION' AND LENGTH(token_verificacion)=43",Integer.class)).isEqualTo(3);
    assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT token_verificacion) FROM empresa_dominio",Integer.class)).isEqualTo(3);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM empresa_dominio WHERE hostname='marca.mx'",Integer.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM empresa_sitio_config WHERE dominio_principal IS NOT NULL",Integer.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM plataforma_rol_permiso",Integer.class)).isEqualTo(1);
    var registry=new StandardServiceRegistryBuilder().applySetting("hibernate.connection.datasource",ds).applySetting("hibernate.hbm2ddl.auto","validate").applySetting("hibernate.type.java_time_use_direct_jdbc",true).applySetting("hibernate.physical_naming_strategy","org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy").build();
    try(var factory=new MetadataSources(registry).addAnnotatedClass(EmpresaDominioEntidad.class).addAnnotatedClass(EmpresaEntidad.class).buildMetadata().buildSessionFactory()){
     var em=SharedEntityManagerCreator.createSharedEntityManager(factory);var repos=new JpaRepositoryFactory(em);
     var raw=repos.getRepository(EmpresaDominioRepositorio.class);var proxy=new ProxyFactory(raw);proxy.addAdvice(new PersistenceExceptionTranslationInterceptor(new HibernateJpaDialect()));var repo=(EmpresaDominioRepositorio)proxy.getProxy();
     var empresas=repos.getRepository(EmpresaRepositorio.class);var tx=new JpaTransactionManager(factory);tx.setDataSource(ds);tx.setJpaDialect(new HibernateJpaDialect());
     var sub=mock(com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.SuscripcionPlataformaRepositorio.class);var planes=mock(com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.PlanPlataformaRepositorio.class);var modulos=mock(com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.PlanModuloPlataformaRepositorio.class);var limites=mock(com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.PlanLimitePlataformaRepositorio.class);var secciones=mock(com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.SeccionAdminPlataformaRepositorio.class);var cota=new java.util.concurrent.atomic.AtomicLong(3);
     var plan=new com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.PlanPlataformaEntidad();plan.setId(4L);plan.setCodigo("FIXTURE_NEUTRO");plan.setNombre("Fixture");when(planes.findById(4L)).thenReturn(Optional.of(plan));when(sub.findByEmpresaIdAndEstadoInOrderByCreadaEnDesc(anyLong(),anyList())).thenAnswer(i->{var ss=new com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.SuscripcionPlataformaEntidad();ss.setId(1L);ss.setEmpresaId(i.getArgument(0));ss.setPlanId(4L);ss.setEstado("ACTIVA");return List.of(ss);});when(modulos.findCapacidadesByPlanId(4L)).thenReturn(List.of(new CapacidadModuloEmpresaResponse("DOMINIO_PROPIO","Dominio",null)));when(limites.findCapacidadesByPlanId(4L)).thenAnswer(i->List.of(new CapacidadLimiteEmpresaResponse("DOMINIOS_PROPIOS_MAX","Dominios","dominios",cota.get())));
     var cap=new ServicioCapacidadesEmpresa(sub,planes,modulos,limites,secciones,Clock.systemUTC());
     var consulta=new ServicioConsultaDominios(repo,cap,new ValidadorHostname("https://platform.example","https://console.example"));var dns=mock(ResolverTxtDominio.class);var barrier=new CountDownLatch(2);
     when(dns.consultar("_fluora.race.example.mx")).thenAnswer(i->{assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();barrier.countDown();assertThat(barrier.await(10,TimeUnit.SECONDS)).isTrue();return jdbc.queryForList("SELECT CONCAT('fluora-verificacion=',token_verificacion) FROM empresa_dominio WHERE hostname='race.example.mx'",String.class);});
     var servicio=new ServicioDominios(repo,empresas,mock(EmpresaSitioConfigRepositorio.class),cap,consulta,mock(ServicioAuditoriaDominios.class),new ValidadorHostname("https://platform.example","https://console.example"),dns,mock(ComprobadorHostingDominio.class),new ObjectMapper().findAndRegisterModules(),Clock.systemUTC(),tx,60,7,"fixture-clientes");
     var a=servicio.solicitar(1L,10L,"race.example.mx");var b=servicio.solicitar(2L,20L,"race.example.mx");
     var workers=Executors.newFixedThreadPool(2);
     try{var x=workers.submit(()->servicio.verificar(1L,10L,a.id(),a.version()));var y=workers.submit(()->servicio.verificar(2L,20L,b.id(),b.version()));var resultados=List.of(x.get(20,TimeUnit.SECONDS),y.get(20,TimeUnit.SECONDS));assertThat(resultados.stream().map(r->r.estado())).containsExactlyInAnyOrder("VERIFICADO","PENDIENTE_VERIFICACION");}finally{workers.shutdownNow();}
     assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM empresa_dominio WHERE hostname_reservado='race.example.mx'",Integer.class)).isEqualTo(1);
     assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM empresa_dominio WHERE hostname='race.example.mx' AND ultimo_error='HOST_EN_USO'",Integer.class)).isEqualTo(1);
     assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM empresa_dominio WHERE chequeo_en_curso=TRUE",Integer.class)).isZero();
     var requests=Executors.newFixedThreadPool(2);
     try{
      var start=new CountDownLatch(1);
      Callable<Integer> first=()->{start.await();return request(servicio,1L,"extra-a.example.mx");};Callable<Integer> second=()->{start.await();return request(servicio,1L,"extra-b.example.mx");};
      var one=requests.submit(first);var two=requests.submit(second);start.countDown();assertThat(List.of(one.get(20,TimeUnit.SECONDS),two.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,402);
      assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM empresa_dominio WHERE empresa_id=1 AND estado<>'RETIRADO'",Integer.class)).isEqualTo(3);
      cota.set(20);var sameStart=new CountDownLatch(1);Callable<Integer> same=()->{sameStart.await();return request(servicio,2L,"same.example.mx");};one=requests.submit(same);two=requests.submit(same);sameStart.countDown();assertThat(List.of(one.get(20,TimeUnit.SECONDS),two.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
      assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM empresa_dominio WHERE hostname='same.example.mx' AND estado<>'RETIRADO'",Integer.class)).isEqualTo(1);
     }finally{requests.shutdownNow();}

    }finally{StandardServiceRegistryBuilder.destroy(registry);}
   }finally{try(var st=origen.createStatement()){st.execute("DROP DATABASE `"+db+"`");}}
  }
 }
 @Test void v142RetiraLegadoInvalidoCuandoV141YaEstabaAplicada()throws Exception{
  String base=System.getenv("FLUORA_REGRESION_MYSQL_URL"),user=System.getenv("FLUORA_REGRESION_MYSQL_USER"),password=System.getenv("FLUORA_REGRESION_MYSQL_PASSWORD");
  String db="fluora_test_012_"+UUID.randomUUID().toString().replace("-","");String hija=MysqlAislado.urlHija(base,db);
  try(var origen=DriverManager.getConnection(base,user,password)){
   MysqlAislado.comprobar(origen);try(var st=origen.createStatement()){st.execute("CREATE DATABASE `"+db+"`");}
   try{var ds=new DriverManagerDataSource(hija,user,password);var jdbc=new JdbcTemplate(ds);
    try(var c=ds.getConnection();var st=c.createStatement()){
     MysqlAislado.comprobar(c);
     st.execute("CREATE TABLE empresa(id BIGINT PRIMARY KEY,nombre VARCHAR(150) NOT NULL,slug VARCHAR(100) NOT NULL,zona_horaria VARCHAR(60) NOT NULL,estado VARCHAR(20) NOT NULL)");st.execute("INSERT INTO empresa VALUES(1,'Fixture A','fixture-a','America/Mexico_City','ACTIVA'),(2,'Fixture B','fixture-b','America/Mexico_City','ACTIVA')");
     st.execute("CREATE TABLE empresa_sitio_config(empresa_id BIGINT PRIMARY KEY,dominio_principal VARCHAR(255) UNIQUE) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");st.execute("INSERT INTO empresa_sitio_config VALUES(1,'anterior-a.example.mx'),(2,'anterior-b.example.mx')");
     st.execute("INSERT INTO empresa VALUES(3,'Fixture C','fixture-c','UTC','ACTIVA'),(4,'Fixture D','fixture-d','UTC','ACTIVA'),(5,'Fixture E','fixture-e','UTC','ACTIVA'),(6,'Fixture F','fixture-f','UTC','ACTIVA')");st.execute("INSERT INTO empresa_sitio_config VALUES(3,'Marca.MX'),(4,'ñandú.mx'),(5,'x.mx.'),(6,'x.mx:443')");
     st.execute("CREATE TABLE plataforma_modulo(id BIGINT AUTO_INCREMENT PRIMARY KEY,codigo VARCHAR(80) UNIQUE,nombre VARCHAR(120),descripcion VARCHAR(255),orden INT)");
     st.execute("CREATE TABLE plataforma_limite(id BIGINT AUTO_INCREMENT PRIMARY KEY,codigo VARCHAR(80) UNIQUE,nombre VARCHAR(120),descripcion VARCHAR(255),unidad VARCHAR(30),orden INT)");
     st.execute("CREATE TABLE plataforma_permiso(id BIGINT AUTO_INCREMENT PRIMARY KEY,codigo VARCHAR(100) UNIQUE,nombre VARCHAR(140),descripcion VARCHAR(255))");
     st.execute("CREATE TABLE plataforma_rol(id BIGINT PRIMARY KEY,codigo VARCHAR(80))");st.execute("INSERT INTO plataforma_rol VALUES(1,'ADMINISTRADOR_PLATAFORMA')");st.execute("CREATE TABLE plataforma_rol_permiso(rol_id BIGINT,permiso_id BIGINT,PRIMARY KEY(rol_id,permiso_id))");

    }

    jdbc.update("UPDATE empresa_sitio_config SET dominio_principal=NULL WHERE empresa_id=4");
    org.flywaydb.core.Flyway.configure().dataSource(ds).locations("filesystem:src/main/resources/db/migration").baselineOnMigrate(true).baselineVersion("140").target("141").load().migrate();
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM empresa_dominio WHERE estado='PENDIENTE_VERIFICACION'",Integer.class)).isEqualTo(5);
    org.flywaydb.core.Flyway.configure().dataSource(ds).locations("filesystem:src/main/resources/db/migration").target("142").load().migrate();
    assertThat(jdbc.queryForList("SELECT hostname FROM empresa_dominio WHERE estado='RETIRADO' AND ultimo_error='DOMINIO_LEGADO_INVALIDO'",String.class)).containsExactlyInAnyOrder("x.mx.","x.mx:443");
   }finally{try(var st=origen.createStatement()){st.execute("DROP DATABASE `"+db+"`");}}
  }
 }
 private int request(ServicioDominios servicio,Long empresa,String hostname){try{servicio.solicitar(empresa,10L,hostname);return 200;}catch(org.springframework.web.server.ResponseStatusException ex){return ex.getStatusCode().value();}}
}
