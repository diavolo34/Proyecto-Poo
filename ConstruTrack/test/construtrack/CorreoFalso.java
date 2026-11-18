package construtrack;

import construtrack.modelo.Invitacion;
import construtrack.servicio.Notificador;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class CorreoFalso implements Notificador {
    public final List<String> destinos = new ArrayList<>();
    public final List<String> tokens = new ArrayList<>();
    public final List<String> codigos = new ArrayList<>();

    @Override
    public void enviarInvitacion(Invitacion invitacion, String correoDestino) {
        destinos.add(correoDestino);
    }

    @Override
    public void enviarRestablecimiento(String correoDestino, String token, Duration vigencia) {
        destinos.add(correoDestino);
        tokens.add(token);
    }

    @Override
    public void enviarCodigoVerificacion(String correoDestino, String codigo, Duration vigencia) {
        destinos.add(correoDestino);
        codigos.add(codigo);
    }

    public String ultimoToken() {
        return tokens.get(tokens.size() - 1);
    }

    public String ultimoCodigo() {
        return codigos.get(codigos.size() - 1);
    }
}
