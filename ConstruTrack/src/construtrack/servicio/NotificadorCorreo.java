package construtrack.servicio;

import construtrack.modelo.Invitacion;
import java.time.Duration;
import java.util.logging.Logger;

public abstract class NotificadorCorreo implements Notificador {
    private static final Logger LOG = Logger.getLogger(NotificadorCorreo.class.getName());

    private final MensajeInvitacion invitaciones;
    private final MensajeRestablecimiento restablecimientos;

    protected NotificadorCorreo(String urlSitio) {
        this.invitaciones = new MensajeInvitacion(urlSitio);
        this.restablecimientos = new MensajeRestablecimiento(urlSitio);
    }

    @Override
    public boolean envioReal() {
        return true;
    }

    @Override
    public void enviarInvitacion(Invitacion invitacion, String correoDestino) {
        enviar(new Correo(correoDestino, invitaciones.asunto(invitacion), invitaciones.html(invitacion, correoDestino),
                invitaciones.texto(invitacion, correoDestino)));
        LOG.info(() -> "Invitación " + invitacion.getCodigo() + " enviada a " + correoDestino);
    }

    @Override
    public void enviarRestablecimiento(String correoDestino, String token, Duration vigencia) {
        enviar(new Correo(correoDestino, restablecimientos.asunto(), restablecimientos.html(token, vigencia),
                restablecimientos.texto(token, vigencia)));
        LOG.info(() -> "Enlace para restablecer la clave enviado a " + correoDestino);
    }

    protected abstract void enviar(Correo correo);
}
