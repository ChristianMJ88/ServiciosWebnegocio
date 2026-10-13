CREATE TABLE empresa_configuracion_reservas (
  empresa_id BIGINT NOT NULL PRIMARY KEY,
  anticipacion_minima_minutos INT NOT NULL,
  ventana_reserva_dias INT NOT NULL,
  liberacion_sin_confirmacion_horas INT NOT NULL,
  minutos_apartado_pago INT NOT NULL,
  CONSTRAINT fk_configuracion_reservas_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);
