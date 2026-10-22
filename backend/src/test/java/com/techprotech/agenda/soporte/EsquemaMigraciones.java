package com.techprotech.agenda.soporte;

import com.techprotech.agenda.AgendaApplication;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Callback;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** Solo pruebas: compara metadatos de todas las entidades contra Flyway, nunca filas del negocio. */
public final class EsquemaMigraciones {
    private EsquemaMigraciones() {}
    public record Columna(String tabla, String nombre, String tipo, String tipoCompleto,
                          String nulable, Long longitud, String charset, String collation,
                          Long precision, Long escala, Long precisionTemporal, String defecto, String extra) {}
    public record Diferencia(String columna, Columna flyway, Columna hibernate) {}
    public record Informe(int entidades, int columnasMapeadas, List<Diferencia> diferencias,
                          List<String> diferenciasInformativas) {}
    public record MedicionV143(int paso, String tabla, long filas, double segundos, boolean completado) {}

    /** Cronometra solo los dos EXECUTE de V143; no modifica ni registra otras sentencias. */
    public static DataSource medirV143(DataSource source, List<MedicionV143> measurements) {
        return new org.springframework.jdbc.datasource.DelegatingDataSource(source) {
            @Override public java.sql.Connection getConnection() throws java.sql.SQLException {
                return instrumentar(super.getConnection(), measurements);
            }
            @Override public java.sql.Connection getConnection(String user,String password) throws java.sql.SQLException {
                return instrumentar(super.getConnection(user,password), measurements);
            }
        };
    }
    private static java.sql.Connection instrumentar(java.sql.Connection connection,List<MedicionV143> measurements) {
        return (java.sql.Connection)java.lang.reflect.Proxy.newProxyInstance(EsquemaMigraciones.class.getClassLoader(),
                new Class<?>[]{java.sql.Connection.class},(proxy,method,args)->{
                    Object result=invocar(connection,method,args);
                    if (result instanceof java.sql.Statement statement) {
                        Class<?> type=result instanceof java.sql.CallableStatement?java.sql.CallableStatement.class
                                :result instanceof java.sql.PreparedStatement?java.sql.PreparedStatement.class:java.sql.Statement.class;
                        return java.lang.reflect.Proxy.newProxyInstance(EsquemaMigraciones.class.getClassLoader(),new Class<?>[]{type},(p,m,a)->{
                            boolean measure=m.getName().startsWith("execute") && a!=null && a.length>0 && a[0] instanceof String sql
                                    && sql.strip().matches("(?is)EXECUTE\\s+v143_stmt\\s*;?");
                            if (!measure) return invocar(statement,m,a);
                            String table=measurements.isEmpty()?"checkout_orden":"sucursal";
                            long rows;
                            try(var count=connection.createStatement();var rowCount=count.executeQuery("SELECT COUNT(*) FROM "+table)) {
                                rowCount.next();rows=rowCount.getLong(1);
                            }
                            long start=System.nanoTime();boolean complete=false;
                            try {Object value=invocar(statement,m,a);complete=true;return value;}
                            finally { measurements.add(new MedicionV143(measurements.size()+1,table,rows,(System.nanoTime()-start)/1e9,complete)); }
                        });
                    }
                    return result;
                });
    }
    private static Object invocar(Object target,java.lang.reflect.Method method,Object[] args) throws Throwable {
        try {return method.invoke(target,args);} catch(java.lang.reflect.InvocationTargetException e) {throw e.getCause();}
    }

    public static List<Class<?>> entidades() {
        var classes = scan(new AnnotationTypeFilter(Entity.class));
        if (classes.size() < 100) throw new IllegalStateException("Exploración incompleta de entidades: " + classes.size());
        return classes;
    }

    private static List<Class<?>> scan(org.springframework.core.type.filter.TypeFilter filter) {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(filter);
        return scanner.findCandidateComponents(AgendaApplication.class.getPackageName()).stream()
                .<Class<?>>map(bean -> { try { return Class.forName(bean.getBeanClassName()); }
                    catch (ClassNotFoundException e) { throw new IllegalStateException(e); } })
                .sorted(Comparator.comparing(Class::getName)).toList();
    }

