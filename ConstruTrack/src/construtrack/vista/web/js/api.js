const Api = {
  base: CONFIG.api,
  token: null,
  servidor: {},

  prefijo() {
    try { return sessionStorage.getItem('construtrack-demo') === '1' ? '/demo' : ''; } catch (e) { return ''; }
  },
  esDemo() {
    return this.prefijo() === '/demo';
  },

  async conectar(alEsperar) {
    try { this.token = sessionStorage.getItem('construtrack-token'); } catch (e) { this.token = null; }
    const inicio = Date.now();
    for (let intento = 1; ; intento++) {
      try {
        const respuesta = await fetch(this.base + '/api/estado', { signal: AbortSignal.timeout(8000) });
        const estado = await respuesta.json();
        if (estado.servidor === 'Java') {
          this.servidor = estado;
          return estado;
        }
      } catch (e) { }
      const segundos = Math.round((Date.now() - inicio) / 1000);
      if (alEsperar) alEsperar(intento, segundos);
      await new Promise((r) => setTimeout(r, Math.min(2000 * intento, 5000)));
    }
  },

  guardarToken(token) {
    this.token = token;
    try {
      if (token) sessionStorage.setItem('construtrack-token', token);
      else {
        sessionStorage.removeItem('construtrack-token');
        sessionStorage.removeItem('construtrack-demo');
      }
    } catch (e) { }
  },

  async pedir(metodo, ruta, cuerpo) {
    const cabeceras = { 'Content-Type': 'application/json' };
    if (this.token) cabeceras.Authorization = 'Bearer ' + this.token;
    let respuesta;
    try {
      respuesta = await fetch(this.base + this.prefijo() + ruta, {
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

  async blob(ruta) {
    const respuesta = await fetch(this.base + this.prefijo() + ruta, { headers: { Authorization: 'Bearer ' + this.token } });
    if (!respuesta.ok) {
      const datos = await respuesta.json().catch(() => ({}));
      throw new Error(datos.error || 'No se pudo abrir el archivo.');
    }
    const disposicion = respuesta.headers.get('Content-Disposition') || '';
    const nombre = (disposicion.match(/filename="([^"]+)"/) || [])[1] || 'archivo';
    return { nombre, tipo: respuesta.headers.get('Content-Type') || '', datos: await respuesta.blob() };
  },

  async descargar(ruta) {
    const respuesta = await fetch(this.base + this.prefijo() + ruta, { headers: { Authorization: 'Bearer ' + this.token } });
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

  async iniciarSesion(identificador, clave) {
    const datos = await this.pedir('POST', '/api/login', { identificador, clave });
    if (datos.token) this.guardarToken(datos.token);
    return datos;
  },
  async completarDosPasos(desafio, codigo) {
    const datos = await this.pedir('POST', '/api/login/dos-pasos', { desafio, codigo });
    this.guardarToken(datos.token);
    return datos;
  },
  dosPasos: () => Api.pedir('GET', '/api/dos-pasos'),
  iniciarDosPasos: () => Api.pedir('POST', '/api/dos-pasos/iniciar'),
  activarDosPasos: (codigo) => Api.pedir('POST', '/api/dos-pasos/activar', { codigo }),
  desactivarDosPasos: (clave, codigo) => Api.pedir('POST', '/api/dos-pasos/desactivar', { clave, codigo }),
  async entrarDemo(clave, rol) {
    try { sessionStorage.setItem('construtrack-demo', '1'); } catch (e) { throw new Error('Tu navegador no permite guardar la sesión.'); }
    try {
      const datos = await this.pedir('POST', '/api/demo/acceso', { clave, rol });
      this.guardarToken(datos.token);
      return datos;
    } catch (e) {
      sessionStorage.removeItem('construtrack-demo');
      throw e;
    }
  },
  async registrar(datos) {
    const respuesta = await this.pedir('POST', '/api/registro', datos);
    this.guardarToken(respuesta.token);
    return respuesta;
  },
  unirse: (codigo) => Api.pedir('POST', '/api/unirse', { codigo }),
  confirmarCorreo: (codigo) => Api.pedir('POST', '/api/verificacion', { codigo }),
  reenviarCodigo: () => Api.pedir('POST', '/api/verificacion/reenviar'),
  olvideClave: (correo) => Api.pedir('POST', '/api/clave/olvido', { correo }),
  restablecerClave: (token, clave, confirmacion) => Api.pedir('POST', '/api/clave/restablecer', { token, clave, confirmacion }),
  async cerrarSesion() {
    try { await this.pedir('POST', '/api/logout'); } catch (e) { }
    this.guardarToken(null);
  },
  estado: () => Api.pedir('GET', '/api/estado'),
  publico: () => Api.pedir('GET', '/api/publico'),
  yo: () => Api.pedir('GET', '/api/yo'),
  crearEmpresa: (nombre) => Api.pedir('POST', '/api/empresas', { nombre }),

  marcarEntrada: (datos) => Api.pedir('POST', '/api/asistencia/entrada', datos || {}),
  marcarSalida: (datos) => Api.pedir('POST', '/api/asistencia/salida', datos || {}),
  porConfirmar: () => Api.pedir('GET', '/api/asistencia/por-confirmar'),
  revisarAsistencia: (cedula, fecha, entrada, estado) =>
    Api.pedir('PUT', `/api/asistencia/${encodeURIComponent(cedula)}/${fecha}/verificacion`, { entrada, estado }),
  codigoObra: (codigo) => Api.pedir('GET', `/api/obras/${encodeURIComponent(codigo)}/codigo-asistencia`),
  listarUsuarios: () => Api.pedir('GET', '/api/usuarios'),
  verUsuario: (cedula) => Api.pedir('GET', '/api/usuarios/' + encodeURIComponent(cedula)),
  registrarUsuario: (datos) => Api.pedir('POST', '/api/usuarios', datos),
  modificarUsuario: (cedula, datos) => Api.pedir('PUT', '/api/usuarios/' + encodeURIComponent(cedula), datos),
  opcionesPersonal: () => Api.pedir('GET', '/api/personal/opciones'),
  ubicaciones: (fecha) => Api.pedir('GET', '/api/asistencia/ubicaciones?fecha=' + encodeURIComponent(fecha || '')),

  novedades: (categoria) => Api.pedir('GET', '/api/novedades?categoria=' + categoria),
  tablero: () => Api.pedir('GET', '/api/novedades?vista=tablero'),
  opcionesNovedades: (categoria) => Api.pedir('GET', '/api/novedades/opciones?categoria=' + categoria),
  registrarNovedad: (datos) => Api.pedir('POST', '/api/novedades', datos),
  revisarNovedad: (id, estado, nota) => Api.pedir('PUT', `/api/novedades/${id}/revision`, { estado, nota }),

  subirArchivo: (datos) => Api.pedir('POST', '/api/archivos', datos),
  archivo: (id) => Api.blob('/api/archivos/' + encodeURIComponent(id)),

  catalogo: () => Api.pedir('GET', '/api/catalogo'),
  agregarCatalogo: (tipo, nombre) => Api.pedir('POST', '/api/catalogo/' + tipo, { nombre }),
  quitarCatalogo: (tipo, nombre) => Api.pedir('POST', `/api/catalogo/${tipo}/quitar`, { nombre }),

  talento: () => Api.pedir('GET', '/api/talento'),
  pendientesEgreso: (cedula) => Api.pedir('GET', '/api/talento/pendientes/' + encodeURIComponent(cedula)),
  registrarEgreso: (cedula, datos) => Api.pedir('POST', '/api/talento/egresos/' + encodeURIComponent(cedula), datos),
  eliminarFicha: (id) => Api.pedir('POST', `/api/talento/egresos/${id}/eliminar-ficha`),
  descargarExpediente: (id) => Api.descargar(`/api/talento/egresos/${id}/expediente`),

  inventario: () => Api.pedir('GET', '/api/inventario'),
  crearArticulo: (datos) => Api.pedir('POST', '/api/inventario', datos),
  editarArticulo: (codigo, datos) => Api.pedir('PUT', '/api/inventario/' + encodeURIComponent(codigo), datos),
  eliminarArticulo: (codigo) => Api.pedir('DELETE', '/api/inventario/' + encodeURIComponent(codigo)),
  entregarArticulo: (codigo, datos) => Api.pedir('POST', `/api/inventario/${encodeURIComponent(codigo)}/entregas`, datos),
  devolverArticulo: (id, condicion, nota) => Api.pedir('PUT', `/api/inventario/entregas/${id}/devolucion`, { condicion, nota }),
  herramientasDe: (cedula) => Api.pedir('GET', '/api/inventario/a-cargo/' + encodeURIComponent(cedula)),

  estadisticas: (periodo) => Api.pedir('GET', '/api/estadisticas?periodo=' + periodo),
  alertas: (periodo) => Api.pedir('GET', '/api/supervision/alertas?periodo=' + periodo),
  horasExtra: () => Api.pedir('GET', '/api/horas-extra'),
  revisarHorasExtra: (cedula, fecha, estado) =>
    Api.pedir('PUT', `/api/horas-extra/${encodeURIComponent(cedula)}/${fecha}`, { estado }),
  descargarReporte: (tipo, formato, periodo) =>
    Api.descargar(`/api/reportes/${tipo}?formato=${formato}&periodo=${periodo}`),

  obras: () => Api.pedir('GET', '/api/obras'),
  crearObra: (datos) => Api.pedir('POST', '/api/obras', datos),
  editarObra: (codigo, datos) => Api.pedir('PUT', '/api/obras/' + encodeURIComponent(codigo), datos),
  eliminarObra: (codigo) => Api.pedir('DELETE', '/api/obras/' + encodeURIComponent(codigo)),
  asignarObra: (cedula, obra) => Api.pedir('PUT', `/api/usuarios/${encodeURIComponent(cedula)}/obra`, { obra }),
  iniciarObra: (codigo) => Api.pedir('POST', `/api/obras/${encodeURIComponent(codigo)}/inicio`),
  concluirObra: (codigo) => Api.pedir('POST', `/api/obras/${encodeURIComponent(codigo)}/conclusion`),
  reportarRetraso: (codigo, motivo, nuevaFecha) =>
    Api.pedir('POST', `/api/obras/${encodeURIComponent(codigo)}/retrasos`, { motivo, nuevaFecha }),
  revisarRetraso: (codigo, id) => Api.pedir('PUT', `/api/obras/${encodeURIComponent(codigo)}/retrasos/${id}/revision`),

  invitaciones: () => Api.pedir('GET', '/api/invitaciones'),
  crearInvitacion: (datos) => Api.pedir('POST', '/api/invitaciones', datos),
  configuracionInvitaciones: () => Api.pedir('GET', '/api/invitaciones/configuracion'),
  enviarInvitacion: (codigo, correo) =>
    Api.pedir('POST', `/api/invitaciones/${encodeURIComponent(codigo)}/enviar`, { correo }),
  cancelarInvitacion: (codigo) => Api.pedir('DELETE', '/api/invitaciones/' + encodeURIComponent(codigo)),
};
