package construtrack.servicio;

import construtrack.modelo.Invitacion;
import java.time.Duration;
import java.util.logging.Logger;

public class NotificadorConsola implements Notificador {
    private static final Logger LOG = Logger.getLogger(NotificadorConsola.class.getName());

    private final MensajeRestablecimiento restablecimientos;

    public NotificadorConsola() {
        this("http://localhost:8080");
    }

    public NotificadorConsola(String urlSitio) {
        this.restablecimientos = new MensajeRestablecimiento(urlSitio);
    }

    @Override
    public void enviarInvitacion(Invitacion invitacion, String correoDestino) {
        LOG.info(() -> "Código de registro " + invitacion.getCodigo() + " (" + invitacion.getRol() + ") para " + correoDestino);
    }

    @Override
    public void enviarCodigoVerificacion(String correoDestino, String codigo, Duration vigencia) {
        LOG.info(() -> "Código para confirmar el correo " + correoDestino + ": " + codigo + " (vence en "
                + vigencia.toMinutes() + " min)");
    }

    @Override
    public void enviarRestablecimiento(String correoDestino, String token, Duration vigencia) {
        LOG.info(() -> "Enlace para restablecer la clave de " + correoDestino + " (vence en " + vigencia.toMinutes()
                + " min): " + restablecimientos.enlace(token));
    }
}
