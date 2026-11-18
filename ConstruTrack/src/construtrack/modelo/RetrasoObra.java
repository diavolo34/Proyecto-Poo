package construtrack.modelo;

import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class RetrasoObra {
    private final int id;
    private final LocalDateTime fecha;
    private final Firma reportadoPor;
    private final String motivo;
    private final LocalDate finAnterior;
    private final LocalDate finNuevo;
    private Firma revisadoPor;
    private LocalDateTime fechaRevision;

    public RetrasoObra(int id, LocalDateTime fecha, Firma reportadoPor, String motivo, LocalDate finAnterior, LocalDate finNuevo) {
        String limpio = motivo == null ? "" : motivo.trim();
        if (limpio.length() < 5 || limpio.length() > 300) {
            throw new DatoInvalidoException("Explica el motivo del retraso (entre 5 y 300 caracteres).");
        }
        if (finNuevo == null || !finNuevo.isAfter(finAnterior)) {
            throw new DatoInvalidoException("La nueva fecha estimada debe ser posterior a la actual (" + finAnterior + ").");
        }
        this.id = id;
        this.fecha = fecha;
        this.reportadoPor = reportadoPor;
        this.motivo = limpio;
        this.finAnterior = finAnterior;
        this.finNuevo = finNuevo;
    }

    public void marcarRevisado(Firma quien, LocalDateTime ahora) {
        if (revisadoPor != null) {
            throw new OperacionInvalidaException("Este retraso ya fue revisado por " + revisadoPor.nombre() + ".");
        }
        this.revisadoPor = quien;
        this.fechaRevision = ahora;
    }

    public void restaurarRevision(Firma quien, LocalDateTime cuando) {
        this.revisadoPor = quien;
        this.fechaRevision = cuando;
    }

    public boolean estaRevisado() {
        return revisadoPor != null;
    }

    public long diasDeRetraso() {
        return java.time.temporal.ChronoUnit.DAYS.between(finAnterior, finNuevo);
    }

    public int getId() {
        return id;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public Firma getReportadoPor() {
        return reportadoPor;
    }

    public String getMotivo() {
        return motivo;
    }

    public LocalDate getFinAnterior() {
        return finAnterior;
    }

    public LocalDate getFinNuevo() {
        return finNuevo;
    }

    public Firma getRevisadoPor() {
        return revisadoPor;
    }

    public LocalDateTime getFechaRevision() {
        return fechaRevision;
    }
}
