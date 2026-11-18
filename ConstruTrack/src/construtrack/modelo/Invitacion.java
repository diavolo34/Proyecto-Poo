package construtrack.modelo;

import construtrack.modelo.excepciones.DatoInvalidoException;
import construtrack.modelo.excepciones.OperacionInvalidaException;
import java.time.LocalDateTime;

public class Invitacion {
    private static final String DIAS_POR_DEFECTO = "LU,MA,MI,JU,VI";

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

    public EstadoInvitacion estadoEn(LocalDateTime ahora) {
        return estado == EstadoInvitacion.VIGENTE && ahora.isAfter(vence) ? EstadoInvitacion.VENCIDA : estado;
    }

    public Usuario canjear(Cuenta cuenta, LocalDateTime ahora) {
        return canjear(cuenta, creadaPor.getEmpresa(), ahora);
    }

    public Usuario canjear(Cuenta cuenta, Empresa empresa, LocalDateTime ahora) {
        if (creadaPor.getEmpresa() != null && creadaPor.getEmpresa() != empresa) {
            throw new OperacionInvalidaException("Esta invitación es de otra empresa.");
        }
        EstadoInvitacion actual = estadoEn(ahora);
        if (actual != EstadoInvitacion.VIGENTE) {
            throw new OperacionInvalidaException("Esta invitación está " + actual.getNombre().toLowerCase()
                    + ". Pide a tu jefe un código nuevo.");
        }
        if (cuenta.perteneceAEmpresa()) {
            throw new OperacionInvalidaException("Tu cuenta ya pertenece a la empresa.");
        }
        cuenta.exigirVerificada();
        if (empresa.existeCedula(cuenta.getDocumento())) {
            throw new OperacionInvalidaException("Ya hay una persona con el documento " + cuenta.getDocumento()
                    + " registrada en ConstruTrack. Habla con tu jefe para vincularla a tu cuenta.");
        }
        Horario horario = Horario.desdeTexto(DIAS_POR_DEFECTO, "07:00", "16:00");
        Usuario nuevo = Usuario.restaurar(rol, cuenta.getDocumento(), cuenta.getNombreCompleto(), cuenta.getClaveResumen(),
                horario);
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
