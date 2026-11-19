package construtrack.servicio;

import construtrack.modelo.ArchivoAdjunto;
import construtrack.modelo.Empleado;
import construtrack.modelo.Firma;
import construtrack.modelo.Plataforma;
import construtrack.modelo.RevisorNovedades;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.NoEncontradoException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.modelo.novedades.Categoria;
import construtrack.modelo.novedades.DatosNovedad;
import construtrack.modelo.novedades.EstadoNovedad;
import construtrack.modelo.novedades.FabricaNovedades;
import construtrack.modelo.novedades.Novedad;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServicioNovedades extends ServicioBase {
    private final FabricaNovedades fabrica;
    private int siguienteId;

    public ServicioNovedades(Plataforma plataforma, EscritorDatos escritor, Clock reloj, FabricaNovedades fabrica,
            int siguienteId) {
        super(plataforma, escritor, reloj);
        this.fabrica = fabrica;
        this.siguienteId = siguienteId;
    }

    public Novedad registrar(Usuario registra, Categoria categoria, String codigoTipo, String cedulaAfectado,
            LocalDate desde, LocalDate hasta, String descripcion, Map<String, String> extras) {
        verificarAcceso(registra, categoria);
        return modificar(() -> {
            Usuario afectado = buscar(registra, cedulaAfectado);
            Map<String, String> datosExtra = new HashMap<>(extras == null ? Map.of() : extras);
            if (categoria == Categoria.REPORTE_OBRA && datosExtra.getOrDefault("obra", "").isBlank() && afectado.getObra() != null) {
                datosExtra.put("obra", afectado.getObra().getNombre());
            }
            String archivo = datosExtra.get("archivo");
            if (archivo != null && !archivo.isBlank()) {
                ArchivoAdjunto adjunto = empresaDe(registra).buscarArchivo(archivo);
                if (adjunto == null || !adjunto.subidoPor().esDe(registra)) {
                    throw new DatoInvalidoException("El archivo adjunto no es válido. Súbelo de nuevo.");
                }
            }
            DatosNovedad datos = new DatosNovedad(siguienteId, afectado, Firma.de(registra), desde, hasta, descripcion,
                    LocalDateTime.now(reloj), datosExtra);
            Novedad novedad = fabrica.crear(codigoTipo, categoria, datos);
            registra.registrarNovedad(novedad);
            siguienteId++;
            return novedad;
        });
    }

    public Map<String, String> tipos(Categoria categoria) {
        return fabrica.tipos(categoria);
    }

    public List<Usuario> personasRegistrables(Usuario actual) {
        return leer(() -> {
            List<Usuario> personas = new ArrayList<>();
            for (Usuario u : empresaDe(actual).listarUsuarios()) {
                if (actual.puedeRegistrarNovedadPara(u)) {
                    personas.add(u);
                }
            }
            return personas;
        });
    }

    public boolean puedeRevisar(Usuario actual, Novedad novedad) {
        return actual instanceof RevisorNovedades r && r.puedeRevisar(novedad) && !novedad.getEstado().esFinal();
    }

    public List<Novedad> listar(Usuario actual, Categoria categoria) {
        verificarAcceso(actual, categoria);
        return leer(() -> {
            List<Novedad> visibles = new ArrayList<>();
            for (Usuario u : empresaDe(actual).listarTodos()) {
                if (actual.puedeVerNovedadesDe(u)) {
                    for (Novedad n : u.getHistorial().getNovedades()) {
                        if (categoria == null || n.getCategoria() == categoria) {
                            visibles.add(n);
                        }
                    }
                }
            }
            visibles.sort(Comparator.comparing(Novedad::getId).reversed());
            return visibles;
        });
    }

    public List<Novedad> listarTablero(Usuario actual) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        List<Novedad> tablero = new ArrayList<>(listar(actual, null));
        tablero.removeIf(n -> n.archivadaEn(ahora));
        return tablero;
    }

    public List<Novedad> pendientesDeRevisar(Usuario revisor) {
        List<Novedad> pendientes = new ArrayList<>(listar(revisor, null));
        pendientes.removeIf(n -> !puedeRevisar(revisor, n));
        return pendientes;
    }

    public Novedad revisar(Usuario revisor, int id, EstadoNovedad nuevo, String nota) {
        return modificar(() -> {
            if (!(revisor instanceof RevisorNovedades r)) {
                throw new PermisoDenegadoException("Solo el gerente aprueba o rechaza permisos, reposos y reportes de proyecto.");
            }
            Novedad novedad = buscarNovedad(revisor, id);
            r.revisar(novedad, nuevo, nota, LocalDateTime.now(reloj));
            return novedad;
        });
    }

    private static void verificarAcceso(Usuario actual, Categoria categoria) {
        if (actual instanceof Empleado && categoria != Categoria.LICENCIA) {
            throw new PermisoDenegadoException("Los reportes de proyecto los gestionan el supervisor y el gerente.");
        }
    }

    private Usuario buscar(Usuario actual, String cedula) {
        Usuario u = empresaDe(actual).buscarUsuario(cedula);
        if (u == null) {
            throw new NoEncontradoException("No existe una persona con la cédula " + cedula + ".");
        }
        return u;
    }

    private Novedad buscarNovedad(Usuario actual, int id) {
        for (Usuario u : empresaDe(actual).listarTodos()) {
            for (Novedad n : u.getHistorial().getNovedades()) {
                if (n.getId() == id) {
                    return n;
                }
            }
        }
        throw new NoEncontradoException("No existe el registro #" + id + ".");
    }
}
