// Capa de acceso a datos de la página.
// Si encuentra el servidor Java (NetBeans, puerto 8080) trabaja con él; si no, usa el modo demostración.
const Api = {
  modo: null,      // 'java' o 'demo'
  base: '',
  token: null,

  async iniciar() {
    try { this.token = sessionStorage.getItem('construtrack-token'); } catch (e) { this.token = null; }

    const candidatos = [];
    if (location.protocol.startsWith('http')) candidatos.push('');   // página servida por el propio servidor Java
    if (location.protocol === 'file:' || ['localhost', '127.0.0.1'].includes(location.hostname)) {
      candidatos.push('http://localhost:8080');                      // index.html abierto directamente
    }
    for (const base of candidatos) {
      try {
        const respuesta = await fetch(base + '/api/estado', { signal: AbortSignal.timeout(1500) });
        const estado = await respuesta.json();
        if (estado.servidor === 'Java') {
          this.modo = 'java';
          this.base = base;
          return;
        }
      } catch (e) { /* no hay servidor en esta dirección */ }
    }
    this.modo = 'demo';
    ServidorLocal.iniciar();
    if (this.token && !this.token.startsWith('demo-')) this.guardarToken(null);
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
    let codigo;
    let datos;
    if (this.modo === 'demo') {
      ({ codigo, datos } = ServidorLocal.atender(metodo, ruta, cuerpo, this.token));
    } else {
      const cabeceras = { 'Content-Type': 'application/json' };
      if (this.token) cabeceras.Authorization = 'Bearer ' + this.token;
      try {
        const respuesta = await fetch(this.base + ruta, {
          method: metodo, headers: cabeceras, body: cuerpo ? JSON.stringify(cuerpo) : undefined,
        });
        codigo = respuesta.status;
        datos = await respuesta.json();
      } catch (e) {
        throw new Error('No hay conexión con el servidor Java. ¿Sigue corriendo en NetBeans?');
      }
    }
    if (codigo === 401 && ruta !== '/api/login') this.guardarToken(null);
    if (codigo >= 400) {
      const error = new Error(datos.error || 'Error ' + codigo);
      error.codigo = codigo;
      throw error;
    }
    return datos;
  },

  async iniciarSesion(cedula, clave) {
    const datos = await this.pedir('POST', '/api/login', { cedula, clave });
    this.guardarToken(datos.token);
    return datos.usuario;
  },
  async cerrarSesion() {
    try { await this.pedir('POST', '/api/logout'); } catch (e) { /* la sesión ya no existía */ }
    this.guardarToken(null);
  },
  yo: () => Api.pedir('GET', '/api/yo'),
  marcarEntrada: () => Api.pedir('POST', '/api/asistencia/entrada'),
  marcarSalida: () => Api.pedir('POST', '/api/asistencia/salida'),
  listarUsuarios: () => Api.pedir('GET', '/api/usuarios'),
  verUsuario: (cedula) => Api.pedir('GET', '/api/usuarios/' + encodeURIComponent(cedula)),
  registrarUsuario: (datos) => Api.pedir('POST', '/api/usuarios', datos),
  modificarUsuario: (cedula, datos) => Api.pedir('PUT', '/api/usuarios/' + encodeURIComponent(cedula), datos),
  eliminarUsuario: (cedula) => Api.pedir('DELETE', '/api/usuarios/' + encodeURIComponent(cedula)),
};
