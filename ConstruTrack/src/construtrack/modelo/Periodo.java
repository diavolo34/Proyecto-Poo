package construtrack.modelo;

import construtrack.modelo.excepciones.DatoInvalidoException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class Periodo {
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LocalDate desde;
    private final LocalDate hasta;

    public Periodo(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new DatoInvalidoException("La fecha final del período no puede ser anterior a la inicial.");
        }
        this.desde = desde;
        this.hasta = hasta;
    }

    public static Periodo semanaDe(LocalDate fecha) {
        LocalDate lunes = fecha.with(DayOfWeek.MONDAY);
        return new Periodo(lunes, lunes.plusDays(6));
    }

    public static Periodo mesDe(LocalDate fecha) {
        return new Periodo(fecha.withDayOfMonth(1), fecha.withDayOfMonth(fecha.lengthOfMonth()));
    }

    public static Periodo ultimosDias(LocalDate hasta, int dias) {
        return new Periodo(hasta.minusDays(dias - 1L), hasta);
    }

    public boolean contiene(LocalDate fecha) {
        return !fecha.isBefore(desde) && !fecha.isAfter(hasta);
    }

    public List<LocalDate> fechas() {
        List<LocalDate> fechas = new ArrayList<>();
        for (LocalDate f = desde; !f.isAfter(hasta); f = f.plusDays(1)) {
            fechas.add(f);
        }
        return fechas;
    }

    public long cantidadDias() {
        return ChronoUnit.DAYS.between(desde, hasta) + 1;
    }

    public LocalDate getDesde() {
        return desde;
    }

    public LocalDate getHasta() {
        return hasta;
    }

    @Override
    public String toString() {
        return FORMATO.format(desde) + " – " + FORMATO.format(hasta);
    }
}
