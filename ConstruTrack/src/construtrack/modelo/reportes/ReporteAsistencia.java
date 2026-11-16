package construtrack.modelo.reportes;

import construtrack.modelo.Periodo;
import construtrack.modelo.Usuario;
import java.time.LocalDate;
import java.util.List;

public class ReporteAsistencia extends Reporte {
    @Override
    protected String titulo() {
        return "Reporte de asistencia";
    }

    @Override
    protected List<String> columnas() {
        return List.of("Cédula", "Nombre", "Rol", "Días laborables", "Asistencias", "% asistencia",
                "Ausencias sin justificar", "Justificadas", "Horas trabajadas", "Horas esperadas",
                "Extra diaria (aprobada)", "Extra día libre (aprobada)", "Extra por aprobar");
    }

    @Override
    protected void agregarFilas(Usuario persona, Periodo periodo, LocalDate hoy, Documento documento) {
        var r = resumen(persona, periodo, hoy);
        documento.agregarFila(r.cedula(), r.nombre(), r.rol(), r.laborables(), r.asistencias(),
                String.format("%.1f", r.tasaAsistencia()), r.ausencias(), r.justificadas(),
                horas(r.minutos()), horas(r.minutosEsperados()), horas(r.extraDiaria()), horas(r.extraDiaLibre()),
                horas(r.extraPendiente()));
    }

    private static String horas(long minutos) {
        return String.format(java.util.Locale.ROOT, "%.1f", minutos / 60.0);
    }
}
