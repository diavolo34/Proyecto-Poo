package construtrack.servicio;

import construtrack.modelo.Invitacion;

/**
 * Envía avisos fuera del sistema (por ejemplo, la invitación por correo). Los servicios dependen de esta
 * interfaz y no de un proveedor concreto: hoy se registra en la consola y mañana se puede conectar un
 * servidor de correo sin cambiar el servicio (principio de inversión de dependencias).
 */
public interface Notificador {

    void enviarInvitacion(Invitacion invitacion, String correoDestino);
}
