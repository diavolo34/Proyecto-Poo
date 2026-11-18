package construtrack.servicio;

import java.time.Duration;

public class MensajeVerificacion {
    public String asunto(String codigo) {
        return codigo + " es tu código para confirmar tu correo en ConstruTrack";
    }

    public String texto(String codigo, Duration vigencia) {
        return "Gracias por registrarte en ConstruTrack.\n\nTu código para confirmar el correo es: " + codigo
                + "\n\nEscríbelo en la aplicación. Vence en " + vigencia.toMinutes() + " minutos.\n"
                + "Si no creaste una cuenta, ignora este correo.";
    }

    public String html(String codigo, Duration vigencia) {
        return "<div style=\"font-family:Arial,sans-serif;max-width:520px;margin:auto;color:#172b4d\">"
                + "<h2 style=\"color:#0c66e4\">Confirma tu correo</h2>"
                + "<p>Gracias por registrarte en ConstruTrack. Escribe este código en la aplicación:</p>"
                + "<p style=\"font-size:30px;font-weight:bold;letter-spacing:6px;background:#e9f2ff;padding:12px;"
                + "text-align:center;border-radius:8px\">" + MensajeInvitacion.escaparHtml(codigo) + "</p>"
                + "<p style=\"color:#44546f\">Vence en " + vigencia.toMinutes() + " minutos.</p>"
                + "<p style=\"color:#626f86;font-size:13px\">Si no creaste una cuenta, ignora este correo.</p></div>";
    }
}
