package construtrack.servicio;

import construtrack.modelo.Plataforma;
import construtrack.modelo.Empleado;
import construtrack.modelo.Empresa;
import construtrack.modelo.Gerente;
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
            if (actual instanceof Gerente) {
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
            } else if (!(actual instanceof Gerente) && usuario != actual) {
                throw new PermisoDenegadoException("No tienes permiso para ver a este usuario.");
            }
            return usuario;
        });
    }

    public List<Supervisor> listarSupervisores(Usuario actual) {
        return leer(() -> {
            List<Supervisor> supervisores = new ArrayList<>();
            for (Usuario u : empresaDe(actual).listarUsuarios()) {
                if (u instanceof Supervisor s && s.estaActivo()) {
                    supervisores.add(s);
                }
            }
            return supervisores;
        });
    }

    public record CifrasPublicas(int empresas, int personas, int presentes, int obras, int avancePromedio) {
    }

    public CifrasPublicas cifrasPublicas() {
        return leer(() -> {
            int personas = 0;
            int presentes = 0;
            int obras = 0;
            int avance = 0;
            java.time.LocalDate hoy = java.time.LocalDate.now(reloj);
            for (Empresa e : plataforma.listarEmpresas()) {
                personas += e.listarUsuarios().size();
                presentes += e.contarPresentes();
                for (Obra o : e.listarObras()) {
                    obras++;
                    avance += o.porcentajeTranscurrido(hoy);
                }
            }
            return new CifrasPublicas(plataforma.listarEmpresas().size(), personas, presentes, obras,
                    obras == 0 ? 0 : Math.round((float) avance / obras));
        });
    }

    public java.time.LocalDate hoy() {
        return java.time.LocalDate.now(reloj);
    }

    public List<Obra> listarObras(Usuario actual) {
        return leer(() -> empresaDe(actual).listarObras());
    }

    public List<Obra> listarObrasPublicas() {
        return leer(() -> plataforma.empresaDemo() == null ? List.<Obra>of() : plataforma.empresaDemo().listarObras());
    }

    public Usuario registrar(Usuario actual, Map<String, String> datos) {
        return modificar(() -> comoGerente(actual).registrarUsuario(datos));
    }

    public Usuario actualizar(Usuario actual, String cedula, Map<String, String> datos) {
        return modificar(() -> comoGerente(actual).modificarUsuario(cedula, datos));
    }

    private Usuario buscarExistente(Usuario actual, String cedula) {
        Usuario usuario = empresaDe(actual).buscarUsuario(cedula);
        if (usuario == null) {
            throw new NoEncontradoException("No existe un usuario con la cédula " + cedula + ".");
        }
        return usuario;
    }

    private Gerente comoGerente(Usuario actual) {
        if (actual instanceof Gerente gerente) {
            return gerente;
        }
        throw new PermisoDenegadoException("Solo el gerente registra o modifica al personal.");
    }
}
