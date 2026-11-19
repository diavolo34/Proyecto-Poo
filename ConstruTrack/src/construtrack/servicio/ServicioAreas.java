package construtrack.servicio;

import construtrack.modelo.Area;
import construtrack.modelo.Empresa;
import construtrack.modelo.Gerente;
import construtrack.modelo.Plataforma;
import construtrack.modelo.Supervisor;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.NoEncontradoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;
import java.util.List;

public class ServicioAreas extends ServicioBase {
    public ServicioAreas(Plataforma plataforma, EscritorDatos escritor, Clock reloj) {
        super(plataforma, escritor, reloj);
    }

    public record AreaConIntegrantes(Area area, List<Usuario> integrantes) {
    }

    public record Vista(List<AreaConIntegrantes> areas, List<Usuario> sinArea, List<Supervisor> supervisores) {
    }

    public Vista ver(Usuario actual) {
        if (!(actual instanceof Gerente) && !(actual instanceof Supervisor)) {
            throw new PermisoDenegadoException("Las áreas las gestionan el gerente y los supervisores.");
        }
        return leer(() -> {
            Empresa empresa = empresaDe(actual);
            List<Area> visibles = actual instanceof Gerente ? empresa.listarAreas() : empresa.areasDirigidasPor(actual);
            List<AreaConIntegrantes> areas = visibles.stream().map(a -> new AreaConIntegrantes(a, empresa.integrantes(a))).toList();
            List<Supervisor> supervisores = empresa.listarUsuarios().stream()
                    .filter(u -> u instanceof Supervisor).map(u -> (Supervisor) u).toList();
            return new Vista(areas, empresa.sinArea(), supervisores);
        });
    }

    public Area crear(Usuario actual, String nombre, String cedulaSupervisor) {
        Empresa empresa = comoGerente(actual);
        return modificar(() -> empresa.crearArea(nombre, persona(empresa, cedulaSupervisor, "el supervisor")));
    }

    public void cambiarSupervisor(Usuario actual, String nombre, String cedulaSupervisor) {
        Empresa empresa = comoGerente(actual);
        modificar(() -> {
            empresa.cambiarSupervisorDeArea(nombre, persona(empresa, cedulaSupervisor, "el supervisor"));
            return null;
        });
    }

    public List<Usuario> eliminar(Usuario actual, String nombre) {
        Empresa empresa = comoGerente(actual);
        return modificar(() -> empresa.eliminarArea(nombre));
    }

    public Usuario incluir(Usuario actual, String cedula, String nombre) {
        return modificar(() -> {
            Empresa empresa = empresaDe(actual);
            Usuario persona = persona(empresa, cedula, "la persona");
            Area area = empresa.buscarArea(nombre);
            if (area == null) {
                throw new DatoInvalidoException("No existe el área «" + (nombre == null ? "" : nombre) + "».");
            }
            if (persona instanceof Gerente) {
                throw new OperacionInvalidaException("El gerente está por encima de todas las áreas: no se incluye en ninguna.");
            }
            if (!actual.puedeIncluirEnArea(persona, area)) {
                throw new PermisoDenegadoException(actual instanceof Supervisor
                        ? "Solo puedes sumar a tu área a personas que todavía no tienen una. Para moverla, habla con el gerente."
                        : "No puedes incluir a esta persona en un área.");
            }
            empresa.incluirEnArea(persona, area.getNombre());
            return persona;
        });
    }

    public Usuario sacar(Usuario actual, String cedula) {
        Empresa empresa = comoGerente(actual);
        return modificar(() -> {
            Usuario persona = persona(empresa, cedula, "la persona");
            empresa.sacarDeArea(persona);
            return persona;
        });
    }

    private static Usuario persona(Empresa empresa, String cedula, String que) {
        Usuario u = empresa.buscarUsuario(cedula);
        if (u == null || !u.estaActivo()) {
            throw new NoEncontradoException("Elige " + que + " entre el personal activo de la empresa.");
        }
        return u;
    }

    private Empresa comoGerente(Usuario actual) {
        if (!(actual instanceof Gerente)) {
            throw new PermisoDenegadoException("Solo el gerente crea, modifica o elimina áreas y saca personas de ellas.");
        }
        return empresaDe(actual);
    }
}
