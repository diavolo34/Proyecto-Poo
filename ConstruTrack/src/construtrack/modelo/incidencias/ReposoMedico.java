package construtrack.modelo.incidencias;

import construtrack.modelo.excepciones.DatoInvalidoException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reposo indicado por un médico (por ejemplo, un trabajador que falta por enfermedad). */
public class ReposoMedico extends Incidencia implements Justificable {

    public static final String CODIGO = "REPOSO_MEDICO";

    private final String centroMedico;
    private final String certificado;

    public ReposoMedico(DatosIncidencia datos) {
        super(datos);
        this.centroMedico = datos.extra("centroMedico");
        this.certificado = datos.extra("certificado");
        if (centroMedico.length() < 3) {
            throw new DatoInvalidoException("Indica el centro médico que emitió el reposo.");
        }
    }

    @Override
    public boolean justificaAusenciaEl(LocalDate fecha) {
        return aprobadaYCubre(fecha);
    }

    @Override
    public String getCodigoTipo() {
        return CODIGO;
    }

    @Override
    public String getNombreTipo() {
        return "Reposo médico";
    }

    @Override
    public Gravedad getGravedad() {
        return getDias() > 3 ? Gravedad.ALTA : Gravedad.MEDIA;
    }

    @Override
    public Map<String, String> getExtras() {
        Map<String, String> extras = new LinkedHashMap<>();
        extras.put("centroMedico", centroMedico);
        extras.put("certificado", certificado);
        return extras;
    }

    @Override
    public String getDetalle() {
        return centroMedico + (certificado.isEmpty() ? " · sin certificado" : " · certificado " + certificado);
    }

    public String getCentroMedico() {
        return centroMedico;
    }

    public String getCertificado() {
        return certificado;
    }
}
