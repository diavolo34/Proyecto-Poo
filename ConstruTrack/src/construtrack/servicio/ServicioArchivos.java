package construtrack.servicio;

import construtrack.modelo.ArchivoAdjunto;
import construtrack.modelo.Empleado;
import construtrack.modelo.Firma;
import construtrack.modelo.Gerente;
import construtrack.modelo.Plataforma;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.NoEncontradoException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.persistencia.AlmacenArchivos;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;
import java.time.LocalDateTime;

public class ServicioArchivos extends ServicioBase {
    public static final int TAMANO_MAXIMO = 5 * 1024 * 1024;

    private final AlmacenArchivos almacen;

    public record Descarga(String nombre, String tipo, byte[] datos) {
    }

    public ServicioArchivos(Plataforma plataforma, EscritorDatos escritor, Clock reloj, AlmacenArchivos almacen) {
        super(plataforma, escritor, reloj);
        this.almacen = almacen;
    }

    public ArchivoAdjunto subir(Usuario actual, String nombre, byte[] datos, String cedulaPersona, boolean confidencial) {
        if (actual instanceof Empleado) {
            throw new PermisoDenegadoException("Los documentos los adjunta el supervisor o el gerente.");
        }
        if (datos == null || datos.length == 0) {
            throw new DatoInvalidoException("El archivo está vacío.");
        }
        if (datos.length > TAMANO_MAXIMO) {
            throw new DatoInvalidoException("El archivo es demasiado grande: el máximo es 5 MB.");
        }
        String tipo = tipoReal(datos);
        String limpio = limpiarNombre(nombre, tipo);
        leer(() -> {
            if (cedulaPersona != null && !cedulaPersona.isBlank() && empresaDe(actual).buscarUsuario(cedulaPersona) == null) {
                throw new NoEncontradoException("No existe una persona con la cédula " + cedulaPersona + ".");
            }
            return null;
        });
        String id = almacen.guardar(limpio, tipo, datos);
        ArchivoAdjunto adjunto = new ArchivoAdjunto(id, limpio, tipo, datos.length, LocalDateTime.now(reloj), Firma.de(actual),
                cedulaPersona == null || cedulaPersona.isBlank() ? null : cedulaPersona.trim(),
                confidencial && actual instanceof Gerente);
        return modificar(() -> {
            empresaDe(actual).agregarArchivo(adjunto);
            return adjunto;
        });
    }

    public Descarga descargar(Usuario actual, String id) {
        ArchivoAdjunto adjunto = leer(() -> {
            ArchivoAdjunto encontrado = empresaDe(actual).buscarArchivo(id);
            if (encontrado == null) {
                throw new NoEncontradoException("El archivo no existe.");
            }
            if (!encontrado.puedeVerlo(actual)) {
                throw new PermisoDenegadoException("No tienes permiso para ver este documento.");
            }
            return encontrado;
        });
        return new Descarga(adjunto.nombre(), adjunto.tipo(), almacen.leer(adjunto.id()));
    }

    public String descripcionAlmacen() {
        return almacen.descripcion();
    }

    static String tipoReal(byte[] d) {
        if (d.length > 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (d.length > 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') {
            return "image/png";
        }
        if (d.length > 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F' && d[8] == 'W' && d[9] == 'E'
                && d[10] == 'B' && d[11] == 'P') {
            return "image/webp";
        }
        if (d.length > 5 && d[0] == '%' && d[1] == 'P' && d[2] == 'D' && d[3] == 'F' && d[4] == '-') {
            return "application/pdf";
        }
        throw new DatoInvalidoException("Solo se aceptan fotos (JPG, PNG o WEBP) o documentos PDF.");
    }

    private static String limpiarNombre(String nombre, String tipo) {
        String base = nombre == null ? "" : nombre.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
        if (base.isEmpty()) {
            base = "documento";
        }
        if (base.length() > 100) {
            base = base.substring(0, 100);
        }
        String extension = switch (tipo) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".pdf";
        };
        return base.toLowerCase().endsWith(extension) ? base : base.replaceAll("\\.[A-Za-z0-9]{1,5}$", "") + extension;
    }
}
