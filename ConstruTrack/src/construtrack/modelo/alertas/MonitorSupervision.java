package construtrack.modelo.alertas;

import construtrack.modelo.Empleado;
import construtrack.modelo.Periodo;
import construtrack.modelo.Usuario;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Aplica un conjunto de reglas a los empleados y reúne las alertas, ordenadas por prioridad.
 * Recibe las reglas por constructor (inversión de dependencias): las pruebas pueden usar reglas propias.
 */
public class MonitorSupervision {

    private final List<ReglaAlerta> reglas;

    public MonitorSupervision(List<ReglaAlerta> reglas) {
        this.reglas = List.copyOf(reglas);
    }

    /** Reglas de ConstruTrack: 3 retardos en el período, ausencias sin justificar, incidencias sin resolver >48 h. */
    public static MonitorSupervision porDefecto() {
        return new MonitorSupervision(List.<ReglaAlerta>of(
                new RetardosReiterados(3),
                new AusenciaSinJustificar(),
                new IncidenciaPendienteVencida(48)));
    }

    public List<Alerta> evaluar(List<Usuario> personas, Periodo periodo, LocalDate hoy, LocalDateTime ahora) {
        List<Alerta> alertas = new ArrayList<>();
        for (Usuario persona : personas) {
            if (persona instanceof Empleado) {
                for (ReglaAlerta regla : reglas) {
                    alertas.addAll(regla.evaluar(persona, periodo, hoy, ahora));
                }
            }
        }
        // De la más antigua a la más reciente: lo que lleva más tiempo sin atenderse va primero
        alertas.sort(Comparator.comparing(Alerta::getDesde).thenComparing(Alerta::getNivel).thenComparing(Alerta::getNombre));
        return alertas;
    }
}
