package construtrack.modelo.alertas;

import construtrack.modelo.Periodo;
import construtrack.modelo.Usuario;
import construtrack.modelo.novedades.Novedad;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NovedadPendienteVencida implements ReglaAlerta {
    private final long horasLimite;

    public NovedadPendienteVencida(long horasLimite) {
        this.horasLimite = horasLimite;
    }

    @Override
    public List<Alerta> evaluar(Usuario persona, Periodo periodo, LocalDate hoy, LocalDateTime ahora) {
        List<Alerta> alertas = new ArrayList<>();
        for (Novedad novedad : persona.getHistorial().getNovedades()) {
            long horas = novedad.horasSinResolver(ahora);
            if (horas > horasLimite) {
                alertas.add(Alerta.de(Nivel.MEDIA, persona, novedad.getNombreTipo() + " #" + novedad.getId()
                        + " sin aprobar desde hace " + horas + " h.", novedad.getFechaRegistro(), ahora));
            }
        }
        return alertas;
    }
}
