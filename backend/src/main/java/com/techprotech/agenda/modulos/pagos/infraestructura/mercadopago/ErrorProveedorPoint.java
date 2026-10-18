package com.techprotech.agenda.modulos.pagos.infraestructura.mercadopago;

/** Código sin cuerpo ni credenciales del proveedor. Solo ciertos 4xx prueban un rechazo definitivo. */
public class ErrorProveedorPoint extends RuntimeException {
    private final int status;
    public ErrorProveedorPoint(int status) {super("Mercado Pago rechazó la operación Point (HTTP "+status+")");this.status=status;}
    public int status() {return status;}
    public boolean definitivo() {return java.util.Set.of(400,401,403,404,409,422).contains(status);}
}
