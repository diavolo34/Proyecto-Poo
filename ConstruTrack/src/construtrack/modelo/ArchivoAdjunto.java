package construtrack.modelo;

import java.time.LocalDateTime;

public record ArchivoAdjunto(String id, String nombre, String tipo, long tamano, LocalDateTime fecha, Firma subidoPor,
        String cedulaPersona, boolean confidencial) {

    public boolean puedeVerlo(Usuario usuario) {
        if (usuario instanceof Gerente) {
            return true;
        }
        if (confidencial) {
            return false;
        }
        if (subidoPor.esDe(usuario) || usuario.getCedula().equals(cedulaPersona)) {
            return true;
        }
        return usuario instanceof Supervisor s && s.supervisa(usuario.getEmpresa().buscarUsuario(cedulaPersona));
    }
}
