package construtrack.servicio;

import construtrack.modelo.Invitacion;

public interface Notificador {
    void enviarInvitacion(Invitacion invitacion, String correoDestino);

    default boolean envioReal() {
        return false;
    }
}
