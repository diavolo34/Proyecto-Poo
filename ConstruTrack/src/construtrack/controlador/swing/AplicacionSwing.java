package construtrack.controlador.swing;

import construtrack.modelo.Cuenta;
import construtrack.modelo.Jefe;
import construtrack.modelo.RevisorIncidencias;
import construtrack.modelo.Usuario;
import construtrack.servicio.Servicios;
import construtrack.vista.swing.DialogoRegistro;
import construtrack.vista.swing.DialogoUnirse;
import construtrack.vista.swing.PanelAsistencia;
import construtrack.vista.swing.PanelIncidencias;
import construtrack.vista.swing.PanelInvitaciones;
import construtrack.vista.swing.PanelPersonal;
import construtrack.vista.swing.PanelSupervision;
import construtrack.vista.swing.PanelTablero;
import construtrack.vista.swing.Tema;
import construtrack.vista.swing.VentanaLogin;
import construtrack.vista.swing.VentanaPrincipal;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class AplicacionSwing {
    private final Servicios servicios;
    private final String urlWeb;
    private final boolean modoDemo;

    public AplicacionSwing(Servicios servicios, String urlWeb) {
        this(servicios, urlWeb, false);
    }

    public AplicacionSwing(Servicios servicios, String urlWeb, boolean modoDemo) {
        this.servicios = servicios;
        this.urlWeb = urlWeb;
        this.modoDemo = modoDemo;
    }

    public void iniciar() {
        Tema.instalar();
        mostrarLogin();
    }

    private void mostrarLogin() {
        VentanaLogin login = new VentanaLogin(servicios.autenticacion().getNombreEmpresa(), modoDemo);
        Consumer<Usuario> entrar = usuario -> {
            login.dispose();
            mostrarPrincipal(usuario);
        };
        new ControladoraLogin(login, servicios.autenticacion(), entrar,
                cuenta -> mostrarUnirse(login, cuenta, entrar),
                () -> mostrarRegistro(login, entrar), urlWeb);
        login.setVisible(true);
    }

    private void mostrarRegistro(Window dueno, Consumer<Usuario> entrar) {
        DialogoRegistro registro = new DialogoRegistro(dueno);
        new ControladoraRegistro(registro, servicios.registro(), cuenta -> {
            if (cuenta.perteneceAEmpresa()) {
                entrar.accept(cuenta.getUsuario());
            } else {
                mostrarUnirse(dueno, cuenta, entrar);
            }
        });
        registro.setVisible(true);
    }

    private void mostrarUnirse(Window dueno, Cuenta cuenta, Consumer<Usuario> entrar) {
        DialogoUnirse unirse = new DialogoUnirse(dueno, cuenta.getNombre());
        new ControladoraUnirse(unirse, servicios.registro(), cuenta, entrar);
        unirse.setVisible(true);
    }

    public VentanaPrincipal crearVentanaPrincipal(Usuario usuario) {
        VentanaPrincipal ventana = new VentanaPrincipal(usuario.getEmpresa().getNombre(),
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

        if (usuario instanceof Jefe) {
            PanelInvitaciones invitaciones = new PanelInvitaciones();
            refrescos.add(new ControladoraInvitaciones(invitaciones, servicios.invitaciones(), servicios.personal(),
                    usuario)::refrescar);
            ventana.agregarPestana("Invitaciones", "Códigos para que nuevas personas se unan", invitaciones);
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
