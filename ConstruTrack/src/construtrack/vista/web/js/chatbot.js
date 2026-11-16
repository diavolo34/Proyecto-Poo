// Asistente "Arqui": responde preguntas frecuentes y, con sesión iniciada, consulta datos según el rol.
class Chatbot {
  /**
   * @param {object} contexto
   *   usuario():  usuario con sesión iniciada o null
   *   alCambiar(): se llama después de marcar entrada o salida desde el chat
   */
  constructor(contexto) {
    this.contexto = contexto;
  }

  /** Preguntas sugeridas según quién usa la página (reconocer en vez de recordar). */
  sugerencias() {
    const usuario = this.contexto.usuario();
    if (!usuario) return ['¿Cómo me registro?', '¿Qué es el código de invitación?', '¿Qué hace cada rol?', 'Normas de seguridad'];
    const comunes = ['Mi horario', 'Marcar entrada', 'Marcar salida', '¿Cómo reporto una falta?'];
    if (usuario.rol === 'Empleado') return comunes;
    return ['¿Hay alertas?', '¿Quién está presente?', '¿Quién llegó tarde hoy?', '¿Quién falta hoy?', ...comunes.slice(0, 2)];
  }

  async responder(mensaje) {
    const texto = mensaje.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
    const tiene = (...palabras) => palabras.some((p) => texto.includes(p));
    const usuario = this.contexto.usuario();
    const solo = texto.replace(/[^a-z ]/g, '').trim();

    if (tiene('marcar entrada', 'marca mi entrada', 'registrar entrada', 'ya llegue') || solo === 'entrada') return this.marcar('entrada');
    if (tiene('marcar salida', 'marca mi salida', 'registrar salida', 'me voy') || solo === 'salida') return this.marcar('salida');
    if (tiene('mi horario', 'que horario tengo')) {
      if (!usuario) return 'Inicia sesión y te digo tu horario.';
      const h = usuario.horario;
      return `Tu horario es ${textoDias(h.dias)}, de ${h.horaEntrada} a ${h.horaSalida}. ` +
        (usuario.trabajaHoy ? 'Hoy te toca trabajar.' : 'Hoy no es día laborable para ti.');
    }
    if (tiene('alerta')) return this.alertas();
    if (tiene('presente', 'quien esta', 'en la obra')) return this.consultarPersonal('presentes');
    if (tiene('llego tarde', 'llegaron tarde', 'tarde hoy', 'impuntual')) return this.consultarPersonal('tarde');
    if (tiene('quien falta', 'ausente', 'no vino', 'falta hoy')) return this.consultarPersonal('ausentes');
    if (tiene('cuantos', 'total de empleados')) return this.consultarPersonal('total');

    if (tiene('codigo', 'invitacion', 'unirme', 'unirse')) {
      return 'El código de invitación (formato CT-XXXX-XXXX) te lo da tu jefe. Sirve una sola vez y define tu rol, tu supervisor y tu obra. Lo escribes después de crear tu cuenta, en la pantalla «Únete a tu empresa».';
    }
    if (tiene('registr', 'crear cuenta', 'inscrib', 'cuenta nueva')) {
      return 'Pulsa «Crear cuenta», escribe tu correo, nombre, apellido, cédula o DNI, teléfono y una clave. Después ingresa el código de invitación que te dé tu jefe y listo.';
    }
    if (tiene('reporto', 'reportar', 'falta', 'reposo', 'enfermo', 'permiso')) {
      return 'Ve a «Incidencias», elige el tipo (reposo médico, permiso personal, retardo, accidente…), las fechas y una descripción. Tu supervisor la revisa y la aprueba o la rechaza; un reposo aprobado no cuenta como falta.';
    }
    if (tiene('seguridad', 'casco', 'epp', 'botas', 'chaleco')) return 'En la obra es obligatorio usar casco, botas de seguridad y chaleco reflectivo. Sin equipo de protección no se permite el ingreso.';
    if (tiene('horario', 'oficina', 'abren')) return 'La oficina de RRHH atiende de lunes a viernes de 8:00 a 17:00.';
    if (tiene('contacto', 'telefono', 'direccion')) return 'Oficina de RRHH en la caseta principal de la obra · 0212-555-0100.';
    if (tiene('rol', 'supervisor', 'jefe')) return 'El Empleado marca su asistencia y reporta incidencias; el Supervisor revisa a su equipo, recibe alertas y genera reportes; el Jefe además gestiona al personal y crea invitaciones.';
    if (tiene('hola', 'buenas', 'buenos')) return `¡Hola${usuario ? ', ' + usuario.nombre.split(' ')[0] : ''}! ¿En qué te ayudo?`;
    if (tiene('gracias')) return '¡Con gusto! Aquí estoy si necesitas algo más.';
    return 'No entendí tu pregunta. Prueba con una de las sugerencias o escribe, por ejemplo, «¿cómo reporto una falta?».';
  }

  async marcar(tipo) {
    if (!this.contexto.usuario()) return `Primero inicia sesión para marcar tu ${tipo}.`;
    try {
      const respuesta = tipo === 'entrada' ? await Api.marcarEntrada() : await Api.marcarSalida();
      await this.contexto.alCambiar();
      return respuesta.mensaje;
    } catch (e) {
      return 'No se pudo: ' + e.message;
    }
  }

