# M1: confirmar correcciones A1/M1 y bajos del código

Autor: Codex. Solo lectura, revisión focalizada; no repetir inventario ni ampliar tarea. Leer008-f4-m1-codigo y comparar estas correcciones.

- Generador ahora recoge tokens de cualquier valor de declaración retenida, incluyendo customprops de animación; comprueba keyframes emitidos contra las referencias. Regenerado contiene @keyframes spinner-border. Guard nuevo check-public-builds verifica un anchor global fuera de noscript, CSSfuente existente, tenant-public.cssnoinyectada y spinner enartefacto.
- createTenantPublicStyles: límite técnico10s, testeable por parámetro; libera navegación conservando único link y pending, load tardío sigueaplicando. Un error elimina link ypermite retry; ready no baja durante retry, evitando desmontarHeader. Fallback preservaCSScrítico ycoloca antes de primerstylecomponente detectado_ngcontent/_nghost; registraerrorfaltaglobal. Tests tiempo falso/loadtardío/únicopending/retry/orden.
- App test con tenant activo yWhatsApp/chat habilitados comprueba tresoverlays/Footer antes/despuésready. Defaultapplicationnoopcontinúa.
- LH alterna orden por paresABBA yregistra sequence. Provenancefingerprint-only identifica elartefacto y measurementCodeidentifica commit/dirty delorquestador, sinatribuiralbaselinefuentesM1. MIMEwoff agregado. NoLighthousecorriendo mientrasrevisas.

Confirma alta/media cerradas y cualquier defecto enestas correcciones. Rebuild/suitefinalencurso porCodex; métricasM1 yvalidaciónvisualrestantes no sepresuponen. Capturas condistintoscrolls/estadosdelcarritoytransicionesnopuedenusarsecomopruebadeequivalencia; están declaradas yseparadas. BorderealBooking196/198/208comprobado, aplicaciónCSSbyteidéntico; P2seguiráverificaciónconperfilCLI. NoFirebase/M2/cambiosnegocio/datos/credenciales/commitnuevo.
