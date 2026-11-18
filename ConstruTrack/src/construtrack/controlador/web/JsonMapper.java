package construtrack.controlador.web;

import construtrack.modelo.ArchivoAdjunto;
import construtrack.modelo.Empleado;
import construtrack.modelo.Horario;
import construtrack.modelo.RegistroAsistencia;
import construtrack.modelo.RetrasoObra;
import construtrack.modelo.Supervisor;
import construtrack.modelo.Usuario;
import construtrack.modelo.inventario.Articulo;
import construtrack.modelo.inventario.Asignacion;
import construtrack.modelo.novedades.Categoria;
import construtrack.modelo.novedades.Licencia;
import construtrack.modelo.novedades.Novedad;
import construtrack.modelo.novedades.ReporteObra;
import construtrack.modelo.talento.Egreso;
import construtrack.util.Json;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class JsonMapper {
    private static final int MAX_HISTORIAL = 30;

    private JsonMapper() {
    }

    public static String usuario(Usuario u, boolean detalle) {
        return usuario(u, detalle, !(u instanceof Empleado));
    }

    public static String usuario(Usuario u, boolean detalle, boolean conReportesObra) {
        java.time.LocalDate fechaHoy = java.time.LocalDate.now();
        RegistroAsistencia hoy = u.jornadaDeHoy(fechaHoy);
        JsonObjeto json = new JsonObjeto()
                .texto("cedula", u.getCedula())
                .texto("nombre", u.getNombre())
                .texto("rol", u.getRol())
                .texto("puesto", u.getPuesto())
                .texto("area", u.getArea())
                .logico("activo", u.estaActivo())
                .texto("fechaEgreso", u.getFechaEgreso())
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

            List<Novedad> novedades = new ArrayList<>(u.getHistorial().getNovedades());
            novedades.removeIf(n -> !conReportesObra && n.getCategoria() != Categoria.LICENCIA);
            novedades.sort(java.util.Comparator.comparing(Novedad::getDesde).reversed());
            json.crudo("novedades", Json.lista(novedades, n -> novedad(n, false)));
            json.texto("fechaIngreso", u.getFechaIngreso());
            Licencia incapacidad = u.getHistorial().incapacidadEl(fechaHoy);
            json.crudo("incapacidadHoy", incapacidad == null ? "null" : new JsonObjeto().numero("id", incapacidad.getId())
                    .texto("desde", incapacidad.getDesde()).texto("hasta", incapacidad.getHasta()).toString());
            json.crudo("ausencias", ausencias(u, fechaHoy));
            json.crudo("herramientas", u.getEmpresa() == null ? "[]"
                    : Json.lista(u.getEmpresa().getInventario().abiertasDe(u.getCedula()), a -> asignacion(a, fechaHoy)));
        }
        return json.toString();
    }

    private static String ausencias(Usuario u, java.time.LocalDate hoy) {
        construtrack.modelo.Historial h = u.getHistorial();
        construtrack.modelo.Periodo periodo = construtrack.modelo.Periodo.ultimosDias(hoy, 30);
        List<String> dias = new ArrayList<>();
        List<java.time.LocalDate> laborables = new ArrayList<>(h.diasLaborables(u.getHorario(), periodo, hoy));
        java.util.Collections.reverse(laborables);
        for (java.time.LocalDate fecha : laborables) {
            if (h.asistenciaValidaDel(fecha) == null) {
                construtrack.modelo.novedades.Justificable j = h.justificacionDel(fecha);
                dias.add(new JsonObjeto().texto("fecha", fecha).texto("tipo", j == null ? "Sin justificar" : j.etiquetaJustificacion())
                        .logico("justificada", j != null).toString());
            }
        }
        return "[" + String.join(",", dias) + "]";
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
                .crudo("ubicacionEntrada", ubicacion(r.getUbicacionEntrada()))
                .crudo("ubicacionSalida", ubicacion(r.getUbicacionSalida()))
                .toString();
    }

    public static String ubicacion(RegistroAsistencia.Ubicacion u) {
        return u == null ? "null" : new JsonObjeto().numero("latitud", u.latitud()).numero("longitud", u.longitud())
                .numero("precision", u.precision()).toString();
    }

    private static String obraMapa(construtrack.modelo.Obra o) {
        return o == null ? "null" : new JsonObjeto().texto("codigo", o.getCodigo()).texto("nombre", o.getNombre())
                .numero("latitud", o.getLatitud()).numero("longitud", o.getLongitud()).numero("radio", o.getRadioMetros()).toString();
    }

    public static String marcaConUbicacion(Usuario u, RegistroAsistencia r) {
        return new JsonObjeto()
                .texto("cedula", u.getCedula())
                .texto("nombre", u.getNombre())
                .texto("rol", u.getRol())
                .crudo("obra", obraMapa(u.getObra()))
                .crudo("jornada", registro(r, u.getHorario()))
                .toString();
    }

    public static String marcaPorConfirmar(Usuario u, RegistroAsistencia r) {
        return new JsonObjeto()
                .texto("cedula", u.getCedula())
                .texto("nombre", u.getNombre())
                .crudo("obraMapa", obraMapa(u.getObra()))
                .crudo("ubicacionEntrada", ubicacion(r.getUbicacionEntrada()))
                .crudo("ubicacionSalida", ubicacion(r.getUbicacionSalida()))
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
                .texto("usada", v.getUsada() == null ? null : v.getUsada().toLocalDate())
                .texto("puesto", v.getPuesto())
                .texto("area", v.getArea())
                .toString();
    }

    public static String usuarios(List<? extends Usuario> usuarios) {
        return Json.lista(usuarios, u -> usuario(u, false));
    }

    public static String mensaje(String mensaje, String datosJson) {
        return new JsonObjeto().texto("mensaje", mensaje).crudo("usuario", datosJson).toString();
    }

    public static String novedad(Novedad n, boolean puedoRevisar) {
        JsonObjeto extras = new JsonObjeto();
        n.getExtras().forEach((clave, valor) -> {
            if (!"archivo".equals(clave)) {
                extras.texto(clave, valor);
            }
        });
        return new JsonObjeto()
                .logico("puedoRevisar", puedoRevisar)
                .numero("id", n.getId())
                .texto("categoria", n.getCategoria().name())
                .texto("categoriaNombre", n.getCategoria().getNombre())
                .texto("tipo", n.getCodigoTipo())
                .texto("nombreTipo", n.getNombreTipo())
                .texto("cedulaAfectado", n.getAfectado().getCedula())
                .texto("nombreAfectado", n.getAfectado().getNombre())
                .texto("registradaPor", n.getRegistradaPor().nombre())
                .texto("obra", n instanceof ReporteObra r ? r.getObra() : null)
                .texto("desde", n.getDesde())
                .texto("hasta", n.getHasta())
                .numero("dias", n.getDias())
                .texto("descripcion", n.getDescripcion())
                .texto("detalle", n.getDetalle())
                .crudo("datos", extras.toString())
                .texto("archivo", n.getArchivo())
                .texto("estado", n.getEstado().name())
                .texto("estadoNombre", n.getEstado().getNombre())
                .texto("revisadaPor", n.getRevisadaPor() == null ? null : n.getRevisadaPor().nombre())
                .texto("nota", n.getNota())
                .texto("fechaRegistro", n.getFechaRegistro())
                .texto("fechaRevision", n.getFechaRevision())
                .logico("incapacidad", n instanceof Licencia l && l.esIncapacidad())
                .toString();
    }

    public static String articulo(Articulo a, java.time.LocalDate hoy) {
        return new JsonObjeto()
                .texto("codigo", a.getCodigo())
                .texto("categoria", a.getCategoria().name())
                .texto("categoriaNombre", a.getCategoria().getNombre())
                .texto("nombre", a.getNombre())
                .texto("marca", a.getMarca())
                .texto("serie", a.getSerie())
                .numero("cantidad", a.getCantidad())
                .numero("enUso", a.enUso())
                .numero("disponibles", a.disponibles())
                .texto("estado", a.getEstado().name())
                .texto("estadoNombre", a.getEstado().getNombre())
                .texto("nota", a.getNota())
                .crudo("entregas", Json.lista(a.getAsignaciones(), x -> asignacion(x, hoy)))
                .toString();
    }

    public static String asignacion(Asignacion a, java.time.LocalDate hoy) {
        return new JsonObjeto()
                .numero("id", a.getId())
                .texto("articulo", a.getArticulo().getCodigo())
                .texto("nombreArticulo", a.getArticulo().getNombre())
                .texto("categoria", a.getArticulo().getCategoria().getNombre())
                .texto("cedula", a.getPersona().cedula())
                .texto("persona", a.getPersona().nombre())
                .numero("cantidad", a.getCantidad())
                .texto("fecha", a.getFecha())
                .texto("entregadoPor", a.getEntregadoPor().nombre())
                .texto("obra", a.getObra())
                .texto("devolucionPrevista", a.getDevolucionPrevista())
                .logico("vencida", a.vencidaEn(hoy))
                .texto("nota", a.getNota())
                .logico("abierta", a.estaAbierta())
                .texto("fechaDevolucion", a.getFechaDevolucion())
                .texto("recibidoPor", a.getRecibidoPor() == null ? null : a.getRecibidoPor().nombre())
                .texto("condicion", a.getCondicion() == null ? null : a.getCondicion().getNombre())
                .texto("notaDevolucion", a.getNotaDevolucion())
                .toString();
    }

    public static String egreso(Egreso e) {
        return new JsonObjeto()
                .numero("id", e.getId())
                .texto("cedula", e.getCedula())
                .texto("nombre", e.getNombre())
                .texto("rol", e.getRol())
                .texto("puesto", e.getPuesto())
                .texto("area", e.getArea())
                .texto("fechaIngreso", e.getFechaIngreso())
                .texto("tipo", e.getTipo().name())
                .texto("tipoNombre", e.getTipo().getNombre())
                .texto("fecha", e.getFecha())
                .texto("motivo", e.getMotivo())
                .texto("registradoPor", e.getRegistradoPor().nombre())
                .texto("fechaRegistro", e.getFechaRegistro())
                .texto("voucher", e.getVoucher())
                .texto("carta", e.getCarta())
                .logico("devolvioEquipos", e.devolvioEquipos())
                .texto("notaEquipos", e.getNotaEquipos())
                .logico("fichaEliminada", e.fichaEliminada())
                .texto("fechaEliminacion", e.getFichaEliminada())
                .toString();
    }

    public static String archivo(ArchivoAdjunto a) {
        return new JsonObjeto().texto("id", a.id()).texto("nombre", a.nombre()).texto("tipo", a.tipo())
                .numero("tamano", a.tamano()).toString();
    }

    public static String retraso(RetrasoObra r) {
        return new JsonObjeto()
                .numero("id", r.getId())
                .texto("fecha", r.getFecha())
                .texto("reportadoPor", r.getReportadoPor().nombre())
                .texto("motivo", r.getMotivo())
                .texto("finAnterior", r.getFinAnterior())
                .texto("finNuevo", r.getFinNuevo())
                .numero("dias", r.diasDeRetraso())
                .logico("revisado", r.estaRevisado())
                .texto("revisadoPor", r.getRevisadoPor() == null ? null : r.getRevisadoPor().nombre())
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
        JsonObjeto novedades = new JsonObjeto();
        e.novedadesPorTipo().forEach(novedades::numero);
        return new JsonObjeto()
                .texto("periodo", e.getPeriodo())
                .numero("tasaAsistencia", redondear(e.tasaAsistencia()))
                .numero("porcentajePuntualidad", redondear(e.porcentajePuntualidad()))
                .numero("minutosTrabajados", e.minutosTrabajados())
                .crudo("novedadesPorTipo", novedades.toString())
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
                .numero("incapacidad", r.incapacidad())
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
