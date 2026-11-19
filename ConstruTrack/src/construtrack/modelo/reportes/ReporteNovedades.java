package construtrack.modelo.reportes;

import construtrack.modelo.Periodo;
import construtrack.modelo.Usuario;
import construtrack.modelo.novedades.Categoria;
import construtrack.modelo.novedades.Novedad;
import construtrack.modelo.novedades.ReporteObra;
import java.time.LocalDate;
import java.util.List;

public class ReporteNovedades extends Reporte {
    private final Categoria categoria;

    public ReporteNovedades(Categoria categoria) {
        this.categoria = categoria;
    }

    @Override
    protected String titulo() {
        return categoria.getNombre();
    }

    @Override
    protected List<String> columnas() {
        return List.of("Id", "Tipo", "Persona", "Proyecto", "Desde", "Hasta", "Días", "Estado", "Detalle", "Descripción",
                "Registrado por", "Fecha de registro", "Autorizado o rechazado por", "Fecha de revisión", "Nota");
    }

    @Override
    protected void agregarFilas(Usuario persona, Periodo periodo, LocalDate hoy, Documento documento) {
        for (Novedad n : persona.getHistorial().novedadesEn(periodo)) {
            if (n.getCategoria() != categoria) {
                continue;
            }
            String obra = n instanceof ReporteObra r && r.getObra() != null ? r.getObra() : "";
            documento.agregarFila(n.getId(), n.getNombreTipo(), persona.getNombre(), obra, n.getDesde(), n.getHasta(),
                    n.getDias(), n.getEstado().getNombre(), n.getDetalle(), n.getDescripcion(), n.getRegistradaPor().nombre(),
                    n.getFechaRegistro().toLocalDate(), n.getRevisadaPor() == null ? "" : n.getRevisadaPor().nombre(),
                    n.getFechaRevision() == null ? "" : n.getFechaRevision().toLocalDate(), n.getNota());
        }
    }
}
