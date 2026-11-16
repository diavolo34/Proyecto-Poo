package construtrack.modelo;

import static construtrack.Datos.AHORA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import construtrack.Datos;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InvitacionTest {
    private final Datos d = new Datos();

    private Cuenta pablo() {
        Cuenta cuenta = new Cuenta("Pablo.Rivas@Correo.com", "pablo123", "Pablo", "Rivas", "28555111", "+58 424 1112233", AHORA);
        d.empresa.agregarCuenta(cuenta);
        return cuenta;
    }

    @Test
    @DisplayName("El código se normaliza (minúsculas y espacios) y se valida su formato")
    void codigoNormalizado() {
        assertEquals(CodigoInvitacion.de("CT-RQ7K-4M2P"), CodigoInvitacion.de(" ct-rq7k-4m2p "));
        assertThrows(DatoInvalidoException.class, () -> CodigoInvitacion.de("CT-0000-OOOO"));
        assertTrue(CodigoInvitacion.generar(new Random(1)).toString().matches("CT-[A-Z2-9]{4}-[A-Z2-9]{4}"));
    }

    @Test
    @DisplayName("Al canjear se crea la ficha con el rol y el supervisor de la invitación; solo funciona una vez")
    void canjeUnico() {
        Invitacion inv = d.jefe.crearInvitacion("Empleado", "20000001", "", 7, AHORA, new Random(7));
        Cuenta cuenta = pablo();
        Usuario nuevo = inv.canjear(cuenta, d.empresa, AHORA);
        assertTrue(nuevo instanceof Empleado);
        assertSame(d.ana, ((Empleado) nuevo).getSupervisor());
        assertSame(nuevo, cuenta.getUsuario());
        assertEquals(EstadoInvitacion.USADA, inv.estadoEn(AHORA));
        assertThrows(OperacionInvalidaException.class, () -> inv.canjear(cuenta, d.empresa, AHORA));
    }

    @Test
    @DisplayName("Se puede entrar con el correo o con la cédula usando la misma clave")
    void claveCompartida() {
        Invitacion inv = d.jefe.crearInvitacion("Supervisor", "", "", 7, AHORA, new Random(3));
        Cuenta cuenta = pablo();
        inv.canjear(cuenta, d.empresa, AHORA);
        assertTrue(cuenta.verificarClave("pablo123"));
        assertNotNull(d.empresa.autenticar("28555111", "pablo123"));
    }

    @Test
    @DisplayName("Una invitación vencida o cancelada no se puede usar")
    void vencidaYCancelada() {
        Invitacion vencida = d.jefe.crearInvitacion("Empleado", "", "", 1, AHORA, new Random(11));
        assertEquals(EstadoInvitacion.VENCIDA, vencida.estadoEn(AHORA.plusDays(2)));
        assertThrows(OperacionInvalidaException.class, () -> vencida.canjear(pablo(), d.empresa, AHORA.plusDays(2)));

        Invitacion cancelada = d.jefe.crearInvitacion("Empleado", "", "", 7, AHORA, new Random(12));
        cancelada.cancelar(AHORA);
        assertEquals(EstadoInvitacion.CANCELADA, cancelada.estadoEn(AHORA));
    }
}
