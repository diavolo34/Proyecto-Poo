package construtrack.servicio;

import construtrack.modelo.DatosMarca;
import construtrack.modelo.Plataforma;
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
        return marcarEntrada(usuario, null, null, null, null);
    }

    public RegistroAsistencia marcarSalida(Usuario usuario) {
        return marcarSalida(usuario, null, null, null, null);
    }

    public RegistroAsistencia marcarEntrada(Usuario usuario, String codigo, Double latitud, Double longitud, Double precision) {
        return modificar(() -> usuario.marcarEntrada(LocalDateTime.now(reloj),
                new DatosMarca(codigo, latitud, longitud, precision, reloj.instant())));
    }

    public RegistroAsistencia marcarSalida(Usuario usuario, String codigo, Double latitud, Double longitud, Double precision) {
        return modificar(() -> usuario.marcarSalida(LocalDateTime.now(reloj),
                new DatosMarca(codigo, latitud, longitud, precision, reloj.instant())));
    }
}
