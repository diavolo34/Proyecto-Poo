package construtrack.modelo.talento;

import construtrack.modelo.Historial;
import construtrack.modelo.Periodo;
import construtrack.modelo.RegistroAsistencia;
import construtrack.modelo.Usuario;
import construtrack.modelo.inventario.Asignacion;
import construtrack.modelo.novedades.Novedad;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public final class Expediente {
    private Expediente() {
    }

    public static String de(Usuario persona, Egreso egreso, List<Asignacion> entregas, LocalDate hoy) {
        StringBuilder t = new StringBuilder();
        t.append("EXPEDIENTE LABORAL · ").append(persona == null ? egreso.getNombre() : persona.getNombre()).append('\n');
        t.append("Empresa: ").append(persona == null || persona.getEmpresa() == null ? "—" : persona.getEmpresa().getNombre()).append("\n\n");
        t.append("DATOS\n");
        linea(t, "Cédula", egreso.getCedula());
        linea(t, "Rol", egreso.getRol());
        linea(t, "Puesto", egreso.getPuesto());
        linea(t, "Área", egreso.getArea());
        linea(t, "Ingreso", egreso.getFechaIngreso());
        if (persona != null && persona.getCuenta() != null) {
            linea(t, "Correo", persona.getCuenta().getCorreo());
            linea(t, "Teléfono", persona.getCuenta().getTelefono());
        }
        t.append("\nEGRESO\n");
        linea(t, "Tipo", egreso.getTipo().getNombre());
        linea(t, "Fecha", egreso.getFecha());
        linea(t, "Motivo", egreso.getMotivo());
        linea(t, "Registrado por", egreso.getRegistradoPor().nombre() + " el " + egreso.getFechaRegistro().toLocalDate());
        linea(t, "Devolvió equipos", egreso.devolvioEquipos() ? "Sí" : "No");
        if (!egreso.getNotaEquipos().isEmpty()) {
            linea(t, "Nota de equipos", egreso.getNotaEquipos());
        }
        if (persona != null) {
            Historial h = persona.getHistorial();
            LocalDate desde = egreso.getFechaIngreso() != null ? egreso.getFechaIngreso()
                    : h.getAsistencias().isEmpty() ? egreso.getFecha() : h.getAsistencias().get(0).getFecha();
            LocalDate hasta = egreso.getFecha().isBefore(desde) ? desde : egreso.getFecha();
            Periodo periodo = new Periodo(desde, hasta);
            t.append("\nASISTENCIA (").append(periodo).append(")\n");
            linea(t, "Jornadas", h.asistenciasEn(periodo).size());
            linea(t, "Horas trabajadas", String.format(Locale.ROOT, "%.1f", h.minutosTrabajados(periodo) / 60.0));
            linea(t, "Llegadas tarde", h.contarRetardos(periodo));
            LocalDate corte = hasta.plusDays(1).isAfter(hoy) ? hoy : hasta.plusDays(1);
            linea(t, "Faltas sin justificar", h.ausenciasSinJustificar(persona.getHorario(), periodo, corte).size());
            linea(t, "Días de incapacidad", h.diasDeIncapacidad(persona.getHorario(), periodo, corte).size());
            List<RegistroAsistencia> rechazadas = h.getAsistencias().stream().filter(r -> !r.cuenta()).toList();
            linea(t, "Marcas rechazadas", rechazadas.size());
            t.append("\nPERMISOS, REPOSOS Y REPORTES EN OBRA\n");
            if (h.getNovedades().isEmpty()) {
                t.append("  Ninguno\n");
            }
            for (Novedad n : h.getNovedades()) {
                t.append("  #").append(n.getId()).append(' ').append(n.getNombreTipo()).append(" · ").append(n.getDesde());
                if (!n.getHasta().equals(n.getDesde())) {
                    t.append(" al ").append(n.getHasta());
                }
                t.append(" · ").append(n.getEstado().getNombre());
                if (n.getRevisadaPor() != null) {
                    t.append(" por ").append(n.getRevisadaPor().nombre());
                }
                t.append(" · ").append(n.getDescripcion()).append('\n');
            }
        }
        t.append("\nHERRAMIENTAS Y EQUIPOS PENDIENTES\n");
        if (entregas.isEmpty()) {
            t.append("  Ninguno\n");
        }
        for (Asignacion a : entregas) {
            t.append("  ").append(a.getArticulo().getCodigo()).append(' ').append(a.getArticulo().getNombre()).append(" × ")
                    .append(a.getCantidad()).append(" (entregado el ").append(a.getFecha().toLocalDate()).append(")\n");
        }
        t.append("\nGenerado por ConstruTrack el ").append(hoy).append('\n');
        return t.toString();
    }

    private static void linea(StringBuilder t, String campo, Object valor) {
        t.append("  ").append(campo).append(": ").append(valor == null ? "—" : valor).append('\n');
    }
}
