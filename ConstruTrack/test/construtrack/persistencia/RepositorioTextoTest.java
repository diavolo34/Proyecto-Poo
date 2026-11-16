package construtrack.persistencia;

import static construtrack.Datos.AHORA;
import static construtrack.Datos.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import construtrack.Datos;
import construtrack.modelo.Cuenta;
import construtrack.modelo.Empleado;
import construtrack.modelo.Empresa;
import construtrack.modelo.EstadoInvitacion;
import construtrack.modelo.Invitacion;
import construtrack.modelo.Usuario;
import construtrack.modelo.incidencias.EstadoIncidencia;
import construtrack.modelo.incidencias.FabricaIncidencias;
import construtrack.modelo.incidencias.Incidencia;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepositorioTextoTest {

    @TempDir
    Path carpeta;

    @Test
    @DisplayName("Guardar y volver a cargar conserva personas, asistencia, incidencias, cuentas e invitaciones")
    void idaYVuelta() {
        FabricaIncidencias fabrica = FabricaIncidencias.conTiposPorDefecto();
        Datos d = new Datos();
        d.jose.marcarEntrada(AHORA);
        Incidencia reposo = fabrica.crear("REPOSO_MEDICO", Datos.incidencia(7, d.jose, d.jose, HOY, HOY.plusDays(1),
                Map.of("centroMedico", "Clínica | Central", "certificado", "CM;1")));
        d.jose.reportar(reposo);
        d.ana.revisar(reposo, EstadoIncidencia.RECHAZADA, "Falta firma\ndel médico", AHORA);
        Cuenta cuenta = new Cuenta("pablo@correo.com", "pablo123", "Pablo", "Rivas", "28555111", "04121234567", AHORA);
        d.empresa.agregarCuenta(cuenta);
        Invitacion inv = d.jefe.crearInvitacion("Empleado", "20000001", "", 7, AHORA, new Random(5));
        inv.canjear(cuenta, d.empresa, AHORA);

        Path archivo = carpeta.resolve("datos.txt");
        new RepositorioTexto(archivo, () -> d.empresa, fabrica).guardar(d.empresa);
        RepositorioTexto repositorio = new RepositorioTexto(archivo, () -> {
            throw new AssertionError("El archivo existe: no debe crear datos iniciales");
        }, fabrica);
        Empresa cargada = repositorio.cargar();

        Usuario jose = cargada.buscarUsuario("30000001");
        assertTrue(jose.estaPresente());
        assertEquals("Ana Rodríguez", ((Empleado) jose).getSupervisor().getNombre());
        Incidencia i = jose.getHistorial().getIncidencias().get(0);
        assertEquals(EstadoIncidencia.RECHAZADA, i.getEstado());
        assertEquals("Falta firma\ndel médico", i.getComentarioRevision(), "Los saltos de línea se conservan");
        assertEquals("Clínica | Central", i.getExtras().get("centroMedico"), "El separador | no rompe el archivo");
        assertEquals(8, repositorio.siguienteIdIncidencia());

        Usuario pablo = cargada.buscarUsuario("28555111");
        assertNotNull(pablo.getCuenta());
        assertNotNull(cargada.autenticar("28555111", "pablo123"));
        assertTrue(cargada.buscarCuenta("pablo@correo.com").verificarClave("pablo123"));
        assertEquals(EstadoInvitacion.USADA, cargada.listarInvitaciones().get(0).estadoEn(AHORA));
    }

    @Test
    @DisplayName("Si el archivo no existe, se crean los datos iniciales y se guardan")
    void datosIniciales() {
        Path archivo = carpeta.resolve("nuevo.txt");
        FabricaIncidencias fabrica = FabricaIncidencias.conTiposPorDefecto();
        Empresa empresa = new RepositorioTexto(archivo, new GeneradorDatosPrueba(Datos.RELOJ, fabrica), fabrica).cargar();
        assertEquals(9, empresa.listarUsuarios().size());
        assertTrue(archivo.toFile().exists());
        assertNotNull(empresa.buscarCuenta("carlos.mendez@construtrack.demo"));
    }

    @Test
    @DisplayName("Con datos iniciales, la siguiente incidencia no repite un número existente")
    void contadorTrasDatosIniciales() {
        FabricaIncidencias fabrica = FabricaIncidencias.conTiposPorDefecto();
        RepositorioTexto repositorio = new RepositorioTexto(carpeta.resolve("nuevo.txt"),
                new GeneradorDatosPrueba(Datos.RELOJ, fabrica), fabrica);
        Empresa empresa = repositorio.cargar();
        int mayor = empresa.listarUsuarios().stream()
                .flatMap(u -> u.getHistorial().getIncidencias().stream())
                .mapToInt(Incidencia::getId).max().orElse(0);
        assertTrue(mayor > 0, "Los datos de prueba traen incidencias");
        assertEquals(mayor + 1, repositorio.siguienteIdIncidencia());
    }

    @Test
    @DisplayName("Funciona con cualquier almacén (por ejemplo, uno en memoria como Upstash)")
    void almacenEnMemoria() {
        FabricaIncidencias fabrica = FabricaIncidencias.conTiposPorDefecto();
        StringBuilder guardado = new StringBuilder();
        Almacen memoria = new Almacen() {
            @Override
            public Optional<String> leer() {
                return guardado.length() == 0 ? Optional.empty() : Optional.of(guardado.toString());
            }

            @Override
            public void escribir(String contenido) {
                guardado.setLength(0);
                guardado.append(contenido);
            }

            @Override
            public String descripcion() {
                return "memoria";
            }
        };
        new RepositorioTexto(memoria, new GeneradorDatosPrueba(Datos.RELOJ, fabrica), fabrica).cargar();
        Empresa otra = new RepositorioTexto(memoria, () -> {
            throw new AssertionError("Ya hay datos: no debe generarlos otra vez");
        }, fabrica).cargar();
        assertEquals(9, otra.listarUsuarios().size());
        assertNotNull(otra.autenticar("10000001", "jefe123"));
    }
}
