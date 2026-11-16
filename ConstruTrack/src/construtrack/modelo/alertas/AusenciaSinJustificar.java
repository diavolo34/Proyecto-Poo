package construtrack.modelo.alertas;

import construtrack.modelo.Periodo;
import construtrack.modelo.Usuario;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class AusenciaSinJustificar implements ReglaAlerta {
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM");

    @Override
    public List<Alerta> evaluar(Usuario empleado, Periodo periodo, LocalDate hoy, LocalDateTime ahora) {
        List<LocalDate> faltas = empleado.getHistorial().ausenciasSinJustificar(empleado.getHorario(), periodo, hoy);
        if (faltas.isEmpty()) {
            return List.of();
        }
        String fechas = faltas.stream().map(f -> f.format(FORMATO)).collect(Collectors.joining(", "));
        Nivel nivel = faltas.size() >= 2 ? Nivel.ALTA : Nivel.MEDIA;
        return List.of(Alerta.de(nivel, empleado,
                faltas.size() + " día(s) sin justificar: " + fechas + ".", faltas.get(0).atStartOfDay(), ahora));
    }
}
