package construtrack.modelo;

import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import java.time.LocalDateTime;

/**
 * Invitación personal de un solo uso que el Jefe genera para que alguien con cuenta se una a la empresa.
 * Define de antemano el rol, el supervisor y la obra; vence en unos días y se puede cancelar.
 */
public class Invitacion {

    /** Horario inicial de quien se une; el Jefe lo puede ajustar después. */
    private static final String DIAS_POR_DEFECTO = "LU,MA,MI,JU,VI";
    private static final String CLAVE_TEMPORAL = "temporal";

    private final CodigoInvitacion codigo;
    private final String rol;
    private final Supervisor supervisor;
    private final Obra obra;
    private final Jefe creadaPor;
    private final LocalDateTime creada;
    private final LocalDateTime vence;
    private EstadoInvitacion estado = EstadoInvitacion.VIGENTE;
    private Cuenta usadaPor;
    private LocalDateTime usada;

    public Invitacion(CodigoInvitacion codigo, String rol, Supervisor supervisor, Obra obra, Jefe creadaPor,
            LocalDateTime creada, LocalDateTime vence) {
        if (!Empleado.ROL.equals(rol) && !Supervisor.ROL.equals(rol)) {
            throw new DatoInvalidoException("La invitación debe ser para un Empleado o un Supervisor.");
        }
        if (supervisor != null && !Empleado.ROL.equals(rol)) {
            throw new DatoInvalidoException("Solo los empleados tienen supervisor.");
        }
        if (!vence.isAfter(creada)) {
            throw new DatoInvalidoException("La invitación debe vencer después de crearse.");
        }
        this.codigo = codigo;
        this.rol = rol;
        this.supervisor = supervisor;
        this.obra = obra;
        this.creadaPor = creadaPor;
        this.creada = creada;
        this.vence = vence;
    }

    /** Estado real en un momento dado: una invitación vigente pasada su fecha se considera vencida. */
    public EstadoInvitacion estadoEn(LocalDateTime ahora) {
        return estado == EstadoInvitacion.VIGENTE && ahora.isAfter(vence) ? EstadoInvitacion.VENCIDA : estado;
    }

    /**
     * Canjea la invitación: crea la ficha de la persona en la empresa con el rol, supervisor y obra definidos.
     * Solo funciona una vez y antes de vencer.
     */
    public Usuario canjear(Cuenta cuenta, Empresa empresa, LocalDateTime ahora) {
        EstadoInvitacion actual = estadoEn(ahora);
        if (actual != EstadoInvitacion.VIGENTE) {
            throw new OperacionInvalidaException("Esta invitación está " + actual.getNombre().toLowerCase()
                    + ". Pide a tu jefe un código nuevo.");
        }
        if (cuenta.perteneceAEmpresa()) {
            throw new OperacionInvalidaException("Tu cuenta ya pertenece a la empresa.");
        }
        if (empresa.existeCedula(cuenta.getDocumento())) {
            throw new OperacionInvalidaException("Ya hay una persona con el documento " + cuenta.getDocumento()
                    + " en la empresa. Habla con tu jefe para vincularla a tu cuenta.");
        }
        Horario horario = Horario.desdeTexto(DIAS_POR_DEFECTO, "07:00", "16:00");
        Usuario nuevo = Usuario.crear(rol, cuenta.getDocumento(), cuenta.getNombreCompleto(), CLAVE_TEMPORAL, horario);
        nuevo.vincularCuenta(cuenta);
        nuevo.setFechaIngreso(ahora.toLocalDate());
        if (nuevo instanceof Empleado empleado && supervisor != null) {
            empleado.setSupervisor(supervisor);
        }
        nuevo.setObra(obra);
        empresa.agregarUsuario(nuevo);
        estado = EstadoInvitacion.USADA;
        usadaPor = cuenta;
        usada = ahora;
        return nuevo;
    }

    public void cancelar(LocalDateTime ahora) {
        if (estadoEn(ahora) != EstadoInvitacion.VIGENTE) {
            throw new OperacionInvalidaException("Solo se pueden cancelar invitaciones vigentes.");
        }
        estado = EstadoInvitacion.CANCELADA;
    }

    /** Restaura el estado guardado (lo usa la persistencia). */
    public void restaurar(EstadoInvitacion estado, Cuenta usadaPor, LocalDateTime usada) {
        this.estado = estado == EstadoInvitacion.VENCIDA ? EstadoInvitacion.VIGENTE : estado;
        this.usadaPor = usadaPor;
        this.usada = usada;
    }

    public CodigoInvitacion getCodigo() {
        return codigo;
    }

    public String getRol() {
        return rol;
    }

    public Supervisor getSupervisor() {
        return supervisor;
    }

    public Obra getObra() {
        return obra;
    }

    public Jefe getCreadaPor() {
        return creadaPor;
    }

    public LocalDateTime getCreada() {
        return creada;
    }

    public LocalDateTime getVence() {
        return vence;
    }

    public EstadoInvitacion getEstadoGuardado() {
        return estado;
    }

    public Cuenta getUsadaPor() {
        return usadaPor;
    }

    public LocalDateTime getUsada() {
        return usada;
    }
}
