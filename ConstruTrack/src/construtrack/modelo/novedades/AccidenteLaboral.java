package construtrack.modelo.novedades;

import construtrack.modelo.excepciones.DatoInvalidoException;
import java.time.LocalDate;
import java.util.Map;

public class AccidenteLaboral extends ReporteObra implements Justificable {
    public static final String CODIGO = "ACCIDENTE_LABORAL";

    private final String lugar;
    private final boolean requirioAtencion;

    public AccidenteLaboral(DatosNovedad datos) {
        super(datos);
        this.lugar = datos.extra("lugar");
        this.requirioAtencion = Boolean.parseBoolean(datos.extra("requirioAtencion"));
        if (lugar.length() < 3) {
            throw new DatoInvalidoException("Indica el lugar del proyecto donde ocurrió el accidente.");
        }
    }

    @Override
    public boolean justificaAusenciaEl(LocalDate fecha) {
        return aprobadaYCubre(fecha);
    }

    @Override
    public String etiquetaJustificacion() {
        return "Accidente laboral";
    }

    @Override
    public String getCodigoTipo() {
        return CODIGO;
    }

    @Override
    public String getNombreTipo() {
        return "Accidente laboral";
    }

    @Override
    public Map<String, String> getExtras() {
        Map<String, String> extras = extrasDeObra();
        extras.put("lugar", lugar);
        extras.put("requirioAtencion", String.valueOf(requirioAtencion));
        return extras;
    }

    @Override
    public String getDetalle() {
        return lugar + (requirioAtencion ? " · requirió atención médica" : "");
    }

    public String getLugar() {
        return lugar;
    }

    public boolean isRequirioAtencion() {
        return requirioAtencion;
    }
}
