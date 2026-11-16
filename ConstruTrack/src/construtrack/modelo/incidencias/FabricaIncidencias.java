package construtrack.modelo.incidencias;

import construtrack.modelo.excepciones.DatoInvalidoException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class FabricaIncidencias {
    private final Map<String, Function<DatosIncidencia, Incidencia>> creadores = new LinkedHashMap<>();
    private final Map<String, String> nombres = new LinkedHashMap<>();

    public static FabricaIncidencias conTiposPorDefecto() {
        return new FabricaIncidencias()
                .registrar(ReposoMedico.CODIGO, "Reposo médico", ReposoMedico::new)
                .registrar(PermisoPersonal.CODIGO, "Permiso personal", PermisoPersonal::new)
                .registrar(Retardo.CODIGO, "Retardo", Retardo::new)
                .registrar(AccidenteLaboral.CODIGO, "Accidente laboral", AccidenteLaboral::new)
                .registrar(Ausencia.CODIGO, "Ausencia injustificada", Ausencia::new);
    }

    public FabricaIncidencias registrar(String codigo, String nombre, Function<DatosIncidencia, Incidencia> creador) {
        creadores.put(codigo, creador);
        nombres.put(codigo, nombre);
        return this;
    }

    public Incidencia crear(String codigo, DatosIncidencia datos) {
        Function<DatosIncidencia, Incidencia> creador = creadores.get(codigo);
        if (creador == null) {
            throw new DatoInvalidoException("Tipo de incidencia desconocido: " + codigo);
        }
        return creador.apply(datos);
    }

    public Map<String, String> tipos() {
        return Collections.unmodifiableMap(nombres);
    }
}
