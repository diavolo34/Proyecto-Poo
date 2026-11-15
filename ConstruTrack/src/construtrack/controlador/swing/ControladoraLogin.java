package construtrack.controlador.swing;

import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.ConstruTrackException;
import construtrack.servicio.ServicioAutenticacion;
import construtrack.vista.swing.VentanaLogin;
import java.awt.Desktop;
import java.net.URI;
import java.util.function.Consumer;
import javax.swing.JOptionPane;

/**
 * Controladora del inicio de sesión: recibe la vista por constructor, registra sus eventos
 * y delega la validación de credenciales en el servicio (la vista no conoce reglas de negocio).
 */
public final class ControladoraLogin {

    private final VentanaLogin vista;
    private final ServicioAutenticacion autenticacion;
    private final Consumer<Usuario> alEntrar;
    private final String urlWeb;

    public ControladoraLogin(VentanaLogin vista, ServicioAutenticacion autenticacion,
            Consumer<Usuario> alEntrar, String urlWeb) {
        this.vista = vista;
        this.autenticacion = autenticacion;
        this.alEntrar = alEntrar;
        this.urlWeb = urlWeb;
        vista.getBtnEntrar().addActionListener(e -> entrar());
        vista.getBtnWeb().addActionListener(e -> abrirWeb());
    }

    private void entrar() {
        String cedula = vista.getTxtCedula().getText().trim();
        String clave = new String(vista.getTxtClave().getPassword());
        if (cedula.isEmpty() || clave.isEmpty()) {
            mostrarError("Escribe tu cédula y tu clave.");
            (cedula.isEmpty() ? vista.getTxtCedula() : vista.getTxtClave()).requestFocusInWindow();
            return;
        }
        try {
            Usuario usuario = autenticacion.autenticar(cedula, clave);
            vista.getLblError().setText(" ");
            alEntrar.accept(usuario);
        } catch (ConstruTrackException e) {
            mostrarError(e.getMessage());
            vista.getTxtClave().selectAll();
            vista.getTxtClave().requestFocusInWindow();
        }
    }

    private void mostrarError(String mensaje) {
        vista.getLblError().setText("<html>" + mensaje + "</html>");
    }

    private void abrirWeb() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(urlWeb));
                return;
            }
        } catch (java.io.IOException | RuntimeException e) {
            // Si el sistema no permite abrir el navegador, se muestra la dirección para copiarla
        }
        JOptionPane.showMessageDialog(vista, "Abre esta dirección en tu navegador:\n" + urlWeb,
                "Versión web", JOptionPane.INFORMATION_MESSAGE);
    }
}
