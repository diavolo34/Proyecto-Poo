package construtrack.modelo.reportes;

import construtrack.modelo.Periodo;
import construtrack.modelo.Usuario;
import java.time.LocalDate;
import java.util.List;

/** Jornadas, retardos, porcentaje de puntualidad y horas trabajadas por persona. */
public class ReportePuntualidad extends Reporte {

    @Override
    protected String titulo() {
        return "Reporte de puntualidad";
    }

    @Override
    protected List<String> columnas() {
        return List.of("Cédula", "Nombre", "Rol", "Jornadas", "Retardos", "% puntualidad", "Minutos trabajados");
    }

    @Override
    protected void agregarFilas(Usuario persona, Periodo periodo, LocalDate hoy, Documento documento) {
        var r = resumen(persona, periodo, hoy);
        documento.agregarFila(r.cedula(), r.nombre(), r.rol(), r.asistencias(), r.retardos(),
                String.format("%.1f", r.puntualidad()), r.minutos());
    }
}
