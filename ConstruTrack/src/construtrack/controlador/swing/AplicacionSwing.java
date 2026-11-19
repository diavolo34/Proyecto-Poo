package construtrack.controlador.swing;

import construtrack.modelo.Cuenta;
import construtrack.modelo.Empleado;
import construtrack.modelo.Gerente;
import construtrack.modelo.Supervisor;
import construtrack.modelo.Usuario;
import construtrack.modelo.novedades.Categoria;
import construtrack.servicio.Servicios;
import construtrack.vista.swing.DialogoCodigo;
import construtrack.vista.swing.DialogoRegistro;
import construtrack.vista.swing.DialogoUnirse;
import construtrack.vista.swing.PanelAreas;
import construtrack.vista.swing.PanelAsistencia;
import construtrack.vista.swing.PanelCatalogo;
import construtrack.vista.swing.PanelInventario;
import construtrack.vista.swing.PanelInvitaciones;
import construtrack.vista.swing.PanelNovedades;
import construtrack.vista.swing.PanelObras;
import construtrack.vista.swing.PanelPersonal;
import construtrack.vista.swing.PanelSupervision;
import construtrack.vista.swing.PanelTablero;
import construtrack.vista.swing.PanelTalento;
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
        new ControladoraLogin(login, servicios.autenticacion(), servicios.dosPasos(), entrar,
                cuenta -> continuarCuenta(login, cuenta, entrar),
                () -> mostrarRegistro(login, entrar), urlWeb);
        login.setVisible(true);
    }

    private void mostrarRegistro(Window dueno, Consumer<Usuario> entrar) {
        DialogoRegistro registro = new DialogoRegistro(dueno);
        new ControladoraRegistro(registro, servicios.registro(), cuenta -> continuarCuenta(dueno, cuenta, entrar));
        registro.setVisible(true);
    }

    private void continuarCuenta(Window dueno, Cuenta cuenta, Consumer<Usuario> entrar) {
        if (cuenta.perteneceAEmpresa()) {
            entrar.accept(cuenta.getUsuario());
        } else if (!cuenta.estaVerificada()) {
            mostrarVerificacion(dueno, cuenta, entrar);
        } else {
            mostrarUnirse(dueno, cuenta, entrar);
        }
    }

    private void mostrarVerificacion(Window dueno, Cuenta cuenta, Consumer<Usuario> entrar) {
        DialogoCodigo dialogo = new DialogoCodigo(dueno, "Confirma tu correo", "Te enviamos un código de 6 dígitos a "
                + cuenta.getCorreo() + ". Escríbelo para activar tu cuenta. Si no te llega, revisa la carpeta de spam.",
                "Código de 6 dígitos *", "Confirmar correo", "Reenviar código");
        new ControladoraVerificacionCorreo(dialogo, servicios.registro(), cuenta, usuario -> {
            if (usuario != null) {
                entrar.accept(usuario);
            } else {
                mostrarUnirse(dueno, cuenta, entrar);
            }
        });
        dialogo.setVisible(true);
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
        ventana.agregarPestana("Asistencia", "Marca tu entrada y salida y revisa tu historial", asistencia);

        boolean esGerente = usuario instanceof Gerente;
        boolean esLider = !(usuario instanceof Empleado);
        PanelNovedades permisos = new PanelNovedades("Permisos y reposos", "El supervisor los registra; el gerente los aprueba o "
                + "rechaza con una nota. Un reposo aprobado cuenta como «Incapacidad» y bloquea la marca mientras dure.",
                ControladoraNovedades.puedeRegistrar(servicios.novedades(), usuario), false, esGerente);
        refrescos.add(new ControladoraNovedades(permisos, servicios.novedades(), servicios.archivos(), usuario, Categoria.LICENCIA,
                refrescarTodo)::refrescar);
        ventana.agregarPestana("Permisos y reposos", "Permisos laborales y reposos médicos", permisos);

        if (esLider) {
            PanelNovedades reportes = new PanelNovedades("Reportes de proyecto", "Retardos, inasistencias y accidentes. Los registra el "
                    + "supervisor y el gerente los aprueba o rechaza con una nota obligatoria.",
                    ControladoraNovedades.puedeRegistrar(servicios.novedades(), usuario), true, esGerente);
            refrescos.add(new ControladoraNovedades(reportes, servicios.novedades(), servicios.archivos(), usuario,
                    Categoria.REPORTE_OBRA, refrescarTodo)::refrescar);
            ventana.agregarPestana("Reportes de proyecto", "Lo que pasa en cada proyecto", reportes);

            PanelTablero tablero = new PanelTablero(ControladoraTablero.columnas());
            refrescos.add(new ControladoraTablero(tablero, servicios.novedades(), usuario, refrescarTodo)::refrescar);
            ventana.agregarPestana("Panel de control", "Permisos y reportes por estado", tablero);

            PanelSupervision supervision = new PanelSupervision();
            refrescos.add(new ControladoraSupervision(supervision, servicios.supervision(), usuario)::refrescar);
            ventana.agregarPestana("Supervisión", "Indicadores, alertas y reportes del personal", supervision);

            PanelObras obras = new PanelObras(usuario instanceof Supervisor, esGerente);
            refrescos.add(new ControladoraObras(obras, servicios.obras(), usuario, refrescarTodo)::refrescar);
            ventana.agregarPestana("Proyectos", "Inicio, conclusión y retrasos de cada proyecto", obras);

            PanelAreas areas = new PanelAreas(esGerente);
            refrescos.add(new ControladoraAreas(areas, servicios.areas(), usuario, refrescarTodo)::refrescar);
            ventana.agregarPestana("Áreas", "Áreas, su supervisor y sus integrantes", areas);

            PanelPersonal personal = new PanelPersonal("Personal", esGerente);
            refrescos.add(new ControladoraPersonal(personal, servicios.personal(), usuario, refrescarTodo)::refrescar);
            ventana.agregarPestana("Personal", "Personas, puestos, áreas y su estado", personal);
        }

        if (esGerente) {
            PanelTalento talento = new PanelTalento();
            refrescos.add(new ControladoraTalento(talento, servicios.talento(), servicios.archivos(), usuario, refrescarTodo)::refrescar);
            ventana.agregarPestana("Talento humano", "Despidos, renuncias y devolución de equipos", talento);

            PanelInventario inventario = new PanelInventario();
            refrescos.add(new ControladoraInventario(inventario, servicios.inventario(), servicios.personal(), usuario,
                    refrescarTodo)::refrescar);
            ventana.agregarPestana("Inventario", "Herramientas, equipos y materiales", inventario);

            PanelCatalogo catalogo = new PanelCatalogo();
            refrescos.add(new ControladoraCatalogo(catalogo, servicios.catalogo(), usuario, refrescarTodo)::refrescar);
            ventana.agregarPestana("Puestos", "Catálogo de cargos", catalogo);

            PanelInvitaciones invitaciones = new PanelInvitaciones();
            refrescos.add(new ControladoraInvitaciones(invitaciones, servicios.invitaciones(), servicios.personal(),
                    usuario)::refrescar);
            ventana.agregarPestana("Registro de personal", "Códigos de registro para que nuevas personas se unan", invitaciones);
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
