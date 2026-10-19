# 008 F4 — optimización tras medición real

Solo lectura. Lighthouse nativoChrome13.4.1 sobre buildmarketinglocal/gzip, MotoGPower/4Glenta. Primera corrida conextensiones:75/100Acc/96BP/100SEO,CLS.001; informeatribuyó469KiBsinusar aAdobe. Enincógnito:65/100/100/100,CLS.002; repeticiónenreposo44/100/100/100,CLS.001. No se descartan ni ocultan resultadosbajos. MetaPerf85aúnnocumplida. Los recursoslegacyAnimate/GoogleFonts siguenbloqueando590ms, pero se conservan porque los tenantslosusan.

Cambio mínimo frontend: src/index.htmlpreloadde2WOFFlocalesconaliasexclusivos+preconnectlegacy, sinquitarfuentesniAnimate. Enmain.marketing únicamente se importaHomeeager paraevitarunviajederedantesdelprimerh1;4bloquesdefer yGSAPsiguenlazy. La ruta compartida fluoraHomeRoute seexportaporidentidad; elarrayseclona sustituyendosoloesa ruta, sinmovertenantHostMatch/resolvers. Elentryapplicationmantienelazyroot. No reglasdebackend,tenant,permisosniprecios nuevas. Seagregaprueba deorden/identidad yloadComponentpreservado enpublicContentRoutes. Suite/build/re-mediciónencurso.

Revisa alcance,aislamiento,ordenderutas,preloads y riesgo. Noescribirappniampliaralcance. Sinbackend/migración/commit/push/despliegue. Nocambiarassetslegacydenegociosporscore.
