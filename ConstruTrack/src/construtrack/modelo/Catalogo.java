package construtrack.modelo;

import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import java.util.ArrayList;
import java.util.List;

public class Catalogo {
    public static final List<String> PUESTOS_INICIALES = List.of("Gerente", "Jefe de obra",
            "Supervisor del equipo de mantenimiento", "Electricista", "Maestro de obra", "Albañil", "Ayudante de obra",
            "Operador de maquinaria");
    public static final List<String> AREAS_INICIALES = List.of("Administración", "Obra civil", "Electricidad",
            "Mantenimiento");

    private final List<String> puestos = new ArrayList<>(PUESTOS_INICIALES);
    private final List<String> areas = new ArrayList<>(AREAS_INICIALES);

    public List<String> getPuestos() {
        return List.copyOf(puestos);
    }

    public List<String> getAreas() {
        return List.copyOf(areas);
    }

    public void vaciar() {
        puestos.clear();
        areas.clear();
    }

    public String agregarPuesto(String nombre) {
        return agregar(puestos, nombre, "puesto");
    }

    public String agregarArea(String nombre) {
        return agregar(areas, nombre, "área");
    }

    public void quitarPuesto(String nombre) {
        quitar(puestos, nombre, "puesto");
    }

    public void quitarArea(String nombre) {
        quitar(areas, nombre, "área");
    }

    public String puestoValido(String nombre) {
        return valido(puestos, nombre, "puesto");
    }

    public String areaValida(String nombre) {
        return valido(areas, nombre, "área");
    }

    private static String agregar(List<String> lista, String nombre, String que) {
        String limpio = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        if (!limpio.matches("[\\p{L}0-9 .,()/&-]{3,60}")) {
            throw new DatoInvalidoException("El nombre del " + que + " debe tener entre 3 y 60 letras.");
        }
        if (buscar(lista, limpio) != null) {
            throw new OperacionInvalidaException("Ya existe el " + que + " «" + limpio + "».");
        }
        lista.add(limpio);
        return limpio;
    }

    private static void quitar(List<String> lista, String nombre, String que) {
        String encontrado = buscar(lista, nombre);
        if (encontrado == null) {
            throw new DatoInvalidoException("No existe el " + que + " «" + nombre + "».");
        }
        lista.remove(encontrado);
    }

    private static String valido(List<String> lista, String nombre, String que) {
        if (nombre == null || nombre.isBlank()) {
            return null;
        }
        String encontrado = buscar(lista, nombre);
        if (encontrado == null) {
            throw new DatoInvalidoException("El " + que + " «" + nombre.trim() + "» no está en el catálogo.");
        }
        return encontrado;
    }

    private static String buscar(List<String> lista, String nombre) {
        if (nombre == null) {
            return null;
        }
        for (String existente : lista) {
            if (existente.equalsIgnoreCase(nombre.trim())) {
                return existente;
            }
        }
        return null;
    }
}
