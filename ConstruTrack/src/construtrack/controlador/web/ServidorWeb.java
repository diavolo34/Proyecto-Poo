package construtrack.controlador.web;

import com.sun.net.httpserver.HttpServer;
import construtrack.servicio.Servicios;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

public class ServidorWeb {
    private final HttpServer servidor;
    private final int puerto;

    public ServidorWeb(Servicios servicios, int puerto, String origenPermitido) throws IOException {
        this(servicios, false, null, "", puerto, origenPermitido);
    }

    public ServidorWeb(Servicios servicios, boolean modoDemo, Servicios demo, String claveDemo, int puerto,
            String origenPermitido) throws IOException {
        this.puerto = puerto;
        boolean demoPrivada = demo != null && claveDemo != null && !claveDemo.isBlank();
        GestorSesiones sesiones = new GestorSesiones(servicios.autenticacion());
        List<ControladorWeb> controladores = List.of(
                new ControladorSesion(servicios.autenticacion(), sesiones, modoDemo, demoPrivada, true),
                new ControladorAsistencia(servicios.asistencia()),
                new ControladorUsuarios(servicios.personal()),
                new ControladorIncidencias(servicios.incidencias()),
                new ControladorSupervision(servicios.supervision()),
                new ControladorReportes(servicios.supervision()),
                new ControladorRegistro(servicios.registro(), sesiones),
                new ControladorRecuperacion(servicios.recuperacion(), servicios.autenticacion(), sesiones),
                new ControladorInvitaciones(servicios.invitaciones()),
                new ControladorObras(servicios.obras()),
                new ControladorPublico(servicios.personal(), modoDemo));
        servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        servidor.createContext("/api/", new ManejadorApi(sesiones, controladores, origenPermitido));
        if (demoPrivada) {
            GestorSesiones sesionesDemo = new GestorSesiones(demo.autenticacion());
            List<ControladorWeb> controladoresDemo = List.of(
                    new ControladorSesion(demo.autenticacion(), sesionesDemo, true, true, false),
                    new ControladorDemo(demo.autenticacion(), sesionesDemo, claveDemo),
                    new ControladorAsistencia(demo.asistencia()),
                    new ControladorUsuarios(demo.personal()),
                    new ControladorIncidencias(demo.incidencias()),
                    new ControladorSupervision(demo.supervision()),
                    new ControladorReportes(demo.supervision()),
                    new ControladorInvitaciones(demo.invitaciones()),
                    new ControladorObras(demo.obras()),
                    new ControladorPublico(demo.personal(), true));
            servidor.createContext("/demo/api/", new ManejadorApi(sesionesDemo, controladoresDemo, origenPermitido, "/demo"));
        }
        servidor.createContext("/", new ArchivosEstaticos());
        servidor.setExecutor(Executors.newFixedThreadPool(8));
    }

    public void iniciar() {
        servidor.start();
    }

    public void detener() {
        servidor.stop(0);
    }

    public String getUrl() {
        return "http://localhost:" + puerto;
    }
}
