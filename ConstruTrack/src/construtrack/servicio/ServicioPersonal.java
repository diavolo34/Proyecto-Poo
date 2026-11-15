package construtrack.servicio;

import construtrack.modelo.Empleado;
import construtrack.modelo.Empresa;
import construtrack.modelo.Jefe;
import construtrack.modelo.Supervisor;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.NoEncontradoException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Casos de uso sobre el personal: consultar, registrar, modificar y eliminar. */
public class ServicioPersonal extends ServicioBase {

    public ServicioPersonal(Empresa empresa, EscritorDatos escritor, Clock reloj) {
        super(empresa, escritor, reloj);
    }

    /** El Jefe ve a todo el personal; el Supervisor solo a sus empleados; el Empleado no tiene acceso. */
    public List<Usuario> listarVisibles(Usuario actual) {
        return leer(() -> {
            if (actual instanceof Jefe) {
                return empresa.listarUsuarios();
            }
            if (actual instanceof Supervisor supervisor) {
                return new ArrayList<Usuario>(supervisor.verEmpleadosACargo());
            }
            throw new PermisoDenegadoException("Tu rol no permite ver la lista de personal.");
        });
    }

    /** Detalle de un usuario, respetando los permisos de quien consulta. */
    public Usuario ver(Usuario actual, String cedula) {
        return leer(() -> {
            Usuario usuario = buscarExistente(cedula);
            if (actual instanceof Supervisor supervisor && usuario instanceof Empleado empleado) {
                supervisor.consultarAsistencia(empleado); // lanza PermisoDenegado si no es su empleado
            } else if (!(actual instanceof Jefe) && usuario != actual) {
                throw new PermisoDenegadoException("No tienes permiso para ver a este usuario.");
            }
            return usuario;
        });
    }

    public List<Supervisor> listarSupervisores() {
        return leer(() -> {
            List<Supervisor> supervisores = new ArrayList<>();
            for (Usuario u : empresa.listarUsuarios()) {
                if (u instanceof Supervisor s) {
                    supervisores.add(s);
                }
            }
            return supervisores;
        });
    }

    public Usuario registrar(Usuario actual, Map<String, String> datos) {
        return modificar(() -> comoJefe(actual).registrarUsuario(datos));
    }

    public Usuario actualizar(Usuario actual, String cedula, Map<String, String> datos) {
        return modificar(() -> comoJefe(actual).modificarUsuario(cedula, datos));
    }

    public void eliminar(Usuario actual, String cedula) {
        modificar(() -> {
            comoJefe(actual).eliminarUsuario(cedula);
            return null;
        });
    }

    private Usuario buscarExistente(String cedula) {
        Usuario usuario = empresa.buscarUsuario(cedula);
        if (usuario == null) {
            throw new NoEncontradoException("No existe un usuario con la cédula " + cedula + ".");
        }
        return usuario;
    }

    private Jefe comoJefe(Usuario actual) {
        if (actual instanceof Jefe jefe) {
            return jefe;
        }
        throw new PermisoDenegadoException("Solo el Jefe puede registrar, modificar o eliminar usuarios.");
    }
}
