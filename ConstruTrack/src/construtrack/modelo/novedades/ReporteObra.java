package construtrack.modelo.novedades;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ReporteObra extends Novedad {
    private final String obra;

    protected ReporteObra(DatosNovedad datos) {
        super(datos);
        this.obra = datos.extra("obra").isEmpty() ? null : datos.extra("obra");
    }

    @Override
    public Categoria getCategoria() {
        return Categoria.REPORTE_OBRA;
    }

    public String getObra() {
        return obra;
    }

    protected Map<String, String> extrasDeObra() {
        Map<String, String> extras = new LinkedHashMap<>();
        if (obra != null) {
            extras.put("obra", obra);
        }
        return extras;
    }

    @Override
    public Map<String, String> getExtras() {
        return extrasDeObra();
    }
}
