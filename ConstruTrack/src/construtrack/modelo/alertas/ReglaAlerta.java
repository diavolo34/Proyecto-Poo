package construtrack.modelo.alertas;

import construtrack.modelo.Periodo;
import construtrack.modelo.Usuario;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ReglaAlerta {
    List<Alerta> evaluar(Usuario empleado, Periodo periodo, LocalDate hoy, LocalDateTime ahora);
}
