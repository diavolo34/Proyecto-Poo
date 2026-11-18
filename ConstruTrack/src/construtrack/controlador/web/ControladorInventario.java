package construtrack.controlador.web;

import construtrack.servicio.ServicioInventario;
import construtrack.util.Json;

public class ControladorInventario implements ControladorWeb {
    private final ServicioInventario inventario;

    public ControladorInventario(ServicioInventario inventario) {
        this.inventario = inventario;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador.get("/api/inventario", p -> Respuesta.json(Json.lista(inventario.listar(p.usuario()),
                        a -> JsonMapper.articulo(a, inventario.hoy()))))
                .post("/api/inventario", p -> Respuesta.creado(JsonMapper.articulo(inventario.crear(p.usuario(), p.cuerpo()),
                        inventario.hoy())))
                .put("/api/inventario/{codigo}", p -> Respuesta.json(JsonMapper.articulo(
                        inventario.editar(p.usuario(), p.parametro("codigo"), p.cuerpo()), inventario.hoy())))
                .delete("/api/inventario/{codigo}", p -> {
                    inventario.eliminar(p.usuario(), p.parametro("codigo"));
                    return Respuesta.json(JsonMapper.ok());
                })
                .post("/api/inventario/{codigo}/entregas", p -> Respuesta.creado(JsonMapper.asignacion(
                        inventario.asignar(p.usuario(), p.parametro("codigo"), p.cuerpo()), inventario.hoy())))
                .put("/api/inventario/entregas/{id}/devolucion", p -> {
                    int id;
                    try {
                        id = Integer.parseInt(p.parametro("id"));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("El número de entrega no es válido.");
                    }
                    return Respuesta.json(JsonMapper.asignacion(inventario.devolver(p.usuario(), id,
                            p.cuerpo().get("condicion"), p.cuerpo().get("nota")), inventario.hoy()));
                })
                .get("/api/inventario/a-cargo/{cedula}", p -> Respuesta.json(Json.lista(
                        inventario.aCargoDe(p.usuario(), p.parametro("cedula")), a -> JsonMapper.asignacion(a, inventario.hoy()))));
    }
}
