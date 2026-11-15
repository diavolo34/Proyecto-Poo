package construtrack.controlador.swing;

import construtrack.modelo.Jefe;
import construtrack.modelo.RevisorIncidencias;
import construtrack.modelo.Usuario;
import construtrack.servicio.Servicios;
import construtrack.vista.swing.PanelAsistencia;
import construtrack.vista.swing.PanelIncidencias;
import construtrack.vista.swing.PanelPersonal;
import construtrack.vista.swing.PanelSupervision;
import construtrack.vista.swing.PanelTablero;
import construtrack.vista.swing.Tema;
import construtrack.vista.swing.VentanaLogin;
import construtrack.vista.swing.VentanaPrincipal;
import java.util.ArrayList;
import java.util.List;

/**
 * Coordina la vista de escritorio: muestra el inicio de sesión y, al entrar, arma la ventana
 * principal con las pestañas que corresponden al rol. Usa los mismos servicios que la web.
 */
public final class AplicacionSwing {

    private final Servicios servicios;
    private final String urlWeb;

    public AplicacionSwing(Servicios servicios, String urlWeb) {
        this.servicios = servicios;
        this.urlWeb = urlWeb;
    }

    public void iniciar() {
        Tema.instalar();
        mostrarLogin();
    }

    private void mostrarLogin() {
        VentanaLogin login = new VentanaLogin(servicios.autenticacion().getNombreEmpresa());
        new ControladoraLogin(login, servicios.autenticacion(), usuario -> {
            login.dispose();
            mostrarPrincipal(usuario);
        }, urlWeb);
        login.setVisible(true);
    }

    /** Arma la ventana principal. Lo que cada rol ve depende de las interfaces que implementa. */
    public VentanaPrincipal crearVentanaPrincipal(Usuario usuario) {
        VentanaPrincipal ventana = new VentanaPrincipal(servicios.autenticacion().getNombreEmpresa(),
                usuario.getNombre(), usuario.getRol());
        List<Runnable> refrescos = new ArrayList<>();
        Runnable refrescarTodo = () -> refrescos.forEach(Runnable::run);

        PanelAsistencia asistencia = new PanelAsistencia();
        refrescos.add(new ControladoraAsistencia(asistencia, servicios.asistencia(), usuario, refrescarTodo)::refrescar);
        ventana.agregarPestana("Mi asistencia", "Marca tu entrada y salida y revisa tu historial", asistencia);

        if (usuario instanceof RevisorIncidencias) {
            PanelSupervision supervision = new PanelSupervision();
            refrescos.add(new ControladoraSupervision(supervision, servicios.supervision(), usuario)::refrescar);
            ventana.agregarPestana("Supervisión", "Indicadores, alertas y reportes de tu equipo", supervision);

            PanelTablero tablero = new PanelTablero(ControladoraTablero.columnas());
            refrescos.add(new ControladoraTablero(tablero, servicios.incidencias(), usuario, refrescarTodo)::refrescar);
            ventana.agregarPestana("Tablero", "Incidencias por estado", tablero);
        }

        PanelIncidencias incidencias = new PanelIncidencias();
        refrescos.add(new ControladoraIncidencias(incidencias, servicios.incidencias(), usuario, refrescarTodo)::refrescar);
        ventana.agregarPestana("Incidencias", "Reporta faltas, retardos, reposos y permisos", incidencias);

        if (usuario instanceof RevisorIncidencias) {
            boolean esJefe = usuario instanceof Jefe;
            PanelPersonal personal = new PanelPersonal(esJefe ? "Personal" : "Mi equipo", esJefe);
            refrescos.add(new ControladoraPersonal(personal, servicios.personal(), usuario, refrescarTodo)::refrescar);
            ventana.agregarPestana(esJefe ? "Personal" : "Mi equipo", "Personas y su estado", personal);
        }

        ventana.getBtnSalir().addActionListener(e -> {
            ventana.dispose();
            mostrarLogin();
        });
        refrescarTodo.run();
        return ventana;
    }

    private void mostrarPrincipal(Usuario usuario) {
        crearVentanaPrincipal(usuario).setVisible(true);
    }
}