    /** Boot descubre y registra los Callback reales del paquete de aplicación; sin una copia de sus reglas. */
    public static void migrar(DataSource ds, String target) {
        var callbacks = scan(new AssignableTypeFilter(Callback.class)).stream()
                .filter(type -> org.springframework.core.annotation.AnnotatedElementUtils.hasAnnotation(type,
                        org.springframework.stereotype.Component.class)).toList();
        if (callbacks.isEmpty()) throw new IllegalStateException("No se descubrieron callbacks Flyway");
        var runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class))
                .withBean(DataSource.class, () -> ds)
                .withUserConfiguration(callbacks.toArray(Class<?>[]::new))
                .withPropertyValues("spring.flyway.locations=classpath:db/migration");
        if (target != null) runner = runner.withPropertyValues("spring.flyway.target=" + target);
        var failure = new AtomicReference<Throwable>();
        runner.run(context -> {
            if (context.getStartupFailure() != null) { failure.set(context.getStartupFailure()); return; }
            var registered = Arrays.stream(context.getBean(Flyway.class).getConfiguration().getCallbacks())
                    .map(Object::getClass).toList();
            if (!registered.containsAll(callbacks)) throw new IllegalStateException("Faltan callbacks de producción");
        });
        if (failure.get() != null) throw new IllegalStateException("Flyway no arrancó", failure.get());
    }

    public static List<String> versiones() throws Exception {
        return Arrays.stream(new PathMatchingResourcePatternResolver().getResources("classpath*:db/migration/V*.sql"))
                .map(r -> {
                    String version = Objects.requireNonNull(r.getFilename()).split("__", 2)[0].substring(1);
                    if (!version.matches("[0-9]+")) throw new IllegalStateException("Versión Flyway no entera: " + version);
                    return version;
                })
                .sorted(Comparator.comparingInt(Integer::parseInt)).toList();
    }

    public static void vacio(DataSource ds) throws Exception {
        try (var c = ds.getConnection()) {
            MysqlAislado.comprobar(c);
            try (var s = c.prepareStatement("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=?")) {
                s.setString(1, c.getCatalog());
                try (var r = s.executeQuery()) {
                    if (!r.next() || r.getLong(1) != 0) throw new IllegalStateException("El esquema de pruebas debe estar vacío");
                }
            }
        }
    }

    public static void hibernate(DataSource ds, String action) throws Exception {
        if (!Set.of("create", "validate").contains(action)) throw new IllegalArgumentException("Solo create de referencia o validate");
        try (var c = ds.getConnection()) { MysqlAislado.comprobar(c); }
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.datasource", ds)
                // create-only evita el drop inicial de claves de tablas que aún no existen.
                .applySetting("hibernate.hbm2ddl.auto", action.equals("create") ? "create-only" : action)
                .applySetting("hibernate.hbm2ddl.halt_on_error", true)
                .applySetting("hibernate.type.java_time_use_direct_jdbc", true)
                .applySetting("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
                .applySetting("hibernate.implicit_naming_strategy", "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy")
                .build();
        try {
            var sources = new MetadataSources(registry);
            var entities = entidades();
            entities.forEach(sources::addAnnotatedClass);
            scan(new AnnotationTypeFilter(Converter.class)).forEach(sources::addAnnotatedClass);
            try (var factory = sources.buildMetadata().buildSessionFactory()) {
                if (factory.getMetamodel().getEntities().size() != entities.size())
                    throw new IllegalStateException("Metamodelo incompleto");
            }
        } finally { StandardServiceRegistryBuilder.destroy(registry); }
    }

    public static SortedMap<String, Columna> columnas(DataSource ds) throws Exception {
        try (var c = ds.getConnection()) {
            MysqlAislado.comprobar(c);
            var result = new TreeMap<String, Columna>();
            try (var s = c.prepareStatement("SELECT TABLE_NAME,COLUMN_NAME,DATA_TYPE,COLUMN_TYPE,IS_NULLABLE,CHARACTER_MAXIMUM_LENGTH,CHARACTER_SET_NAME,COLLATION_NAME,NUMERIC_PRECISION,NUMERIC_SCALE,DATETIME_PRECISION,COLUMN_DEFAULT,EXTRA FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=? ORDER BY TABLE_NAME,ORDINAL_POSITION")) {
                s.setString(1, c.getCatalog());
                try (var r = s.executeQuery()) { while (r.next()) {
                    var column = new Columna(r.getString(1), r.getString(2), r.getString(3), r.getString(4),
                            r.getString(5), r.getObject(6, Long.class), r.getString(7), r.getString(8),
                            r.getObject(9,Long.class),r.getObject(10,Long.class),r.getObject(11,Long.class),r.getString(12),r.getString(13));
                    result.put(column.tabla() + "." + column.nombre(), column);
                } }
            }
            return result;
        }
    }

    // Lista explícita de equivalencias de almacenamiento aceptadas por Hibernate/MySQL.
    // CHAR nunca pertenece a la familia VARCHAR: se debe detectar el defecto de V88/V119.
    public static boolean equivalentes(Columna a, Columna b) {
        if (a == null || b == null) return false;
        if (a.tipo().equals(b.tipo())) return true;
        var text = Set.of("varchar", "tinytext", "text", "mediumtext", "longtext", "enum");
        if (text.contains(a.tipo()) && text.contains(b.tipo())) return true;
        if (Set.of("datetime", "timestamp").contains(a.tipo()) && Set.of("datetime", "timestamp").contains(b.tipo())) return true;
        return (a.tipo().equals("bit") && b.tipoCompleto().equals("tinyint(1)"))
                || (b.tipo().equals("bit") && a.tipoCompleto().equals("tinyint(1)"));
    }

    public static Informe comparar(DataSource migrated, DataSource reference) throws Exception {
        var actual = columnas(migrated); var expected = columnas(reference);
        if (expected.size() < 500) throw new IllegalStateException("Referencia de columnas incompleta");
        var differences = new ArrayList<Diferencia>(); var informational = new ArrayList<String>();
        for (var entry : expected.entrySet()) {
            var a = actual.get(entry.getKey()); var b = entry.getValue();
            if (!equivalentes(a, b)) differences.add(new Diferencia(entry.getKey(), a, b));
            if (a != null && (!a.nulable().equals(b.nulable()) || !Objects.equals(a.longitud(), b.longitud())))
                informational.add(entry.getKey() + ": Flyway nullable=" + a.nulable() + ", length=" + a.longitud()
                        + "; Hibernate nullable=" + b.nulable() + ", length=" + b.longitud());
            if (a != null) {
                if (!a.tipo().equals(b.tipo()) && equivalentes(a,b))
                    informational.add(entry.getKey()+": equivalencia aceptada de almacenamiento Flyway="+a.tipo()+"; Hibernate="+b.tipo());
                if (!Objects.equals(a.precision(),b.precision()) || !Objects.equals(a.escala(),b.escala()))
                    informational.add(entry.getKey()+": precisión/escala Flyway="+a.precision()+"/"+a.escala()+"; Hibernate="+b.precision()+"/"+b.escala());
                if (!Objects.equals(a.precisionTemporal(),b.precisionTemporal()))
                    informational.add(entry.getKey()+": precisión temporal Flyway="+a.precisionTemporal()+"; Hibernate="+b.precisionTemporal());
                if (!Objects.equals(a.charset(),b.charset()) || !Objects.equals(a.collation(),b.collation()))
                    informational.add(entry.getKey()+": charset/collation Flyway="+a.charset()+"/"+a.collation()+"; Hibernate="+b.charset()+"/"+b.collation());
                var integers=List.of("tinyint","smallint","mediumint","int","bigint");
                if (integers.contains(a.tipo()) && integers.contains(b.tipo()) && integers.indexOf(a.tipo())<integers.indexOf(b.tipo()))
                    informational.add(entry.getKey()+": entero Flyway más estrecho ("+a.tipo()+" < "+b.tipo()+")");
            }
        }
        var mappedTables=expected.values().stream().map(Columna::tabla).collect(java.util.stream.Collectors.toSet());
        for (var entry:actual.entrySet()) {
            var column=entry.getValue();
            if (mappedTables.contains(column.tabla()) && !expected.containsKey(entry.getKey())
                    && column.nulable().equals("NO") && column.defecto()==null
                    && !column.extra().toLowerCase(Locale.ROOT).matches(".*(auto_increment|generated).*"))
                informational.add(entry.getKey()+": columna solo Flyway NOT NULL sin DEFAULT");
        }
        return new Informe(entidades().size(), expected.size(), List.copyOf(differences), List.copyOf(informational));
    }
}
