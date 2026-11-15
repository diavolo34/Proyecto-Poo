// Chatbot de Recursos Humanos: responde preguntas frecuentes y, con la sesión iniciada,
// atiende consultas según el rol (horario, marcar entrada/salida, quién está presente, etc.).
class Chatbot {
  /**
   * @param {object} contexto
   *   usuario():  usuario con sesión iniciada o null
   *   alCambiarUsuario(usuario): se llama después de marcar entrada o salida desde el chat
   */
  constructor(contexto) {
    this.contexto = contexto;
  }

  /** Botones de preguntas sugeridas según quién esté usando la página. */
  sugerencias() {
    const usuario = this.contexto.usuario();
    if (!usuario) return ['¿Cuándo pagan la nómina?', '¿Cómo pido un permiso?', 'Normas de seguridad', '¿Cómo me registro?'];
    const comunes = ['Mi horario', 'Marcar entrada', 'Marcar salida'];
    if (usuario.rol === 'Empleado') return [...comunes, '¿Cuándo pagan la nómina?'];
    if (usuario.rol === 'Supervisor') return ['¿Quién está presente?', '¿Quién llegó tarde hoy?', '¿Quién falta hoy?', ...comunes];
    return ['¿Cuántos empleados hay?', '¿Quién está presente?', '¿Quién llegó tarde hoy?', ...comunes];
  }

  /** Devuelve la respuesta (texto) al mensaje del usuario. */
  async responder(mensaje) {
    const texto = mensaje.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
    const tiene = (...palabras) => palabras.some((p) => texto.includes(p));
    const usuario = this.contexto.usuario();

    // Consultas que dependen del usuario con sesión iniciada
    const solo = texto.replace(/[^a-z ]/g, '').trim();
    if (tiene('marcar entrada', 'marca mi entrada', 'registrar entrada', 'registra mi entrada', 'ya llegue') || solo === 'entrada') {
      return this.marcar('entrada');
    }
    if (tiene('marcar salida', 'marca mi salida', 'registrar salida', 'registra mi salida', 'me voy') || solo === 'salida') {
      return this.marcar('salida');
    }
    if (tiene('mi horario', 'que horario tengo', 'cual es mi horario')) {
      if (!usuario) return 'Inicia sesión con tu cédula y te digo tu horario.';
      const h = usuario.horario;
      return `Tu horario es ${textoDias(h.dias)}, de ${h.horaEntrada} a ${h.horaSalida}. ` +
        (usuario.trabajaHoy ? 'Hoy te toca trabajar.' : 'Hoy no es día laborable para ti.');
    }
    if (tiene('presente', 'quien esta', 'quienes estan', 'en la obra')) return this.consultarPersonal('presentes');
    if (tiene('llego tarde', 'llegaron tarde', 'tarde hoy', 'retraso', 'impuntual')) return this.consultarPersonal('tarde');
    if (tiene('falta', 'ausente', 'no vino', 'no ha llegado')) return this.consultarPersonal('ausentes');
    if (tiene('cuantos', 'total de empleados', 'cantidad')) return this.consultarPersonal('total');

    // Preguntas frecuentes
    if (tiene('pago', 'nomina', 'sueldo', 'salario', 'quincena')) return 'La nómina se cierra los jueves y se paga los viernes por transferencia.';
    if (tiene('permiso', 'vacacion', 'reposo')) return 'Pide permiso con 24h de anticipación a tu supervisor. Los reposos médicos se entregan en la oficina de RRHH.';
    if (tiene('seguridad', 'casco', 'epp', 'botas', 'chaleco')) return 'En la obra es obligatorio usar casco, botas de seguridad y chaleco reflectivo. Sin equipo de protección no se permite el ingreso.';
    if (tiene('registr', 'nuevo empleado', 'inscrib', 'cuenta')) return 'El registro de empleados y supervisores lo hace el Jefe desde su panel (botón “Registrar usuario”). Luego entras con tu cédula y la clave que te asigne.';
    if (tiene('horario', 'atencion', 'oficina', 'abren')) return 'La oficina de RRHH atiende de lunes a viernes de 8:00 a 17:00. En la obra se trabaja según el horario asignado a cada persona.';
    if (tiene('contacto', 'telefono', 'ubicacion', 'direccion', 'donde')) return 'Puedes ir a la oficina de RRHH en la caseta principal de la obra o llamar al 0212-555-0100.';
    if (tiene('supervisor', 'jefe', 'rol')) return 'Roles: el Empleado marca su entrada y salida; el Supervisor ve el horario y la asistencia de sus empleados; el Jefe registra, edita y elimina empleados y supervisores.';
    if (tiene('hola', 'buenas', 'buenos')) return `¡Hola${usuario ? ', ' + usuario.nombre.split(' ')[0] : ''}! ¿En qué te ayudo?`;
    if (tiene('gracias')) return '¡Con gusto! Aquí estoy si necesitas algo más.';
    if (tiene('ayuda', 'que puedes', 'opciones')) return 'Puedo responder sobre nómina, permisos, seguridad y horarios. Con tu sesión iniciada también puedo marcar tu entrada o salida y decirte quién está en la obra.';
    return 'No entendí tu pregunta. Prueba con una de las sugerencias o consulta con tu supervisor.';
  }

