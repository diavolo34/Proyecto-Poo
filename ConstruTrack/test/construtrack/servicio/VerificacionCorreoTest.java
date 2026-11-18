package construtrack.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import construtrack.CorreoFalso;
import construtrack.Datos;
import construtrack.RelojAjustable;
import construtrack.modelo.Cuenta;
import construtrack.modelo.Plataforma;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.DemasiadosIntentosException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.modelo.novedades.FabricaNovedades;
import construtrack.persistencia.AlmacenMemoria;
import construtrack.persistencia.RepositorioMemoria;
import construtrack.persistencia.RepositorioTexto;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VerificacionCorreoTest {
    private final Datos d = new Datos();
    private final RelojAjustable reloj = new RelojAjustable(Datos.RELOJ);
    private final RepositorioMemoria repositorio = new RepositorioMemoria(d.empresa);
    private final CorreoFalso correo = new CorreoFalso();
    private final Servicios servicios = new Servicios(d.empresa.getPlataforma(), repositorio, reloj, 1, correo);

    private Cuenta registrar(String tipo) {
        Map<String, String> datos = new java.util.HashMap<>(Map.of("correo", "pablo@correo.com", "clave", "pablo123",
                "confirmacion", "pablo123", "nombre", "Pablo", "apellido", "Rivas", "documento", "28555111",
                "telefono", "04121234567"));
        if (tipo != null) {
            datos.put("tipo", tipo);
            datos.put("empresa", "Obras Pablo");
        }
        return servicios.registro().registrar(datos);
    }

    @Test
    @DisplayName("Al registrarse llega un código y, sin confirmarlo, no puede unirse ni crear empresa")
    void sinConfirmarNoAvanza() {
        Cuenta cuenta = registrar(null);
        assertFalse(cuenta.estaVerificada());
        assertEquals("pablo@correo.com", correo.destinos.get(0));
        assertTrue(correo.ultimoCodigo().matches("\\d{6}"));
        assertNotNull(servicios.autenticacion().ingresar("pablo@correo.com", "pablo123"), "Puede entrar para confirmar");
        var inv = servicios.invitaciones().crear(d.gerente, "Empleado", "", "", 7, "");
        assertThrows(OperacionInvalidaException.class, () -> servicios.registro().unirse(cuenta, inv.getCodigo().toString()));
        assertThrows(OperacionInvalidaException.class, () -> servicios.registro().crearEmpresa(cuenta, "Obras Pablo"));

        int guardados = repositorio.getGuardados();
        servicios.registro().confirmarCorreo(cuenta, correo.ultimoCodigo());
        assertTrue(cuenta.estaVerificada());
        assertEquals(guardados + 1, repositorio.getGuardados());
        assertNotNull(servicios.registro().unirse(cuenta, inv.getCodigo().toString()));
    }

    @Test
    @DisplayName("El gerente registra su empresa, pero la empresa se crea al confirmar el correo")
    void gerenteAlConfirmar() {
        Cuenta cuenta = registrar("gerente");
        assertFalse(cuenta.perteneceAEmpresa());
        var gerente = servicios.registro().confirmarCorreo(cuenta, correo.ultimoCodigo());
        assertEquals("Obras Pablo", gerente.getEmpresa().getNombre());
    }

    @Test
    @DisplayName("Tras 5 códigos incorrectos hay que pedir otro, y el código vence a los 15 minutos")
    void intentosYVencimiento() {
        Cuenta cuenta = registrar(null);
        for (int i = 1; i < ServicioVerificacionCorreo.INTENTOS; i++) {
            DatoInvalidoException e = assertThrows(DatoInvalidoException.class,
                    () -> servicios.registro().confirmarCorreo(cuenta, "000000"));
            assertTrue(e.getMessage().contains("Te quedan"), e.getMessage());
        }
        String bueno = correo.ultimoCodigo();
        assertThrows(OperacionInvalidaException.class, () -> servicios.registro().confirmarCorreo(cuenta, "000000"));
        assertThrows(OperacionInvalidaException.class, () -> servicios.registro().confirmarCorreo(cuenta, bueno),
                "Ese código ya no sirve");

        servicios.registro().reenviarCodigo(cuenta);
        String nuevo = correo.ultimoCodigo();
        reloj.avanzar(ServicioVerificacionCorreo.VIGENCIA.plusMinutes(1));
        assertThrows(OperacionInvalidaException.class, () -> servicios.registro().confirmarCorreo(cuenta, nuevo));
        assertFalse(cuenta.estaVerificada());
    }

    @Test
    @DisplayName("Se pueden pedir como máximo 4 códigos por hora")
    void limiteDeEnvios() {
        Cuenta cuenta = registrar(null);
        for (int i = 1; i < ServicioVerificacionCorreo.ENVIOS_POR_HORA; i++) {
            servicios.registro().reenviarCodigo(cuenta);
        }
        assertThrows(DemasiadosIntentosException.class, () -> servicios.registro().reenviarCodigo(cuenta));
        reloj.avanzar(Duration.ofHours(1));
        servicios.registro().reenviarCodigo(cuenta);
        assertEquals(ServicioVerificacionCorreo.ENVIOS_POR_HORA + 1, correo.codigos.size());
    }

    @Test
    @DisplayName("Restablecer la clave con el enlace del correo también confirma el correo")
    void restablecerConfirma() {
        Cuenta cuenta = registrar(null);
        servicios.recuperacion().solicitar("pablo@correo.com");
        servicios.recuperacion().restablecer(correo.ultimoToken(), "nueva2026", "nueva2026");
        assertTrue(cuenta.estaVerificada());
    }

    @Test
    @DisplayName("Se guarda si la cuenta está confirmada; las cuentas antiguas, sin ese dato, quedan confirmadas")
    void persistencia() {
        Cuenta cuenta = registrar(null);
        FabricaNovedades fabrica = FabricaNovedades.conTiposPorDefecto();
        AlmacenMemoria almacen = new AlmacenMemoria();
        new RepositorioTexto(almacen, () -> null, fabrica).guardar(d.empresa.getPlataforma());
        Plataforma cargada = new RepositorioTexto(almacen, () -> null, fabrica).cargar();
        assertFalse(cargada.buscarCuenta(cuenta.getCorreo()).estaVerificada());

        StringBuilder antiguo = new StringBuilder();
        for (String linea : almacen.leer().orElseThrow().split("\n")) {
            antiguo.append(linea.startsWith("C|pablo@correo.com") ? linea.substring(0, linea.lastIndexOf('|')) : linea).append('\n');
        }
        almacen.escribir(antiguo.toString());
        assertTrue(new RepositorioTexto(almacen, () -> null, fabrica).cargar().buscarCuenta(cuenta.getCorreo()).estaVerificada());
    }
}
