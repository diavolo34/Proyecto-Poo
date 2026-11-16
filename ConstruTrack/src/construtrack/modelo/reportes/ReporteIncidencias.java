package construtrack.modelo.reportes;

import construtrack.modelo.Periodo;
import construtrack.modelo.Usuario;
import java.time.LocalDate;
import java.util.List;

public class ReporteIncidencias extends Reporte {
    @Override
    protected String titulo() {
        return "Reporte de incidencias";
    }

    @Override
    protected List<String> columnas() {
        return List.of("Id", "Tipo", "Persona", "Desde", "Hasta", "Gravedad", "Estado", "Detalle");
    }

    @Override
    protected void agregarFilas(Usuario persona, Periodo periodo, LocalDate hoy, Documento documento) {
        for (var i : persona.getHistorial().incidenciasEn(periodo)) {
            documento.agregarFila(i.getId(), i.getNombreTipo(), persona.getNombre(), i.getDesde(), i.getHasta(),
                    i.getGravedad().getNombre(), i.getEstado().getNombre(), i.getDetalle());
        }
    }
}
