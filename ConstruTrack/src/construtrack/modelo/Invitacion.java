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
    private final Gerente creadaPor;
    private final LocalDateTime creada;
    private final LocalDateTime vence;
    private EstadoInvitacion estado = EstadoInvitacion.VIGENTE;
    private String puesto;
    private String area;
    private Cuenta usadaPor;
    private LocalDateTime usada;

    public Invitacion(CodigoInvitacion codigo, String rol, Supervisor supervisor, Obra obra, Gerente creadaPor,
            LocalDateTime creada, LocalDateTime vence) {
        if (!Empleado.ROL.equals(rol) && !Supervisor.ROL.equals(rol)) {
            throw new DatoInvalidoException("El código de registro debe ser para un Empleado o un Supervisor.");
        }
        if (supervisor != null && !Empleado.ROL.equals(rol)) {
            throw new DatoInvalidoException("Solo los empleados tienen supervisor.");
        }
        if (!vence.isAfter(creada)) {
            throw new DatoInvalidoException("El código de registro debe vencer después de generarse.");
        }
        this.codigo = codigo;
        this.rol = rol;
        this.supervisor = supervisor;
        this.obra = obra;
        this.creadaPor = creadaPor;
        this.creada = creada;
        this.vence = vence;
    }

    public void asignarPuesto(String puesto, String area) {
        this.puesto = puesto;
        this.area = area;
    }

    public String getPuesto() {
        return puesto;
    }

    public String getArea() {
        return area;
    }

    public boolean caducadaEn(LocalDateTime ahora, int diasConservarUsadas) {
        EstadoInvitacion actual = estadoEn(ahora);
        return actual == EstadoInvitacion.VENCIDA || actual == EstadoInvitacion.CANCELADA
                || actual == EstadoInvitacion.USADA && usada != null && ahora.isAfter(usada.plusDays(diasConservarUsadas));
    }

    public EstadoInvitacion estadoEn(LocalDateTime ahora) {
        return estado == EstadoInvitacion.VIGENTE && ahora.isAfter(vence) ? EstadoInvitacion.VENCIDA : estado;
    }

    public Usuario canjear(Cuenta cuenta, LocalDateTime ahora) {
        return canjear(cuenta, creadaPor.getEmpresa(), ahora);
    }

    public Usuario canjear(Cuenta cuenta, Empresa empresa, LocalDateTime ahora) {
        if (creadaPor.getEmpresa() != null && creadaPor.getEmpresa() != empresa) {
            throw new OperacionInvalidaException("Este código de registro es de otra empresa.");
        }
        EstadoInvitacion actual = estadoEn(ahora);
        if (actual != EstadoInvitacion.VIGENTE) {
            throw new OperacionInvalidaException("Este código de registro está " + actual.getNombre().toLowerCase()
                    + ". Pide a tu gerente un código nuevo.");
        }
        if (cuenta.perteneceAEmpresa()) {
            throw new OperacionInvalidaException("Tu cuenta ya pertenece a la empresa.");
        }
        cuenta.exigirVerificada();
        if (empresa.existeCedula(cuenta.getDocumento())) {
            throw new OperacionInvalidaException("Ya hay una persona con el documento " + cuenta.getDocumento()
                    + " registrada en ConstruTrack. Habla con tu gerente para vincularla a tu cuenta.");
        }
        Horario horario = Horario.desdeTexto(DIAS_POR_DEFECTO, "07:00", "16:00");
        Usuario nuevo = Usuario.restaurar(rol, cuenta.getDocumento(), cuenta.getNombreCompleto(), cuenta.getClaveResumen(),
                horario);
        nuevo.vincularCuenta(cuenta);
        nuevo.setFechaIngreso(ahora.toLocalDate());
        nuevo.setPuesto(puesto);
        if (nuevo instanceof Empleado empleado && supervisor != null && supervisor.estaActivo()) {
            empleado.setSupervisor(supervisor);
        }
        nuevo.setObra(obra != null && obra.admitePersonal() ? obra : null);
        empresa.agregarUsuario(nuevo);
        if (area != null && empresa.buscarArea(area) != null) {
            empresa.incluirEnArea(nuevo, area);
        }
        estado = EstadoInvitacion.USADA;
        usadaPor = cuenta;
        usada = ahora;
        return nuevo;
    }

    public void cancelar(LocalDateTime ahora) {
        if (estadoEn(ahora) != EstadoInvitacion.VIGENTE) {
            throw new OperacionInvalidaException("Solo se pueden cancelar códigos de registro vigentes.");
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

    public Gerente getCreadaPor() {
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
