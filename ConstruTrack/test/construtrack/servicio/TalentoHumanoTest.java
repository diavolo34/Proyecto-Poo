package construtrack.servicio;

import static construtrack.Datos.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import construtrack.CorreoFalso;
import construtrack.Datos;
import construtrack.modelo.ArchivoAdjunto;
import construtrack.modelo.Cuenta;
import construtrack.modelo.Plataforma;
import construtrack.modelo.Usuario;
import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.modelo.inventario.CategoriaArticulo;
import construtrack.modelo.novedades.FabricaNovedades;
import construtrack.modelo.talento.Egreso;
import construtrack.persistencia.AlmacenArchivosMemoria;
import construtrack.persistencia.AlmacenMemoria;
import construtrack.persistencia.RepositorioMemoria;
import construtrack.persistencia.RepositorioTexto;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TalentoHumanoTest {
    static final byte[] PDF = "%PDF-1.4\n%prueba\n".getBytes(StandardCharsets.ISO_8859_1);
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13};

    private final Datos d = new Datos();
    private final Servicios servicios = new Servicios(d.empresa.getPlataforma(), new RepositorioMemoria(d.empresa), Datos.RELOJ, 1,
            new CorreoFalso(), Runnable::run, new AlmacenArchivosMemoria());

    private String subir(byte[] datos, String nombre) {
        return servicios.archivos().subir(d.gerente, nombre, datos, "30000001", true).id();
    }

    private Map<String, String> renuncia() {
        Map<String, String> datos = new HashMap<>();
        datos.put("tipo", "RENUNCIA");
        datos.put("fecha", HOY.toString());
        datos.put("motivo", "Se muda a otra ciudad");
        datos.put("voucher", subir(PDF, "voucher.pdf"));
        datos.put("carta", subir(PNG, "carta.png"));
        datos.put("devolvioEquipos", "false");
        datos.put("notaEquipos", "No devolvió el taladro");
        return datos;
    }

    @Test
    @DisplayName("Solo se aceptan fotos o PDF de hasta 5 MB; los confidenciales solo los ve el gerente")
    void archivos() {
        assertThrows(DatoInvalidoException.class, () -> servicios.archivos().subir(d.gerente, "x.exe", "MZ\u0090".getBytes(), null, false));
        assertThrows(DatoInvalidoException.class, () -> servicios.archivos().subir(d.gerente, "grande.pdf", new byte[5 * 1024 * 1024 + 1],
                null, false));
        assertThrows(PermisoDenegadoException.class, () -> servicios.archivos().subir(d.jose, "a.pdf", PDF, null, false));
        ArchivoAdjunto reposo = servicios.archivos().subir(d.ana, "reposo jose", PDF, "30000001", false);
        assertEquals("reposo jose.pdf", reposo.nombre());
        assertEquals("application/pdf", reposo.tipo());
        assertNotNull(servicios.archivos().descargar(d.jose, reposo.id()), "La persona ve su propio reposo");
        assertThrows(PermisoDenegadoException.class, () -> servicios.archivos().descargar(d.luis, reposo.id()));
        String confidencial = subir(PDF, "voucher.pdf");
        assertThrows(PermisoDenegadoException.class, () -> servicios.archivos().descargar(d.jose, confidencial));
        assertEquals(PDF.length, servicios.archivos().descargar(d.gerente, confidencial).datos().length);
    }

    @Test
    @DisplayName("El egreso desactiva a la persona: no entra, no aparece en el personal y su historial se conserva")
    void egresoDesactiva() {
        Cuenta cuenta = new Cuenta("jose@correo.com", "jose1234", "José", "García", "30000001", "04141234567", Datos.AHORA);
        cuenta.marcarVerificada();
        d.empresa.agregarCuenta(cuenta);
        d.jose.vincularCuenta(cuenta);
        d.jose.marcarEntrada(Datos.AHORA.minusHours(2));
        String taladro = d.empresa.getInventario().agregar(CategoriaArticulo.HERRAMIENTA, "Taladro", "", "", 1, "").getCodigo();
        servicios.inventario().asignar(d.gerente, taladro, Map.of("cedula", "30000001"));

        Map<String, String> sinCarta = renuncia();
        sinCarta.remove("carta");
        assertThrows(DatoInvalidoException.class, () -> servicios.talento().registrarEgreso(d.gerente, "30000001", sinCarta));
        Map<String, String> sinNota = renuncia();
        sinNota.put("notaEquipos", "");
        assertThrows(DatoInvalidoException.class, () -> servicios.talento().registrarEgreso(d.gerente, "30000001", sinNota));
        assertThrows(PermisoDenegadoException.class, () -> servicios.talento().registrarEgreso(d.ana, "30000001", renuncia()));

        Egreso egreso = servicios.talento().registrarEgreso(d.gerente, "30000001", renuncia());
        assertFalse(d.jose.estaActivo());
        assertFalse(d.jose.estaPresente(), "La jornada abierta se cierra");
        assertTrue(d.ana.verEmpleadosACargo().isEmpty());
        assertFalse(servicios.personal().listarVisibles(d.gerente).contains(d.jose));
        assertEquals(1, servicios.talento().pendientes(d.gerente, "30000001").entregas().size(), "El taladro sigue pendiente");
        assertThrows(PermisoDenegadoException.class, () -> servicios.autenticacion().ingresar("jose@correo.com", "jose1234"));
        assertThrows(OperacionInvalidaException.class, () -> d.jose.marcarEntrada(Datos.AHORA));
        assertEquals(1, d.jose.getAsistencias().size(), "El historial se conserva");
        assertTrue(servicios.talento().expediente(d.gerente, egreso.getId()).contains("No devolvió el taladro"));
    }

    @Test
    @DisplayName("Si devolvió los equipos, sus entregas se cierran solas; eliminar la ficha conserva el expediente")
    void eliminarFicha() {
        String taladro = d.empresa.getInventario().agregar(CategoriaArticulo.HERRAMIENTA, "Taladro", "", "", 1, "").getCodigo();
        servicios.inventario().asignar(d.gerente, taladro, Map.of("cedula", "30000001"));
        Map<String, String> despido = renuncia();
        despido.put("tipo", "DESPIDO");
        despido.remove("carta");
        despido.put("devolvioEquipos", "true");
        Egreso egreso = servicios.talento().registrarEgreso(d.gerente, "30000001", despido);
        assertTrue(servicios.talento().pendientes(d.gerente, "30000001").entregas().isEmpty());
        assertEquals(1, d.empresa.getInventario().buscar(taladro).disponibles());

        servicios.talento().eliminarFicha(d.gerente, egreso.getId());
        assertNull(d.empresa.buscarUsuario("30000001"), "La ficha sale de la empresa y su cédula queda libre");
        Egreso archivado = servicios.talento().listar(d.gerente).get(0);
        assertTrue(archivado.fichaEliminada());
        assertTrue(servicios.talento().expediente(d.gerente, egreso.getId()).contains("EXPEDIENTE LABORAL · José García"));
        assertThrows(OperacionInvalidaException.class, () -> servicios.talento().eliminarFicha(d.gerente, egreso.getId()));
    }

    @Test
    @DisplayName("Egresos, archivos, puestos e inactivos sobreviven a guardar y cargar")
    void persistencia() {
        d.jose.setPuesto("Albañil");
        d.jose.setArea("Obra civil");
        servicios.talento().registrarEgreso(d.gerente, "30000004", renuncia());
        servicios.catalogo().agregar(d.gerente, ServicioCatalogo.Tipo.AREAS, "Seguridad industrial");
        servicios.catalogo().quitar(d.gerente, ServicioCatalogo.Tipo.PUESTOS, "Operador de maquinaria");
        AlmacenMemoria almacen = new AlmacenMemoria();
        FabricaNovedades fabrica = FabricaNovedades.conTiposPorDefecto();
        new RepositorioTexto(almacen, () -> null, fabrica).guardar(d.empresa.getPlataforma());
        Plataforma cargada = new RepositorioTexto(almacen, () -> null, fabrica).cargar();
        var empresa = cargada.listarEmpresas().get(0);
        Usuario laura = empresa.buscarUsuario("30000004");
        assertFalse(laura.estaActivo());
        assertEquals(HOY, laura.getFechaEgreso());
        assertEquals("Albañil", empresa.buscarUsuario("30000001").getPuesto());
        Egreso egreso = empresa.listarEgresos().get(0);
        assertEquals("No devolvió el taladro", egreso.getNotaEquipos());
        assertNotNull(empresa.buscarArchivo(egreso.getCarta()));
        assertTrue(empresa.getCatalogo().getAreas().contains("Seguridad industrial"));
        assertFalse(empresa.getCatalogo().getPuestos().contains("Operador de maquinaria"));
        assertNull(cargada.autenticar("30000004", "emp123"), "Inactiva no puede entrar");
    }

    @Test
    @DisplayName("El catálogo lo administra el gerente y no deja quitar un puesto en uso")
    void catalogo() {
        assertTrue(servicios.catalogo().ver(d.jose).getPuestos().containsAll(java.util.List.of("Electricista",
                "Supervisor del equipo de mantenimiento", "Gerente", "Jefe de obra")));
        assertThrows(PermisoDenegadoException.class, () -> servicios.catalogo().agregar(d.ana, ServicioCatalogo.Tipo.PUESTOS, "Soldador"));
        servicios.personal().actualizar(d.gerente, "30000001", Map.of("puesto", "electricista", "area", "Electricidad"));
        assertEquals("Electricista", d.jose.getPuesto(), "Se guarda con el nombre del catálogo");
        assertThrows(OperacionInvalidaException.class, () -> servicios.catalogo().quitar(d.gerente, ServicioCatalogo.Tipo.PUESTOS,
                "Electricista"));
        assertThrows(DatoInvalidoException.class, () -> servicios.personal().actualizar(d.gerente, "30000001",
                Map.of("puesto", "Astronauta")));
        assertThrows(OperacionInvalidaException.class, () -> servicios.catalogo().agregar(d.gerente, ServicioCatalogo.Tipo.AREAS,
                "electricidad"));
    }
}
