package construtrack.controlador.swing;

import construtrack.modelo.Cuenta;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.ConstruTrackException;
import construtrack.servicio.Ingreso;
import construtrack.servicio.ServicioAutenticacion;
import construtrack.vista.swing.VentanaLogin;
import java.awt.Desktop;
import java.net.URI;
import java.util.function.Consumer;
import javax.swing.JOptionPane;

public final class ControladoraLogin {
    private final VentanaLogin vista;
    private final ServicioAutenticacion autenticacion;
    private final Consumer<Usuario> alEntrar;
    private final Consumer<Cuenta> alFaltarEmpresa;
    private final String urlWeb;

    public ControladoraLogin(VentanaLogin vista, ServicioAutenticacion autenticacion, Consumer<Usuario> alEntrar,
            Consumer<Cuenta> alFaltarEmpresa, Runnable alCrearCuenta, String urlWeb) {
        this.vista = vista;
        this.autenticacion = autenticacion;
        this.alEntrar = alEntrar;
        this.alFaltarEmpresa = alFaltarEmpresa;
        this.urlWeb = urlWeb;
        vista.getBtnEntrar().addActionListener(e -> entrar());
        vista.getBtnCrearCuenta().addActionListener(e -> alCrearCuenta.run());
        vista.getBtnWeb().addActionListener(e -> abrirWeb());
    }

    private void entrar() {
        String identificador = vista.getTxtIdentificador().getText().trim();
        String clave = new String(vista.getTxtClave().getPassword());
        if (identificador.isEmpty() || clave.isEmpty()) {
            mostrarError("Escribe tu correo o cédula y tu clave.");
            (identificador.isEmpty() ? vista.getTxtIdentificador() : vista.getTxtClave()).requestFocusInWindow();
            return;
        }
        try {
            Ingreso ingreso = autenticacion.ingresar(identificador, clave);
            vista.getLblError().setText(" ");
            if (ingreso.pendienteDeUnirse()) {
                alFaltarEmpresa.accept(ingreso.cuenta());
            } else {
                alEntrar.accept(ingreso.usuario());
            }
        } catch (ConstruTrackException e) {
            mostrarError(e.getMessage());
            vista.getTxtClave().selectAll();
            vista.getTxtClave().requestFocusInWindow();
        }
    }

    private void mostrarError(String mensaje) {
        vista.getLblError().setText("<html><div style='width:300px'>" + mensaje + "</div></html>");
    }

    private void abrirWeb() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(urlWeb));
                return;
            }
        } catch (java.io.IOException | RuntimeException e) {
        }
        JOptionPane.showMessageDialog(vista, "Abre esta dirección en tu navegador:\n" + urlWeb,
                "Versión web", JOptionPane.INFORMATION_MESSAGE);
    }
}
