package construtrack.modelo.inventario;

import construtrack.modelo.Firma;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Inventario {
    private final Map<String, Articulo> articulos = new LinkedHashMap<>();
    private int siguienteAsignacion = 1;

    public Articulo agregar(CategoriaArticulo categoria, String nombre, String marca, String serie, int cantidad, String nota) {
        if (!serie(serie).isEmpty() && buscarPorSerie(serie) != null) {
            throw new OperacionInvalidaException("Ya hay un artículo con el número de serie " + serie.trim() + ".");
        }
        Articulo articulo = new Articulo(siguienteCodigo(categoria), categoria, nombre, marca, serie, cantidad, nota);
        articulos.put(articulo.getCodigo(), articulo);
        return articulo;
    }

    public void restaurar(Articulo articulo, EstadoArticulo estado) {
        articulo.restaurarEstado(estado);
        articulos.put(articulo.getCodigo(), articulo);
    }

    private static String serie(String serie) {
        return serie == null ? "" : serie.trim();
    }

    private Articulo buscarPorSerie(String serie) {
        for (Articulo a : articulos.values()) {
            if (!a.getSerie().isEmpty() && a.getSerie().equalsIgnoreCase(serie.trim())) {
                return a;
            }
        }
        return null;
    }

    private String siguienteCodigo(CategoriaArticulo categoria) {
        int mayor = 0;
        for (String codigo : articulos.keySet()) {
            if (codigo.startsWith(categoria.getPrefijo() + "-")) {
                mayor = Math.max(mayor, Integer.parseInt(codigo.substring(3)));
            }
        }
        return String.format("%s-%03d", categoria.getPrefijo(), mayor + 1);
    }

    public Articulo buscar(String codigo) {
        Articulo articulo = codigo == null ? null : articulos.get(codigo.trim().toUpperCase());
        if (articulo == null) {
            throw new DatoInvalidoException("No existe el artículo " + codigo + ".");
        }
        return articulo;
    }

    public void eliminar(String codigo) {
        Articulo articulo = buscar(codigo);
        if (!articulo.getAsignaciones().isEmpty()) {
            throw new OperacionInvalidaException("«" + articulo.getNombre() + "» ya tiene entregas registradas. "
                    + "Para conservar su historial, márcalo «De baja» en vez de eliminarlo.");
        }
        articulos.remove(articulo.getCodigo());
    }

    public Asignacion asignar(String codigo, Firma persona, int cantidad, Firma entrega, String obra, LocalDate prevista,
            String nota, LocalDateTime ahora) {
        Articulo articulo = buscar(codigo);
        articulo.verificarDisponible(cantidad);
        Asignacion asignacion = new Asignacion(siguienteAsignacion, articulo, persona, cantidad, ahora, entrega, obra, prevista, nota);
        articulo.agregarAsignacion(asignacion);
        siguienteAsignacion++;
        return asignacion;
    }

    public void restaurarAsignacion(Asignacion asignacion) {
        asignacion.getArticulo().agregarAsignacion(asignacion);
        siguienteAsignacion = Math.max(siguienteAsignacion, asignacion.getId() + 1);
    }

    public Asignacion devolver(int id, CondicionDevolucion condicion, Firma recibe, String nota, LocalDateTime ahora) {
        Asignacion asignacion = buscarAsignacion(id);
        asignacion.devolver(condicion, recibe, nota, ahora);
        Articulo articulo = asignacion.getArticulo();
        if (condicion == CondicionDevolucion.EXTRAVIADA) {
            articulo.registrarPerdida(asignacion.getCantidad());
        } else if (condicion == CondicionDevolucion.DANADA && articulo.enUso() == 0) {
            articulo.cambiarEstado(EstadoArticulo.EN_MANTENIMIENTO);
        }
        return asignacion;
    }

    public List<Asignacion> devolverTodoDe(String cedula, Firma recibe, LocalDateTime ahora) {
        List<Asignacion> devueltas = new ArrayList<>();
        for (Asignacion a : abiertasDe(cedula)) {
            devueltas.add(devolver(a.getId(), CondicionDevolucion.BUENA, recibe, "Devuelto al egresar", ahora));
        }
        return devueltas;
    }

    public Asignacion buscarAsignacion(int id) {
        for (Articulo a : articulos.values()) {
            for (Asignacion x : a.getAsignaciones()) {
                if (x.getId() == id) {
                    return x;
                }
            }
        }
        throw new DatoInvalidoException("No existe la entrega #" + id + ".");
    }

    public List<Articulo> listar() {
        return new ArrayList<>(articulos.values());
    }

    public List<Asignacion> asignaciones() {
        List<Asignacion> todas = new ArrayList<>();
        for (Articulo a : articulos.values()) {
            todas.addAll(a.getAsignaciones());
        }
        todas.sort(Comparator.comparing(Asignacion::getFecha).reversed());
        return todas;
    }

    public List<Asignacion> abiertasDe(String cedula) {
        List<Asignacion> abiertas = new ArrayList<>();
        for (Asignacion a : asignaciones()) {
            if (a.estaAbierta() && a.getPersona().cedula().equals(cedula)) {
                abiertas.add(a);
            }
        }
        return abiertas;
    }

    public List<Asignacion> vencidas(LocalDate hoy) {
        return asignaciones().stream().filter(a -> a.vencidaEn(hoy)).toList();
    }
}
