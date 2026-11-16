package construtrack.servicio;

import construtrack.modelo.Empresa;
import construtrack.modelo.Obra;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.NoEncontradoException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Casos de uso de las obras (los lugares de trabajo): las gestionan el Supervisor y el Jefe.
 * Los permisos los decide el modelo (Usuario.puedeGestionarObras / puedeAsignarObraA).
 */
public class ServicioObras extends ServicioBase {

    public ServicioObras(Empresa empresa, EscritorDatos escritor, Clock reloj) {
        super(empresa, escritor, reloj);
    }

    /** Una obra con las personas asignadas, para mostrarla en la vista. */
    public record ObraConPersonas(Obra obra, List<Usuario> personas) {
    }

    public List<ObraConPersonas> listar(Usuario actual) {
        return leer(() -> empresa.listarObras().stream()
                .map(o -> new ObraConPersonas(o, empresa.personasEn(o))).toList());
    }

    /** Datos: nombre, ubicacion, inicio, fin (AAAA-MM-DD). */
    public Obra crear(Usuario actual, Map<String, String> datos) {
        verificarGestion(actual);
        return modificar(() -> {
            Obra obra = new Obra(empresa.siguienteCodigoObra(), datos.get("nombre"), datos.get("ubicacion"),
                    fecha(datos.get("inicio"), "inicio"), fecha(datos.get("fin"), "fin estimado"));
            empresa.agregarObra(obra);
            return obra;
        });
    }

    public Obra editar(Usuario actual, String codigo, Map<String, String> datos) {
        verificarGestion(actual);
        return modificar(() -> {
            Obra obra = buscar(codigo);
            obra.setNombre(datos.get("nombre"));
            obra.setUbicacion(datos.get("ubicacion"));
            obra.setFechas(fecha(datos.get("inicio"), "inicio"), fecha(datos.get("fin"), "fin estimado"));
            return obra;
        });
    }

    public void eliminar(Usuario actual, String codigo) {
        verificarGestion(actual);
        modificar(() -> {
            empresa.eliminarObra(codigo);
            return null;
        });
    }

    /** Asigna una persona a una obra; con código vacío la deja sin obra. */
    public Usuario asignar(Usuario actual, String cedula, String codigoObra) {
        return modificar(() -> {
            Usuario persona = empresa.buscarUsuario(cedula);
            if (persona == null) {
                throw new NoEncontradoException("No existe una persona con la cédula " + cedula + ".");
            }
            Obra obra = codigoObra == null || codigoObra.isBlank() ? null : buscar(codigoObra);
            actual.asignarObra(persona, obra);
            return persona;
        });
    }

    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    private Obra buscar(String codigo) {
        Obra obra = empresa.buscarObra(codigo);
        if (obra == null) {
            throw new NoEncontradoException("No existe la obra " + codigo + ".");
        }
        return obra;
    }

    private static void verificarGestion(Usuario actual) {
        if (!actual.puedeGestionarObras()) {
            throw new PermisoDenegadoException("Solo el Supervisor y el Jefe pueden gestionar obras.");
        }
    }

    private static LocalDate fecha(String texto, String campo) {
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new DatoInvalidoException("Indica la fecha de " + campo + " de la obra.");
        }
    }
}
