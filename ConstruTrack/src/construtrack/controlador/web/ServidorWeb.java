package construtrack.controlador.web;

import com.sun.net.httpserver.HttpServer;
import construtrack.servicio.Servicios;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

/** Servidor HTTP nativo de Java: publica la API REST y los archivos de la vista web. */
public class ServidorWeb {

    private final HttpServer servidor;
    private final int puerto;

    public ServidorWeb(Servicios servicios, int puerto, String origenPermitido) throws IOException {
        this.puerto = puerto;
        GestorSesiones sesiones = new GestorSesiones(servicios.autenticacion());
        List<ControladorWeb> controladores = List.of(
                new ControladorSesion(servicios.autenticacion(), sesiones),
                new ControladorAsistencia(servicios.asistencia()),
                new ControladorUsuarios(servicios.personal()),
                new ControladorIncidencias(servicios.incidencias()),
                new ControladorSupervision(servicios.supervision()),
                new ControladorReportes(servicios.supervision()),
                new ControladorRegistro(servicios.registro(), sesiones),
                new ControladorInvitaciones(servicios.invitaciones()),
                new ControladorPublico(servicios.personal()));
        servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        servidor.createContext("/api/", new ManejadorApi(sesiones, controladores, origenPermitido));
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
