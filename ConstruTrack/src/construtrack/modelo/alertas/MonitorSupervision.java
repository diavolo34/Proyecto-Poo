package construtrack.modelo.alertas;

import construtrack.modelo.Gerente;
import construtrack.modelo.Periodo;
import construtrack.modelo.Usuario;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MonitorSupervision {
    private final List<ReglaAlerta> reglas;

    public MonitorSupervision(List<ReglaAlerta> reglas) {
        this.reglas = List.copyOf(reglas);
    }

    public static MonitorSupervision porDefecto() {
        return new MonitorSupervision(List.<ReglaAlerta>of(
                new RetardosReiterados(3),
                new AusenciaSinJustificar(),
                new FaltasConsecutivas(2),
                new NovedadPendienteVencida(48)));
    }

    public List<Alerta> evaluar(List<Usuario> personas, Periodo periodo, LocalDate hoy, LocalDateTime ahora) {
        List<Alerta> alertas = new ArrayList<>();
        for (Usuario persona : personas) {
            if (!(persona instanceof Gerente)) {
                for (ReglaAlerta regla : reglas) {
                    alertas.addAll(regla.evaluar(persona, periodo, hoy, ahora));
                }
            }
        }

        alertas.sort(Comparator.comparing(Alerta::getDesde).thenComparing(Alerta::getNivel).thenComparing(Alerta::getNombre));
        return alertas;
    }
}
