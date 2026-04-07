CREATE TABLE empresa_sitio_config (
    empresa_id BIGINT PRIMARY KEY,
    slug VARCHAR(100) NOT NULL UNIQUE,
    nombre_comercial VARCHAR(150) NOT NULL,
    logo_url VARCHAR(255) NULL,
    descripcion_corta VARCHAR(255) NULL,
    color_primario VARCHAR(20) NULL,
    color_secundario VARCHAR(20) NULL,
    hero_titulo VARCHAR(180) NULL,
    hero_subtitulo VARCHAR(500) NULL,
    whatsapp VARCHAR(30) NULL,
    telefono VARCHAR(30) NULL,
    correo VARCHAR(150) NULL,
    direccion VARCHAR(255) NULL,
    instagram_url VARCHAR(255) NULL,
    facebook_url VARCHAR(255) NULL,
    tema VARCHAR(50) NOT NULL DEFAULT 'nail-art-base',
    publicado BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_empresa_sitio_config_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);

UPDATE empresa
SET nombre = 'Nail Art Studio', slug = 'nail-art'
WHERE id = 1 AND slug = 'empresa-demo';

INSERT INTO empresa_sitio_config (
    empresa_id,
    slug,
    nombre_comercial,
    logo_url,
    descripcion_corta,
    color_primario,
    color_secundario,
    hero_titulo,
    hero_subtitulo,
    whatsapp,
    telefono,
    correo,
    direccion,
    instagram_url,
    facebook_url,
    tema,
    publicado
)
VALUES (
    1,
    'nail-art',
    'Nail Art Studio',
    '/NailArt_logo.jpeg',
    'Diseño, técnica y cuidado para manos y pies.',
    '#d14f7d',
    '#f6d9e3',
    'Diseños que se sienten tuyos desde el primer vistazo.',
    'Reserva, conoce servicios y atiende a tus clientes con una experiencia clara y cuidada.',
    '522204292573',
    '220 429 25 73',
    'christianmejia@techprotech.com.mx',
    'Calle Diagonal Benito Juarez #19, Col. Nueva Antequera, Puebla',
    '',
    '',
    'nail-art-base',
    TRUE
)
ON DUPLICATE KEY UPDATE
    slug = VALUES(slug),
    nombre_comercial = VALUES(nombre_comercial),
    logo_url = VALUES(logo_url),
    descripcion_corta = VALUES(descripcion_corta),
    color_primario = VALUES(color_primario),
    color_secundario = VALUES(color_secundario),
    hero_titulo = VALUES(hero_titulo),
    hero_subtitulo = VALUES(hero_subtitulo),
    whatsapp = VALUES(whatsapp),
    telefono = VALUES(telefono),
    correo = VALUES(correo),
    direccion = VALUES(direccion),
    instagram_url = VALUES(instagram_url),
    facebook_url = VALUES(facebook_url),
    tema = VALUES(tema),
    publicado = VALUES(publicado);
