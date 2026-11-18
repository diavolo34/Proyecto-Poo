package construtrack.modelo;

import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import construtrack.modelo.excepciones.PermisoDenegadoException;
import construtrack.modelo.novedades.EstadoNovedad;
import construtrack.modelo.novedades.Licencia;
import construtrack.modelo.novedades.Novedad;
import construtrack.util.Totp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public abstract class Usuario {
    private String cedula;
    private String nombre;
    private String claveResumen;
    private Horario horario;
    private Obra obra;
    private Cuenta cuenta;
    private final Historial historial = new Historial();
    private String secretoDosPasos;
    private final List<String> respaldosDosPasos = new ArrayList<>();
    private long ultimoPasoDosPasos = Long.MIN_VALUE;
    private Empresa empresa;
    private boolean activo = true;
    private LocalDate fechaEgreso;
    private String puesto;
    private String area;

    protected Usuario(String cedula, String nombre, Horario horario) {
        setCedula(cedula);
        setNombre(nombre);
        setHorario(horario);
    }

    protected Usuario(String cedula, String nombre, String clave, Horario horario) {
        this(cedula, nombre, horario);
        setClave(clave);
    }

    public static Usuario crear(String rol, String cedula, String nombre, String clave, Horario horario) {
        return switch (rolValido(rol)) {
            case Empleado.ROL -> new Empleado(cedula, nombre, clave, horario);
            case Supervisor.ROL -> new Supervisor(cedula, nombre, clave, horario);
            case Gerente.ROL -> new Gerente(cedula, nombre, clave, horario);
            default -> throw new DatoInvalidoException("Rol no válido: " + rol);
        };
    }

    public static Usuario restaurar(String rol, String cedula, String nombre, String claveResumen, Horario horario) {
        Usuario usuario = switch (rolValido(rol)) {
            case Empleado.ROL -> new Empleado(cedula, nombre, horario);
            case Supervisor.ROL -> new Supervisor(cedula, nombre, horario);
            case Gerente.ROL -> new Gerente(cedula, nombre, horario);
            default -> throw new DatoInvalidoException("Rol no válido: " + rol);
        };
        usuario.restaurarClaveResumen(claveResumen);
        return usuario;
    }

    public static final String ROL_ANTERIOR_GERENTE = "Jefe";

    private static String rolValido(String rol) {
        if (rol == null) {
            throw new DatoInvalidoException("Selecciona un rol.");
        }
        return ROL_ANTERIOR_GERENTE.equals(rol) ? Gerente.ROL : rol;
    }

    public abstract String getRol();

    public RegistroAsistencia marcarEntrada(LocalDateTime ahora) {
        verificarActivo();
        Licencia incapacidad = historial.incapacidadEl(ahora.toLocalDate());
        if (incapacidad != null) {
            throw new OperacionInvalidaException(nombre + " tiene un reposo médico aprobado del " + incapacidad.getDesde()
                    + " al " + incapacidad.getHasta() + ". Mientras dure la incapacidad no se marca asistencia.");
        }
        if (estaPresente()) {
            throw new OperacionInvalidaException(nombre + " ya registró su entrada y sigue en la obra.");
        }
        LocalTime hora = ahora.toLocalTime().withSecond(0).withNano(0);
        RegistroAsistencia registro = new RegistroAsistencia(ahora.toLocalDate(), hora, horario.esLlegadaTarde(hora));
        historial.agregarAsistencia(registro);
        return registro;
    }

    public RegistroAsistencia marcarEntrada(LocalDateTime ahora, DatosMarca marca) {
        List<String> motivos = new ArrayList<>(controlaAsistencia() ? obra.motivosDeDuda(marca) : List.of());
        if (exigeUbicacionAlMarcar() && !marca.tieneUbicacion() && !motivos.contains(SIN_UBICACION)) {
            motivos.add(SIN_UBICACION);
        }
        RegistroAsistencia registro = marcarEntrada(ahora);
        registro.registrarUbicacionEntrada(marca.latitud(), marca.longitud(), marca.precision());
        if (!motivos.isEmpty()) {
            registro.marcarPorConfirmar("Entrada " + String.join(" y ", motivos));
        } else if (controlaAsistencia()) {
            registro.marcarVerificada();
        }
        return registro;
    }

    public static final String SIN_UBICACION = "sin ubicación";

    protected boolean exigeUbicacionAlMarcar() {
        return true;
    }

    public RegistroAsistencia marcarSalida(LocalDateTime ahora, DatosMarca marca) {
        RegistroAsistencia abierta = historial.jornadaAbierta();
        if (abierta == null || !controlaAsistencia()) {
            RegistroAsistencia registro = marcarSalida(ahora);
            registro.registrarUbicacionSalida(marca.latitud(), marca.longitud(), marca.precision());
            return registro;
        }
        boolean olvidada = !abierta.getFecha().equals(ahora.toLocalDate());
        List<String> motivos = olvidada ? List.of() : obra.motivosDeDuda(marca);
        RegistroAsistencia registro = marcarSalida(ahora);
        registro.registrarUbicacionSalida(marca.latitud(), marca.longitud(), marca.precision());
        if (olvidada) {
            registro.marcarPorConfirmar("salida olvidada, se cerró con el horario");
        } else if (!motivos.isEmpty()) {
            registro.marcarPorConfirmar("Salida " + String.join(" y ", motivos));
        } else {
            registro.marcarVerificada();
        }
        return registro;
    }

    private boolean controlaAsistencia() {
        return obra != null && obra.controlaAsistencia();
    }

    public RegistroAsistencia revisarAsistencia(Usuario persona, LocalDate fecha, LocalTime entrada, EstadoVerificacion nuevo) {
        if (!puedeAprobarHorasDe(persona)) {
            throw new PermisoDenegadoException("No puedes confirmar la asistencia de " + persona.getNombre() + ".");
        }
        RegistroAsistencia jornada = persona.getHistorial().jornada(fecha, entrada);
        if (jornada == null) {
            throw new OperacionInvalidaException(persona.getNombre() + " no tiene esa jornada el " + fecha + ".");
        }
        jornada.revisarVerificacion(nuevo);
        return jornada;
    }

    public RegistroAsistencia marcarSalida(LocalDateTime ahora) {
        RegistroAsistencia abierta = historial.jornadaAbierta();
        if (abierta == null) {
            throw new OperacionInvalidaException(nombre + " no tiene una entrada abierta.");
        }
        LocalTime hora = ahora.toLocalTime().withSecond(0).withNano(0);
        LocalTime entrada = abierta.getHoraEntrada();
        LocalTime salida;
        if (!abierta.getFecha().equals(ahora.toLocalDate())) {
            salida = horario.getHoraSalida().isAfter(entrada) ? horario.getHoraSalida() : entrada;
        } else {
            salida = hora.isBefore(entrada) ? entrada : hora;
        }
        abierta.registrarSalida(salida);
        return abierta;
    }

    public boolean estaPresente() {
        return historial.jornadaAbierta() != null;
    }

    public RegistroAsistencia jornadaDeHoy(LocalDate hoy) {
        RegistroAsistencia abierta = historial.jornadaAbierta();
        return abierta != null ? abierta : historial.asistenciaDel(hoy);
    }

    public RegistroAsistencia getAsistenciaDel(LocalDate fecha) {
        return historial.asistenciaDel(fecha);
    }

    public boolean trabajaEl(LocalDate fecha) {
        return horario.incluyeDia(fecha.getDayOfWeek());
    }

    public void agregarAsistencia(RegistroAsistencia registro) {
        historial.agregarAsistencia(registro);
    }

    public List<RegistroAsistencia> getAsistencias() {
        return historial.getAsistencias();
    }

    public boolean puedeRegistrarNovedadPara(Usuario afectado) {
        return false;
    }

    public boolean puedeVerNovedadesDe(Usuario afectado) {
        return afectado == this;
    }

    public void registrarNovedad(Novedad novedad) {
        if (!novedad.getRegistradaPor().esDe(this) || !puedeRegistrarNovedadPara(novedad.getAfectado())) {
            throw new PermisoDenegadoException("Solo el supervisor registra permisos, reposos y reportes en obra de su personal.");
        }
        if (!novedad.getAfectado().estaActivo()) {
            throw new OperacionInvalidaException(novedad.getAfectado().getNombre() + " ya no está activo en la empresa.");
        }
        novedad.validarNueva();
        novedad.getAfectado().historial.agregarNovedad(novedad);
    }

    protected void revisarComo(RevisorNovedades revisor, Novedad novedad, EstadoNovedad nuevo, String nota,
            LocalDateTime ahora) {
        if (!revisor.puedeRevisar(novedad)) {
            throw new PermisoDenegadoException("No tienes autoridad para aprobar o rechazar el registro #" + novedad.getId() + ".");
        }
        novedad.revisar(nuevo, Firma.de(this), nota, ahora);
    }

    public boolean admiteDosPasos() {
        return false;
    }

    public boolean tieneDosPasos() {
        return secretoDosPasos != null;
    }

    public void activarDosPasos(String secreto, List<String> huellasRespaldo) {
        if (!admiteDosPasos()) {
            throw new PermisoDenegadoException("Solo los gerentes pueden activar la verificación en dos pasos.");
        }
        restaurarDosPasos(secreto, huellasRespaldo);
    }

    public void desactivarDosPasos() {
        secretoDosPasos = null;
        respaldosDosPasos.clear();
    }

    public void restaurarDosPasos(String secreto, List<String> huellasRespaldo) {
        this.secretoDosPasos = secreto;
        respaldosDosPasos.clear();
        respaldosDosPasos.addAll(huellasRespaldo);
    }

    public boolean verificarDosPasos(String codigo, Instant ahora) {
        if (secretoDosPasos == null || codigo == null) {
            return false;
        }
        long paso = Totp.verificar(secretoDosPasos, codigo, ahora, 1);
        if (paso != Totp.SIN_COINCIDENCIA) {
            if (paso <= ultimoPasoDosPasos) {
                return false;
            }
            ultimoPasoDosPasos = paso;
            return true;
        }
        String respaldo = codigo.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        return !respaldo.isEmpty() && respaldosDosPasos.remove(CifradoClave.huella(respaldo));
    }

    public String getSecretoDosPasos() {
        return secretoDosPasos;
    }

    public List<String> getRespaldosDosPasos() {
        return Collections.unmodifiableList(respaldosDosPasos);
    }

    public boolean puedeGestionarObras() {
        return false;
    }

    public boolean puedeAsignarObraA(Usuario otro) {
        return false;
    }

    public void asignarObra(Usuario persona, Obra obra) {
        if (!puedeAsignarObraA(persona)) {
            throw new PermisoDenegadoException("No puedes cambiar la obra de " + persona.getNombre() + ".");
        }
        persona.setObra(obra);
    }

    public boolean puedeAprobarHorasDe(Usuario otro) {
        return false;
    }

    public RegistroAsistencia revisarHorasExtra(Usuario empleado, LocalDate fecha, EstadoHorasExtra nuevo) {
        if (!puedeAprobarHorasDe(empleado)) {
            throw new PermisoDenegadoException("No puedes aprobar las horas extra de " + empleado.getNombre() + ".");
        }
        RegistroAsistencia jornada = empleado.getAsistenciaDel(fecha);
        if (jornada == null) {
            throw new OperacionInvalidaException(empleado.getNombre() + " no tiene jornada el " + fecha + ".");
        }
        jornada.revisarHorasExtra(nuevo, empleado.getHorario());
        return jornada;
    }

    public boolean estaActivo() {
        return activo;
    }

    public void verificarActivo() {
        if (!activo) {
            throw new OperacionInvalidaException(nombre + " ya no trabaja en la empresa (egreso el " + fechaEgreso + ").");
        }
    }

    void desactivar(LocalDate fecha) {
        if (estaPresente()) {
            marcarSalida(LocalDateTime.of(fecha, horario.getHoraSalida()));
        }
        activo = false;
        fechaEgreso = fecha;
        obra = null;
    }

    public void restaurarEgreso(LocalDate fecha) {
        activo = false;
        fechaEgreso = fecha;
    }

    public LocalDate getFechaEgreso() {
        return fechaEgreso;
    }

    public String getPuesto() {
        return puesto;
    }

    public String getArea() {
        return area;
    }

    public void setPuesto(String puesto) {
        this.puesto = puesto == null || puesto.isBlank() ? null : puesto.trim();
    }

    public void setArea(String area) {
        this.area = area == null || area.isBlank() ? null : area.trim();
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public boolean mismaEmpresaQue(Usuario otro) {
        return otro != null && empresa != null && otro.empresa == empresa;
    }

    public Historial getHistorial() {
        return historial;
    }

    public LocalDate getFechaIngreso() {
        return historial.getInicio();
    }

    public void setFechaIngreso(LocalDate fecha) {
        historial.setInicio(fecha);
    }

    void copiarEstadoDe(Usuario otro) {
        historial.copiarDe(otro.historial);
        obra = otro.obra;
        puesto = otro.puesto;
        area = otro.area;
        claveResumen = otro.claveResumen;
        if (otro.cuenta != null) {
            vincularCuenta(otro.cuenta);
        }
    }

    public void vincularCuenta(Cuenta cuenta) {
        if (!cuenta.getDocumento().equals(cedula)) {
            throw new DatoInvalidoException("El documento de la cuenta no coincide con la cédula de la ficha.");
        }
        this.cuenta = cuenta;
        this.claveResumen = cuenta.getClaveResumen();
        cuenta.vincular(this);
    }

    void desvincularCuenta() {
        if (cuenta != null) {
            cuenta.desvincular();
            cuenta = null;
        }
    }

    public Cuenta getCuenta() {
        return cuenta;
    }

    public boolean verificarClave(String clave) {
        return CifradoClave.verificar(claveResumen, cedula, clave);
    }

    public boolean tieneCifradoAntiguo() {
        return CifradoClave.esAntiguo(claveResumen);
    }

    public String getCedula() {
        return cedula;
    }

    private void setCedula(String cedula) {
        this.cedula = validarDocumento(cedula);
    }

    public static String validarDocumento(String documento) {
        String limpio = documento == null ? "" : documento.trim().toUpperCase();
        if (!limpio.matches("[0-9A-Z]{6,12}")) {
            throw new DatoInvalidoException("La cédula o DNI debe tener solo números, entre 6 y 10 dígitos.");
        }
        return limpio;
    }

    public static String validarCedulaNueva(String documento) {
        String limpio = documento == null ? "" : documento.trim();
        if (!limpio.matches("[0-9]{6,10}")) {
            throw new DatoInvalidoException("La cédula o DNI debe tener solo números, entre 6 y 10 dígitos (sin V, puntos ni guiones).");
        }
        return limpio;
    }

    public String getNombre() {
        return nombre;
    }

    public final void setNombre(String nombre) {
        this.nombre = validarNombre(nombre);
    }

    public static String validarNombre(String nombre) {
        String limpio = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        if (!limpio.matches("[\\p{L} .'-]{3,60}")) {
            throw new DatoInvalidoException("El nombre debe tener entre 3 y 60 letras.");
        }
        return limpio;
    }

    public final void setClave(String clave) {
        validarClave(clave);
        renovarCifrado(clave);
    }

    public final void renovarCifrado(String clave) {
        this.claveResumen = CifradoClave.resumir(clave);
        if (cuenta != null) {
            cuenta.sincronizarClave(claveResumen);
        }
    }

    public static void validarClave(String clave) {
        if (clave == null || !clave.matches("[^|\\s]{4,64}")) {
            throw new DatoInvalidoException("La clave debe tener entre 4 y 64 caracteres, sin espacios.");
        }
    }

    public static void validarClaveNueva(String clave) {
        if (clave == null || !clave.matches("[^|\\s]{8,64}") || !clave.matches(".*\\p{L}.*") || !clave.matches(".*[0-9].*")) {
            throw new DatoInvalidoException("La clave debe tener entre 8 y 64 caracteres, con al menos una letra y un número, "
                    + "sin espacios.");
        }
    }

    public String getClaveResumen() {
        return claveResumen;
    }

    public void restaurarClaveResumen(String resumen) {
        if (!CifradoClave.esValido(resumen)) {
            throw new DatoInvalidoException("Resumen de clave inválido.");
        }
        this.claveResumen = resumen;
    }

    public Horario getHorario() {
        return horario;
    }

    public final void setHorario(Horario horario) {
        if (horario == null) {
            throw new DatoInvalidoException("El horario es obligatorio.");
        }
        this.horario = horario;
    }

    public Obra getObra() {
        return obra;
    }

    public void setObra(Obra obra) {
        this.obra = obra;
    }

    @Override
    public String toString() {
        return nombre + " (" + getRol() + ")";
    }
}
