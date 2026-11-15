// Modo demostración: reproduce en el navegador la misma lógica del servidor Java
// (mismas rutas, mismas reglas de cada rol y mismas respuestas JSON), guardando los datos en localStorage.
// Se usa cuando la página está publicada en internet y el servidor Java no está disponible.
const ServidorLocal = (() => {
  const CLAVE_ALMACEN = 'construtrack-datos-v1';
  const CODIGOS = ['LU', 'MA', 'MI', 'JU', 'VI', 'SA', 'DO'];
  const TOLERANCIA_MINUTOS = 10;
  let datos = null;

  class ErrorApi extends Error {
    constructor(codigo, mensaje) { super(mensaje); this.codigo = codigo; }
  }
  const invalido = (mensaje) => new ErrorApi(400, mensaje);
  const conflicto = (mensaje) => new ErrorApi(409, mensaje);
  const prohibido = (mensaje) => new ErrorApi(403, mensaje);

  // ---------- Fechas y horas ----------
  const pad = (n) => String(n).padStart(2, '0');
  const fecha = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  const hoy = () => fecha(new Date());
  const ayer = () => { const d = new Date(); d.setDate(d.getDate() - 1); return fecha(d); };
  const horaActual = () => { const d = new Date(); return `${pad(d.getHours())}:${pad(d.getMinutes())}`; };
  const minutos = (hora) => { const [h, m] = hora.split(':').map(Number); return h * 60 + m; };
  const codigoDeHoy = () => CODIGOS[(new Date().getDay() + 6) % 7]; // getDay(): 0 = domingo

  // ---------- Persistencia ----------
  function guardar() {
    try { localStorage.setItem(CLAVE_ALMACEN, JSON.stringify(datos)); } catch (e) { /* sin almacenamiento: solo en memoria */ }
  }

  function cargar() {
    try {
      const texto = localStorage.getItem(CLAVE_ALMACEN);
      if (texto) { datos = JSON.parse(texto); return; }
    } catch (e) { /* almacenamiento no disponible */ }
    datos = crearDatosIniciales();
    guardar();
  }

  /** Mismo personal de prueba que BaseDatos.crearDatosIniciales() en Java. */
  function crearDatosIniciales() {
    const oficina = { dias: 'LU,MA,MI,JU,VI', horaEntrada: '08:00', horaSalida: '17:00' };
    const obra = { dias: 'LU,MA,MI,JU,VI', horaEntrada: '07:00', horaSalida: '16:00' };
    const sabados = { dias: 'MA,MI,JU,VI,SA', horaEntrada: '07:00', horaSalida: '15:00' };
    const usuario = (rol, cedula, nombre, clave, horario) =>
      ({ rol, cedula, nombre, clave, horario: { ...horario }, presente: false, supervisor: null, asistencias: [] });

    const usuarios = [
      usuario('Jefe', '10000001', 'Carlos Méndez', 'jefe123', oficina),
      usuario('Supervisor', '20000001', 'Ana Rodríguez', 'super123', obra),
      usuario('Supervisor', '20000002', 'Luis Pérez', 'super123', sabados),
    ];
    [['30000001', 'José García'], ['30000002', 'María López'], ['30000003', 'Pedro Ramírez'],
     ['30000004', 'Laura Torres'], ['30000005', 'Miguel Herrera'], ['30000006', 'Sofía Castillo']]
      .forEach(([cedula, nombre], i) => {
        const e = usuario('Empleado', cedula, nombre, 'emp123', i < 3 ? obra : sabados);
        e.supervisor = i < 3 ? '20000001' : '20000002';
        e.asistencias.push({ fecha: ayer(), horaEntrada: '06:55', horaSalida: '16:02', tarde: false });
        if (i % 2 === 0) {
          e.asistencias.push({ fecha: hoy(), horaEntrada: i === 4 ? '07:25' : '06:58', horaSalida: null, tarde: i === 4 });
          e.presente = true;
        }
        usuarios.push(e);
      });
    return { empresa: 'ConstruTrack', usuarios };
  }

  // ---------- Reglas (equivalentes a las clases Java) ----------
  const buscar = (cedula) => datos.usuarios.find((u) => u.cedula === cedula) || null;
  const empleadosDe = (supervisor) => datos.usuarios.filter((u) => u.rol === 'Empleado' && u.supervisor === supervisor.cedula);
  const ultima = (u) => u.asistencias[u.asistencias.length - 1] || null;
  const asistenciaDeHoy = (u) => { const a = ultima(u); return a && a.fecha === hoy() ? a : null; };

  function validarCedula(cedula) {
    if (!/^\d{6,9}$/.test(cedula || '')) throw invalido('La cédula debe tener entre 6 y 9 dígitos, sin puntos ni letras.');
    return cedula;
  }
  function validarNombre(nombre) {
    const limpio = (nombre || '').trim().replace(/\s+/g, ' ');
    if (!/^[\p{L} .'-]{3,60}$/u.test(limpio)) throw invalido('El nombre debe tener entre 3 y 60 letras.');
    return limpio;
  }
  function validarClave(clave) {
    if (!/^[^|\s]{4,30}$/.test(clave || '')) throw invalido('La clave debe tener entre 4 y 30 caracteres, sin espacios.');
    return clave;
  }
  function crearHorario(dias, horaEntrada, horaSalida) {
    const elegidos = (dias || '').toUpperCase().split(',').map((d) => d.trim()).filter(Boolean);
    if (!elegidos.length) throw invalido('El horario debe tener al menos un día.');
    elegidos.forEach((d) => { if (!CODIGOS.includes(d)) throw invalido('Día no válido en el horario: ' + d); });
    if (!/^\d{2}:\d{2}$/.test(horaEntrada || '') || !/^\d{2}:\d{2}$/.test(horaSalida || '')) {
      throw invalido('Las horas deben tener el formato HH:mm.');
    }
    if (minutos(horaEntrada) >= minutos(horaSalida)) throw invalido('La hora de entrada debe ser anterior a la hora de salida.');
    return { dias: CODIGOS.filter((c) => elegidos.includes(c)).join(','), horaEntrada, horaSalida };
  }

  function marcarEntrada(u) {
    if (u.presente) throw conflicto(`${u.nombre} ya registró su entrada y sigue en la obra.`);
    const hora = horaActual();
    const registro = { fecha: hoy(), horaEntrada: hora, horaSalida: null, tarde: minutos(hora) > minutos(u.horario.horaEntrada) + TOLERANCIA_MINUTOS };
    u.asistencias.push(registro);
    u.presente = true;
    return registro;
  }

  function marcarSalida(u) {
    const registro = ultima(u);
    if (!u.presente || !registro || registro.horaSalida) throw conflicto(`${u.nombre} no tiene una entrada abierta.`);
    registro.horaSalida = horaActual();
    u.presente = false;
    return registro;
  }

  function verificarRolGestionable(rol) {
    if (rol !== 'Empleado' && rol !== 'Supervisor') {
      throw invalido('El jefe solo puede gestionar usuarios con rol Empleado o Supervisor.');
    }
  }
  function buscarGestionable(cedula) {
    const u = buscar(cedula);
    if (!u) throw invalido(`No existe un usuario con la cédula ${cedula}.`);
    verificarRolGestionable(u.rol);
    return u;
  }
  function validarSupervisor(cedula) {
    if (!cedula) return null;
    const s = buscar(cedula);
    if (!s || s.rol !== 'Supervisor') throw invalido(`La cédula ${cedula} no pertenece a un supervisor.`);
    return cedula;
  }

  function registrarUsuario(d) {
    verificarRolGestionable(d.rol);
    if (buscar(d.cedula)) throw conflicto(`Ya existe un usuario con la cédula ${d.cedula}.`);
    const horario = crearHorario(d.dias, d.horaEntrada, d.horaSalida);
    const nuevo = {
      rol: d.rol, cedula: validarCedula(d.cedula), nombre: validarNombre(d.nombre), clave: validarClave(d.clave),
      horario, presente: false, supervisor: null, asistencias: [],
    };
    if (nuevo.rol === 'Empleado') nuevo.supervisor = validarSupervisor(d.supervisor);
    datos.usuarios.push(nuevo);
    if (d.presente === true || d.presente === 'true') marcarEntrada(nuevo);
    return nuevo;
  }

  function modificarUsuario(cedula, d) {
    const u = buscarGestionable(cedula);
    const rol = d.rol ?? u.rol;
    verificarRolGestionable(rol);
    const nombre = 'nombre' in d ? validarNombre(d.nombre) : u.nombre;
    if (d.clave) validarClave(d.clave);
    const horario = crearHorario(d.dias ?? u.horario.dias, d.horaEntrada ?? u.horario.horaEntrada, d.horaSalida ?? u.horario.horaSalida);
    if (rol === 'Empleado' && d.supervisor && d.supervisor === u.cedula) throw invalido('Un empleado no puede ser su propio supervisor.');
    const supervisor = rol === 'Empleado' && 'supervisor' in d ? validarSupervisor(d.supervisor) : u.supervisor;

    u.nombre = nombre;
    if (d.clave) u.clave = d.clave;
    u.horario = horario;
    if (rol !== u.rol) {
      if (u.rol === 'Supervisor') empleadosDe(u).forEach((e) => { e.supervisor = null; });
      u.rol = rol;
    }
    u.supervisor = rol === 'Empleado' ? supervisor : null;
    if ('presente' in d) {
      const presente = d.presente === true || d.presente === 'true';
      if (presente && !u.presente) marcarEntrada(u);
      else if (!presente && u.presente) marcarSalida(u);
    }
    return u;
  }

  function eliminarUsuario(cedula) {
    const u = buscarGestionable(cedula);
    if (u.rol === 'Supervisor') empleadosDe(u).forEach((e) => { e.supervisor = null; });
    datos.usuarios = datos.usuarios.filter((x) => x !== u);
  }

  /** Misma forma que Usuario.toJson() en Java. */
  function aJson(u, detalle) {
    const json = {
      cedula: u.cedula, nombre: u.nombre, rol: u.rol, presente: u.presente,
      trabajaHoy: u.horario.dias.split(',').includes(codigoDeHoy()),
      horario: { ...u.horario }, asistenciaHoy: asistenciaDeHoy(u) ? { ...asistenciaDeHoy(u) } : null,
    };
    if (u.rol === 'Empleado') {
      const s = buscar(u.supervisor);
      json.supervisor = s ? { cedula: s.cedula, nombre: s.nombre } : null;
    }
    if (u.rol === 'Supervisor') json.empleadosACargo = empleadosDe(u).length;
    if (detalle) json.asistencias = u.asistencias.slice(-30).reverse().map((a) => ({ ...a }));
    return json;
  }

  // ---------- Rutas (las mismas de ControladorApi.java) ----------
  function enrutar(metodo, ruta, cuerpo, token) {
    if (ruta === '/api/estado' && metodo === 'GET') return { status: 'ok', servidor: 'Demo', empresa: datos.empresa };
    if (ruta === '/api/login' && metodo === 'POST') {
      const u = buscar(cuerpo.cedula);
      if (!u || u.clave !== cuerpo.clave) throw new ErrorApi(401, 'Cédula o clave incorrecta.');
      return { token: 'demo-' + u.cedula, usuario: aJson(u, true) };
    }

    const actual = token && token.startsWith('demo-') ? buscar(token.slice(5)) : null;
    if (!actual) throw new ErrorApi(401, 'Debes iniciar sesión.');

    if (ruta === '/api/logout' && metodo === 'POST') return { status: 'ok' };
    if (ruta === '/api/yo' && metodo === 'GET') return aJson(actual, true);
    if (ruta === '/api/asistencia/entrada' && metodo === 'POST') {
      const r = marcarEntrada(actual);
      return { mensaje: `Entrada registrada a las ${r.horaEntrada}${r.tarde ? ' (llegada tarde).' : '.'}`, usuario: aJson(actual, true) };
    }
    if (ruta === '/api/asistencia/salida' && metodo === 'POST') {
      const r = marcarSalida(actual);
      return { mensaje: `Salida registrada a las ${r.horaSalida}.`, usuario: aJson(actual, true) };
    }

    const soloJefe = () => {
      if (actual.rol !== 'Jefe') throw prohibido('Solo el Jefe puede registrar, modificar o eliminar usuarios.');
    };
    if (ruta === '/api/usuarios' && metodo === 'GET') {
      if (actual.rol === 'Jefe') return datos.usuarios.map((u) => aJson(u, false));
      if (actual.rol === 'Supervisor') return empleadosDe(actual).map((u) => aJson(u, false));
      throw prohibido('Tu rol no permite ver la lista de personal.');
    }
    if (ruta === '/api/usuarios' && metodo === 'POST') { soloJefe(); return aJson(registrarUsuario(cuerpo), false); }
    if (ruta.startsWith('/api/usuarios/')) {
      const cedula = ruta.slice('/api/usuarios/'.length);
      if (metodo === 'GET') {
        const u = buscar(cedula);
        if (!u) throw new ErrorApi(404, `No existe un usuario con la cédula ${cedula}.`);
        const permitido = actual.rol === 'Jefe' || u === actual || (actual.rol === 'Supervisor' && u.supervisor === actual.cedula);
        if (!permitido) throw prohibido('Solo puedes consultar a los empleados que supervisas.');
        return aJson(u, true);
      }
      if (metodo === 'PUT') { soloJefe(); return aJson(modificarUsuario(cedula, cuerpo), false); }
      if (metodo === 'DELETE') { soloJefe(); eliminarUsuario(cedula); return { status: 'ok' }; }
    }
    throw new ErrorApi(404, `Ruta no encontrada: ${metodo} ${ruta}`);
  }

  return {
    iniciar: cargar,

    /** Atiende una petición como lo haría el servidor Java. Devuelve {codigo, datos}. */
    atender(metodo, ruta, cuerpo, token) {
      if (!datos) cargar();
      try {
        const respuesta = enrutar(metodo, ruta, cuerpo || {}, token);
        guardar();
        return { codigo: 200, datos: JSON.parse(JSON.stringify(respuesta)) };
      } catch (e) {
        if (e instanceof ErrorApi) return { codigo: e.codigo, datos: { error: e.message } };
        console.error(e);
        return { codigo: 500, datos: { error: 'Error interno.' } };
      }
    },

    /** Borra los cambios hechos en el modo demostración y vuelve a los datos de prueba. */
    reiniciar() {
      datos = crearDatosIniciales();
      guardar();
    },
  };
})();
