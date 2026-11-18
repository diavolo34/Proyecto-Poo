package construtrack.controlador.swing;

import construtrack.modelo.Cuenta;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.ConstruTrackException;
import construtrack.servicio.ServicioRegistro;
import construtrack.vista.swing.DialogoCodigo;
import java.util.function.Consumer;

public final class ControladoraVerificacionCorreo {
    public ControladoraVerificacionCorreo(DialogoCodigo vista, ServicioRegistro registro, Cuenta cuenta,
            Consumer<Usuario> alConfirmar) {
        vista.getBtnCancelar().addActionListener(e -> vista.dispose());
        vista.getBtnSecundario().addActionListener(e -> {
            try {
                registro.reenviarCodigo(cuenta);
                vista.mostrarAviso("Te enviamos un código nuevo. Revisa tu correo y la carpeta de spam.");
            } catch (ConstruTrackException ex) {
                vista.mostrarError(ex.getMessage());
            }
        });
        vista.getBtnConfirmar().addActionListener(e -> {
            try {
                Usuario usuario = registro.confirmarCorreo(cuenta, vista.codigo());
                vista.dispose();
                alConfirmar.accept(usuario);
            } catch (ConstruTrackException ex) {
                vista.mostrarError(ex.getMessage());
            }
        });
    }
}
