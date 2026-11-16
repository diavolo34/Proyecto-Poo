package construtrack.servicio;

import construtrack.modelo.CodigoInvitacion;
import construtrack.modelo.Cuenta;
import construtrack.modelo.Empresa;
import construtrack.modelo.Invitacion;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.NoEncontradoException;
import construtrack.persistencia.EscritorDatos;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

/** Casos de uso públicos: crear una cuenta con correo y unirse a la empresa con un código de invitación. */
public class ServicioRegistro extends ServicioBase {

    public ServicioRegistro(Empresa empresa, EscritorDatos escritor, Clock reloj) {
        super(empresa, escritor, reloj);
    }

    /**
     * Crea una cuenta. Datos: correo, clave, confirmacion, nombre, apellido, documento, telefono.
     * La cuenta queda sin empresa hasta que canjee una invitación.
     */
    public Cuenta registrar(Map<String, String> datos) {
        String clave = datos.get("clave");
        if (clave == null || !clave.equals(datos.get("confirmacion"))) {
            throw new DatoInvalidoException("Las dos claves no coinciden. Escríbelas de nuevo.");
        }
        Usuario.validarCedulaNueva(datos.get("documento"));
        return modificar(() -> {
            Cuenta cuenta = new Cuenta(datos.get("correo"), clave, datos.get("nombre"), datos.get("apellido"),
                    datos.get("documento"), datos.get("telefono"), LocalDateTime.now(reloj));
            empresa.agregarCuenta(cuenta);
            return cuenta;
        });
    }

    /** Canjea un código de invitación: crea la ficha de la persona en la empresa con el rol de la invitación. */
    public Usuario unirse(Cuenta cuenta, String textoCodigo) {
        CodigoInvitacion codigo = CodigoInvitacion.de(textoCodigo);
        return modificar(() -> {
            Invitacion invitacion = empresa.buscarInvitacion(codigo);
            if (invitacion == null) {
                throw new NoEncontradoException("No encontramos la invitación " + codigo + ". Revisa el código o pide uno nuevo.");
            }
            return invitacion.canjear(cuenta, empresa, LocalDateTime.now(reloj));
        });
    }
}
