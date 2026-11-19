package construtrack.controlador.web;

import construtrack.modelo.Catalogo;
import construtrack.servicio.ServicioCatalogo;
import construtrack.util.Json;

public class ControladorCatalogo implements ControladorWeb {
    private final ServicioCatalogo catalogo;

    public ControladorCatalogo(ServicioCatalogo catalogo) {
        this.catalogo = catalogo;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador.get("/api/catalogo", p -> Respuesta.json(json(catalogo.ver(p.usuario()))))
                .post("/api/catalogo/{tipo}", p -> {
                    catalogo.agregar(p.usuario(), ServicioCatalogo.Tipo.desdeTexto(p.parametro("tipo")), p.cuerpo().get("nombre"));
                    return Respuesta.creado(json(catalogo.ver(p.usuario())));
                })
                .post("/api/catalogo/{tipo}/quitar", p -> {
                    catalogo.quitar(p.usuario(), ServicioCatalogo.Tipo.desdeTexto(p.parametro("tipo")), p.cuerpo().get("nombre"));
                    return Respuesta.json(json(catalogo.ver(p.usuario())));
                });
    }

    static String json(Catalogo c) {
        return new JsonObjeto().crudo("puestos", Json.lista(c.getPuestos(), Json::texto)).toString();
    }
}
