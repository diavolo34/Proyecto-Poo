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
import construtrack.modelo.novedades.EstadoNovedad;
import construtrack.modelo.novedades.FabricaNovedades;
import construtrack.modelo.novedades.Novedad;
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
    @DisplayName("Guardar y volver a cargar conserva personas, asistencia, permisos, cuentas e invitaciones")
    void idaYVuelta() {
        FabricaNovedades fabrica = FabricaNovedades.conTiposPorDefecto();
        Datos d = new Datos();
        d.jose.marcarEntrada(AHORA);
        Map<String, String> datos = new java.util.HashMap<>(Datos.reposoCompleto());
        datos.put("centro", "Clínica | Central;1");
        Novedad reposo = fabrica.crear("REPOSO_MEDICO", Datos.novedad(7, d.jose, d.ana, HOY, HOY.plusDays(1), datos));
        d.ana.registrarNovedad(reposo);
        d.gerente.revisar(reposo, EstadoNovedad.RECHAZADA, "Falta firma\ndel médico", AHORA);
        Cuenta cuenta = new Cuenta("pablo@correo.com", "pablo123", "Pablo", "Rivas", "28555111", "04121234567", AHORA);
        cuenta.marcarVerificada();
        d.empresa.agregarCuenta(cuenta);
        Invitacion inv = d.gerente.crearInvitacion("Empleado", "20000001", "", 7, AHORA, new Random(5));
        inv.canjear(cuenta, d.empresa, AHORA);

        d.laura.setFechaIngreso(HOY.minusDays(3));
        Path archivo = carpeta.resolve("datos.txt");
        new RepositorioTexto(archivo, () -> d.empresa, fabrica).guardar(d.empresa.getPlataforma());
        RepositorioTexto repositorio = new RepositorioTexto(archivo, () -> {
            throw new AssertionError("El archivo existe: no debe crear datos iniciales");
        }, fabrica);
        Empresa cargada = repositorio.cargar().empresaDemo();

        Usuario jose = cargada.buscarUsuario("30000001");
        assertTrue(jose.estaPresente());
        assertEquals("Ana Rodríguez", ((Empleado) jose).getSupervisor().getNombre());
        Novedad i = jose.getHistorial().getNovedades().get(0);
        assertEquals(EstadoNovedad.RECHAZADA, i.getEstado());
        assertEquals("Falta firma\ndel médico", i.getNota(), "Los saltos de línea se conservan");
        assertEquals("Clínica | Central;1", i.getExtras().get("centro"), "El separador | no rompe el archivo");
        assertEquals("Ana Rodríguez", i.getRegistradaPor().nombre());
        assertEquals("Carlos Méndez", i.getRevisadaPor().nombre());
        assertEquals(8, repositorio.siguienteIdNovedad());

        assertEquals(HOY.minusDays(3), cargada.buscarUsuario("30000004").getFechaIngreso());
        assertEquals(null, jose.getFechaIngreso(), "Sin fecha de ingreso sigue sin límite");
        Usuario pablo = cargada.buscarUsuario("28555111");
        assertNotNull(pablo.getCuenta());
        assertEquals(AHORA.toLocalDate(), pablo.getFechaIngreso(), "Al unirse con invitación se guarda el día de ingreso");
        assertNotNull(cargada.getPlataforma().autenticar("28555111", "pablo123"));
        assertTrue(cargada.buscarCuenta("pablo@correo.com").verificarClave("pablo123"));
        assertEquals(EstadoInvitacion.USADA, cargada.listarInvitaciones().get(0).estadoEn(AHORA));
    }

    @Test
    @DisplayName("Si el archivo no existe, se crean los datos iniciales y se guardan")
    void datosIniciales() {
        Path archivo = carpeta.resolve("nuevo.txt");
        FabricaNovedades fabrica = FabricaNovedades.conTiposPorDefecto();
        Empresa empresa = new RepositorioTexto(archivo, new GeneradorDatosPrueba(Datos.RELOJ, fabrica), fabrica).cargar().empresaDemo();
        assertEquals(9, empresa.listarUsuarios().size());
        assertTrue(archivo.toFile().exists());
        assertNotNull(empresa.buscarCuenta("carlos.mendez@construtrack.demo"));
    }

    @Test
    @DisplayName("Archivos viejos: la cédula con letras se sigue cargando y sus jornadas no entran en horas extra")
    void compatibleConDatosViejos() throws java.io.IOException {
        Path archivo = carpeta.resolve("viejo.txt");
        java.nio.file.Files.writeString(archivo, String.join("\n",
                "EMPRESA|ConstruTrack",
                "U|Supervisor|V29955199|Persona Vieja|" + "a".repeat(64) + "|LU,MA,MI,JU,VI|07:00|16:00|false",
                "A|V29955199|2026-11-16|06:45|18:10|false"));
        FabricaNovedades fabrica = FabricaNovedades.conTiposPorDefecto();
        Empresa empresa = new RepositorioTexto(archivo, () -> null, fabrica).cargar().empresaDemo();
        Usuario vieja = empresa.buscarUsuario("V29955199");
        assertEquals(construtrack.modelo.EstadoHorasExtra.NO_APLICA, vieja.getAsistencias().get(0).getEstadoExtra());
        assertTrue(vieja.getHistorial().horasExtraPendientes(vieja.getHorario()).isEmpty());
    }

    @Test
    @DisplayName("Con datos iniciales, el siguiente registro no repite un número existente")
    void contadorTrasDatosIniciales() {
        FabricaNovedades fabrica = FabricaNovedades.conTiposPorDefecto();
        RepositorioTexto repositorio = new RepositorioTexto(carpeta.resolve("nuevo.txt"),
                new GeneradorDatosPrueba(Datos.RELOJ, fabrica), fabrica);
        Empresa empresa = repositorio.cargar().empresaDemo();
        int mayor = empresa.listarUsuarios().stream()
                .flatMap(u -> u.getHistorial().getNovedades().stream())
                .mapToInt(Novedad::getId).max().orElse(0);
        assertTrue(mayor > 0, "Los datos de prueba traen permisos y reportes");
        assertEquals(mayor + 1, repositorio.siguienteIdNovedad());
    }

    @Test
    @DisplayName("Funciona con cualquier almacén (por ejemplo, uno en memoria como Upstash)")
    void almacenEnMemoria() {
        FabricaNovedades fabrica = FabricaNovedades.conTiposPorDefecto();
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
        }, fabrica).cargar().empresaDemo();
        assertEquals(9, otra.listarUsuarios().size());
        assertNotNull(otra.autenticar("10000001", "gerente123"));
    }

    @Test
    @DisplayName("En modo producción arranca vacío: sin empresas ni usuarios de prueba")
    void produccionArrancaVacio() {
        FabricaNovedades fabrica = FabricaNovedades.conTiposPorDefecto();
        AlmacenMemoria memoria = new AlmacenMemoria();
        var plataforma = new RepositorioTexto(memoria, () -> null, fabrica).cargar();
        assertEquals(0, plataforma.listarEmpresas().size());
        assertEquals(null, plataforma.autenticar("10000001", "gerente123"));
        assertEquals(0, new RepositorioTexto(memoria, () -> null, fabrica).cargar().listarEmpresas().size());
    }
}
