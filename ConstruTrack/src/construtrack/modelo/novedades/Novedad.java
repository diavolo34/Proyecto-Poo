package construtrack.modelo.novedades;

import construtrack.modelo.Firma;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class Novedad {
    public static final int HORAS_EN_TABLERO = 3;
    public static final int MINIMO_NOTA = 3;

    private final int id;
    private final Usuario afectado;
    private final Firma registradaPor;
    private final LocalDate desde;
    private final LocalDate hasta;
    private final String descripcion;
    private final LocalDateTime fechaRegistro;

    private EstadoNovedad estado = EstadoNovedad.PENDIENTE;
    private Firma revisadaPor;
    private String nota = "";
    private LocalDateTime fechaRevision;

    protected Novedad(DatosNovedad datos) {
        if (datos.afectado == null || datos.registradaPor == null) {
            throw new DatoInvalidoException("Indica a quién corresponde y quién lo registra.");
        }
        if (datos.desde == null || datos.hasta.isBefore(datos.desde)) {
            throw new DatoInvalidoException("La fecha final no puede ser anterior a la inicial.");
        }
        String texto = datos.descripcion == null ? "" : datos.descripcion.trim();
        if (texto.length() < 5 || texto.length() > 300) {
            throw new DatoInvalidoException("Escribe la descripción con al menos 5 caracteres (máximo 300).");
        }
        this.id = datos.id;
        this.afectado = datos.afectado;
        this.registradaPor = datos.registradaPor;
        this.desde = datos.desde;
        this.hasta = datos.hasta;
        this.descripcion = texto;
        this.fechaRegistro = datos.fechaRegistro;
    }

    public abstract String getCodigoTipo();

    public abstract String getNombreTipo();

    public abstract Categoria getCategoria();

    public void validarNueva() {
    }

    public Map<String, String> getExtras() {
        return new LinkedHashMap<>();
    }

    public String getDetalle() {
        return "";
    }

    public String getArchivo() {
        return null;
    }

    public void revisar(EstadoNovedad nuevo, Firma quien, String nota, LocalDateTime ahora) {
        if (estado.esFinal()) {
            throw new OperacionInvalidaException(getNombreTipo() + " #" + id + " ya está " + estado.getNombre().toLowerCase() + ".");
        }
        if (nuevo == null || nuevo == EstadoNovedad.PENDIENTE) {
            throw new DatoInvalidoException("Elige aprobar o rechazar.");
        }
        String limpia = nota == null ? "" : nota.trim();
        if (limpia.length() < MINIMO_NOTA || limpia.length() > 300) {
            throw new DatoInvalidoException("Escribe una nota para " + (nuevo == EstadoNovedad.APROBADA ? "aprobar" : "rechazar")
                    + " (entre " + MINIMO_NOTA + " y 300 caracteres). La verá quien lo registró.");
        }
        this.estado = nuevo;
        this.revisadaPor = quien;
        this.nota = limpia;
        this.fechaRevision = ahora;
    }

    public void restaurarRevision(EstadoNovedad estado, Firma revisadaPor, String nota, LocalDateTime fecha) {
        this.estado = estado;
        this.revisadaPor = revisadaPor;
        this.nota = nota == null ? "" : nota;
        this.fechaRevision = fecha;
    }

    public boolean cubre(LocalDate fecha) {
        return !fecha.isBefore(desde) && !fecha.isAfter(hasta);
    }

    public boolean estaAprobada() {
        return estado == EstadoNovedad.APROBADA;
    }

    protected boolean aprobadaYCubre(LocalDate fecha) {
        return estaAprobada() && cubre(fecha);
    }

    public long horasSinResolver(LocalDateTime ahora) {
        return estado.esFinal() ? 0 : ChronoUnit.HOURS.between(fechaRegistro, ahora);
    }

    public boolean archivadaEn(LocalDateTime ahora) {
        return estado.esFinal() && fechaRevision != null && !ahora.isBefore(fechaRevision.plusHours(HORAS_EN_TABLERO));
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

    public Firma getRegistradaPor() {
        return registradaPor;
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

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public EstadoNovedad getEstado() {
        return estado;
    }

    public Firma getRevisadaPor() {
        return revisadaPor;
    }

    public String getNota() {
        return nota;
    }

    public LocalDateTime getFechaRevision() {
        return fechaRevision;
    }

    @Override
    public String toString() {
        return "#" + id + " " + getNombreTipo() + " · " + afectado.getNombre() + " (" + estado + ")";
    }
}
