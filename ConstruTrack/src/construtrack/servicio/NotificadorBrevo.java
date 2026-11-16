package construtrack.servicio;

import construtrack.modelo.Invitacion;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.util.Json;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * Envía la invitación por correo con la API de Brevo (antes Sendinblue), sin librerías: java.net.http.
 * Se usa la API HTTP porque los servidores gratuitos (como Render) bloquean el envío por SMTP.
 * Otra implementación de Notificador: el servicio de invitaciones no cambia (inversión de dependencias).
 */
public class NotificadorBrevo implements Notificador {

    public static final String API = "https://api.brevo.com/v3/smtp/email";
    private static final Logger LOG = Logger.getLogger(NotificadorBrevo.class.getName());
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final String api;
    private final String clave;
    private final String remitente;
    private final String urlSitio;
    private final HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    /**
     * @param api       dirección de la API (la de Brevo; en las pruebas, un servidor falso)
     * @param clave     API key de Brevo
     * @param remitente correo verificado en Brevo desde el que salen las invitaciones
     * @param urlSitio  dirección pública de la página, para el botón «Unirme»
     */
    public NotificadorBrevo(String api, String clave, String remitente, String urlSitio) {
        this.api = api;
        this.clave = clave;
        this.remitente = remitente;
        this.urlSitio = urlSitio.endsWith("/") ? urlSitio.substring(0, urlSitio.length() - 1) : urlSitio;
    }

    @Override
    public boolean envioReal() {
        return true;
    }

    @Override
    public void enviarInvitacion(Invitacion invitacion, String correoDestino) {
        String cuerpo = "{\"sender\":{\"name\":\"ConstruTrack\",\"email\":" + Json.texto(remitente) + "},"
                + "\"to\":[{\"email\":" + Json.texto(correoDestino) + "}],"
                + "\"subject\":" + Json.texto("Tu invitación a ConstruTrack: " + invitacion.getCodigo()) + ","
                + "\"htmlContent\":" + Json.texto(html(invitacion, correoDestino)) + ","
                + "\"textContent\":" + Json.texto(texto(invitacion, correoDestino)) + "}";
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(api))
                .timeout(Duration.ofSeconds(15))
                .header("api-key", clave)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() / 100 != 2) {
                LOG.warning(() -> "Brevo respondió " + respuesta.statusCode() + ": " + respuesta.body());
                throw new OperacionInvalidaException("El servicio de correo rechazó el envío ("
                        + respuesta.statusCode() + "). Revisa la configuración de Brevo.");
            }
            LOG.info(() -> "Invitación " + invitacion.getCodigo() + " enviada a " + correoDestino);
        } catch (IOException e) {
            throw new OperacionInvalidaException("No se pudo conectar con el servicio de correo. Intenta de nuevo.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OperacionInvalidaException("Se interrumpió el envío del correo.");
        }
    }

    /** Enlace que abre la página de registro con el correo ya escrito. */
    String enlace(String correo) {
        return urlSitio + "/app.html#registro?correo=" + URLEncoder.encode(correo, StandardCharsets.UTF_8);
    }

    private String detalle(Invitacion v) {
        return "Rol: " + v.getRol()
                + (v.getSupervisor() == null ? "" : " · Supervisor: " + v.getSupervisor().getNombre())
                + (v.getObra() == null ? "" : " · Obra: " + v.getObra().getNombre())
                + " · Vence el " + v.getVence().format(FECHA);
    }

    private String texto(Invitacion v, String correo) {
        return "Te invitaron a unirte a ConstruTrack.\n\nTu código de invitación es: " + v.getCodigo()
                + "\n" + detalle(v) + "\n\n1. Crea tu cuenta con este correo: " + enlace(correo)
                + "\n2. Escribe el código en «Únete a tu empresa».\n\nEl código sirve una sola vez.";
    }

    private String html(Invitacion v, String correo) {
        return "<div style=\"font-family:Arial,sans-serif;max-width:520px;margin:auto;color:#172b4d\">"
                + "<h2 style=\"color:#0c66e4\">Te invitaron a ConstruTrack</h2>"
                + "<p>Tu jefe te invitó a unirte al sistema de asistencia de la obra. Este es tu código:</p>"
                + "<p style=\"font-size:26px;font-weight:bold;letter-spacing:2px;background:#e9f2ff;padding:12px;"
                + "text-align:center;border-radius:8px\">" + v.getCodigo() + "</p>"
                + "<p style=\"color:#44546f\">" + escaparHtml(detalle(v)) + "</p>"
                + "<p><a href=\"" + escaparHtml(enlace(correo)) + "\" style=\"background:#0c66e4;color:#fff;"
                + "padding:12px 20px;border-radius:6px;text-decoration:none;display:inline-block\">Crear mi cuenta y unirme</a></p>"
                + "<p style=\"color:#626f86;font-size:13px\">Crea tu cuenta con este correo y luego escribe el código en "
                + "«Únete a tu empresa». El código sirve una sola vez.</p></div>";
    }

    private static String escaparHtml(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
