package construtrack.servicio;

import construtrack.modelo.Cuenta;
import construtrack.modelo.Usuario;

public record Ingreso(Cuenta cuenta, Usuario usuario) {
    public boolean pendienteDeUnirse() {
        return usuario == null;
    }
}
