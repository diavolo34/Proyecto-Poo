package construtrack.controlador.web;

import construtrack.modelo.Empleado;
import construtrack.modelo.Horario;
import construtrack.modelo.RegistroAsistencia;
import construtrack.modelo.Supervisor;
import construtrack.modelo.Usuario;
import construtrack.modelo.incidencias.Incidencia;
import construtrack.util.Json;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class JsonMapper {
    private static final int MAX_HISTORIAL = 30;

    private JsonMapper() {
    }

    public static String usuario(Usuario u, boolean detalle) {
        RegistroAsistencia hoy = u.jornadaDeHoy(java.time.LocalDate.now());
        JsonObjeto json = new JsonObjeto()
                .texto("cedula", u.getCedula())
                .texto("nombre", u.getNombre())
                .texto("rol", u.getRol())
                .texto("empresa", u.getEmpresa() == null ? null : u.getEmpresa().getNombre())
                .logico("presente", u.estaPresente())
                .logico("trabajaHoy", u.trabajaEl(java.time.LocalDate.now()))
                .crudo("horario", horario(u.getHorario()))
                .crudo("asistenciaHoy", hoy == null ? "null" : registro(hoy, u.getHorario()));
        if (u instanceof Empleado e) {
            Supervisor s = e.getSupervisor();
            json.crudo("supervisor", s == null ? "null"
                    : new JsonObjeto().texto("cedula", s.getCedula()).texto("nombre", s.getNombre()).toString());
        }
        if (u instanceof Supervisor s) {
            json.numero("empleadosACargo", s.verEmpleadosACargo().size());
        }
        if (u.getCuenta() != null) {
            json.texto("correo", u.getCuenta().getCorreo()).texto("telefono", u.getCuenta().getTelefono());
        }
        if (detalle) {
            json.logico("admiteDosPasos", u.admiteDosPasos()).logico("dosPasos", u.tieneDosPasos());
        }
        if (u.getObra() != null) {
            json.texto("obra", u.getObra().getNombre());
            json.crudo("controlObra", new JsonObjeto().texto("codigo", u.getObra().getCodigo())
                    .logico("exigeCodigo", u.getObra().exigeCodigo()).logico("geocerca", u.getObra().tieneGeocerca()).toString());
        }
        if (detalle) {
            List<RegistroAsistencia> recientes = new ArrayList<>(u.getAsistencias());
            Collections.reverse(recientes);
            json.crudo("asistencias", Json.lista(recientes.subList(0, Math.min(MAX_HISTORIAL, recientes.size())),
                    r -> registro(r, u.getHorario())));

            List<Incidencia> incidencias = new ArrayList<>(u.getHistorial().getIncidencias());
            incidencias.sort(java.util.Comparator.comparing(Incidencia::getDesde).reversed());
            json.crudo("incidencias", Json.lista(incidencias, i -> incidencia(i, false)));
            json.texto("fechaIngreso", u.getFechaIngreso());
        }
        return json.toString();
    }

    public static String horario(Horario h) {
        return new JsonObjeto()
                .texto("dias", h.getDias())
                .texto("horaEntrada", h.getHoraEntrada())
                .texto("horaSalida", h.getHoraSalida())
                .toString();
    }

    public static String registro(RegistroAsistencia r, Horario horario) {
        long extra = r.getEstadoExtra() == construtrack.modelo.EstadoHorasExtra.NO_APLICA ? 0 : r.minutosExtra(horario);
        return new JsonObjeto()
                .texto("fecha", r.getFecha())
                .texto("horaEntrada", r.getHoraEntrada())
                .texto("horaSalida", r.getHoraSalida())
                .logico("tarde", r.isTarde())
                .numero("minutos", r.minutosTrabajados())
                .numero("minutosExtra", extra)
                .texto("tipoExtra", extra == 0 ? null : r.tipoExtra(horario).getNombre())
                .texto("estadoExtra", extra == 0 ? null : r.getEstadoExtra().getNombre())
                .texto("verificacion", r.getVerificacion().name())
                .texto("verificacionTexto", r.getVerificacion().getNombre())
                .texto("motivo", r.getMotivoVerificacion())
                .toString();
    }

    public static String marcaPorConfirmar(Usuario u, RegistroAsistencia r) {
        return new JsonObjeto()
                .texto("cedula", u.getCedula())
                .texto("nombre", u.getNombre())
                .texto("obra", u.getObra() == null ? null : u.getObra().getNombre())
                .texto("fecha", r.getFecha())
                .texto("horaEntrada", r.getHoraEntrada())
                .texto("horaSalida", r.getHoraSalida())
                .texto("motivo", r.getMotivoVerificacion())
                .toString();
    }

    public static String horasExtra(Usuario u, RegistroAsistencia r) {
        return new JsonObjeto()
                .texto("cedula", u.getCedula())
                .texto("nombre", u.getNombre())
                .crudo("jornada", registro(r, u.getHorario()))
                .toString();
    }

    public static String sesion(String token, construtrack.servicio.Ingreso ingreso) {
        return new JsonObjeto()
                .texto("token", token)
                .logico("pendiente", ingreso.pendienteDeUnirse())
                .crudo("cuenta", ingreso.cuenta() == null ? "null" : cuenta(ingreso.cuenta()))
                .crudo("usuario", ingreso.usuario() == null ? "null" : usuario(ingreso.usuario(), true))
                .toString();
    }

    public static String cuenta(construtrack.modelo.Cuenta c) {
        return new JsonObjeto()
                .texto("correo", c.getCorreo())
                .texto("nombre", c.getNombre())
                .texto("apellido", c.getApellido())
                .texto("documento", c.getDocumento())
                .texto("telefono", c.getTelefono())
                .logico("perteneceAEmpresa", c.perteneceAEmpresa())
                .logico("verificada", c.estaVerificada())
                .toString();
    }

    public static String invitacion(construtrack.modelo.Invitacion v, java.time.LocalDateTime ahora) {
        return new JsonObjeto()
                .texto("codigo", v.getCodigo())
                .texto("rol", v.getRol())
                .texto("supervisor", v.getSupervisor() == null ? null : v.getSupervisor().getNombre())
                .texto("obra", v.getObra() == null ? null : v.getObra().getNombre())
                .texto("estado", v.estadoEn(ahora).getNombre())
                .texto("creada", v.getCreada().toLocalDate())
                .texto("vence", v.getVence().toLocalDate())
                .texto("usadaPor", v.getUsadaPor() == null ? null : v.getUsadaPor().getNombreCompleto())
                .toString();
    }

    public static String usuarios(List<? extends Usuario> usuarios) {
        return Json.lista(usuarios, u -> usuario(u, false));
    }

    public static String mensaje(String mensaje, String datosJson) {
        return new JsonObjeto().texto("mensaje", mensaje).crudo("usuario", datosJson).toString();
    }

    public static String incidencia(Incidencia i, boolean puedoRevisar) {
        Usuario revisor = i.getRevisor();
        return new JsonObjeto()
                .logico("puedoRevisar", puedoRevisar)
                .numero("id", i.getId())
                .texto("tipo", i.getCodigoTipo())
                .texto("nombreTipo", i.getNombreTipo())
                .texto("gravedad", i.getGravedad().getNombre())
                .texto("cedulaAfectado", i.getAfectado().getCedula())
                .texto("nombreAfectado", i.getAfectado().getNombre())
                .texto("reportadaPor", i.getReportadoPor().getNombre())
                .texto("desde", i.getDesde())
                .texto("hasta", i.getHasta())
                .numero("dias", i.getDias())
                .texto("descripcion", i.getDescripcion())
                .texto("detalle", i.getDetalle())
                .texto("estado", i.getEstado().name())
                .texto("estadoNombre", i.getEstado().getNombre())
                .texto("revisor", revisor == null ? null : revisor.getNombre())
                .texto("comentarioRevision", i.getComentarioRevision())
                .texto("fechaReporte", i.getFechaReporte())
                .toString();
    }

    public static String alerta(construtrack.modelo.alertas.Alerta a) {
        return new JsonObjeto()
                .texto("nivel", a.getNivel().getNombre())
                .texto("cedula", a.getCedula())
                .texto("nombre", a.getNombre())
                .texto("mensaje", a.getMensaje())
                .texto("desde", a.getDesde())
                .texto("generada", a.getGenerada())
                .toString();
    }

    public static String estadisticas(construtrack.modelo.Estadisticas e) {
        JsonObjeto incidencias = new JsonObjeto();
        e.incidenciasPorTipo().forEach(incidencias::numero);
        return new JsonObjeto()
                .texto("periodo", e.getPeriodo())
                .numero("tasaAsistencia", redondear(e.tasaAsistencia()))
                .numero("porcentajePuntualidad", redondear(e.porcentajePuntualidad()))
                .numero("minutosTrabajados", e.minutosTrabajados())
                .crudo("incidenciasPorTipo", incidencias.toString())
                .crudo("personas", Json.lista(e.ranking(), JsonMapper::resumen))
                .toString();
    }

    private static String resumen(construtrack.modelo.Estadisticas.Resumen r) {
        return new JsonObjeto()
                .texto("cedula", r.cedula())
                .texto("nombre", r.nombre())
                .texto("rol", r.rol())
                .numero("laborables", r.laborables())
                .numero("asistencias", r.asistencias())
                .numero("tasaAsistencia", redondear(r.tasaAsistencia()))
                .numero("retardos", r.retardos())
                .numero("ausencias", r.ausencias())
                .numero("justificadas", r.justificadas())
                .numero("puntualidad", redondear(r.puntualidad()))
                .numero("minutos", r.minutos())
                .numero("minutosEsperados", r.minutosEsperados())
                .numero("extraDiaria", r.extraDiaria())
                .numero("extraDiaLibre", r.extraDiaLibre())
                .numero("extraPendiente", r.extraPendiente())
                .toString();
    }

    private static double redondear(double valor) {
        return Math.round(valor * 10) / 10.0;
    }

    public static String ok() {
        return "{\"status\":\"ok\"}";
    }

    public static String error(String mensaje) {
        return new JsonObjeto().texto("error", mensaje).toString();
    }
}
