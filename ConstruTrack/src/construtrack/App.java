package construtrack;

import construtrack.controlador.swing.AplicacionSwing;
import construtrack.controlador.web.ServidorWeb;
import construtrack.modelo.Plataforma;
import construtrack.modelo.incidencias.FabricaIncidencias;
import construtrack.persistencia.Almacen;
import construtrack.persistencia.AlmacenArchivo;
import construtrack.persistencia.AlmacenUpstash;
import construtrack.persistencia.GeneradorDatosPrueba;
import construtrack.persistencia.RepositorioTexto;
import construtrack.servicio.Notificador;
import construtrack.servicio.NotificadorBrevo;
import construtrack.servicio.NotificadorConsola;
import construtrack.servicio.NotificadorGmail;
import construtrack.servicio.Servicios;
import java.awt.GraphicsEnvironment;
import java.net.BindException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.TimeZone;
import javax.swing.SwingUtilities;

/**
 * Punto de entrada de ConstruTrack. Arma todas las piezas (composición de dependencias),
 * levanta el servidor web y abre la ventana de escritorio. Las dos vistas comparten el mismo modelo.
 *
 * Variables de entorno opcionales:
 *   PORT          puerto HTTP (por defecto 8080; los hostings como Render lo definen solos)
 *   DATOS         archivo de datos (por defecto datos_construtrack.txt)
 *   CORS_ORIGEN   dominio autorizado a usar la API (por defecto *)
 */
public final class App {

    private App() {
    }

    public static void main(String[] args) throws Exception {
        int puerto = Integer.parseInt(entorno("PORT", "8080"));
        // La hora de la obra, no la del servidor: Render y Docker usan UTC (4 h adelantado respecto a Venezuela)
        ZoneId zona = ZoneId.of(entorno("ZONA", "America/Caracas"));
        TimeZone.setDefault(TimeZone.getTimeZone(zona));
        Clock reloj = Clock.system(zona);

        FabricaIncidencias fabrica = FabricaIncidencias.conTiposPorDefecto();
        RepositorioTexto repositorio = new RepositorioTexto(almacen(), new GeneradorDatosPrueba(reloj, fabrica), fabrica);
        Plataforma plataforma = repositorio.cargar();
        Servicios servicios = new Servicios(plataforma, repositorio, reloj, repositorio.siguienteIdIncidencia(), notificador());

        ServidorWeb web;
        try {
            web = new ServidorWeb(servicios, puerto, entorno("CORS_ORIGEN", "*"));
        } catch (BindException e) {
            System.err.println("El puerto " + puerto + " ya está en uso. ¿Tienes ConstruTrack abierto en otra ventana?"
                    + " Ciérralo y vuelve a ejecutar, o define otro puerto con la variable de entorno PORT.");
            System.exit(1);
            return;
        }
        web.iniciar();
        System.out.println("ConstruTrack listo en " + web.getUrl()
                + " (" + plataforma.listarEmpresas().size() + " empresa(s), datos en " + repositorio.getAlmacen().descripcion()
                + ", correo " + (servicios.invitaciones().correoReal() ? "real activado" : "solo registrado en consola") + ")");

        // Vista de escritorio: se abre salvo que no haya pantalla (servidor) o se pida --sin-ventana
        boolean sinVentana = Arrays.asList(args).contains("--sin-ventana");
        if (!sinVentana && !GraphicsEnvironment.isHeadless()) {
            SwingUtilities.invokeLater(() -> new AplicacionSwing(servicios, web.getUrl()).iniciar());
        }
    }

    /**
     * Cómo se envían las invitaciones: desde Gmail con el Apps Script (GMAIL_SCRIPT_URL y GMAIL_SCRIPT_CLAVE),
     * con Brevo (BREVO_API_KEY y CORREO_REMITENTE) o, sin configuración (NetBeans), solo registradas en la consola.
     */
    private static Notificador notificador() {
        String sitio = entorno("URL_SITIO", "https://startling-toffee-914e34.netlify.app");
        String script = entorno("GMAIL_SCRIPT_URL", "");
        String claveScript = entorno("GMAIL_SCRIPT_CLAVE", "");
        if (!script.isEmpty() && !claveScript.isEmpty()) {
            return new NotificadorGmail(script, claveScript, sitio);
        }
        String clave = entorno("BREVO_API_KEY", "");
        String remitente = entorno("CORREO_REMITENTE", "");
        if (!clave.isEmpty() && !remitente.isEmpty()) {
            return new NotificadorBrevo(NotificadorBrevo.API, clave, remitente, sitio);
        }
        return new NotificadorConsola();
    }

    /**
     * Dónde se guardan los datos: en Upstash si están sus variables (servidores sin disco permanente, como Render),
     * o en un archivo local (NetBeans, terminal).
     */
    private static Almacen almacen() {
        String url = entorno("UPSTASH_REDIS_REST_URL", "");
        String token = entorno("UPSTASH_REDIS_REST_TOKEN", "");
        if (!url.isEmpty() && !token.isEmpty()) {
            return new AlmacenUpstash(url, token, entorno("UPSTASH_CLAVE", "construtrack:datos"));
        }
        return new AlmacenArchivo(Path.of(entorno("DATOS", "datos_construtrack.txt")));
    }

    private static String entorno(String nombre, String porDefecto) {
        String valor = System.getenv(nombre);
        return valor == null || valor.isBlank() ? porDefecto : valor;
    }
}
