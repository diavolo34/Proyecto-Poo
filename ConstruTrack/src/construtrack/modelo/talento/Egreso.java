package construtrack.modelo.talento;

import construtrack.modelo.Firma;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Egreso {
    private final int id;
    private final String cedula;
    private final String nombre;
    private final String rol;
    private final String puesto;
    private final String area;
    private final LocalDate fechaIngreso;
    private final TipoEgreso tipo;
    private final LocalDate fecha;
    private final String motivo;
    private final Firma registradoPor;
    private final LocalDateTime fechaRegistro;
    private final String voucher;
    private final String carta;
    private final boolean devolvioEquipos;
    private final String notaEquipos;
    private LocalDateTime fichaEliminada;
    private String expediente;

    public record Datos(TipoEgreso tipo, LocalDate fecha, String motivo, String voucher, String carta, Boolean devolvioEquipos,
            String notaEquipos) {
    }

    public static Egreso de(int id, Usuario persona, Datos datos, Firma registradoPor, LocalDateTime ahora) {
        if (datos.tipo() == null) {
            throw new DatoInvalidoException("Elige si es un despido o una renuncia.");
        }
        if (datos.fecha() == null || datos.fecha().isAfter(ahora.toLocalDate().plusDays(30))) {
            throw new DatoInvalidoException("Indica la fecha de egreso (como máximo 30 días hacia adelante).");
        }
        if (persona.getFechaIngreso() != null && datos.fecha().isBefore(persona.getFechaIngreso())) {
            throw new DatoInvalidoException("La fecha de egreso no puede ser anterior al ingreso (" + persona.getFechaIngreso() + ").");
        }
        String motivo = datos.motivo() == null ? "" : datos.motivo().trim();
        if (motivo.length() < 5 || motivo.length() > 500) {
            throw new DatoInvalidoException("Escribe el motivo del egreso (entre 5 y 500 caracteres).");
        }
        if (vacio(datos.voucher())) {
            throw new DatoInvalidoException("Adjunta el voucher de pago de la liquidación.");
        }
        if (datos.tipo() == TipoEgreso.RENUNCIA && vacio(datos.carta())) {
            throw new DatoInvalidoException("Adjunta la carta de renuncia firmada.");
        }
        if (datos.devolvioEquipos() == null) {
            throw new DatoInvalidoException("Indica si devolvió los equipos y herramientas a su cargo.");
        }
        String nota = datos.notaEquipos() == null ? "" : datos.notaEquipos().trim();
        if (!datos.devolvioEquipos() && nota.length() < 3) {
            throw new DatoInvalidoException("Anota qué equipos o herramientas no devolvió.");
        }
        return new Egreso(id, persona.getCedula(), persona.getNombre(), persona.getRol(), persona.getPuesto(), persona.getArea(),
                persona.getFechaIngreso(), datos.tipo(), datos.fecha(), motivo, registradoPor, ahora, datos.voucher(),
                vacio(datos.carta()) ? null : datos.carta(), datos.devolvioEquipos(), nota);
    }

    public Egreso(int id, String cedula, String nombre, String rol, String puesto, String area, LocalDate fechaIngreso,
            TipoEgreso tipo, LocalDate fecha, String motivo, Firma registradoPor, LocalDateTime fechaRegistro, String voucher,
            String carta, boolean devolvioEquipos, String notaEquipos) {
        this.id = id;
        this.cedula = cedula;
        this.nombre = nombre;
        this.rol = rol;
        this.puesto = puesto;
        this.area = area;
        this.fechaIngreso = fechaIngreso;
        this.tipo = tipo;
        this.fecha = fecha;
        this.motivo = motivo;
        this.registradoPor = registradoPor;
        this.fechaRegistro = fechaRegistro;
        this.voucher = voucher;
        this.carta = carta;
        this.devolvioEquipos = devolvioEquipos;
        this.notaEquipos = notaEquipos == null ? "" : notaEquipos;
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    public void eliminarFicha(String expediente, LocalDateTime ahora) {
        if (fichaEliminada != null) {
            throw new OperacionInvalidaException("La ficha de " + nombre + " ya fue eliminada.");
        }
        this.expediente = expediente;
        this.fichaEliminada = ahora;
    }

    public void restaurarEliminacion(LocalDateTime cuando, String expediente) {
        this.fichaEliminada = cuando;
        this.expediente = expediente;
    }

    public boolean fichaEliminada() {
        return fichaEliminada != null;
    }

    public int getId() {
        return id;
    }

    public String getCedula() {
        return cedula;
    }

    public String getNombre() {
        return nombre;
    }

    public String getRol() {
        return rol;
    }

    public String getPuesto() {
        return puesto;
    }

    public String getArea() {
        return area;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public TipoEgreso getTipo() {
        return tipo;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getMotivo() {
        return motivo;
    }

    public Firma getRegistradoPor() {
        return registradoPor;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public String getVoucher() {
        return voucher;
    }

    public String getCarta() {
        return carta;
    }

    public boolean devolvioEquipos() {
        return devolvioEquipos;
    }

    public String getNotaEquipos() {
        return notaEquipos;
    }

    public LocalDateTime getFichaEliminada() {
        return fichaEliminada;
    }

    public String getExpediente() {
        return expediente;
    }
}
