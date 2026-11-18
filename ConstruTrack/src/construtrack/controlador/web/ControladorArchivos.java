package construtrack.controlador.web;

import construtrack.servicio.ServicioArchivos;
import java.util.Base64;
import java.util.Map;

public class ControladorArchivos implements ControladorWeb {
    private final ServicioArchivos archivos;

    public ControladorArchivos(ServicioArchivos archivos) {
        this.archivos = archivos;
    }

    @Override
    public void registrarRutas(Enrutador enrutador) {
        enrutador.post("/api/archivos", p -> {
                    Map<String, String> datos = p.cuerpo(Peticion.TAMANO_MAXIMO_ARCHIVOS);
                    byte[] bytes;
                    try {
                        String base64 = datos.getOrDefault("datos", "");
                        bytes = Base64.getDecoder().decode(base64.substring(base64.indexOf(',') + 1));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("El archivo llegó dañado. Intenta de nuevo.");
                    }
                    return Respuesta.creado(JsonMapper.archivo(archivos.subir(p.usuario(), datos.get("nombre"), bytes,
                            datos.get("persona"), "true".equals(datos.get("confidencial")))));
                })
                .get("/api/archivos/{id}", p -> {
                    ServicioArchivos.Descarga d = archivos.descargar(p.usuario(), p.parametro("id"));
                    return Respuesta.archivo(d.datos(), d.tipo(), d.nombre());
                });
    }
}
