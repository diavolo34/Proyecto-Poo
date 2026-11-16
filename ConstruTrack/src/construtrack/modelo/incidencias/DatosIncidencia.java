package construtrack.modelo.incidencias;

import construtrack.modelo.Usuario;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class DatosIncidencia {
    final int id;
    final Usuario afectado;
    final Usuario reportadoPor;
    final LocalDate desde;
    final LocalDate hasta;
    final String descripcion;
    final LocalDateTime fechaReporte;
    private final Map<String, String> extras;

    public DatosIncidencia(int id, Usuario afectado, Usuario reportadoPor, LocalDate desde, LocalDate hasta,
            String descripcion, LocalDateTime fechaReporte, Map<String, String> extras) {
        this.id = id;
        this.afectado = afectado;
        this.reportadoPor = reportadoPor;
        this.desde = desde;
        this.hasta = hasta == null ? desde : hasta;
        this.descripcion = descripcion;
        this.fechaReporte = fechaReporte;
        this.extras = extras == null ? new HashMap<>() : new HashMap<>(extras);
    }

    public String extra(String clave) {
        String valor = extras.get(clave);
        return valor == null ? "" : valor.trim();
    }
}
