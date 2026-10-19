#!/usr/bin/env python3
"""Postman local contra copia efímera. Nunca usa root para importar o ejecutar pruebas."""
import argparse, json, os, pathlib, re, shutil, socket, subprocess, tempfile, time, urllib.request, uuid
import xml.etree.ElementTree as ET

ROOT=pathlib.Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--mysql-env',type=pathlib.Path,required=True,help='JSON privado con FLUORA_REGRESION_MYSQL_*')
parser.add_argument('--source-dump',type=pathlib.Path,help='Dump privado sin CREATE DATABASE ni USE; lectura de origen hecha fuera de las pruebas')
parser.add_argument('--copy-local',action='store_true',help='Exportar agenda_db del contenedor local en modo solo lectura')
parser.add_argument('--keep-on-failure',action='store_true',help='Conservar solo logs privados, nunca la base o el proceso')
args=parser.parse_args()
if bool(args.source_dump)==bool(args.copy_local):parser.error('Selecciona exactamente --source-dump o --copy-local')
config=json.loads(args.mysql_env.read_text())
if config['FLUORA_REGRESION_MYSQL_USER'].lower()=='root':raise SystemExit('No se permite root en las pruebas')
base=config['FLUORA_REGRESION_MYSQL_URL']
match=re.fullmatch(r'jdbc:mysql://(127\.0\.0\.1|localhost):(\d+)/fluora_test_[a-z0-9_]+(?:\?.*)?',base)
if not match:raise SystemExit('Solo MySQL local con base fluora_test_* explícita')
with socket.socket() as probe:
 if probe.connect_ex(('127.0.0.1',18080))==0:raise SystemExit('18080 está ocupado; no se interrumpe ningún proceso ajeno')
env={k:os.environ[k] for k in ['PATH','HOME','TMPDIR','LANG','LC_ALL','JAVA_HOME'] if k in os.environ}
postman_env=dict(env)
mysql_env={**env,'MYSQL_PWD':config['FLUORA_REGRESION_MYSQL_PASSWORD']}
mysql=['mysql','-h'+match[1],'-P'+match[2],'-u'+config['FLUORA_REGRESION_MYSQL_USER'],'--batch','--skip-column-names']
work=pathlib.Path(tempfile.mkdtemp(prefix='fluora-postman-'));os.chmod(work,0o700)
db='fluora_test_011_'+uuid.uuid4().hex[:12]
backend=None;created=False;exit_code=1

def query(sql,database=None):
 command=mysql+([database] if database else [])
 result=subprocess.run(command,input=sql,text=True,env=mysql_env,capture_output=True)
 if result.returncode:raise RuntimeError('MySQL restringido falló: '+result.stderr[:1500])
 return result.stdout.strip()

