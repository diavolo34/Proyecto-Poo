package construtrack.modelo.novedades;

import construtrack.modelo.excepciones.DatoInvalidoException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class FabricaNovedades {
    private record Tipo(String nombre, Categoria categoria, Function<DatosNovedad, Novedad> creador) {
    }

    private final Map<String, Tipo> tipos = new LinkedHashMap<>();
    private final Map<String, String> alias = new HashMap<>();

    public static FabricaNovedades conTiposPorDefecto() {
        return new FabricaNovedades()
                .registrar(PermisoLaboral.CODIGO, "Permiso laboral", Categoria.LICENCIA, PermisoLaboral::new)
                .registrar(ReposoMedico.CODIGO, "Reposo médico", Categoria.LICENCIA, ReposoMedico::new)
                .registrar(Retardo.CODIGO, "Retardo", Categoria.REPORTE_OBRA, Retardo::new)
                .registrar(Inasistencia.CODIGO, "Inasistencia injustificada", Categoria.REPORTE_OBRA, Inasistencia::new)
                .registrar(AccidenteLaboral.CODIGO, "Accidente laboral", Categoria.REPORTE_OBRA, AccidenteLaboral::new)
                .alias(PermisoLaboral.CODIGO_ANTERIOR, PermisoLaboral.CODIGO)
                .alias(Inasistencia.CODIGO_ANTERIOR, Inasistencia.CODIGO);
    }

    public FabricaNovedades registrar(String codigo, String nombre, Categoria categoria,
            Function<DatosNovedad, Novedad> creador) {
        tipos.put(codigo, new Tipo(nombre, categoria, creador));
        return this;
    }

    public FabricaNovedades alias(String anterior, String actual) {
        alias.put(anterior, actual);
        return this;
    }

    public Novedad crear(String codigo, DatosNovedad datos) {
        Tipo tipo = codigo == null ? null : tipos.get(alias.getOrDefault(codigo, codigo));
        if (tipo == null) {
            throw new DatoInvalidoException("Tipo desconocido: " + codigo);
        }
        return tipo.creador().apply(datos);
    }

    public Novedad crear(String codigo, Categoria categoria, DatosNovedad datos) {
        if (categoria(codigo) != categoria) {
            throw new DatoInvalidoException("Elige un tipo de " + categoria.getSingular() + ".");
        }
        return crear(codigo, datos);
    }

    public Categoria categoria(String codigo) {
        Tipo tipo = codigo == null ? null : tipos.get(alias.getOrDefault(codigo, codigo));
        return tipo == null ? null : tipo.categoria();
    }

    public Map<String, String> tipos(Categoria categoria) {
        Map<String, String> resultado = new LinkedHashMap<>();
        tipos.forEach((codigo, tipo) -> {
            if (tipo.categoria() == categoria) {
                resultado.put(codigo, tipo.nombre());
            }
        });
        return Collections.unmodifiableMap(resultado);
    }
}
