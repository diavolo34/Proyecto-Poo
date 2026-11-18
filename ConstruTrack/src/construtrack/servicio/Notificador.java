package construtrack.servicio;

import construtrack.modelo.Invitacion;
import java.time.Duration;

public interface Notificador {
    void enviarInvitacion(Invitacion invitacion, String correoDestino);

    void enviarRestablecimiento(String correoDestino, String token, Duration vigencia);

    default boolean envioReal() {
        return false;
    }
}
