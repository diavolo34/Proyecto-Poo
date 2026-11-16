package construtrack.controlador.swing;

import construtrack.modelo.Cuenta;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.ConstruTrackException;
import construtrack.servicio.ServicioRegistro;
import construtrack.vista.swing.DialogoUnirse;
import java.util.function.Consumer;

public final class ControladoraUnirse {
    public ControladoraUnirse(DialogoUnirse vista, ServicioRegistro registro, Cuenta cuenta, Consumer<Usuario> alUnirse) {
        vista.getBtnMasTarde().addActionListener(e -> vista.dispose());
        vista.getBtnUnirse().addActionListener(e -> {
            try {
                Usuario usuario = registro.unirse(cuenta, vista.getTxtCodigo().getText());
                vista.dispose();
                alUnirse.accept(usuario);
            } catch (ConstruTrackException ex) {
                vista.mostrarError(ex.getMessage());
                vista.getTxtCodigo().selectAll();
                vista.getTxtCodigo().requestFocusInWindow();
            }
        });
    }
}
