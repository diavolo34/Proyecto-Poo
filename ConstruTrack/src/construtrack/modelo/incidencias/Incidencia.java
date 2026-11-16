package construtrack.modelo.incidencias;

import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class Incidencia {
    private final int id;
    private final Usuario afectado;
    private final Usuario reportadoPor;
    private final LocalDate desde;
    private final LocalDate hasta;
    private final String descripcion;
    private final LocalDateTime fechaReporte;

    public static final int HORAS_EN_TABLERO = 3;

    private EstadoIncidencia estado = EstadoIncidencia.PENDIENTE;
    private Usuario revisor;
    private String comentarioRevision = "";
    private LocalDateTime fechaRevision;

    protected Incidencia(DatosIncidencia datos) {
        if (datos.afectado == null || datos.reportadoPor == null) {
            throw new DatoInvalidoException("La incidencia debe indicar a quién afecta y quién la reporta.");
        }
        if (datos.desde == null || datos.hasta.isBefore(datos.desde)) {
            throw new DatoInvalidoException("La fecha final de la incidencia no puede ser anterior a la inicial.");
        }
        String texto = datos.descripcion == null ? "" : datos.descripcion.trim();
        if (texto.length() < 5 || texto.length() > 300) {
            throw new DatoInvalidoException("Describe la incidencia con al menos 5 caracteres (máximo 300).");
        }
        this.id = datos.id;
        this.afectado = datos.afectado;
        this.reportadoPor = datos.reportadoPor;
        this.desde = datos.desde;
        this.hasta = datos.hasta;
        this.descripcion = texto;
        this.fechaReporte = datos.fechaReporte;
    }

    public abstract String getCodigoTipo();

    public abstract String getNombreTipo();

    public abstract Gravedad getGravedad();

    public Map<String, String> getExtras() {
        return new LinkedHashMap<>();
    }

    public String getDetalle() {
        return "";
    }

    public void registrarRevision(EstadoIncidencia nuevo, Usuario quien, String comentario, LocalDateTime ahora) {
        if (!estado.puedePasarA(nuevo)) {
            throw new OperacionInvalidaException("La incidencia #" + id + " está " + estado.getNombre().toLowerCase()
                    + " y no puede pasar a " + nuevo.getNombre().toLowerCase() + ".");
        }
        if (nuevo == EstadoIncidencia.RECHAZADA && (comentario == null || comentario.isBlank())) {
            throw new DatoInvalidoException("Indica el motivo del rechazo para que el trabajador lo sepa.");
        }
        this.estado = nuevo;
        this.revisor = quien;
        this.comentarioRevision = comentario == null ? "" : comentario.trim();
        this.fechaRevision = ahora;
    }

    public void restaurarRevision(EstadoIncidencia estado, Usuario revisor, String comentario, LocalDateTime fecha) {
        this.estado = estado;
        this.revisor = revisor;
        this.comentarioRevision = comentario == null ? "" : comentario;
        this.fechaRevision = fecha;
    }

    public boolean cubre(LocalDate fecha) {
        return !fecha.isBefore(desde) && !fecha.isAfter(hasta);
    }

    public boolean estaAprobada() {
        return estado == EstadoIncidencia.APROBADA;
    }

    protected boolean aprobadaYCubre(LocalDate fecha) {
        return estaAprobada() && cubre(fecha);
    }

    public long horasSinResolver(LocalDateTime ahora) {
        return estado.esFinal() ? 0 : ChronoUnit.HOURS.between(fechaReporte, ahora);
    }

    public long getDias() {
        return ChronoUnit.DAYS.between(desde, hasta) + 1;
    }

    public int getId() {
        return id;
    }

    public Usuario getAfectado() {
        return afectado;
    }

    public Usuario getReportadoPor() {
        return reportadoPor;
    }

    public LocalDate getDesde() {
        return desde;
    }

    public LocalDate getHasta() {
        return hasta;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public LocalDateTime getFechaReporte() {
        return fechaReporte;
    }

    public EstadoIncidencia getEstado() {
        return estado;
    }

    public Usuario getRevisor() {
        return revisor;
    }

    public String getComentarioRevision() {
        return comentarioRevision;
    }

    public boolean archivadaEn(LocalDateTime ahora) {
        return estado.esFinal() && fechaRevision != null && !ahora.isBefore(fechaRevision.plusHours(HORAS_EN_TABLERO));
    }

    public LocalDateTime getFechaRevision() {
        return fechaRevision;
    }

    @Override
    public String toString() {
        return "#" + id + " " + getNombreTipo() + " · " + afectado.getNombre() + " (" + estado + ")";
    }
}
