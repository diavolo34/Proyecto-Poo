package construtrack.modelo;

import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.util.Totp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Obra {
    private final String codigo;
    private String nombre;
    private String ubicacion;
    private LocalDate fechaInicio;
    private LocalDate fechaFinEstimada;
    private Double latitud;
    private Double longitud;
    private int radioMetros;
    private boolean exigeCodigo;
    private String secretoCodigo;
    private EstadoObra estado = EstadoObra.PLANIFICADA;
    private LocalDate fechaInicioReal;
    private LocalDate fechaConclusion;
    private final List<RetrasoObra> retrasos = new ArrayList<>();

    public static final int RADIO_MINIMO = 20;
    public static final int RADIO_MAXIMO = 2000;
    private static final int VENTANA_CODIGO = 2;
    private static final double TOLERANCIA_MAXIMA = 100;

    public Obra(String codigo, String nombre, String ubicacion, LocalDate fechaInicio, LocalDate fechaFinEstimada) {
        if (codigo == null || !codigo.matches("[A-Z]{2,4}-\\d{2,4}")) {
            throw new DatoInvalidoException("El código de obra debe tener el formato OB-01.");
        }
        this.codigo = codigo;
        setNombre(nombre);
        setUbicacion(ubicacion);
        setFechas(fechaInicio, fechaFinEstimada);
    }

    public void iniciar(LocalDate hoy) {
        if (estado != EstadoObra.PLANIFICADA) {
            throw new OperacionInvalidaException("La obra «" + nombre + "» ya está " + estado.getNombre().toLowerCase() + ".");
        }
        estado = EstadoObra.EN_CURSO;
        fechaInicioReal = hoy;
    }

    public void concluir(LocalDate hoy) {
        if (estado != EstadoObra.EN_CURSO) {
            throw new OperacionInvalidaException("Solo se puede concluir una obra en curso. «" + nombre + "» está "
                    + estado.getNombre().toLowerCase() + ".");
        }
        estado = EstadoObra.CONCLUIDA;
        fechaConclusion = hoy;
    }

    public RetrasoObra reportarRetraso(Firma quien, String motivo, LocalDate nuevoFin, LocalDateTime ahora) {
        if (estado == EstadoObra.CONCLUIDA) {
            throw new OperacionInvalidaException("La obra «" + nombre + "» ya concluyó.");
        }
        RetrasoObra retraso = new RetrasoObra(retrasos.size() + 1, ahora, quien, motivo, fechaFinEstimada, nuevoFin);
        fechaFinEstimada = nuevoFin;
        retrasos.add(retraso);
        return retraso;
    }

    public RetrasoObra buscarRetraso(int id) {
        for (RetrasoObra r : retrasos) {
            if (r.getId() == id) {
                return r;
            }
        }
        throw new DatoInvalidoException("La obra «" + nombre + "» no tiene el retraso #" + id + ".");
    }

    public List<RetrasoObra> getRetrasos() {
        return List.copyOf(retrasos);
    }

    public List<RetrasoObra> retrasosSinRevisar() {
        return retrasos.stream().filter(r -> !r.estaRevisado()).toList();
    }

    public void agregarRetraso(RetrasoObra retraso) {
        retrasos.add(retraso);
    }

    public void restaurarCiclo(EstadoObra estado, LocalDate inicioReal, LocalDate conclusion) {
        this.estado = estado;
        this.fechaInicioReal = inicioReal;
        this.fechaConclusion = conclusion;
    }

    public boolean admitePersonal() {
        return estado != EstadoObra.CONCLUIDA;
    }

    public EstadoObra getEstado() {
        return estado;
    }

    public LocalDate getFechaInicioReal() {
        return fechaInicioReal;
    }

    public LocalDate getFechaConclusion() {
        return fechaConclusion;
    }

    public int porcentajeTranscurrido(LocalDate hoy) {
        if (estado == EstadoObra.CONCLUIDA) {
            return 100;
        }
        if (estado == EstadoObra.PLANIFICADA || !hoy.isAfter(fechaInicio)) {
            return 0;
        }
        if (!hoy.isBefore(fechaFinEstimada)) {
            return 100;
        }
        long total = fechaFinEstimada.toEpochDay() - fechaInicio.toEpochDay();
        return (int) Math.round(100.0 * (hoy.toEpochDay() - fechaInicio.toEpochDay()) / total);
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public final void setNombre(String nombre) {
        if (nombre == null || nombre.trim().length() < 3) {
            throw new DatoInvalidoException("El nombre de la obra debe tener al menos 3 caracteres.");
        }
        this.nombre = nombre.trim();
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public final void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion == null ? "" : ubicacion.trim();
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFinEstimada() {
        return fechaFinEstimada;
    }

    public final void setFechas(LocalDate inicio, LocalDate finEstimada) {
        if (inicio == null || finEstimada == null || !finEstimada.isAfter(inicio)) {
            throw new DatoInvalidoException("La fecha estimada de fin debe ser posterior al inicio de la obra.");
        }
        this.fechaInicio = inicio;
        this.fechaFinEstimada = finEstimada;
    }

    public void configurarControl(Double latitud, Double longitud, Integer radio, boolean exigeCodigo) {
        if ((latitud == null) != (longitud == null)) {
            throw new DatoInvalidoException("Indica la latitud y la longitud de la obra, o deja ambas vacías.");
        }
        if (latitud != null && (latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180)) {
            throw new DatoInvalidoException("La ubicación de la obra no es válida.");
        }
        int r = radio == null ? 150 : radio;
        if (latitud != null && (r < RADIO_MINIMO || r > RADIO_MAXIMO)) {
            throw new DatoInvalidoException("El radio de la obra debe estar entre " + RADIO_MINIMO + " y " + RADIO_MAXIMO + " metros.");
        }
        this.latitud = latitud;
        this.longitud = longitud;
        this.radioMetros = latitud == null ? 0 : r;
        this.exigeCodigo = exigeCodigo;
        if (exigeCodigo && secretoCodigo == null) {
            secretoCodigo = Totp.nuevoSecreto();
        }
    }

    public void restaurarControl(Double latitud, Double longitud, int radio, boolean exigeCodigo, String secreto) {
        this.latitud = latitud;
        this.longitud = longitud;
        this.radioMetros = radio;
        this.exigeCodigo = exigeCodigo;
        this.secretoCodigo = secreto;
    }

    public boolean tieneGeocerca() {
        return latitud != null && longitud != null && radioMetros > 0;
    }

    public boolean controlaAsistencia() {
        return tieneGeocerca() || exigeCodigo;
    }

    public String codigoAsistencia(Instant ahora) {
        if (!exigeCodigo) {
            throw new DatoInvalidoException("La obra " + nombre + " no usa código de asistencia.");
        }
        return Totp.codigo(secretoCodigo, Totp.paso(ahora));
    }

    public double distanciaMetros(double lat, double lon) {
        double radioTierra = 6_371_000;
        double dLat = Math.toRadians(lat - latitud);
        double dLon = Math.toRadians(lon - longitud);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(latitud)) * Math.cos(Math.toRadians(lat))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * radioTierra * Math.asin(Math.min(1, Math.sqrt(a)));
    }

    public List<String> motivosDeDuda(DatosMarca marca) {
        List<String> motivos = new ArrayList<>();
        if (exigeCodigo) {
            if (!marca.tieneCodigo()) {
                motivos.add("sin el código de la obra");
            } else if (Totp.verificar(secretoCodigo, marca.codigo(), marca.momento(), VENTANA_CODIGO) == Totp.SIN_COINCIDENCIA) {
                throw new DatoInvalidoException("El código de la obra no es correcto o ya cambió. Escanea el QR otra vez "
                        + "o escribe el código que se ve ahora.");
            }
        }
        if (tieneGeocerca()) {
            if (!marca.tieneUbicacion()) {
                motivos.add("sin ubicación");
            } else {
                double distancia = distanciaMetros(marca.latitud(), marca.longitud());
                double tolerancia = marca.precision() == null ? 0 : Math.min(Math.max(marca.precision(), 0), TOLERANCIA_MAXIMA);
                if (distancia > radioMetros + tolerancia) {
                    motivos.add("a " + textoDistancia(distancia) + " de la obra");
                }
            }
        }
        return motivos;
    }

    private static String textoDistancia(double metros) {
        return metros < 1000 ? Math.round(metros) + " m" : String.format(Locale.forLanguageTag("es"), "%.1f km", metros / 1000);
    }

    public Double getLatitud() {
        return latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public int getRadioMetros() {
        return radioMetros;
    }

    public boolean exigeCodigo() {
        return exigeCodigo;
    }

    public String getSecretoCodigo() {
        return secretoCodigo;
    }

    @Override
    public String toString() {
        return codigo + " · " + nombre;
    }
}
