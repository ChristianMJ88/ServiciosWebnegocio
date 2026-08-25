# WhatsApp list picker templates

Para usar list pickers dinamicos en los flujos de WhatsApp, crea en Twilio Content API un contenido `twilio/list-picker` por cada cantidad de opciones que quieras soportar.

La forma recomendada es guardarlos en el admin de WhatsApp, en el catalogo de plantillas por tenant:

```text
Nombre: Menu bienvenida
Uso backend: MENU_BIENVENIDA
Content SID: HX...

Nombre: List picker 3 opciones
Uso backend: LIST_PICKER_3
Content SID: HX...
```

Para list pickers dinamicos, crea y registra de `LIST_PICKER_1` a `LIST_PICKER_10` segun lo que quieras soportar. El backend elige automaticamente el `ContentSid` correcto segun cuantas opciones tenga que mostrar.

Como fallback para el demo, el backend todavia soporta guardar los SIDs en la configuracion operativa con este formato:

```text
1=HX...
2=HX...
3=HX...
4=HX...
5=HX...
6=HX...
7=HX...
8=HX...
9=HX...
10=HX...
```

## Variables

Todos los templates usan estas variables base:

```text
{{body}}
{{button}}
```

Y por cada opcion:

```text
{{item1}} {{id1}} {{desc1}}
{{item2}} {{id2}} {{desc2}}
...
{{item10}} {{id10}} {{desc10}}
```

## Template de 3 opciones

Este ejemplo sirve como base. Para crear los demas, cambia el `friendly_name` y deja solo la cantidad de items necesaria.

```json
{
  "friendly_name": "agenda_list_picker_3",
  "language": "es_MX",
  "variables": {
    "body": "Texto principal",
    "button": "Ver opciones",
    "item1": "Opcion 1",
    "id1": "PAYLOAD_1",
    "desc1": "Descripcion 1",
    "item2": "Opcion 2",
    "id2": "PAYLOAD_2",
    "desc2": "Descripcion 2",
    "item3": "Opcion 3",
    "id3": "PAYLOAD_3",
    "desc3": "Descripcion 3"
  },
  "types": {
    "twilio/list-picker": {
      "body": "{{body}}",
      "button": "{{button}}",
      "items": [
        {
          "item": "{{item1}}",
          "id": "{{id1}}",
          "description": "{{desc1}}"
        },
        {
          "item": "{{item2}}",
          "id": "{{id2}}",
          "description": "{{desc2}}"
        },
        {
          "item": "{{item3}}",
          "id": "{{id3}}",
          "description": "{{desc3}}"
        }
      ]
    }
  }
}
```

## Payloads que envia el backend

El backend ya reconoce estos payloads:

```text
SUCURSAL|{sucursalId}
SERVICIO|{servicioId}
HORA|{HH:mm}
```

Si Twilio no tiene un SID configurado para la cantidad de opciones, el sistema responde con texto plano como fallback.

## Donde se usan

- Elegir sucursal, solo si hay mas de una sucursal.
- Elegir servicio.
- Elegir hora disponible para agendar.

Si hay mas de 10 opciones, el backend conserva texto plano porque WhatsApp list picker soporta maximo 10 items.
