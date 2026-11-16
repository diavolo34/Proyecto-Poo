package construtrack.modelo.alertas;

import construtrack.modelo.Periodo;
import construtrack.modelo.RegistroAsistencia;
import construtrack.modelo.Usuario;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Alerta cuando el empleado llega tarde al menos {@code umbral} veces en el período. */
public class RetardosReiterados implements ReglaAlerta {

    private final int umbral;

    public RetardosReiterados(int umbral) {
        this.umbral = umbral;
    }

    @Override
    public List<Alerta> evaluar(Usuario empleado, Periodo periodo, LocalDate hoy, LocalDateTime ahora) {
        int retardos = empleado.getHistorial().contarRetardos(periodo);
        if (retardos < umbral) {
            return List.of();
        }
        Nivel nivel = retardos >= umbral + 2 ? Nivel.ALTA : Nivel.MEDIA;
        LocalDate primero = empleado.getHistorial().asistenciasEn(periodo).stream()
                .filter(RegistroAsistencia::isTarde).map(RegistroAsistencia::getFecha).min(LocalDate::compareTo).orElse(hoy);
        return List.of(Alerta.de(nivel, empleado, retardos + " llegadas tarde en " + periodo + ".",
                primero.atStartOfDay(), ahora));
    }
}
