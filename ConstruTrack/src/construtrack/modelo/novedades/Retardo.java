package construtrack.modelo.novedades;

import construtrack.modelo.excepciones.DatoInvalidoException;
import java.util.Map;

public class Retardo extends ReporteObra {
    public static final String CODIGO = "RETARDO";

    private final int minutos;

    public Retardo(DatosNovedad datos) {
        super(datos);
        try {
            this.minutos = Integer.parseInt(datos.extra("minutos"));
        } catch (NumberFormatException e) {
            throw new DatoInvalidoException("Indica cuántos minutos de retraso hubo.");
        }
        if (minutos < 1 || minutos > 600) {
            throw new DatoInvalidoException("Los minutos de retraso deben estar entre 1 y 600.");
        }
    }

    @Override
    public String getCodigoTipo() {
        return CODIGO;
    }

    @Override
    public String getNombreTipo() {
        return "Retardo";
    }

    @Override
    public Map<String, String> getExtras() {
        Map<String, String> extras = extrasDeObra();
        extras.put("minutos", String.valueOf(minutos));
        return extras;
    }

    @Override
    public String getDetalle() {
        return minutos + " min de retraso";
    }

    public int getMinutos() {
        return minutos;
    }
}
