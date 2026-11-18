package construtrack;

import construtrack.controlador.swing.AplicacionSwing;
import construtrack.controlador.web.ServidorWeb;
import construtrack.modelo.Plataforma;
import construtrack.modelo.incidencias.FabricaIncidencias;
import construtrack.persistencia.Almacen;
import construtrack.persistencia.AlmacenArchivo;
import construtrack.persistencia.AlmacenMemoria;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.SwingUtilities;

public final class App {
    private App() {
    }

    public static void main(String[] args) throws Exception {
        int puerto = Integer.parseInt(entorno("PORT", "8080"));

        ZoneId zona = ZoneId.of(entorno("ZONA", "America/Caracas"));
        TimeZone.setDefault(TimeZone.getTimeZone(zona));
        Clock reloj = Clock.system(zona);

        boolean modoDemo = Arrays.asList(args).contains("--demo") || "true".equalsIgnoreCase(entorno("DEMO", "false"));
        String claveDemo = entorno("DEMO_CLAVE", "");

        FabricaIncidencias fabrica = FabricaIncidencias.conTiposPorDefecto();
        RepositorioTexto repositorio = new RepositorioTexto(almacen(),
                modoDemo ? new GeneradorDatosPrueba(reloj, fabrica) : () -> null, fabrica);
        Plataforma plataforma = repositorio.cargar();
        ExecutorService correos = Executors.newSingleThreadExecutor(tarea -> {
            Thread hilo = new Thread(tarea, "envio-correos");
            hilo.setDaemon(true);
            return hilo;
        });
        Servicios servicios = new Servicios(plataforma, repositorio, reloj, repositorio.siguienteIdIncidencia(),
                notificador(puerto), correos);
        Servicios demo = claveDemo.isEmpty() ? null : demoPrivada(reloj, fabrica);

        ServidorWeb web;
        try {
            web = new ServidorWeb(servicios, modoDemo, demo, claveDemo, puerto, entorno("CORS_ORIGEN", "*"));
        } catch (BindException e) {
            System.err.println("El puerto " + puerto + " ya está en uso. ¿Tienes ConstruTrack abierto en otra ventana?"
                    + " Ciérralo y vuelve a ejecutar, o define otro puerto con la variable de entorno PORT.");
            System.exit(1);
            return;
        }
        web.iniciar();
        System.out.println("ConstruTrack listo en " + web.getUrl()
                + " (" + plataforma.listarEmpresas().size() + " empresa(s), datos en " + repositorio.getAlmacen().descripcion()
                + ", correo " + (servicios.invitaciones().correoReal() ? "real activado" : "solo registrado en consola")
                + ", " + (modoDemo ? "modo demostración" : "modo producción")
                + (demo == null ? "" : ", demo privada en /demo") + ")");

        boolean sinVentana = Arrays.asList(args).contains("--sin-ventana");
        if (!sinVentana && !GraphicsEnvironment.isHeadless()) {
            SwingUtilities.invokeLater(() -> new AplicacionSwing(servicios, web.getUrl(), modoDemo).iniciar());
        }
    }

    private static Servicios demoPrivada(Clock reloj, FabricaIncidencias fabrica) {
        RepositorioTexto memoria = new RepositorioTexto(new AlmacenMemoria(), new GeneradorDatosPrueba(reloj, fabrica), fabrica);
        Plataforma plataforma = memoria.cargar();
        return new Servicios(plataforma, memoria, reloj, memoria.siguienteIdIncidencia(), new NotificadorConsola());
    }

    private static Notificador notificador(int puerto) {
        String sitio = entorno("URL_SITIO", "http://localhost:" + puerto);
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
        return new NotificadorConsola(sitio);
    }

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
