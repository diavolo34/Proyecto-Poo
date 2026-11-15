package construtrack.modelo.reportes;

import construtrack.modelo.Periodo;
import construtrack.modelo.Usuario;
import java.time.LocalDate;
import java.util.List;

/** Días laborables, asistencias, porcentaje y faltas por persona. */
public class ReporteAsistencia extends Reporte {

    @Override
    protected String titulo() {
        return "Reporte de asistencia";
    }

    @Override
    protected List<String> columnas() {
        return List.of("Cédula", "Nombre", "Rol", "Días laborables", "Asistencias", "% asistencia",
                "Ausencias sin justificar", "Justificadas");
    }

    @Override
    protected void agregarFilas(Usuario persona, Periodo periodo, LocalDate hoy, Documento documento) {
        var r = resumen(persona, periodo, hoy);
        documento.agregarFila(r.cedula(), r.nombre(), r.rol(), r.laborables(), r.asistencias(),
                String.format("%.1f", r.tasaAsistencia()), r.ausencias(), r.justificadas());
    }
}
