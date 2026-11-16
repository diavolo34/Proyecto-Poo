package construtrack.servicio;

import construtrack.modelo.Invitacion;

/**
 * Envía avisos fuera del sistema (por ejemplo, la invitación por correo). Los servicios dependen de esta
 * interfaz y no de un proveedor concreto: hoy se registra en la consola y mañana se puede conectar un
 * servidor de correo sin cambiar el servicio (principio de inversión de dependencias).
 */
public interface Notificador {

    /**
     * Envía la invitación al correo indicado.
     * Si el envío falla, lanza OperacionInvalidaException con el motivo (la invitación ya quedó creada).
     */
    void enviarInvitacion(Invitacion invitacion, String correoDestino);

    /** true si los avisos llegan de verdad a la persona; false si solo se registran (modo de demostración). */
    default boolean envioReal() {
        return false;
    }
}
