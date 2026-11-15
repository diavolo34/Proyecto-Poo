package construtrack.modelo.reportes;

import java.util.ArrayList;
import java.util.List;

/** Tabla con título, columnas y filas. Es el resultado neutral de un reporte: el formateador decide cómo se ve. */
public final class Documento {

    private final String titulo;
    private final List<String> columnas = new ArrayList<>();
    private final List<List<String>> filas = new ArrayList<>();

    public Documento(String titulo) {
        this.titulo = titulo;
    }

    public void definirColumnas(List<String> nombres) {
        columnas.clear();
        columnas.addAll(nombres);
    }

    /** Agrega una fila; cada celda se convierte en texto para que nunca haya valores nulos. */
    public void agregarFila(Object... celdas) {
        List<String> fila = new ArrayList<>();
        for (Object celda : celdas) {
            fila.add(celda == null ? "" : celda.toString());
        }
        filas.add(List.copyOf(fila));
    }

    public String getTitulo() {
        return titulo;
    }

    public List<String> getColumnas() {
        return List.copyOf(columnas);
    }

    public List<List<String>> getFilas() {
        return List.copyOf(filas);
    }
}
