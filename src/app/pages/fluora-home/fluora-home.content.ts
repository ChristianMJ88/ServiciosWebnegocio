/** Copy approved in 008/D1–D5. Commercial terms and support data come from the API. */
export const HOME_CONTENT = {
  hero: {
    kicker: 'Agenda · Sitio · Tienda · Caja · CRM',
    title: 'La agenda en línea para salones, barberías y spas.',
    lead: 'Reservas y ventas en tu sitio. Equipo y cobros en una app.',
    secondary: 'Ver cómo funciona',
    reassurance: 'Sin contrato'
  },
  pillars: [
    { icon: 'bi-calendar2-check', title: 'Recibe reservas', description: 'Tu sitio, servicios, precios y horarios disponibles.' },
    { icon: 'bi-people', title: 'Organiza a tu equipo', description: 'Agendas por integrante y sucursal, sin cruces.' },
    { icon: 'bi-wallet2', title: 'Cobra y da seguimiento', description: 'Recepción, caja, clientes y recordatorios conectados.' },
    { icon: 'bi-bag', title: 'Vende en línea', description: 'Carrito, pagos y envíos; inventario y finanzas en tu panel.' }
  ],
  workflow: [
    { title: 'Publica tu negocio', description: 'Regístrate y publica tu sitio con agenda y tienda.' },
    { title: 'Recibe reservas y pedidos', description: 'Tus clientes eligen servicios, horarios y productos.' },
    { title: 'Atiende y cobra', description: 'Tu equipo continúa la atención con toda la información.' }
  ],
  product: [
    { id: 'sitio', label: 'Minisitio', icon: 'bi-window', title: 'Tu marca. Tu propio espacio.', description: 'Personaliza tu sitio con imágenes, servicios, precios y contacto. Comparte el enlace para recibir reservas.', items: ['Identidad de tu negocio', 'Catálogo y reservas en línea', 'Una experiencia para cada dispositivo'] },
    { id: 'agenda', label: 'Agenda', icon: 'bi-calendar2-week', title: 'Cada cita en su lugar.', description: 'Relaciona servicios, sucursales y disponibilidad de cada integrante para organizar la atención.', items: ['Horarios y bloqueos por integrante', 'Servicios con duración y responsable', 'Recepción y seguimiento de las citas'] },
    { id: 'tienda', label: 'Tienda en línea', icon: 'bi-bag', title: 'Tus productos, también en línea.', description: 'Publica tu catálogo con carrito, pagos y opciones de entrega. Consulta inventario y finanzas desde el mismo panel.', items: ['Catálogo y carrito', 'Pagos en línea y opciones de envío', 'Inventario y reportes conectados'] },
    { id: 'whatsapp', label: 'WhatsApp', icon: 'bi-whatsapp', title: 'De la conversación a tu agenda.', description: 'Conecta mensajes y recordatorios con la atención de tu negocio. El enlace de reserva lleva al cliente a elegir su servicio y horario.', items: ['Mensajes y plantillas', 'Enlaces a tu sitio de reservas', 'Historial de atención del cliente'] },
    { id: 'operacion', label: 'Caja y CRM', icon: 'bi-receipt', title: 'Atiende con contexto. Cobra con claridad.', description: 'Reúne servicios y productos en Caja y conserva el historial del cliente para continuar la atención.', items: ['Cobros de servicios y productos', 'Clientes e historial de citas', 'Reportes para consultar tu operación'] }
  ],
  faq: [
    { id: 'para-quien', question: '¿Qué es Fluora y para quién es?', answer: 'Es una agenda en línea para negocios de servicios, con sitio propio y herramientas para coordinar citas, equipo y clientes. Los módulos disponibles dependen del plan que elijas.' },
    { id: 'precio', question: '¿Cuánto cuesta? ¿Cómo funciona la prueba?', answer: 'Los precios, módulos y límites vigentes están en los planes de esta página. No requiere contrato y puedes cancelar en cualquier momento.' },
    { id: 'whatsapp', question: '¿WhatsApp está incluido? ¿Tiene otros costos?', answer: 'Su disponibilidad depende del plan. Los mensajes y servicios de proveedores externos pueden generar consumos adicionales; consulta los módulos y condiciones del plan.' },
    { id: 'migracion', question: '¿Me ayudan si ya uso otra agenda?', answer: 'Sí. Te ayudamos a migrar y te acompañamos durante la configuración de tu negocio para que le saques el máximo provecho desde el primer día.' },
    { id: 'tienda', question: '¿Puedo vender mis productos?', answer: 'Sí. Tu sitio puede incluir tienda, carrito, pagos en línea y opciones de entrega, con inventario y finanzas en el panel. Estas funciones dependen de los módulos de tu plan.' },
    { id: 'inasistencias', question: '¿Cómo puedo dar seguimiento a las inasistencias?', answer: 'Configura políticas de reserva y, según tu plan, confirmaciones, recordatorios y anticipos. Consulta el estado y el historial de cada cita para continuar la atención.' }
  ],
  closing: { title: 'Empieza con Fluora.', description: 'Te acompañamos en la configuración de tu negocio, desde tu sitio hasta tu operación diaria.' }
} as const;
