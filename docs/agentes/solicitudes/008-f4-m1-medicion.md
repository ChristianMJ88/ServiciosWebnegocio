# 008 F4 — M1 medido y paso a M2

Codex solicita revisión de solo lectura. No escribir código de aplicación.

Releer revisiones/008-f4-rendimiento-plan.md, m1-codigo y m1-correcciones. Altos/medios y bajos anteriores corregidos; los dos últimos bajos también: hashes de scripts/procedencia y global último hijo (regresión nueva). Angular completo 265/79 verde. Ambos builds exit0, check:public-builds/public-assets/marketing-css verdes; CSS application sigue idéntico a M0. Cobertura de clases DOM Bootstrap/Icons en 15 rutas/estados pasa. Las capturas previas no prueban equivalencia exacta por scroll, animaciones y estados distintos del carrito; esa limitación se mantiene abierta.

Comparación alternada AB/BA/AB/BA/AB, mismos scripts, ambos builds precomprimidos, API real 8080, una warmup aparte, cinco perfiles Chrome nuevos, Lighthouse13.4.1/simulate/375×812/AC. Informes ignorados tmp/lighthouse/m1-paired/{m0,m1}-summary.json. M0:82/82/85/83/83, med83; M1:81/85/79/84/85, med84. A/BP/SEO100 en todas, CLS<=.000707, Header/tenantCSS ausentes. No atribuir causalidad firme a un punto de diferencia ni declarar meta85 alcanzada. Previews4173/4174 retirados, servicios del usuario4200/8080 conservados.

Solicito confirmar si hay bloqueantes de código para continuar M2 autorizado. Propuesta M2 manual: builder @angular/build:application y serve @angular/build:dev-server; main→browser en default/marketing/application; outputPath objeto base dist/{ServiciosWebnegocio,marketing,application} y browser:'browser', conservando rutas finales, index, assets, fileReplacements, estilos y budgets. No SSR/hosting/functions runtime ni dependencias actualizadas. Verificar builds3, plantilla SEO, assets, suite Angular completa y dev-server temporal propio (nunca detener4200 del usuario). Corregir además NG8107 según adenda con .at(0)??null y prueba real de componente sin módulos. Después medir M2 y P2; M3 requiere usuario. D1 caché tenantCSS queda requisito previo a despliegue, sin cambio de hosting aquí.

Árbol compartido contiene cambios ajenos en AGENTS.md y revisiones005/006/007/plan, preservarlos. Commits011 798137b y008 e642571 hechos; sin push ni despliegue.
