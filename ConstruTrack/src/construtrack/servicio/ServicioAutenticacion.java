package construtrack.servicio;

import construtrack.modelo.Empresa;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.CredencialesInvalidasException;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;

/** Caso de uso: iniciar sesión con cédula y clave. */
public class ServicioAutenticacion extends ServicioBase {

    public ServicioAutenticacion(Empresa empresa, EscritorDatos escritor, Clock reloj) {
        super(empresa, escritor, reloj);
    }

    public Usuario autenticar(String cedula, String clave) {
        return leer(() -> {
            Usuario usuario = empresa.autenticar(cedula, clave);
            if (usuario == null) {
                throw new CredencialesInvalidasException();
            }
            return usuario;
        });
    }

    public Usuario buscar(String cedula) {
        return leer(() -> empresa.buscarUsuario(cedula));
    }

    public String getNombreEmpresa() {
        return empresa.getNombre();
    }
}
