// Capa de acceso a datos: todas las llamadas al servidor Java pasan por aquí.
// No hay modo demostración: si el servidor no responde, la página lo dice y reintenta.
const Api = {
  base: CONFIG.api,
  token: null,

  /** Espera a que el servidor responda. El plan gratuito de Render "duerme" y tarda hasta ~1 minuto en despertar. */
  async conectar(alEsperar) {
    try { this.token = sessionStorage.getItem('construtrack-token'); } catch (e) { this.token = null; }
    const inicio = Date.now();
    for (let intento = 1; ; intento++) {
      try {
        const respuesta = await fetch(this.base + '/api/estado', { signal: AbortSignal.timeout(8000) });
        const estado = await respuesta.json();
        if (estado.servidor === 'Java') return estado;
      } catch (e) { /* sigue esperando */ }
      const segundos = Math.round((Date.now() - inicio) / 1000);
      if (alEsperar) alEsperar(intento, segundos);
      await new Promise((r) => setTimeout(r, Math.min(2000 * intento, 5000)));
    }
  },

  guardarToken(token) {
    this.token = token;
    try {
      if (token) sessionStorage.setItem('construtrack-token', token);
      else sessionStorage.removeItem('construtrack-token');
    } catch (e) { /* sesión solo en memoria */ }
  },

  /** Envía una petición y devuelve los datos; lanza un Error con el mensaje del servidor si falla. */
  async pedir(metodo, ruta, cuerpo) {
    const cabeceras = { 'Content-Type': 'application/json' };
    if (this.token) cabeceras.Authorization = 'Bearer ' + this.token;
    let respuesta;
    try {
      respuesta = await fetch(this.base + ruta, {
        method: metodo, headers: cabeceras, body: cuerpo ? JSON.stringify(cuerpo) : undefined,
      });
    } catch (e) {
      const error = new Error('No hay conexión con el servidor. Revisa tu internet e intenta de nuevo.');
      error.codigo = 0;
      throw error;
    }
    const datos = await respuesta.json().catch(() => ({}));
    if (respuesta.status === 401 && !['/api/login', '/api/registro'].includes(ruta)) this.guardarToken(null);
    if (!respuesta.ok) {
      const error = new Error(datos.error || 'Error ' + respuesta.status);
      error.codigo = respuesta.status;
      throw error;
    }
    return datos;
  },

  /** Descarga un archivo (reportes) respetando el nombre que propone el servidor. */
  async descargar(ruta) {
    const respuesta = await fetch(this.base + ruta, { headers: { Authorization: 'Bearer ' + this.token } });
    if (!respuesta.ok) {
      const datos = await respuesta.json().catch(() => ({}));
      throw new Error(datos.error || 'No se pudo descargar el reporte.');
    }
    const disposicion = respuesta.headers.get('Content-Disposition') || '';
    const nombre = (disposicion.match(/filename="([^"]+)"/) || [])[1] || 'reporte.txt';
    const enlace = document.createElement('a');
    enlace.href = URL.createObjectURL(await respuesta.blob());
    enlace.download = nombre;
    document.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
    return nombre;
  },

  // ---- sesión y registro
  async iniciarSesion(identificador, clave) {
    const datos = await this.pedir('POST', '/api/login', { identificador, clave });
    this.guardarToken(datos.token);
    return datos;
  },
  async registrar(datos) {
    const respuesta = await this.pedir('POST', '/api/registro', datos);
    this.guardarToken(respuesta.token);
    return respuesta;
  },
  unirse: (codigo) => Api.pedir('POST', '/api/unirse', { codigo }),
  async cerrarSesion() {
    try { await this.pedir('POST', '/api/logout'); } catch (e) { /* la sesión ya no existía */ }
    this.guardarToken(null);
  },
  estado: () => Api.pedir('GET', '/api/estado'),
  publico: () => Api.pedir('GET', '/api/publico'),
  yo: () => Api.pedir('GET', '/api/yo'),
  crearEmpresa: (nombre) => Api.pedir('POST', '/api/empresas', { nombre }),

  // ---- asistencia y personal
  marcarEntrada: () => Api.pedir('POST', '/api/asistencia/entrada'),
  marcarSalida: () => Api.pedir('POST', '/api/asistencia/salida'),
  listarUsuarios: () => Api.pedir('GET', '/api/usuarios'),
  verUsuario: (cedula) => Api.pedir('GET', '/api/usuarios/' + encodeURIComponent(cedula)),
  registrarUsuario: (datos) => Api.pedir('POST', '/api/usuarios', datos),
  modificarUsuario: (cedula, datos) => Api.pedir('PUT', '/api/usuarios/' + encodeURIComponent(cedula), datos),
  eliminarUsuario: (cedula) => Api.pedir('DELETE', '/api/usuarios/' + encodeURIComponent(cedula)),
  opcionesPersonal: () => Api.pedir('GET', '/api/personal/opciones'),

  // ---- incidencias
  incidencias: () => Api.pedir('GET', '/api/incidencias'),
  opcionesIncidencias: () => Api.pedir('GET', '/api/incidencias/opciones'),
  reportarIncidencia: (datos) => Api.pedir('POST', '/api/incidencias', datos),
  revisarIncidencia: (id, estado, comentario) =>
    Api.pedir('PUT', `/api/incidencias/${id}/revision`, { estado, comentario }),

  // ---- supervisión
  estadisticas: (periodo) => Api.pedir('GET', '/api/estadisticas?periodo=' + periodo),
  alertas: (periodo) => Api.pedir('GET', '/api/supervision/alertas?periodo=' + periodo),
  horasExtra: () => Api.pedir('GET', '/api/horas-extra'),
  revisarHorasExtra: (cedula, fecha, estado) =>
    Api.pedir('PUT', `/api/horas-extra/${encodeURIComponent(cedula)}/${fecha}`, { estado }),
  descargarReporte: (tipo, formato, periodo) =>
    Api.descargar(`/api/reportes/${tipo}?formato=${formato}&periodo=${periodo}`),

  // ---- obras
  obras: () => Api.pedir('GET', '/api/obras'),
  crearObra: (datos) => Api.pedir('POST', '/api/obras', datos),
  editarObra: (codigo, datos) => Api.pedir('PUT', '/api/obras/' + encodeURIComponent(codigo), datos),
  eliminarObra: (codigo) => Api.pedir('DELETE', '/api/obras/' + encodeURIComponent(codigo)),
  asignarObra: (cedula, obra) => Api.pedir('PUT', `/api/usuarios/${encodeURIComponent(cedula)}/obra`, { obra }),

  // ---- invitaciones
  invitaciones: () => Api.pedir('GET', '/api/invitaciones'),
  crearInvitacion: (datos) => Api.pedir('POST', '/api/invitaciones', datos),
  configuracionInvitaciones: () => Api.pedir('GET', '/api/invitaciones/configuracion'),
  enviarInvitacion: (codigo, correo) =>
    Api.pedir('POST', `/api/invitaciones/${encodeURIComponent(codigo)}/enviar`, { correo }),
  cancelarInvitacion: (codigo) => Api.pedir('DELETE', '/api/invitaciones/' + encodeURIComponent(codigo)),
};
