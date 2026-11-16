package construtrack.servicio;

import construtrack.modelo.Plataforma;
import construtrack.modelo.Empleado;
import construtrack.modelo.Empresa;
import construtrack.modelo.Jefe;
import construtrack.modelo.Obra;
import construtrack.modelo.Supervisor;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.NoEncontradoException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServicioPersonal extends ServicioBase {
    public ServicioPersonal(Plataforma plataforma, EscritorDatos escritor, Clock reloj) {
        super(plataforma, escritor, reloj);
    }

    public List<Usuario> listarVisibles(Usuario actual) {
        return leer(() -> {
            if (actual instanceof Jefe) {
                return empresaDe(actual).listarUsuarios();
            }
            if (actual instanceof Supervisor supervisor) {
                return new ArrayList<Usuario>(supervisor.verEmpleadosACargo());
            }
            throw new PermisoDenegadoException("Tu rol no permite ver la lista de personal.");
        });
    }

    public Usuario ver(Usuario actual, String cedula) {
        return leer(() -> {
            Usuario usuario = buscarExistente(actual, cedula);
            if (actual instanceof Supervisor supervisor && usuario instanceof Empleado empleado) {
                supervisor.consultarAsistencia(empleado);
            } else if (!(actual instanceof Jefe) && usuario != actual) {
                throw new PermisoDenegadoException("No tienes permiso para ver a este usuario.");
            }
            return usuario;
        });
    }

    public List<Supervisor> listarSupervisores(Usuario actual) {
        return leer(() -> {
            List<Supervisor> supervisores = new ArrayList<>();
            for (Usuario u : empresaDe(actual).listarUsuarios()) {
                if (u instanceof Supervisor s) {
                    supervisores.add(s);
                }
            }
            return supervisores;
        });
    }

    public int[] contarPersonalYPresentes() {
        return leer(() -> {
            Empresa demo = plataforma.empresaDemo();
            return new int[] {demo == null ? 0 : demo.listarUsuarios().size(), demo == null ? 0 : demo.contarPresentes(),
                plataforma.listarEmpresas().size()};
        });
    }

    public java.time.LocalDate hoy() {
        return java.time.LocalDate.now(reloj);
    }

    public List<Obra> listarObras(Usuario actual) {
        return leer(() -> empresaDe(actual).listarObras());
    }

    public List<Obra> listarObrasDemo() {
        return leer(() -> plataforma.empresaDemo() == null ? List.<Obra>of() : plataforma.empresaDemo().listarObras());
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

    private Usuario buscarExistente(Usuario actual, String cedula) {
        Usuario usuario = empresaDe(actual).buscarUsuario(cedula);
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
