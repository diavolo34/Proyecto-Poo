package construtrack.servicio;

import construtrack.modelo.Invitacion;
import java.util.logging.Logger;

/** Implementación de demostración: deja constancia en el registro en lugar de enviar un correo real. */
public class NotificadorConsola implements Notificador {

    private static final Logger LOG = Logger.getLogger(NotificadorConsola.class.getName());

    @Override
    public void enviarInvitacion(Invitacion invitacion, String correoDestino) {
        LOG.info(() -> "Invitación " + invitacion.getCodigo() + " (" + invitacion.getRol() + ") para " + correoDestino);
    }
}
