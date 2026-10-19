package com.techprotech.agenda.modulos.marketing.infraestructura;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techprotech.agenda.modulos.marketing.aplicacion.*;
import com.techprotech.agenda.modulos.marketing.infraestructura.entidad.ContactoPlataformaEntidad;
import com.techprotech.agenda.modulos.marketing.infraestructura.repositorio.ContactoPlataformaRepositorio;
import com.techprotech.agenda.modulos.plataforma.aplicacion.ServicioAuditoriaPlataforma;
import com.techprotech.agenda.soporte.MysqlAislado;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.dao.support.PersistenceExceptionTranslationInterceptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.web.server.ResponseStatusException;
import java.sql.*;import java.time.*;import java.util.*;import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named="FLUORA_REGRESION_MYSQL_URL",matches=".+")
class SoportePlataformaMysqlTest {
 @Test void migracionValidateYDosCambiosSimultaneosSoloUnaAuditoria() throws Exception {
  String base=System.getenv("FLUORA_REGRESION_MYSQL_URL"),user=System.getenv("FLUORA_REGRESION_MYSQL_USER"),password=System.getenv("FLUORA_REGRESION_MYSQL_PASSWORD");
  String db="fluora_test_008_"+UUID.randomUUID().toString().replace("-","");String hija=MysqlAislado.urlHija(base,db);
  try(var origen=DriverManager.getConnection(base,user,password)){
   MysqlAislado.comprobar(origen);
   try(var st=origen.createStatement()){st.execute("CREATE DATABASE `"+db+"`");}
   try{
    try(var c=DriverManager.getConnection(hija,user,password);var st=c.createStatement()){
     MysqlAislado.comprobar(c);
     st.execute("CREATE TABLE plataforma_usuario(id BIGINT PRIMARY KEY)");st.execute("INSERT INTO plataforma_usuario VALUES(12)");
     st.execute("CREATE TABLE plataforma_auditoria(id BIGINT AUTO_INCREMENT PRIMARY KEY,usuario_id BIGINT NOT NULL,accion VARCHAR(100) NOT NULL,recurso VARCHAR(100) NOT NULL,recurso_id VARCHAR(120),detalle JSON,creada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),FOREIGN KEY(usuario_id) REFERENCES plataforma_usuario(id))");
     ScriptUtils.executeSqlScript(c,new org.springframework.core.io.FileSystemResource("src/main/resources/db/migration/V140__contacto_soporte_plataforma.sql"));
     try(var rs=st.executeQuery("SELECT correo_legal,dias FROM plataforma_contacto")){assertThat(rs.next()).isTrue();assertThat(rs.getString(1)).contains("@");assertThat(rs.getString(2)).contains("1");}
     assertThatThrownBy(()->st.execute("INSERT INTO plataforma_contacto SELECT 2,correo,correo_legal,telefono,dias,hora_inicio,hora_fin,zona_horaria,visible,actualizado_en,actualizado_por,version FROM plataforma_contacto")).isInstanceOf(SQLException.class);
     assertThatThrownBy(()->st.execute("UPDATE plataforma_contacto SET correo_legal=NULL")).isInstanceOf(SQLException.class);
    }
    var ds=new DriverManagerDataSource(hija,user,password);
    var registry=new StandardServiceRegistryBuilder().applySetting("hibernate.connection.datasource",ds)
      .applySetting("hibernate.hbm2ddl.auto","validate").applySetting("hibernate.type.java_time_use_direct_jdbc",true)
      .applySetting("hibernate.physical_naming_strategy","org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy").build();
    try(var factory=new MetadataSources(registry).addAnnotatedClass(ContactoPlataformaEntidad.class).addAnnotatedClass(com.techprotech.agenda.modulos.plataforma.infraestructura.entidad.AuditoriaPlataformaEntidad.class).buildMetadata().buildSessionFactory()){
     var em=SharedEntityManagerCreator.createSharedEntityManager(factory);
     var repository=new JpaRepositoryFactory(em).getRepository(ContactoPlataformaRepositorio.class);
     var reads=new CountDownLatch(2);
     var translation=new ProxyFactory(repository);
     translation.addAdvice(new PersistenceExceptionTranslationInterceptor(new HibernateJpaDialect()));
     translation.addAdvice((org.aopalliance.intercept.MethodInterceptor) invocation -> {
      var value=invocation.proceed();
      if(invocation.getMethod().getName().equals("findById")){
       reads.countDown();assertThat(reads.await(10,TimeUnit.SECONDS)).isTrue();
      }
      return value;
     });
     var repo=(ContactoPlataformaRepositorio)translation.getProxy();
     var jdbc=new JdbcTemplate(ds);
     var auditRepo=new JpaRepositoryFactory(em).getRepository(com.techprotech.agenda.modulos.plataforma.infraestructura.repositorio.AuditoriaPlataformaRepositorio.class);
     var auditoria=new ServicioAuditoriaPlataforma(auditRepo,new ObjectMapper().findAndRegisterModules());
     var target=new ServicioSoportePlataformaImpl(repo,new ObjectMapper(),auditoria,Clock.systemUTC());
     var tx=new JpaTransactionManager(factory);tx.setDataSource(ds);tx.setJpaDialect(new HibernateJpaDialect());
     var proxy=new ProxyFactory(target);proxy.setProxyTargetClass(true);proxy.addAdvice(new TransactionInterceptor(tx,new AnnotationTransactionAttributeSource()));
     var service=(ServicioSoportePlataforma)proxy.getProxy();
     var executor=Executors.newFixedThreadPool(2);
     try{
      Callable<Integer> one=()->save(service,false),two=()->save(service,true);
      var a=executor.submit(one);var b=executor.submit(two);
      assertThat(List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
      assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM plataforma_auditoria",Integer.class)).isEqualTo(1);
      assertThat(jdbc.queryForObject("SELECT version FROM plataforma_contacto",Long.class)).isEqualTo(1L);
     }finally{executor.shutdownNow();}
    }finally{StandardServiceRegistryBuilder.destroy(registry);}
   }finally{try(var st=origen.createStatement()){st.execute("DROP DATABASE `"+db+"`");}}
  }
 }
 private int save(ServicioSoportePlataforma service,boolean legal){
  var v=ServicioSoportePlataformaTest.valida(0L);
  var s=new com.techprotech.agenda.modulos.marketing.api.dto.SoportePlataformaSolicitud(0L,legal?v.correo():"other@example.test",legal?"other-legal@example.test":v.correoLegal(),v.telefono(),v.dias(),v.horaInicio(),v.horaFin(),v.zonaHoraria(),true);
  try{service.guardar(s,12L);return 200;}catch(ResponseStatusException ex){return ex.getStatusCode().value();}
 }
}
