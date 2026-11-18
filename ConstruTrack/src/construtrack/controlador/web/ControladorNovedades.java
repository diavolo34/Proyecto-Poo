package construtrack.controlador.web;

import construtrack.modelo.Obra;
import construtrack.modelo.Usuario;
import construtrack.modelo.novedades.Categoria;
import construtrack.modelo.novedades.EstadoNovedad;
import construtrack.modelo.novedades.Novedad;
import construtrack.servicio.ServicioNovedades;
import construtrack.util.Json;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ControladorNovedades implements ControladorWeb {
    private final ServicioNovedades novedades;

    public ControladorNovedades(ServicioNovedades novedades) {
        this.novedades = novedades;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador.get("/api/novedades/opciones", this::opciones)
                .get("/api/novedades", this::listar)
                .post("/api/novedades", this::registrar)
                .put("/api/novedades/{id}/revision", this::revisar);
    }

    private static Categoria categoria(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Categoria.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("La categoría debe ser LICENCIA o REPORTE_OBRA.");
        }
    }

    private Respuesta listar(Peticion p) {
        Usuario actual = p.usuario();
        List<Novedad> lista = "tablero".equals(p.consulta("vista", "")) ? novedades.listarTablero(actual)
                : novedades.listar(actual, categoria(p.consulta("categoria", "")));
        return Respuesta.json(Json.lista(lista, n -> JsonMapper.novedad(n, novedades.puedeRevisar(actual, n))));
    }

    private Respuesta opciones(Peticion p) {
        Usuario actual = p.usuario();
        Categoria categoria = categoria(p.consulta("categoria", "LICENCIA"));
        JsonObjeto tipos = new JsonObjeto();
        novedades.tipos(categoria == null ? Categoria.LICENCIA : categoria).forEach(tipos::texto);
        List<Usuario> personas = novedades.personasRegistrables(actual);
        String lista = Json.lista(personas, u -> new JsonObjeto()
                .texto("cedula", u.getCedula())
                .texto("nombre", u == actual ? u.getNombre() + " (yo)" : u.getNombre())
                .texto("puesto", u.getPuesto())
                .texto("obra", u.getObra() == null ? null : u.getObra().getNombre())
                .toString());
        String obras = actual.getEmpresa() == null ? "[]" : Json.lista(actual.getEmpresa().listarObras(), (Obra o) -> new JsonObjeto()
                .texto("codigo", o.getCodigo()).texto("nombre", o.getNombre()).texto("estado", o.getEstado().getNombre()).toString());
        return Respuesta.json(new JsonObjeto().crudo("tipos", tipos.toString()).crudo("personas", lista)
                .crudo("obras", obras).logico("puedeRegistrar", !personas.isEmpty()).toString());
    }

    private Respuesta registrar(Peticion p) {
        Usuario actual = p.usuario();
        Map<String, String> datos = p.cuerpo();
        Map<String, String> extras = new HashMap<>(datos);
        for (String clave : List.of("categoria", "codigoTipo", "cedula", "desde", "hasta", "descripcion")) {
            extras.remove(clave);
        }
        Categoria categoria = categoria(datos.get("categoria"));
        Novedad nueva = novedades.registrar(actual, categoria == null ? Categoria.LICENCIA : categoria, datos.get("codigoTipo"),
                datos.get("cedula"), fecha(datos.get("desde")), fecha(datos.get("hasta")), datos.get("descripcion"), extras);
        return Respuesta.creado(JsonMapper.novedad(nueva, false));
    }

    private Respuesta revisar(Peticion p) {
        Map<String, String> datos = p.cuerpo();
        EstadoNovedad estado;
        try {
            estado = EstadoNovedad.valueOf(datos.get("estado"));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Indica un estado válido: APROBADA o RECHAZADA.");
        }
        int id;
        try {
            id = Integer.parseInt(p.parametro("id"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El número de registro no es válido.");
        }
        Novedad revisada = novedades.revisar(p.usuario(), id, estado, datos.get("nota"));
        return Respuesta.json(JsonMapper.novedad(revisada, false));
    }

    private static LocalDate fecha(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("La fecha debe tener el formato AAAA-MM-DD.");
        }
    }
}
