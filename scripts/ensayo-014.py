#!/usr/bin/env python3
"""Ensayo local V58→classpath actual; entorno limpio, sin .env ni proveedores reales."""
import argparse, hashlib, json, os, pathlib, subprocess, sys, tempfile
if sys.flags.optimize:
    raise SystemExit('No se admite Python optimizado: las guardas deben ejecutarse')
ROOT = pathlib.Path(__file__).resolve().parents[1]
p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--mysql-env', type=pathlib.Path)
p.add_argument('--output', type=pathlib.Path)
p.add_argument('--collation')
p.add_argument('--check-guards', action='store_true')
a = p.parse_args()
if not a.check_guards and not (a.mysql_env and a.output and a.collation):
    p.error('Especifica --mysql-env, --output y --collation o --check-guards')
# Equivalente a env -i: nunca heredar variables de perfiles, credenciales o proveedores.
env = {k: os.environ[k] for k in ['PATH','HOME','TMPDIR','LANG','LC_ALL','JAVA_HOME'] if k in os.environ}
java_home = pathlib.Path(env.get('JAVA_HOME','/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home'))
env['JAVA_HOME'] = str(java_home)
with tempfile.TemporaryDirectory(prefix='fluora-014-launch-') as temporary:
    work = pathlib.Path(temporary)
    subprocess.run(['mvn','-q','test-compile'],cwd=ROOT/'backend',env=env,check=True)
    cpfile = work/'classpath.txt'
    subprocess.run(['mvn','-q','dependency:build-classpath','-Dmdep.includeScope=test','-Dmdep.outputFile='+str(cpfile)],cwd=ROOT/'backend',env=env,check=True)
    all_dependencies = cpfile.read_text().strip().split(os.pathsep)
    dependencies = [entry for entry in all_dependencies if 'spring-boot-devtools' not in pathlib.Path(entry).name]
    devtools = [entry for entry in all_dependencies if 'spring-boot-devtools' in pathlib.Path(entry).name]
    cp = os.pathsep.join([str(ROOT/'backend/target/test-classes'),str(ROOT/'backend/target/classes'),*dependencies])
    subprocess.run([str(java_home/'bin/javac'),'-encoding','UTF-8','-cp',cp,'-d',str(work),str(ROOT/'docs/agentes/evidencias/014-ensayo/Ensayo014.java')],env=env,check=True)
    cmd = [str(java_home/'bin/java'),'-Dspring.devtools.restart.enabled=false','-Duser.timezone=UTC','-Dhttp.proxyHost=127.0.0.1','-Dhttp.proxyPort=9','-Dhttps.proxyHost=127.0.0.1','-Dhttps.proxyPort=9','-Dhttp.nonProxyHosts=localhost|127.*','-cp',str(work)+os.pathsep+cp,'Ensayo014']
    if a.check_guards:
        for extra, expected in [({'SPRING_PROFILES_ACTIVE':'dev'},'Inherited provider/profile environment rejected before database access'),({'FLUORA_REGRESION_MYSQL_URL':'jdbc:mysql://remote.invalid:3306/fluora_test_guard'},'Only local MySQL URLs are allowed')]:
            r = subprocess.run(cmd,env={**env,**extra},text=True,capture_output=True,timeout=30,cwd=ROOT/'backend')
            assert r.returncode != 0 and expected in r.stderr, 'La guarda debe rechazar antes de acceder a MySQL: '+r.stderr
        assert len(devtools) == 1, 'Se necesita la dependencia real para la negativa DevTools'
        with_devtools = cmd.copy(); with_devtools[with_devtools.index('-cp')+1] += os.pathsep+devtools[0]
        r = subprocess.run(with_devtools,env=env,text=True,capture_output=True,timeout=30,cwd=ROOT/'backend')
        assert r.returncode != 0 and 'DevTools rejected before database access' in r.stderr
        print('014 guardas: 3/3; sin conexión a MySQL')
    else:
        assert not a.output.exists(), 'El resultado debe usar una ruta nueva'
        a.output.resolve().parent.mkdir(parents=True,exist_ok=True)
        source = {p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in (ROOT/'backend/src/main/resources/db/migration').glob('V*.sql')}
        compiled = {p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in (ROOT/'backend/target/classes/db/migration').glob('V*.sql')}
        assert source == compiled, 'Migraciones compiladas distintas de las fuentes; ejecutar la suite primero'
        config = json.loads(a.mysql_env.read_text())
        assert set(config) == {'FLUORA_REGRESION_MYSQL_URL','FLUORA_REGRESION_MYSQL_USER','FLUORA_REGRESION_MYSQL_PASSWORD'}, 'Solo configuración MySQL restringida'
        env.update(config)
        env.update(FLUORA_014_COLLATION=a.collation,FLUORA_014_RESULT=str(a.output.resolve()))
        result = subprocess.run(cmd,env=env,cwd=ROOT/'backend')
        raise SystemExit(result.returncode)
