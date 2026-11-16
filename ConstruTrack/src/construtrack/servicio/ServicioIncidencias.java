package construtrack.servicio;

import construtrack.modelo.Plataforma;
import construtrack.modelo.Empresa;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.NoEncontradoException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.modelo.incidencias.DatosIncidencia;
import construtrack.modelo.incidencias.EstadoIncidencia;
import construtrack.modelo.incidencias.FabricaIncidencias;
import construtrack.modelo.incidencias.Incidencia;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Casos de uso: reportar, listar y revisar incidencias (faltas, retardos, reposos, permisos). */
public class ServicioIncidencias extends ServicioBase {

    private final FabricaIncidencias fabrica;
    private int siguienteId;

    public ServicioIncidencias(Plataforma plataforma, EscritorDatos escritor, Clock reloj,
            FabricaIncidencias fabrica, int siguienteId) {
        super(plataforma, escritor, reloj);
        this.fabrica = fabrica;
        this.siguienteId = siguienteId;
    }

    /** Reporta una incidencia para el afectado. Quien reporta debe tener autoridad sobre él. */
    public Incidencia reportar(Usuario reporta, String codigoTipo, String cedulaAfectado,
            LocalDate desde, LocalDate hasta, String descripcion, Map<String, String> extras) {
        return modificar(() -> {
            Usuario afectado = buscar(reporta, cedulaAfectado);
            DatosIncidencia datos = new DatosIncidencia(siguienteId, afectado, reporta, desde, hasta,
                    descripcion, LocalDateTime.now(reloj), extras);
            Incidencia incidencia = fabrica.crear(codigoTipo, datos);
            reporta.reportar(incidencia);
            siguienteId++;
            return incidencia;
        });
    }

    /** Tipos de incidencia disponibles (código → nombre), para llenar listas desplegables. */
    public Map<String, String> tipos() {
        return fabrica.tipos();
    }

    /** Personas para las que el usuario puede reportar: él mismo, su equipo (supervisor) o todos (jefe). */
    public List<Usuario> personasReportables(Usuario actual) {
        return leer(() -> {
            List<Usuario> personas = new ArrayList<>();
            for (Usuario u : empresaDe(actual).listarUsuarios()) {
                if (actual.puedeReportarPara(u)) {
                    personas.add(u);
                }
            }
            return personas;
        });
    }

    /** Indica si el usuario tiene autoridad para revisar esa incidencia (y si todavía se puede revisar). */
    public boolean puedeRevisar(Usuario actual, Incidencia incidencia) {
        return actual instanceof construtrack.modelo.RevisorIncidencias r && r.puedeRevisar(incidencia)
                && !incidencia.getEstado().esFinal();
    }

    /** Incidencias que el usuario puede ver: las suyas, las de su equipo (supervisor) o todas (jefe). */
    public List<Incidencia> listarVisibles(Usuario actual) {
        return leer(() -> {
            List<Incidencia> visibles = new ArrayList<>();
            for (Usuario u : empresaDe(actual).listarUsuarios()) {
                if (puedeVer(actual, u)) {
                    visibles.addAll(u.getHistorial().getIncidencias());
                }
            }
            return visibles;
        });
    }

    /** Pendientes de revisión que el usuario tiene autoridad para resolver. */
    public List<Incidencia> pendientesDeRevisar(Usuario revisor) {
        return leer(() -> {
            List<Incidencia> pendientes = new ArrayList<>();
            for (Usuario u : empresaDe(revisor).listarUsuarios()) {
                for (Incidencia i : u.getHistorial().getIncidencias()) {
                    if (!i.getEstado().esFinal() && revisor instanceof construtrack.modelo.RevisorIncidencias r
                            && r.puedeRevisar(i)) {
                        pendientes.add(i);
                    }
                }
            }
            return pendientes;
        });
    }

    public void revisar(Usuario revisor, int id, EstadoIncidencia nuevo, String comentario) {
        modificar(() -> {
            if (!(revisor instanceof construtrack.modelo.RevisorIncidencias r)) {
                throw new PermisoDenegadoException("Tu rol no puede revisar incidencias.");
            }
            Incidencia incidencia = buscarIncidencia(revisor, id);
            r.revisar(incidencia, nuevo, comentario, LocalDateTime.now(reloj));
            return null;
        });
    }

    private boolean puedeVer(Usuario actual, Usuario afectado) {
        if (actual == afectado) {
            return true;
        }
        if (actual instanceof construtrack.modelo.Jefe) {
            return true;
        }
        return actual instanceof construtrack.modelo.Supervisor s && s.supervisa(afectado);
    }

    /** Busca solo en la empresa de quien hace la operación. */
    private Usuario buscar(Usuario actual, String cedula) {
        Usuario u = empresaDe(actual).buscarUsuario(cedula);
        if (u == null) {
            throw new NoEncontradoException("No existe un usuario con la cédula " + cedula + ".");
        }
        return u;
    }

    private Incidencia buscarIncidencia(Usuario actual, int id) {
        for (Usuario u : empresaDe(actual).listarUsuarios()) {
            for (Incidencia i : u.getHistorial().getIncidencias()) {
                if (i.getId() == id) {
                    return i;
                }
            }
        }
        throw new NoEncontradoException("No existe la incidencia #" + id + ".");
    }
}
