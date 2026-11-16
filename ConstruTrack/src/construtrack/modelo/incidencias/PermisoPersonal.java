package construtrack.modelo.incidencias;

import java.time.LocalDate;

public class PermisoPersonal extends Incidencia implements Justificable {
    public static final String CODIGO = "PERMISO_PERSONAL";

    public PermisoPersonal(DatosIncidencia datos) {
        super(datos);
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
        return "Permiso personal";
    }

    @Override
    public Gravedad getGravedad() {
        return Gravedad.BAJA;
    }
}
