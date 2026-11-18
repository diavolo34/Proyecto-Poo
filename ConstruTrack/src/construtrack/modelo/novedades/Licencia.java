package construtrack.modelo.novedades;

import java.time.LocalDate;

public abstract class Licencia extends Novedad implements Justificable {
    protected Licencia(DatosNovedad datos) {
        super(datos);
    }

    @Override
    public Categoria getCategoria() {
        return Categoria.LICENCIA;
    }

    @Override
    public boolean justificaAusenciaEl(LocalDate fecha) {
        return aprobadaYCubre(fecha);
    }

    @Override
    public String etiquetaJustificacion() {
        return getNombreTipo();
    }

    public boolean esIncapacidad() {
        return false;
    }

    public boolean incapacitaEl(LocalDate fecha) {
        return esIncapacidad() && aprobadaYCubre(fecha);
    }
}
