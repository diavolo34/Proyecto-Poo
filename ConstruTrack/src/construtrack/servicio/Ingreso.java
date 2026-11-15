package construtrack.servicio;

import construtrack.modelo.Cuenta;
import construtrack.modelo.Usuario;

/**
 * Resultado de iniciar sesión: la cuenta personal (si existe) y la ficha en la empresa (si ya se unió).
 * Si no hay ficha, la persona debe canjear una invitación antes de usar el sistema.
 */
public record Ingreso(Cuenta cuenta, Usuario usuario) {

    public boolean pendienteDeUnirse() {
        return usuario == null;
    }
}
