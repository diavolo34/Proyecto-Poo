package construtrack.servicio;

import construtrack.modelo.Empresa;
import construtrack.modelo.Obra;
import construtrack.modelo.Plataforma;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.NoEncontradoException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.persistencia.EscritorDatos;
import construtrack.util.Totp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public class ServicioObras extends ServicioBase {
    public ServicioObras(Plataforma plataforma, EscritorDatos escritor, Clock reloj) {
        super(plataforma, escritor, reloj);
    }

    public record ObraConPersonas(Obra obra, List<Usuario> personas) {
    }

    public List<ObraConPersonas> listar(Usuario actual) {
        Empresa empresa = empresaDe(actual);
        return leer(() -> empresa.listarObras().stream()
                .map(o -> new ObraConPersonas(o, empresa.personasEn(o))).toList());
    }

    public Obra crear(Usuario actual, Map<String, String> datos) {
        verificarGestion(actual);
        Empresa empresa = empresaDe(actual);
        return modificar(() -> {
            Obra obra = new Obra(empresa.siguienteCodigoObra(), datos.get("nombre"), datos.get("ubicacion"),
                    fecha(datos.get("inicio"), "inicio"), fecha(datos.get("fin"), "fin estimado"));
            configurarControl(obra, datos);
            empresa.agregarObra(obra);
            return obra;
        });
    }

    public Obra editar(Usuario actual, String codigo, Map<String, String> datos) {
        verificarGestion(actual);
        return modificar(() -> {
            Obra obra = buscar(actual, codigo);
            obra.setNombre(datos.get("nombre"));
            obra.setUbicacion(datos.get("ubicacion"));
            obra.setFechas(fecha(datos.get("inicio"), "inicio"), fecha(datos.get("fin"), "fin estimado"));
            configurarControl(obra, datos);
            return obra;
        });
    }

    public void eliminar(Usuario actual, String codigo) {
        verificarGestion(actual);
        modificar(() -> {
            empresaDe(actual).eliminarObra(codigo);
            return null;
        });
    }

    public Usuario asignar(Usuario actual, String cedula, String codigoObra) {
        return modificar(() -> {
            Usuario persona = empresaDe(actual).buscarUsuario(cedula);
            if (persona == null) {
                throw new NoEncontradoException("No existe una persona con la cédula " + cedula + ".");
            }
            Obra obra = codigoObra == null || codigoObra.isBlank() ? null : buscar(actual, codigoObra);
            actual.asignarObra(persona, obra);
            return persona;
        });
    }

    public record CodigoObra(Obra obra, String codigo, long segundosRestantes) {
    }

    public CodigoObra codigoAsistencia(Usuario actual, String codigo) {
        verificarGestion(actual);
        Instant ahora = reloj.instant();
        return leer(() -> {
            Obra obra = buscar(actual, codigo);
            return new CodigoObra(obra, obra.codigoAsistencia(ahora), Totp.segundosRestantes(ahora));
        });
    }

    private static void configurarControl(Obra obra, Map<String, String> datos) {
        obra.configurarControl(numero(datos.get("latitud"), "latitud"), numero(datos.get("longitud"), "longitud"),
                datos.get("radio") == null || datos.get("radio").isBlank() ? null : (int) Math.round(numero(datos.get("radio"), "radio")),
                "true".equals(datos.get("exigeCodigo")));
    }

    private static Double numero(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Double.valueOf(texto.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new DatoInvalidoException("El campo " + campo + " debe ser un número.");
        }
    }

    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    private Obra buscar(Usuario actual, String codigo) {
        Obra obra = empresaDe(actual).buscarObra(codigo);
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