try:
 grants=query('SHOW GRANTS;').splitlines()
 for grant in grants:
  # mysql --batch escapa cada barra; normalizar únicamente la salida de texto.
  grant=grant.replace('\\\\','\\');rule=grant.split(' TO ',1)[0]
  if 'WITH GRANT OPTION' in grant or not (rule=='GRANT USAGE ON *.*' or re.fullmatch(r'GRANT [A-Z_, ]+ ON `fluora\\_test\\_%`\.\*',rule)):
   raise RuntimeError('Cuenta MySQL con privilegios fuera del patrón de pruebas')
 if query('SELECT CURRENT_ROLE();')!='NONE':raise RuntimeError('No se permiten roles MySQL')
 query('CREATE DATABASE `'+db+'` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;');created=True
 if query('SELECT DATABASE();',db)!=db:raise RuntimeError('Base destino incorrecta')
 if args.copy_local:
  dump=subprocess.run(['docker','exec','agenda-mysql','sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot --no-tablespaces --set-gtid-purged=OFF --single-transaction --skip-comments --skip-triggers --skip-add-locks agenda_db'],capture_output=True,check=True).stdout.decode()
 else:dump=args.source_dump.read_text()
 if re.search(r'(?im)^\s*(CREATE\s+DATABASE|USE\s+)',dump):raise RuntimeError('Dump con selección de base no permitido')
 query(dump,db);dump=None
 java_home=pathlib.Path(env.get('JAVA_HOME','/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home'))
 env['JAVA_HOME']=str(java_home)
 with (work/'compile.log').open('w') as log:subprocess.run(['mvn','-q','test-compile'],cwd=ROOT/'backend',env=env,stdout=log,stderr=subprocess.STDOUT,check=True)
 with (work/'classpath.log').open('w') as log:
  subprocess.run(['mvn','-q','dependency:build-classpath','-Dmdep.includeScope=test','-Dmdep.outputFile='+str(work/'classpath.txt')],cwd=ROOT/'backend',env=env,stdout=log,stderr=subprocess.STDOUT,check=True)
 cp=str(ROOT/'backend/target/classes')+':'+(work/'classpath.txt').read_text().strip()
 for folder in ['fluora/postman','com/techprotech/agenda/soporte']:
  dest=work/'classes'/folder;dest.parent.mkdir(parents=True,exist_ok=True)
  shutil.copytree(ROOT/'backend/target/test-classes'/folder,dest)
 env_path=work/'aislado.postman_environment.json'
 env.update(SPRING_PROFILES_ACTIVE='api-aislado',SPRING_DATASOURCE_URL=re.sub(r'/fluora_test_[a-z0-9_]+(?=\?|$)','/'+db,base),SPRING_DATASOURCE_USERNAME=config['FLUORA_REGRESION_MYSQL_USER'],SPRING_DATASOURCE_PASSWORD=config['FLUORA_REGRESION_MYSQL_PASSWORD'],FLUORA_API_ENTORNO='aislado',INVITACIONES_URL_ACEPTACION='http://127.0.0.1:4200/invitacion/aceptar',RECUPERACION_CONTRASENA_URL='http://127.0.0.1:4200/recuperar-contrasena/confirmar',FLUORA_API_ENV_PATH=str(env_path),JWT_SECRETO=uuid.uuid4().hex+uuid.uuid4().hex)
 # Flyway actualiza únicamente la copia cuyo nombre y privilegios se comprobaron antes.
 # No se usa la .env del desarrollador como fuente de proveedores en la instancia de prueba.
 opts=['--server.port=18080','--server.address=127.0.0.1','--aplicacion.tareas-programadas.habilitadas=false','--spring.jpa.hibernate.ddl-auto=validate','--spring.flyway.enabled=true','--management.health.mail.enabled=false','--aplicacion.correo.habilitado=false','--aplicacion.whatsapp.habilitado=false','--aplicacion.correo.plataforma.habilitado=false','--aplicacion.plataforma.stripe.habilitado=false','--aplicacion.limite-solicitudes.habilitado=false','--aplicacion.pagos.mercado-pago.ambiente=PRUEBA','--aplicacion.pagos.mercado-pago.point-terminal-prueba-id=API-VIRTUAL']
 command=[str(java_home/'bin/java'),'-Dspring.devtools.restart.enabled=false','-Duser.timezone=UTC','-Dhttp.proxyHost=127.0.0.1','-Dhttp.proxyPort=9','-Dhttps.proxyHost=127.0.0.1','-Dhttps.proxyPort=9','-cp',str(work/'classes')+':'+cp,'fluora.postman.ApiAisladaApplication',*opts]
 log=(work/'backend.log').open('w');os.chmod(work/'backend.log',0o600)
 backend=subprocess.Popen(command,cwd=ROOT/'backend',env=env,stdout=log,stderr=subprocess.STDOUT)
 for _ in range(240):
  if backend.poll() is not None:raise RuntimeError('La instancia aislada no arrancó; ver log privado '+str(work/'backend.log'))
  if env_path.exists():break
  time.sleep(.5)
 else:raise RuntimeError('Tiempo de arranque agotado')
 # Verificar el marcador antes del runner y no solo puerto o variables declaradas.
 values={v['key']:v['value'] for v in json.loads(env_path.read_text())['values']}
 marker=urllib.request.Request(values['base']+'/api/v1/publico/__pruebas/aislamiento',headers={'X-Fluora-Prueba':values['nonce']})
 with urllib.request.urlopen(marker,timeout=10) as response:proof=json.load(response)
 if proof['database']!=db or proof['nonce']!=values['nonce']:raise RuntimeError('Marcador aislado incorrecto')
 report=ROOT/'backend/target/postman';report.mkdir(exist_ok=True);os.chmod(report,0o700)
 run=['postman','collection','run',str(ROOT/'backend/postman/agenda-web.postman_collection.json'),'-e',str(env_path),'--no-report-events','--reporters','cli,junit','--reporter-junit-export',str(report/'regresion.xml')]
 for folder in ['00 Humo (lectura)','01 Seguridad: tenant y RBAC','02 Políticas (007-1)','03 Reservas y anticipo (009, 010)','04 Checkout y aceptación (007-1)','05 Recepción (009 P2)','06 Caja (009 P3 y 010)','07 Point y pago dividido (010)']:run+=['-i',folder]
 with (report/'regresion.log').open('w') as log:
  result=subprocess.run(run,cwd=ROOT,env=postman_env,stdout=log,stderr=subprocess.STDOUT)
 exit_code=result.returncode
 print('Postman desde archivo local; exit',exit_code,'; informe',report/'regresion.xml',flush=True)
except Exception as error:
 print(str(error),flush=True)
finally:
 if backend:
  backend.terminate()
  try:backend.wait(timeout=15)
  except subprocess.TimeoutExpired:backend.kill();backend.wait()
 if created:
  if not re.fullmatch('fluora_test_011_[a-z0-9]+',db):raise RuntimeError('Nombre no seguro para retirar copia')
  query('DROP DATABASE `'+db+'`;')
  if query("SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='"+db+"';")!='0':raise RuntimeError('La copia no se retiró')
 with socket.socket() as probe:
  if probe.connect_ex(('127.0.0.1',18080))==0:raise RuntimeError('Puerto 18080 continúa ocupado')
 print('Copia retirada y puerto18080 libre.',flush=True)
 if exit_code and args.keep_on_failure:print('Logs privados de diagnóstico:',work,flush=True)
 else:shutil.rmtree(work)
raise SystemExit(exit_code)
