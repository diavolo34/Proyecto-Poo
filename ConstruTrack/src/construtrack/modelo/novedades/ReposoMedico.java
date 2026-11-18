package construtrack.modelo.novedades;

import construtrack.modelo.excepciones.DatoInvalidoException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReposoMedico extends Licencia {
    public static final String CODIGO = "REPOSO_MEDICO";

    private final String medico;
    private final String matricula;
    private final String centro;
    private final TipoConsulta tipoConsulta;
    private final String archivo;

    public ReposoMedico(DatosNovedad datos) {
        super(datos);
        this.medico = datos.extra("medico");
        this.matricula = datos.extra("matricula");
        String centroAnterior = datos.extra("centroMedico");
        this.centro = datos.extra("centro").isEmpty() ? centroAnterior : datos.extra("centro");
        this.tipoConsulta = TipoConsulta.desdeTexto(datos.extra("tipoConsulta"));
        this.archivo = datos.extra("archivo").isEmpty() ? null : datos.extra("archivo");
    }

    @Override
    public void validarNueva() {
        if (!medico.matches("[\\p{L} .'-]{3,80}")) {
            throw new DatoInvalidoException("Escribe el nombre del médico que firmó el reposo.");
        }
        if (!matricula.matches("[\\p{L}0-9 .-]{3,20}")) {
            throw new DatoInvalidoException("Escribe la matrícula médica (MPPS o colegio de médicos), de 3 a 20 caracteres.");
        }
        if (centro.length() < 3 || centro.length() > 100) {
            throw new DatoInvalidoException("Indica el centro asistencial que emitió el reposo.");
        }
        if (tipoConsulta == null) {
            throw new DatoInvalidoException("Indica si la consulta fue interna o por emergencia.");
        }
        if (archivo == null) {
            throw new DatoInvalidoException("Adjunta la imagen o el documento del reposo.");
        }
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
    public boolean esIncapacidad() {
        return true;
    }

    @Override
    public String etiquetaJustificacion() {
        return "Incapacidad";
    }

    @Override
    public String getArchivo() {
        return archivo;
    }

    @Override
    public Map<String, String> getExtras() {
        Map<String, String> extras = new LinkedHashMap<>();
        extras.put("medico", medico);
        extras.put("matricula", matricula);
        extras.put("centro", centro);
        extras.put("tipoConsulta", tipoConsulta == null ? "" : tipoConsulta.name());
        extras.put("archivo", archivo == null ? "" : archivo);
        return extras;
    }

    @Override
    public String getDetalle() {
        StringBuilder detalle = new StringBuilder(centro.isEmpty() ? "Centro no indicado" : centro);
        if (tipoConsulta != null) {
            detalle.append(" · consulta ").append(tipoConsulta.getNombre().toLowerCase());
        }
        if (!medico.isEmpty()) {
            detalle.append(" · Dr(a). ").append(medico);
        }
        if (!matricula.isEmpty()) {
            detalle.append(" · matrícula ").append(matricula);
        }
        return detalle.toString();
    }

    public String getMedico() {
        return medico;
    }

    public String getMatricula() {
        return matricula;
    }

    public String getCentro() {
        return centro;
    }

    public TipoConsulta getTipoConsulta() {
        return tipoConsulta;
    }
}
