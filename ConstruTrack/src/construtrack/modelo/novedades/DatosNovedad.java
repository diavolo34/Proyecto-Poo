package construtrack.modelo.novedades;

import construtrack.modelo.Firma;
import construtrack.modelo.Usuario;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class DatosNovedad {
    final int id;
    final Usuario afectado;
    final Firma registradaPor;
    final LocalDate desde;
    final LocalDate hasta;
    final String descripcion;
    final LocalDateTime fechaRegistro;
    private final Map<String, String> extras;

    public DatosNovedad(int id, Usuario afectado, Firma registradaPor, LocalDate desde, LocalDate hasta,
            String descripcion, LocalDateTime fechaRegistro, Map<String, String> extras) {
        this.id = id;
        this.afectado = afectado;
        this.registradaPor = registradaPor;
        this.desde = desde;
        this.hasta = hasta == null ? desde : hasta;
        this.descripcion = descripcion;
        this.fechaRegistro = fechaRegistro;
        this.extras = extras == null ? new HashMap<>() : new HashMap<>(extras);
    }

    public String extra(String clave) {
        String valor = extras.get(clave);
        return valor == null ? "" : valor.trim();
    }
}
