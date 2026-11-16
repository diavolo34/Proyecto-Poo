package construtrack.controlador.swing;

import construtrack.modelo.Cuenta;
import construtrack.modelo.excepciones.ConstruTrackException;
import construtrack.servicio.ServicioRegistro;
import construtrack.vista.swing.DialogoRegistro;
import java.util.function.Consumer;

public final class ControladoraRegistro {
    public ControladoraRegistro(DialogoRegistro vista, ServicioRegistro registro, Consumer<Cuenta> alRegistrarse) {
        vista.getBtnCancelar().addActionListener(e -> vista.dispose());
        vista.getBtnCrear().addActionListener(e -> {
            try {
                Cuenta cuenta = registro.registrar(vista.datos());
                vista.dispose();
                alRegistrarse.accept(cuenta);
            } catch (ConstruTrackException ex) {
                vista.mostrarError(ex.getMessage());
            }
        });
    }
}
