package construtrack.servicio;

import construtrack.modelo.Empresa;
import construtrack.modelo.RegistroAsistencia;
import construtrack.modelo.Usuario;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;
import java.time.LocalDateTime;

/** Casos de uso: marcar entrada y salida de la obra. */
public class ServicioAsistencia extends ServicioBase {

    public ServicioAsistencia(Empresa empresa, EscritorDatos escritor, Clock reloj) {
        super(empresa, escritor, reloj);
    }

    public RegistroAsistencia marcarEntrada(Usuario usuario) {
        return modificar(() -> usuario.marcarEntrada(LocalDateTime.now(reloj)));
    }

    public RegistroAsistencia marcarSalida(Usuario usuario) {
        return modificar(() -> usuario.marcarSalida(LocalDateTime.now(reloj)));
    }
}
