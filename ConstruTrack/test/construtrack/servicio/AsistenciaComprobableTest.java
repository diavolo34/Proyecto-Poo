package construtrack.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import construtrack.CorreoFalso;
import construtrack.Datos;
import construtrack.RelojAjustable;
import construtrack.modelo.EstadoVerificacion;
import construtrack.modelo.Obra;
import construtrack.modelo.Periodo;
import construtrack.modelo.Plataforma;
import construtrack.modelo.RegistroAsistencia;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.modelo.novedades.FabricaNovedades;
import construtrack.persistencia.AlmacenMemoria;
import construtrack.persistencia.RepositorioMemoria;
import construtrack.persistencia.RepositorioTexto;
import construtrack.util.Totp;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AsistenciaComprobableTest {
    private static final double LAT = 10.4806;
    private static final double LON = -66.9036;

    private final Datos d = new Datos();
    private final RelojAjustable reloj = new RelojAjustable(Datos.RELOJ);
    private final Servicios servicios = new Servicios(d.empresa.getPlataforma(), new RepositorioMemoria(d.empresa), reloj, 1,
            new CorreoFalso());
    private final Obra obra = new Obra("OB-01", "Torre Este", "Caracas", Datos.HOY.minusMonths(1), Datos.HOY.plusMonths(6));

    AsistenciaComprobableTest() {
        d.empresa.agregarObra(obra);
        d.jose.setObra(obra);
    }

    private String codigoActual() {
        return Totp.codigo(obra.getSecretoCodigo(), Totp.paso(reloj.instant()));
    }

    @Test
    @DisplayName("Sin control en la obra, la entrada con ubicación se guarda con sus coordenadas; sin ubicación queda por confirmar")
    void sinControl() {
        RegistroAsistencia r = servicios.asistencia().marcarEntrada(d.jose, null, LAT, LON, 12.0);
        assertEquals(EstadoVerificacion.NO_APLICA, r.getVerificacion());
        assertEquals(LAT, r.getUbicacionEntrada().latitud());
        reloj.avanzar(Duration.ofHours(1));
        servicios.asistencia().marcarSalida(d.jose);
        reloj.avanzar(Duration.ofHours(1));
        RegistroAsistencia sinUbicacion = servicios.asistencia().marcarEntrada(d.jose);
        assertEquals(EstadoVerificacion.POR_CONFIRMAR, sinUbicacion.getVerificacion());
        assertEquals("Entrada sin ubicación", sinUbicacion.getMotivoVerificacion());
        reloj.avanzar(Duration.ofHours(1));
        assertEquals(EstadoVerificacion.NO_APLICA, servicios.asistencia().marcarEntrada(d.gerente).getVerificacion(),
                "Al gerente no se le exige la ubicación");
    }

    @Test
    @DisplayName("Con el código vigente la marca queda verificada; sin código, por confirmar; uno incorrecto se rechaza")
    void codigoDeLaObra() {
        obra.configurarControl(null, null, null, true);
        assertThrows(DatoInvalidoException.class, () -> servicios.asistencia().marcarEntrada(d.jose, "000000", null, null, null));
        assertFalse(d.jose.estaPresente(), "Un código incorrecto no registra la marca");

        String codigo = codigoActual();
        reloj.avanzar(Duration.ofSeconds(Totp.PASO_SEGUNDOS * 2L));
        RegistroAsistencia entrada = servicios.asistencia().marcarEntrada(d.jose, codigo, LAT, LON, 10.0);
        assertEquals(EstadoVerificacion.VERIFICADA, entrada.getVerificacion(), "Un código de hace un minuto aún sirve");

        reloj.avanzar(Duration.ofHours(2));
        RegistroAsistencia salida = servicios.asistencia().marcarSalida(d.jose, null, null, null, null);
        assertEquals(EstadoVerificacion.POR_CONFIRMAR, salida.getVerificacion());
        assertEquals("Salida sin el código del proyecto", salida.getMotivoVerificacion());
    }

    @Test
    @DisplayName("El código del proyecto cambia cada 30 segundos y solo lo ven el supervisor y el gerente")
    void codigoRotativo() {
        obra.configurarControl(null, null, null, true);
        String primero = servicios.obras().codigoAsistencia(d.ana, "OB-01").codigo();
        reloj.avanzar(Duration.ofSeconds(Totp.PASO_SEGUNDOS));
        String segundo = servicios.obras().codigoAsistencia(d.gerente, "OB-01").codigo();
        assertTrue(primero.matches("\\d{6}") && segundo.matches("\\d{6}"));
        assertEquals(codigoActual(), segundo, "Es el código del paso actual");
        assertThrows(PermisoDenegadoException.class, () -> servicios.obras().codigoAsistencia(d.jose, "OB-01"));
        reloj.avanzar(Duration.ofSeconds(Totp.PASO_SEGUNDOS * 3L));
        assertThrows(DatoInvalidoException.class, () -> servicios.asistencia().marcarEntrada(d.jose, primero, LAT, LON, 10.0),
                "Un código de hace 2 minutos ya no sirve");
    }

    @Test
    @DisplayName("Con geocerca: dentro del radio queda verificada; lejos o sin ubicación, por confirmar")
    void geocerca() {
        obra.configurarControl(LAT, LON, 150, false);
        RegistroAsistencia dentro = servicios.asistencia().marcarEntrada(d.jose, null, LAT + 0.0005, LON, 10.0);
        assertEquals(EstadoVerificacion.VERIFICADA, dentro.getVerificacion());

        reloj.avanzar(Duration.ofHours(1));
        RegistroAsistencia salida = servicios.asistencia().marcarSalida(d.jose, null, LAT + 0.011, LON, 10.0);
        assertEquals(EstadoVerificacion.POR_CONFIRMAR, salida.getVerificacion());
        assertEquals("Salida a 1,2 km del proyecto", salida.getMotivoVerificacion());

        reloj.avanzar(Duration.ofMinutes(10));
        RegistroAsistencia sinUbicacion = servicios.asistencia().marcarEntrada(d.jose, null, null, null, null);
        assertEquals("Entrada sin ubicación", sinUbicacion.getMotivoVerificacion());
    }

    @Test
    @DisplayName("La precisión del GPS da margen: a 180 m con radio 150 y precisión 50 está dentro")
    void precision() {
        obra.configurarControl(LAT, LON, 150, false);
        double norte = LAT + 180 / 111_195.0;
        assertTrue(obra.motivosDeDuda(new construtrack.modelo.DatosMarca(null, norte, LON, 50.0, reloj.instant())).isEmpty());
        assertEquals(1, obra.motivosDeDuda(new construtrack.modelo.DatosMarca(null, norte, LON, 5.0, reloj.instant())).size());
        assertThrows(DatoInvalidoException.class, () -> obra.configurarControl(LAT, null, 150, false));
        assertThrows(DatoInvalidoException.class, () -> obra.configurarControl(LAT, LON, 5, false));
    }

    @Test
    @DisplayName("El supervisor confirma o rechaza; una jornada rechazada no cuenta como asistencia")
    void confirmarYRechazar() {
        obra.configurarControl(LAT, LON, 150, false);
        servicios.asistencia().marcarEntrada(d.jose, null, LAT + 0.02, LON, 10.0);
        reloj.avanzar(Duration.ofHours(2));
        servicios.asistencia().marcarSalida(d.jose, null, LAT, LON, 10.0);
        Periodo semana = Periodo.semanaDe(Datos.HOY);
        assertEquals(120, d.jose.getHistorial().minutosTrabajados(semana));

        assertEquals(1, servicios.supervision().asistenciasPorConfirmar(d.ana).size());
        assertTrue(servicios.supervision().asistenciasPorConfirmar(d.luis).isEmpty(), "Luis no supervisa a José");
        assertThrows(PermisoDenegadoException.class,
                () -> servicios.supervision().revisarAsistencia(d.luis, "30000001", Datos.HOY, null, "RECHAZADA"));
        RegistroAsistencia rechazada = servicios.supervision().revisarAsistencia(d.ana, "30000001", Datos.HOY,
                d.jose.getAsistencias().get(0).getHoraEntrada(), "rechazada");
        assertEquals(EstadoVerificacion.RECHAZADA, rechazada.getVerificacion());
        assertEquals(0, d.jose.getHistorial().minutosTrabajados(semana));
        assertNull(d.jose.getHistorial().asistenciaValidaDel(Datos.HOY));
        assertTrue(servicios.supervision().asistenciasPorConfirmar(d.gerente).isEmpty());
        assertThrows(RuntimeException.class,
                () -> servicios.supervision().revisarAsistencia(d.ana, "30000001", Datos.HOY, null, "CONFIRMADA"),
                "Ya no está por confirmar");
    }

    @Test
    @DisplayName("La configuración del proyecto y el estado de cada marca se guardan; los datos antiguos no tienen control")
    void persistencia() {
        servicios.obras().editar(d.gerente, "OB-01", Map.of("nombre", "Torre Este", "ubicacion", "Caracas",
                "inicio", obra.getFechaInicio().toString(), "fin", obra.getFechaFinEstimada().toString(),
                "latitud", "10,4806", "longitud", "-66.9036", "radio", "200", "exigeCodigo", "true"));
        servicios.asistencia().marcarEntrada(d.jose, null, null, null, null);
        FabricaNovedades fabrica = FabricaNovedades.conTiposPorDefecto();
        AlmacenMemoria almacen = new AlmacenMemoria();
        new RepositorioTexto(almacen, () -> null, fabrica).guardar(d.empresa.getPlataforma());
        Plataforma cargada = new RepositorioTexto(almacen, () -> null, fabrica).cargar();
        Obra copia = cargada.listarEmpresas().get(0).buscarObra("OB-01");
        assertEquals(10.4806, copia.getLatitud());
        assertEquals(200, copia.getRadioMetros());
        assertTrue(copia.exigeCodigo());
        assertEquals(obra.getSecretoCodigo(), copia.getSecretoCodigo());
        RegistroAsistencia marca = cargada.buscarUsuario("30000001").getAsistencias().get(0);
        assertEquals(EstadoVerificacion.POR_CONFIRMAR, marca.getVerificacion());
        assertEquals("Entrada sin el código del proyecto y sin ubicación", marca.getMotivoVerificacion());

        StringBuilder antiguo = new StringBuilder();
        for (String linea : almacen.leer().orElseThrow().split("\n")) {
            String[] c = linea.split("\\|", -1);
            String recortada = linea.startsWith("O|") ? String.join("|", java.util.Arrays.copyOf(c, 6))
                    : linea.startsWith("A|") ? String.join("|", java.util.Arrays.copyOf(c, 7)) : linea;
            antiguo.append(recortada).append('\n');
        }
        almacen.escribir(antiguo.toString());
        Plataforma vieja = new RepositorioTexto(almacen, () -> null, fabrica).cargar();
        assertFalse(vieja.listarEmpresas().get(0).buscarObra("OB-01").controlaAsistencia());
        assertEquals(EstadoVerificacion.NO_APLICA, vieja.buscarUsuario("30000001").getAsistencias().get(0).getVerificacion());
    }
}
