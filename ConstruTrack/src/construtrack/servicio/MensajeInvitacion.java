package construtrack.servicio;

import construtrack.modelo.Invitacion;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

public class MensajeInvitacion {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final String urlSitio;

    public MensajeInvitacion(String urlSitio) {
        this.urlSitio = urlSitio.endsWith("/") ? urlSitio.substring(0, urlSitio.length() - 1) : urlSitio;
    }

    public String asunto(Invitacion invitacion) {
        String empresa = invitacion.getCreadaPor().getEmpresa() == null ? "ConstruTrack"
                : invitacion.getCreadaPor().getEmpresa().getNombre();
        return "Tu código de registro para " + empresa + " en ConstruTrack: " + invitacion.getCodigo();
    }

    public String enlace(String correo) {
        return urlSitio + "/app.html#registro?correo=" + URLEncoder.encode(correo, StandardCharsets.UTF_8);
    }

    private String detalle(Invitacion v) {
        return "Rol: " + v.getRol()
                + (v.getSupervisor() == null ? "" : " · Supervisor: " + v.getSupervisor().getNombre())
                + (v.getObra() == null ? "" : " · Proyecto: " + v.getObra().getNombre())
                + " · Vence el " + v.getVence().format(FECHA);
    }

    public String texto(Invitacion v, String correo) {
        return "Tu gerente te registró en ConstruTrack.\n\nTu código de registro es: " + v.getCodigo()
                + "\n" + detalle(v) + "\n\n1. Crea tu cuenta con este correo: " + enlace(correo)
                + "\n2. Escribe el código en «Únete a tu empresa».\n\nEl código sirve una sola vez.";
    }

    public String html(Invitacion v, String correo) {
        return "<div style=\"font-family:Arial,sans-serif;max-width:520px;margin:auto;color:#172b4d\">"
                + "<h2 style=\"color:#0c66e4\">Tu código de registro en ConstruTrack</h2>"
                + "<p>Tu gerente te registró en el sistema de asistencia de la empresa. Este es tu código de registro:</p>"
                + "<p style=\"font-size:26px;font-weight:bold;letter-spacing:2px;background:#e9f2ff;padding:12px;"
                + "text-align:center;border-radius:8px\">" + v.getCodigo() + "</p>"
                + "<p style=\"color:#44546f\">" + escaparHtml(detalle(v)) + "</p>"
                + "<p><a href=\"" + escaparHtml(enlace(correo)) + "\" style=\"background:#0c66e4;color:#fff;"
                + "padding:12px 20px;border-radius:6px;text-decoration:none;display:inline-block\">Crear mi cuenta y unirme</a></p>"
                + "<p style=\"color:#626f86;font-size:13px\">Crea tu cuenta con este correo y luego escribe el código en "
                + "«Únete a tu empresa». El código sirve una sola vez.</p></div>";
    }

    static String escaparHtml(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