  async alertas() {
    const usuario = this.contexto.usuario();
    if (!usuario || usuario.rol === 'Empleado') return 'Las alertas son para supervisores y jefes.';
    try {
      const alertas = await Api.alertas('semana');
      return alertas.length
        ? `Tienes ${alertas.length} alerta(s) esta semana:\n` + alertas.map((a) => `• [${a.nivel}] ${a.nombre}: ${a.mensaje}`).join('\n')
        : 'No hay alertas esta semana. Todo en orden.';
    } catch (e) {
      return 'No se pudo consultar: ' + e.message;
    }
  }

  async consultarPersonal(consulta) {
    const usuario = this.contexto.usuario();
    if (!usuario) return 'Inicia sesión como Supervisor o Jefe para consultar al personal.';
    if (usuario.rol === 'Empleado') return 'Esa consulta es para supervisores y jefes. Tú puedes preguntarme por tu horario o marcar tu entrada.';
    let personal;
    try {
      personal = await Api.listarUsuarios();
    } catch (e) {
      return 'No se pudo consultar: ' + e.message;
    }
    const lista = (personas) => personas.map((p) => `• ${p.nombre}`).join('\n');
    if (consulta === 'total') {
      return `Hay ${personal.length} personas a tu cargo; ${personal.filter((p) => p.presente).length} están en la obra ahora.`;
    }
    if (consulta === 'presentes') {
      const presentes = personal.filter((p) => p.presente);
      return presentes.length ? `En la obra ahora (${presentes.length}):\n${lista(presentes)}` : 'Nadie está en la obra ahora.';
    }
    if (consulta === 'tarde') {
      const tarde = personal.filter((p) => p.asistenciaHoy && p.asistenciaHoy.tarde);
      return tarde.length ? 'Llegaron tarde hoy:\n' + tarde.map((p) => `• ${p.nombre} (${p.asistenciaHoy.horaEntrada})`).join('\n') : 'Hoy nadie ha llegado tarde.';
    }
    const ausentes = personal.filter((p) => p.trabajaHoy && !p.asistenciaHoy);
    return ausentes.length ? `Les toca trabajar hoy y no han marcado entrada:\n${lista(ausentes)}` : 'Todos los que trabajan hoy ya marcaron su entrada.';
  }
}

/** Ventana flotante del asistente. Se monta igual en la portada y en la aplicación. */
const ChatUI = {
  bot: null,

  montar(contexto) {
    this.bot = new Chatbot(contexto);
    document.body.insertAdjacentHTML('beforeend', `
      <button class="btn btn-primario chat-boton" id="chat-abrir" aria-expanded="false" aria-controls="chat-panel">Arqui</button>
      <section class="chat-panel oculto" id="chat-panel" aria-label="Asistente Arqui">
        <div class="chat-cabecera"><strong>Arqui</strong>
          <button class="btn btn-enlace" type="button" id="chat-cerrar" aria-label="Cerrar el asistente">✕</button></div>
        <div class="chat-mensajes" id="chat-mensajes" aria-live="polite"></div>
        <div class="chat-sugerencias" id="chat-sugerencias"></div>
        <form class="chat-formulario" id="chat-formulario">
          <label for="chat-texto" class="solo-lector">Tu pregunta</label>
          <input id="chat-texto" autocomplete="off" placeholder="Escribe tu pregunta…">
          <button class="btn btn-primario" type="submit">Enviar</button>
        </form>
      </section>`);
    $('#chat-abrir').addEventListener('click', () => this.alternar());
    $('#chat-cerrar').addEventListener('click', () => this.alternar());
    $('#chat-formulario').addEventListener('submit', (e) => { e.preventDefault(); this.enviar(); });
    $('#chat-sugerencias').addEventListener('click', (e) => { if (e.target.tagName === 'BUTTON') this.enviar(e.target.textContent); });
    this.agregar('¡Hola! Soy Arqui, el asistente de ConstruTrack. Elige una pregunta o escríbeme.', false);
  },

  alternar() {
    const panel = $('#chat-panel');
    panel.classList.toggle('oculto');
    $('#chat-abrir').setAttribute('aria-expanded', String(!panel.classList.contains('oculto')));
    if (!panel.classList.contains('oculto')) {
      this.actualizarSugerencias();
      $('#chat-texto').focus();
    }
  },

  agregar(texto, deUsuario) {
    const burbuja = document.createElement('div');
    burbuja.className = 'burbuja ' + (deUsuario ? 'burbuja-yo' : 'burbuja-bot');
    burbuja.style.whiteSpace = 'pre-line';
    burbuja.textContent = texto;
    const caja = $('#chat-mensajes');
    caja.appendChild(burbuja);
    caja.scrollTop = caja.scrollHeight;
    return burbuja;
  },

  async enviar(texto) {
    const entrada = $('#chat-texto');
    const mensaje = (texto ?? entrada.value).trim();
    if (!mensaje) return;
    entrada.value = '';
    this.agregar(mensaje, true);
    // Microinteracción: tres puntos "escribiendo…" mientras se prepara la respuesta
    const escribiendo = this.agregar('', false);
    escribiendo.innerHTML = '<span class="escribiendo" aria-label="Escribiendo"><span></span><span></span><span></span></span>';
    const respuesta = await this.bot.responder(mensaje);
    setTimeout(() => { escribiendo.textContent = respuesta; this.actualizarSugerencias(); }, 350);
  },

  actualizarSugerencias() {
    $('#chat-sugerencias').innerHTML = this.bot.sugerencias().map((s) => `<button type="button">${esc(s)}</button>`).join('');
  },
};
