package construtrack.servicio;

import construtrack.modelo.ArchivoAdjunto;
import construtrack.modelo.Empresa;
import construtrack.modelo.Gerente;
import construtrack.modelo.Plataforma;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.NoEncontradoException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.modelo.inventario.Asignacion;
import construtrack.modelo.talento.Egreso;
import construtrack.modelo.talento.Expediente;
import construtrack.modelo.talento.TipoEgreso;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ServicioTalento extends ServicioBase {
    public ServicioTalento(Plataforma plataforma, EscritorDatos escritor, Clock reloj) {
        super(plataforma, escritor, reloj);
    }

    public record Pendientes(Usuario persona, List<Asignacion> entregas) {
    }

    public List<Egreso> listar(Usuario actual) {
        Empresa empresa = comoGerente(actual);
        return leer(() -> empresa.listarEgresos().stream().sorted(Comparator.comparing(Egreso::getFechaRegistro).reversed()).toList());
    }

    public List<Usuario> activos(Usuario actual) {
        Empresa empresa = comoGerente(actual);
        return leer(() -> empresa.listarUsuarios().stream().filter(u -> !(u instanceof Gerente)).toList());
    }

    public Pendientes pendientes(Usuario actual, String cedula) {
        Empresa empresa = comoGerente(actual);
        return leer(() -> {
            Usuario persona = empresa.buscarUsuario(cedula);
            if (persona == null) {
                throw new NoEncontradoException("No existe una persona con la cédula " + cedula + ".");
            }
            return new Pendientes(persona, empresa.getInventario().abiertasDe(cedula));
        });
    }

    public Egreso registrarEgreso(Usuario actual, String cedula, Map<String, String> datos) {
        Empresa empresa = comoGerente(actual);
        LocalDate fecha = fecha(datos.get("fecha"));
        String devolvio = datos.get("devolvioEquipos");
        Egreso.Datos d = new Egreso.Datos(TipoEgreso.desdeTexto(datos.get("tipo")), fecha, datos.get("motivo"),
                datos.get("voucher"), datos.get("carta"), devolvio == null || devolvio.isBlank() ? null : Boolean.valueOf(devolvio),
                datos.get("notaEquipos"));
        return modificar(() -> {
            verificarArchivo(empresa, d.voucher(), "voucher de pago");
            verificarArchivo(empresa, d.carta(), "carta de renuncia");
            return ((Gerente) actual).registrarEgreso(cedula, d, LocalDateTime.now(reloj));
        });
    }

    public void eliminarFicha(Usuario actual, int id) {
        Empresa empresa = comoGerente(actual);
        modificar(() -> {
            Egreso egreso = empresa.buscarEgreso(id);
            String expediente = generar(empresa, egreso);
            ((Gerente) actual).eliminarFicha(id, expediente, LocalDateTime.now(reloj));
            return null;
        });
    }

    public String expediente(Usuario actual, int id) {
        Empresa empresa = comoGerente(actual);
        return leer(() -> {
            Egreso egreso = empresa.buscarEgreso(id);
            return egreso.fichaEliminada() ? egreso.getExpediente() : generar(empresa, egreso);
        });
    }

    private String generar(Empresa empresa, Egreso egreso) {
        return Expediente.de(empresa.buscarUsuario(egreso.getCedula()), egreso,
                empresa.getInventario().abiertasDe(egreso.getCedula()), LocalDate.now(reloj));
    }

    private static void verificarArchivo(Empresa empresa, String id, String que) {
        if (id == null || id.isBlank()) {
            return;
        }
        ArchivoAdjunto adjunto = empresa.buscarArchivo(id);
        if (adjunto == null) {
            throw new DatoInvalidoException("El archivo del " + que + " no es válido. Súbelo de nuevo.");
        }
    }

    private Empresa comoGerente(Usuario actual) {
        if (!(actual instanceof Gerente)) {
            throw new PermisoDenegadoException("Talento humano es exclusivo del gerente.");
        }
        return empresaDe(actual);
    }

    private static LocalDate fecha(String texto) {
        try {
            return texto == null || texto.isBlank() ? null : LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            throw new DatoInvalidoException("La fecha debe tener el formato AAAA-MM-DD.");
        }
    }
}
