package construtrack.servicio;

import construtrack.modelo.Plataforma;
import construtrack.modelo.Empresa;
import construtrack.modelo.RegistroAsistencia;
import construtrack.modelo.Usuario;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;
import java.time.LocalDateTime;

public class ServicioAsistencia extends ServicioBase {
    public ServicioAsistencia(Plataforma plataforma, EscritorDatos escritor, Clock reloj) {
        super(plataforma, escritor, reloj);
    }

    public RegistroAsistencia marcarEntrada(Usuario usuario) {
        return modificar(() -> usuario.marcarEntrada(LocalDateTime.now(reloj)));
    }

    public RegistroAsistencia marcarSalida(Usuario usuario) {
        return modificar(() -> usuario.marcarSalida(LocalDateTime.now(reloj)));
    }
}
