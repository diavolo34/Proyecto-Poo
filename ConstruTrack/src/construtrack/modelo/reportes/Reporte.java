package construtrack.modelo.reportes;

import construtrack.modelo.Estadisticas;
import construtrack.modelo.Periodo;
import construtrack.modelo.Usuario;
import java.time.LocalDate;
import java.util.List;

/**
 * Esqueleto de un reporte (patrón Template Method): el orden de los pasos está fijo aquí
 * y cada subclase define solo su título, sus columnas y el contenido de cada fila.
 */
public abstract class Reporte {

    /** Plantilla: título, columnas y una o más filas por persona. No se sobrescribe. */
    public final Documento generar(List<Usuario> personas, Periodo periodo, LocalDate hoy) {
        Documento documento = new Documento(titulo() + " · " + periodo);
        documento.definirColumnas(columnas());
        for (Usuario persona : personas) {
            agregarFilas(persona, periodo, hoy, documento);
        }
        return documento;
    }

    protected abstract String titulo();

    protected abstract List<String> columnas();

    protected abstract void agregarFilas(Usuario persona, Periodo periodo, LocalDate hoy, Documento documento);

    /** Resumen numérico de una persona, calculado con la misma lógica que las estadísticas. */
    protected Estadisticas.Resumen resumen(Usuario persona, Periodo periodo, LocalDate hoy) {
        return new Estadisticas(List.of(persona), periodo, hoy).resumenes().get(0);
    }
}
