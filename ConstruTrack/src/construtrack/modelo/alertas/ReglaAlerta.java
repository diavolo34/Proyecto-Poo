package construtrack.modelo.alertas;

import construtrack.modelo.Periodo;
import construtrack.modelo.Usuario;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Una regla que revisa a un empleado y devuelve las alertas que correspondan.
 * Para agregar una regla nueva basta con implementar esta interfaz y registrarla en el monitor:
 * el monitor y el resto del sistema no cambian (principio abierto/cerrado).
 */
public interface ReglaAlerta {

    List<Alerta> evaluar(Usuario empleado, Periodo periodo, LocalDate hoy, LocalDateTime ahora);
}