  async marcar(tipo) {
    const usuario = this.contexto.usuario();
    if (!usuario) return `Primero inicia sesión para marcar tu ${tipo}.`;
    try {
      const respuesta = tipo === 'entrada' ? await Api.marcarEntrada() : await Api.marcarSalida();
      await this.contexto.alCambiarUsuario(respuesta.usuario);
      return '✅ ' + respuesta.mensaje;
    } catch (e) {
      return '⚠️ ' + e.message;
    }
  }

  async consultarPersonal(consulta) {
    const usuario = this.contexto.usuario();
    if (!usuario) return 'Inicia sesión como Supervisor o Jefe para consultar al personal.';
    if (usuario.rol === 'Empleado') return 'Esa consulta es solo para supervisores y jefes. Tú puedes preguntarme por tu horario o marcar tu entrada.';
    let personal;
    try {
      personal = await Api.listarUsuarios();
    } catch (e) {
      return '⚠️ ' + e.message;
    }
    const lista = (personas) => personas.map((p) => `• ${p.nombre}`).join('\n');
    const alcance = usuario.rol === 'Supervisor' ? 'de tus empleados' : 'del personal';

    if (consulta === 'total') {
      const contar = (rol) => personal.filter((p) => p.rol === rol).length;
      return usuario.rol === 'Supervisor'
        ? `Tienes ${personal.length} empleados a cargo; ${personal.filter((p) => p.presente).length} están en la obra.`
        : `Hay ${personal.length} personas registradas: ${contar('Empleado')} empleados, ${contar('Supervisor')} supervisores y ${contar('Jefe')} jefe(s). Presentes ahora: ${personal.filter((p) => p.presente).length}.`;
    }
    if (consulta === 'presentes') {
      const presentes = personal.filter((p) => p.presente);
      return presentes.length ? `Presentes ${alcance} (${presentes.length}):\n${lista(presentes)}` : `Nadie ${alcance} está en la obra ahora.`;
    }
    if (consulta === 'tarde') {
      const tarde = personal.filter((p) => p.asistenciaHoy && p.asistenciaHoy.tarde);
      return tarde.length ? `Llegaron tarde hoy:\n${tarde.map((p) => `• ${p.nombre} (${p.asistenciaHoy.horaEntrada})`).join('\n')}` : 'Hoy nadie ha llegado tarde. 👏';
    }
    const ausentes = personal.filter((p) => p.trabajaHoy && !p.asistenciaHoy);
    return ausentes.length ? `Les toca trabajar hoy y no han marcado entrada:\n${lista(ausentes)}` : 'Todos los que trabajan hoy ya marcaron su entrada.';
  }
}
