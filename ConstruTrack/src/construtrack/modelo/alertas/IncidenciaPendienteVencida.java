package construtrack.modelo.alertas;

import construtrack.modelo.Periodo;
import construtrack.modelo.Usuario;
import construtrack.modelo.incidencias.Incidencia;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class IncidenciaPendienteVencida implements ReglaAlerta {
    private final long horasLimite;

    public IncidenciaPendienteVencida(long horasLimite) {
        this.horasLimite = horasLimite;
    }

    @Override
    public List<Alerta> evaluar(Usuario empleado, Periodo periodo, LocalDate hoy, LocalDateTime ahora) {
        List<Alerta> alertas = new ArrayList<>();
        for (Incidencia incidencia : empleado.getHistorial().getIncidencias()) {
            long horas = incidencia.horasSinResolver(ahora);
            if (horas > horasLimite) {
                alertas.add(Alerta.de(Nivel.MEDIA, empleado, "Incidencia #" + incidencia.getId()
                        + " (" + incidencia.getNombreTipo() + ") sin resolver desde hace " + horas + " h.",
                        incidencia.getFechaReporte(), ahora));
            }
        }
        return alertas;
    }
}
