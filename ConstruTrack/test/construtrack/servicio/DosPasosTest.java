package construtrack.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import construtrack.CorreoFalso;
import construtrack.Datos;
import construtrack.RelojAjustable;
import construtrack.modelo.Plataforma;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.modelo.incidencias.FabricaIncidencias;
import construtrack.persistencia.AlmacenMemoria;
import construtrack.persistencia.RepositorioMemoria;
import construtrack.persistencia.RepositorioTexto;
import construtrack.util.Totp;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DosPasosTest {
    private final Datos d = new Datos();
    private final RelojAjustable reloj = new RelojAjustable(Datos.RELOJ);
    private final Servicios servicios = new Servicios(d.empresa.getPlataforma(), new RepositorioMemoria(d.empresa), reloj, 1,
            new CorreoFalso());

    private String codigoActual(String secreto) {
        return Totp.codigo(secreto, Totp.paso(reloj.instant()));
    }

    private List<String> activarParaCarlos() {
        ServicioDosPasos.Inscripcion inscripcion = servicios.dosPasos().iniciar(d.gerente);
        assertTrue(inscripcion.enlace().startsWith("otpauth://totp/ConstruTrack%3A10000001?secret=" + inscripcion.secreto()));
        assertThrows(DatoInvalidoException.class, () -> servicios.dosPasos().activar(d.gerente, "000000"));
        return servicios.dosPasos().activar(d.gerente, codigoActual(inscripcion.secreto()));
    }

    @Test
    @DisplayName("Solo el gerente puede activarla; al activarla recibe 8 códigos de respaldo")
    void activar() {
        assertThrows(PermisoDenegadoException.class, () -> servicios.dosPasos().iniciar(d.ana));
        List<String> respaldos = activarParaCarlos();
        assertEquals(ServicioDosPasos.RESPALDOS, respaldos.size());
        assertTrue(respaldos.get(0).matches("[A-Z2-9]{4}-[A-Z2-9]{4}"));
        assertTrue(d.gerente.tieneDosPasos());
        assertThrows(OperacionInvalidaException.class, () -> servicios.dosPasos().iniciar(d.gerente));
    }

    @Test
    @DisplayName("Con la verificación activa, la clave sola no basta: hace falta el código de la aplicación")
    void ingresoConCodigo() {
        activarParaCarlos();
        reloj.avanzar(Duration.ofSeconds(Totp.PASO_SEGUNDOS));
        Ingreso ingreso = servicios.autenticacion().ingresar("10000001", "gerente123");
        assertTrue(ingreso.requiereDosPasos());
        assertFalse(servicios.autenticacion().ingresar("20000001", "super123").requiereDosPasos());

        String desafio = servicios.dosPasos().crearDesafio(ingreso);
        assertThrows(DatoInvalidoException.class, () -> servicios.dosPasos().resolverDesafio(desafio, "123456"));
        String codigo = codigoActual(d.gerente.getSecretoDosPasos());
        assertSame(d.gerente, servicios.dosPasos().resolverDesafio(desafio, codigo).usuario());
        assertThrows(OperacionInvalidaException.class, () -> servicios.dosPasos().resolverDesafio(desafio, codigo),
                "El desafío se usa una sola vez");

        String otro = servicios.dosPasos().crearDesafio(ingreso);
        assertThrows(DatoInvalidoException.class, () -> servicios.dosPasos().resolverDesafio(otro, codigo),
                "Un mismo código no se puede repetir");
    }

    @Test
    @DisplayName("Un código de respaldo sirve una sola vez")
    void respaldo() {
        List<String> respaldos = activarParaCarlos();
        Ingreso ingreso = servicios.autenticacion().ingresar("10000001", "gerente123");
        String uno = servicios.dosPasos().crearDesafio(ingreso);
        servicios.dosPasos().resolverDesafio(uno, respaldos.get(0).toLowerCase().replace("-", " "));
        assertEquals(ServicioDosPasos.RESPALDOS - 1, d.gerente.getRespaldosDosPasos().size());
        String dos = servicios.dosPasos().crearDesafio(ingreso);
        assertThrows(DatoInvalidoException.class, () -> servicios.dosPasos().resolverDesafio(dos, respaldos.get(0)));
    }

    @Test
    @DisplayName("Tras 5 códigos incorrectos, o a los 5 minutos, hay que volver a iniciar sesión")
    void limites() {
        activarParaCarlos();
        Ingreso ingreso = servicios.autenticacion().ingresar("10000001", "gerente123");
        String desafio = servicios.dosPasos().crearDesafio(ingreso);
        for (int i = 1; i < ServicioDosPasos.INTENTOS; i++) {
            assertThrows(DatoInvalidoException.class, () -> servicios.dosPasos().resolverDesafio(desafio, "999999"));
        }
        assertThrows(OperacionInvalidaException.class, () -> servicios.dosPasos().resolverDesafio(desafio, "999999"));
        assertThrows(OperacionInvalidaException.class,
                () -> servicios.dosPasos().resolverDesafio(desafio, codigoActual(d.gerente.getSecretoDosPasos())));

        String otro = servicios.dosPasos().crearDesafio(ingreso);
        reloj.avanzar(ServicioDosPasos.VIGENCIA_DESAFIO.plusSeconds(1));
        assertThrows(OperacionInvalidaException.class,
                () -> servicios.dosPasos().resolverDesafio(otro, codigoActual(d.gerente.getSecretoDosPasos())));
    }

    @Test
    @DisplayName("Desactivarla exige la clave y un código válido")
    void desactivar() {
        activarParaCarlos();
        reloj.avanzar(Duration.ofSeconds(Totp.PASO_SEGUNDOS));
        String codigo = codigoActual(d.gerente.getSecretoDosPasos());
        assertThrows(DatoInvalidoException.class, () -> servicios.dosPasos().desactivar(d.gerente, "mala", codigo));
        assertThrows(DatoInvalidoException.class, () -> servicios.dosPasos().desactivar(d.gerente, "gerente123", "000000"));
        servicios.dosPasos().desactivar(d.gerente, "gerente123", codigo);
        assertFalse(d.gerente.tieneDosPasos());
        assertFalse(servicios.autenticacion().ingresar("10000001", "gerente123").requiereDosPasos());
    }

    @Test
    @DisplayName("El secreto y los códigos de respaldo se guardan y se recuperan al cargar")
    void persistencia() {
        activarParaCarlos();
        FabricaIncidencias fabrica = FabricaIncidencias.conTiposPorDefecto();
        AlmacenMemoria almacen = new AlmacenMemoria();
        new RepositorioTexto(almacen, () -> null, fabrica).guardar(d.empresa.getPlataforma());
        Plataforma cargada = new RepositorioTexto(almacen, () -> null, fabrica).cargar();
        var carlos = cargada.buscarUsuario("10000001");
        assertEquals(d.gerente.getSecretoDosPasos(), carlos.getSecretoDosPasos());
        assertEquals(ServicioDosPasos.RESPALDOS, carlos.getRespaldosDosPasos().size());
        assertFalse(cargada.buscarUsuario("20000001").tieneDosPasos());
    }
}
