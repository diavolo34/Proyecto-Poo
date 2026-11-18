package construtrack.servicio;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class MensajeRestablecimiento {
    private final String urlSitio;

    public MensajeRestablecimiento(String urlSitio) {
        this.urlSitio = urlSitio.endsWith("/") ? urlSitio.substring(0, urlSitio.length() - 1) : urlSitio;
    }

    public String asunto() {
        return "Restablece tu clave de ConstruTrack";
    }

    public String enlace(String token) {
        return urlSitio + "/app.html#restablecer?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    public String texto(String token, Duration vigencia) {
        return "Recibimos una solicitud para restablecer la clave de tu cuenta de ConstruTrack.\n\n"
                + "Abre este enlace para elegir una clave nueva:\n" + enlace(token) + "\n\n"
                + "El enlace vence en " + vigencia.toMinutes() + " minutos y sirve una sola vez.\n"
                + "Si no lo pediste tú, ignora este correo: tu clave no cambia.";
    }

    public String html(String token, Duration vigencia) {
        return "<div style=\"font-family:Arial,sans-serif;max-width:520px;margin:auto;color:#172b4d\">"
                + "<h2 style=\"color:#0c66e4\">Restablece tu clave</h2>"
                + "<p>Recibimos una solicitud para restablecer la clave de tu cuenta de ConstruTrack.</p>"
                + "<p><a href=\"" + MensajeInvitacion.escaparHtml(enlace(token)) + "\" style=\"background:#0c66e4;color:#fff;"
                + "padding:12px 20px;border-radius:6px;text-decoration:none;display:inline-block\">Elegir una clave nueva</a></p>"
                + "<p style=\"color:#44546f\">El enlace vence en " + vigencia.toMinutes() + " minutos y sirve una sola vez.</p>"
                + "<p style=\"color:#626f86;font-size:13px\">Si no lo pediste tú, ignora este correo: tu clave no cambia. "
                + "Nadie de ConstruTrack te pedirá tu clave por correo ni por teléfono.</p></div>";
    }
}
