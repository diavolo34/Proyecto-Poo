package construtrack.servicio;

import construtrack.modelo.Empresa;
import construtrack.modelo.Firma;
import construtrack.modelo.Gerente;
import construtrack.modelo.Plataforma;
import construtrack.modelo.Supervisor;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.NoEncontradoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.modelo.inventario.Articulo;
import construtrack.modelo.inventario.Asignacion;
import construtrack.modelo.inventario.CategoriaArticulo;
import construtrack.modelo.inventario.CondicionDevolucion;
import construtrack.modelo.inventario.EstadoArticulo;
import construtrack.modelo.inventario.Inventario;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public class ServicioInventario extends ServicioBase {
    public ServicioInventario(Plataforma plataforma, EscritorDatos escritor, Clock reloj) {
        super(plataforma, escritor, reloj);
    }

    public List<Articulo> listar(Usuario actual) {
        Inventario inventario = comoGerente(actual);
        return leer(inventario::listar);
    }

    public List<Asignacion> asignaciones(Usuario actual) {
        Inventario inventario = comoGerente(actual);
        return leer(inventario::asignaciones);
    }

    public Articulo crear(Usuario actual, Map<String, String> datos) {
        Inventario inventario = comoGerente(actual);
        return modificar(() -> inventario.agregar(CategoriaArticulo.desdeTexto(datos.get("categoria")), datos.get("nombre"),
                datos.get("marca"), datos.get("serie"), entero(datos.get("cantidad"), 1, "cantidad"), datos.get("nota")));
    }

    public Articulo editar(Usuario actual, String codigo, Map<String, String> datos) {
        Inventario inventario = comoGerente(actual);
        return modificar(() -> {
            Articulo articulo = inventario.buscar(codigo);
            articulo.actualizar(datos.get("nombre"), datos.get("marca"), datos.get("serie"),
                    entero(datos.get("cantidad"), articulo.getCantidad(), "cantidad"), datos.get("nota"));
            if (datos.containsKey("estado")) {
                articulo.cambiarEstado(EstadoArticulo.desdeTexto(datos.get("estado")));
            }
            return articulo;
        });
    }

    public void eliminar(Usuario actual, String codigo) {
        Inventario inventario = comoGerente(actual);
        modificar(() -> {
            inventario.eliminar(codigo);
            return null;
        });
    }

    public Asignacion asignar(Usuario actual, String codigo, Map<String, String> datos) {
        Inventario inventario = comoGerente(actual);
        return modificar(() -> {
            Usuario persona = empresaDe(actual).buscarUsuario(datos.get("cedula"));
            if (persona == null || !persona.estaActivo()) {
                throw new DatoInvalidoException("Elige a una persona activa de la empresa.");
            }
            String obra = datos.get("obra");
            String nombreObra = null;
            if (obra != null && !obra.isBlank()) {
                if (empresaDe(actual).buscarObra(obra) == null) {
                    throw new NoEncontradoException("No existe el proyecto " + obra + ".");
                }
                nombreObra = empresaDe(actual).buscarObra(obra).getNombre();
            } else if (persona.getObra() != null) {
                nombreObra = persona.getObra().getNombre();
            }
            return inventario.asignar(codigo, Firma.de(persona), entero(datos.get("cantidad"), 1, "cantidad"), Firma.de(actual),
                    nombreObra, fecha(datos.get("devolucionPrevista")), datos.get("nota"), LocalDateTime.now(reloj));
        });
    }

    public Asignacion devolver(Usuario actual, int id, String condicion, String nota) {
        Inventario inventario = comoGerente(actual);
        return modificar(() -> inventario.devolver(id, CondicionDevolucion.desdeTexto(condicion), Firma.de(actual), nota,
                LocalDateTime.now(reloj)));
    }

    public List<Asignacion> aCargoDe(Usuario actual, String cedula) {
        return leer(() -> {
            Empresa empresa = empresaDe(actual);
            Usuario persona = empresa.buscarUsuario(cedula);
            boolean permitido = actual instanceof Gerente || actual.getCedula().equals(cedula)
                    || actual instanceof Supervisor s && persona != null && s.supervisa(persona);
            if (!permitido) {
                throw new PermisoDenegadoException("No puedes ver las herramientas de esta persona.");
            }
            return empresa.getInventario().abiertasDe(cedula);
        });
    }

    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    private Inventario comoGerente(Usuario actual) {
        if (!(actual instanceof Gerente)) {
            throw new PermisoDenegadoException("El inventario lo administra el gerente.");
        }
        return empresaDe(actual).getInventario();
    }

    private static int entero(String texto, int porDefecto, String campo) {
        if (texto == null || texto.isBlank()) {
            return porDefecto;
        }
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            throw new DatoInvalidoException("La " + campo + " debe ser un número entero.");
        }
    }

    private static LocalDate fecha(String texto) {
        try {
            return texto == null || texto.isBlank() ? null : LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            throw new OperacionInvalidaException("La fecha debe tener el formato AAAA-MM-DD.");
        }
    }
}
