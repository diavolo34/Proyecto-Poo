package construtrack.servicio;

import construtrack.modelo.Invitacion;
import java.util.logging.Logger;

public class NotificadorConsola implements Notificador {
    private static final Logger LOG = Logger.getLogger(NotificadorConsola.class.getName());

    @Override
    public void enviarInvitacion(Invitacion invitacion, String correoDestino) {
        LOG.info(() -> "Invitación " + invitacion.getCodigo() + " (" + invitacion.getRol() + ") para " + correoDestino);
    }
}
