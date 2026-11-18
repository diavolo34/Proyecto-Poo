const ILUSTRACION_IZQUIERDA = `<svg viewBox="0 0 400 420" aria-hidden="true">
  <rect x="0" y="250" width="400" height="170" fill="#1d7afc"/><rect x="0" y="300" width="400" height="120" fill="#0c66e4"/>
  <rect x="40" y="150" width="70" height="150" fill="#e9f2ff"/><rect x="130" y="90" width="80" height="210" fill="#fff"/>
  <rect x="230" y="170" width="60" height="130" fill="#e9f2ff"/>
  ${[0, 1, 2, 3, 4, 5].map((i) => `<rect x="146" y="${110 + i * 30}" width="18" height="14" fill="#85b8ff"/><rect x="176" y="${110 + i * 30}" width="18" height="14" fill="#85b8ff"/>`).join('')}
  <rect x="318" y="40" width="6" height="260" fill="#fca700"/><rect x="230" y="40" width="150" height="6" fill="#fca700"/>
  <rect x="250" y="46" width="2" height="60" fill="#fca700"/><rect x="242" y="104" width="18" height="14" fill="#fca700"/>
  <circle cx="70" cy="290" r="9" fill="#172b4d"/><rect x="62" y="298" width="16" height="30" rx="4" fill="#fca700"/>
</svg>`;

const ILUSTRACION_DERECHA = `<svg viewBox="0 0 400 420" aria-hidden="true">
  <rect x="0" y="260" width="400" height="160" fill="#1d7afc"/><rect x="0" y="310" width="400" height="110" fill="#0c66e4"/>
  <rect x="120" y="110" width="200" height="190" rx="6" fill="#fff" stroke="#dcdfe4"/>
  <rect x="136" y="128" width="50" height="8" rx="3" fill="#44546f"/>
  ${[0, 1, 2].map((c) => `<rect x="${136 + c * 60}" y="146" width="52" height="140" rx="4" fill="#f1f2f4"/>
    <rect x="${140 + c * 60}" y="${156}" width="44" height="30" rx="3" fill="#fff"/><rect x="${144 + c * 60}" y="${176}" width="8" height="6" fill="${['#0c66e4', '#6a9a23', '#fca700'][c]}"/>`).join('')}
  <circle cx="300" cy="120" r="38" fill="none" stroke="#0c66e4" stroke-width="10"/>
  <rect x="320" y="150" width="14" height="60" rx="6" transform="rotate(-40 327 180)" fill="#0c66e4"/>
  <rect x="285" y="112" width="30" height="12" rx="4" fill="#fca700"/>
</svg>`;

function marcoAcceso(contenido) {
  $('#raiz').innerHTML = `<div class="pagina-acceso">
    <div class="acceso-lado">${ILUSTRACION_IZQUIERDA}</div>
    <main class="acceso-caja" id="principal">
      <a class="marca" href="index.html">${LOGO} ConstruTrack</a>
      ${contenido}
      <div class="acceso-pie"><a href="index.html">Volver a la portada</a></div>
    </main>
    <div class="acceso-lado">${ILUSTRACION_DERECHA}</div>
  </div>`;
}

function mostrarErrorFormulario(formulario, mensaje) {
  const caja = formulario.querySelector('.mensaje-error');
  caja.textContent = mensaje;
  caja.classList.remove('oculto');
}

function pantallaLogin() {
  marcoAcceso(`
    <h1>Inicia sesión para continuar</h1>
    <p class="bajada">Usa tu correo o tu cédula y tu clave.</p>
    <form id="form-login" novalidate>
      <div class="campo"><label for="identificador">Correo o cédula *</label>
        <input id="identificador" name="identificador" autocomplete="username" required placeholder="tu@correo.com o 30000001"></div>
      <div class="campo"><label for="clave">Clave *</label>
        <input id="clave" name="clave" type="password" autocomplete="current-password" required></div>
      <p class="enlace-olvido"><a href="#recuperar">¿Olvidaste tu clave?</a></p>
      <div class="mensaje mensaje-error oculto" role="alert"></div>
      <button class="btn btn-primario btn-bloque" type="submit">Continuar</button>
    </form>
    <div class="separador">¿Primera vez en ConstruTrack?</div>
    <a class="btn btn-contorno btn-bloque" href="#registro">Crear una cuenta</a>
    ${Api.servidor.demo ? `<div class="demo-usuarios"><strong>Usuarios de prueba</strong> (clic para completar):<br>
      <button class="btn btn-enlace" data-demo="carlos.mendez@construtrack.demo|gerente123">Gerente</button>·
      <button class="btn btn-enlace" data-demo="20000001|super123">Supervisor</button>·
      <button class="btn btn-enlace" data-demo="30000001|emp123">Empleado</button>·
      <button class="btn btn-enlace" data-demo="rosa.gutierrez@correo.com|rosa1234">Sin empresa</button>
    </div>` : ''}`);
  const formulario = $('#form-login');
  document.querySelectorAll('[data-demo]').forEach((boton) => boton.addEventListener('click', () => {
    const [id, clave] = boton.dataset.demo.split('|');
    $('#identificador').value = id;
    $('#clave').value = clave;
    $('#clave').focus();
  }));
  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const { identificador, clave } = datosFormulario(formulario);
    if (!identificador.trim() || !clave) {
      mostrarErrorFormulario(formulario, 'Escribe tu correo o cédula y tu clave.');
      return;
    }
    const boton = formulario.querySelector('button[type=submit]');
    boton.disabled = true;
    boton.textContent = 'Entrando…';
    try {
      const sesion = await Api.iniciarSesion(identificador, clave);
      if (sesion.dosPasos) {
        pantallaDosPasos(sesion.desafio);
        return;
      }
      estado.usuario = sesion.usuario;
      estado.cuenta = sesion.cuenta;
      location.hash = sesion.pendiente ? 'unirse' : 'asistencia';
      enrutar();
    } catch (e) {
      mostrarErrorFormulario(formulario, e.message);
      $('#clave').select();
    } finally {
      boton.disabled = false;
      boton.textContent = 'Continuar';
    }
  });
  $('#identificador').focus();
}

let tokenRestablecer = null;

function pantallaDosPasos(desafio) {
  marcoAcceso(`
    <h1>Verificación en dos pasos</h1>
    <p class="bajada">Abre tu aplicación de autenticación (Google Authenticator, Microsoft Authenticator…) y escribe
      el código de 6 dígitos de ConstruTrack.</p>
    <form id="form-dos-pasos" novalidate>
      <div class="campo"><label for="codigo-dos-pasos">Código *</label>
        <input id="codigo-dos-pasos" name="codigo" class="codigo-grande" inputmode="numeric" autocomplete="one-time-code"
          maxlength="9" required placeholder="000000">
        <span class="ayuda">¿Perdiste el teléfono? Escribe uno de tus códigos de respaldo, por ejemplo ABCD-EF23.</span></div>
      <div class="mensaje mensaje-error oculto" role="alert"></div>
      <button class="btn btn-primario btn-bloque" type="submit">Verificar y entrar</button>
    </form>
    <button class="btn btn-enlace btn-bloque" id="volver-login" type="button">Volver a iniciar sesión</button>`);
  const formulario = $('#form-dos-pasos');
  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const codigo = $('#codigo-dos-pasos').value.trim();
    if (!codigo) {
      mostrarErrorFormulario(formulario, 'Escribe el código de tu aplicación.');
      return;
    }
    const boton = formulario.querySelector('button[type=submit]');
    boton.disabled = true;
    try {
      const sesion = await Api.completarDosPasos(desafio, codigo);
      estado.usuario = sesion.usuario;
      estado.cuenta = sesion.cuenta;
      location.hash = sesion.pendiente ? 'unirse' : 'asistencia';
      enrutar();
    } catch (e) {
      mostrarErrorFormulario(formulario, e.message);
      $('#codigo-dos-pasos').select();
      boton.disabled = false;
    }
  });
  $('#volver-login').addEventListener('click', pantallaLogin);
  $('#codigo-dos-pasos').focus();
}

function pantallaRecuperar() {
  marcoAcceso(`
    <h1>¿Olvidaste tu clave?</h1>
    <p class="bajada">Escribe el correo con el que te registraste y te enviaremos un enlace para elegir una clave nueva.</p>
    <form id="form-recuperar" novalidate>
      <div class="campo"><label for="correo-recuperar">Correo electrónico *</label>
        <input id="correo-recuperar" name="correo" type="email" autocomplete="email" required placeholder="tu@correo.com"></div>
      <div class="mensaje mensaje-error oculto" role="alert"></div>
      <button class="btn btn-primario btn-bloque" type="submit">Enviarme el enlace</button>
    </form>
    <p class="nota-acceso">Si tu gerente creó tu usuario solo con la cédula, sin correo, pídele que te asigne una clave nueva.</p>
    <div class="separador">¿La recordaste?</div>
    <a class="btn btn-contorno btn-bloque" href="#login">Volver a iniciar sesión</a>`);
  const formulario = $('#form-recuperar');
  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const correo = $('#correo-recuperar').value.trim();
    if (!correo.includes('@')) {
      mostrarErrorFormulario(formulario, 'Escribe el correo con el que te registraste.');
      return;
    }
    const boton = formulario.querySelector('button[type=submit]');
    boton.disabled = true;
    boton.textContent = 'Enviando…';
    try {
      const respuesta = await Api.olvideClave(correo);
      formulario.outerHTML = `<div class="mensaje mensaje-info" role="status">${esc(respuesta.mensaje)}
        El enlace vence en 30 minutos y sirve una sola vez.</div>`;
    } catch (e) {
      mostrarErrorFormulario(formulario, e.message);
      boton.disabled = false;
      boton.textContent = 'Enviarme el enlace';
    }
  });
  $('#correo-recuperar').focus();
}

function pantallaRestablecer(parametros) {
  if (parametros.get('token')) {
    tokenRestablecer = parametros.get('token');
    history.replaceState(null, '', '#restablecer');
  }
  if (!tokenRestablecer) {
    marcoAcceso(`
      <h1>Enlace no válido</h1>
      <p class="mensaje mensaje-error" role="alert">Este enlace para restablecer la clave no es válido o ya se usó.</p>
      <a class="btn btn-primario btn-bloque" href="#recuperar">Pedir un enlace nuevo</a>`);
    return;
  }
  marcoAcceso(`
    <h1>Elige una clave nueva</h1>
    <p class="bajada">Al guardarla, se cerrarán las sesiones abiertas con tu clave anterior.</p>
    <form id="form-restablecer" novalidate>
      <div class="campo"><label for="clave-nueva">Clave nueva *</label>
        <input id="clave-nueva" name="clave" type="password" autocomplete="new-password" required>
        <span class="ayuda">Mínimo 8 caracteres, con letras y números.</span></div>
      <div class="campo"><label for="clave-confirmacion">Repite la clave nueva *</label>
        <input id="clave-confirmacion" name="confirmacion" type="password" autocomplete="new-password" required></div>
      <div class="mensaje mensaje-error oculto" role="alert"></div>
      <button class="btn btn-primario btn-bloque" type="submit">Guardar clave nueva</button>
    </form>`);
  const formulario = $('#form-restablecer');
  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const { clave, confirmacion } = datosFormulario(formulario);
    if (clave !== confirmacion) {
      mostrarErrorFormulario(formulario, 'Las dos claves no coinciden. Escríbelas de nuevo.');
      return;
    }
    const boton = formulario.querySelector('button[type=submit]');
    boton.disabled = true;
    try {
      const respuesta = await Api.restablecerClave(tokenRestablecer, clave, confirmacion);
      tokenRestablecer = null;
      Api.guardarToken(null);
      estado.usuario = null;
      estado.cuenta = null;
      aviso(respuesta.mensaje);
      location.hash = 'login';
    } catch (e) {
      mostrarErrorFormulario(formulario, e.message);
      boton.disabled = false;
    }
  });
  $('#clave-nueva').focus();
}

function pantallaRegistro(parametros) {
  marcoAcceso(`
    <h1>Crea tu cuenta</h1>
    <p class="bajada">Elige cómo vas a usar ConstruTrack.</p>
    <form id="form-registro" novalidate>
      <fieldset class="opciones-rol">
        <legend class="solo-lector">¿Cómo usarás ConstruTrack?</legend>
        <label class="opcion-rol"><input type="radio" name="tipo" value="unirme" checked>
          <span><strong>Me uno a una empresa</strong><span class="pequeno suave">Mi gerente me dará un código de invitación (empleado o supervisor).</span></span></label>
        <label class="opcion-rol"><input type="radio" name="tipo" value="gerente">
          <span><strong>Soy gerente: crear mi empresa</strong><span class="pequeno suave">Creo la empresa e invito a mi equipo.</span></span></label>
      </fieldset>
      <div class="campo oculto" id="campo-empresa"><label for="r-empresa">Nombre de la empresa *</label>
        <input id="r-empresa" name="empresa" autocomplete="organization" maxlength="60" placeholder="Ej.: Constructora Los Andes">
        <span class="ayuda">Serás su Gerente: podrás crear obras e invitar a supervisores y empleados.</span></div>
      <div class="campo"><label for="correo">Correo electrónico *</label>
        <input id="correo" name="correo" type="email" autocomplete="email" required>
        <span class="ayuda">Lo usarás para iniciar sesión.</span></div>
      <div class="fila">
        <div class="campo"><label for="nombre">Nombre *</label><input id="nombre" name="nombre" autocomplete="given-name" required></div>
        <div class="campo"><label for="apellido">Apellido *</label><input id="apellido" name="apellido" autocomplete="family-name" required></div>
      </div>
      <div class="fila">
        <div class="campo"><label for="documento">Cédula o DNI *</label><input id="documento" name="documento" required inputmode="numeric" pattern="[0-9]{6,10}" data-solo-numeros>
          <span class="ayuda">Solo números, sin V, puntos ni guiones.</span></div>
        <div class="campo"><label for="telefono">Teléfono *</label><input id="telefono" name="telefono" type="tel" autocomplete="tel" required>
          <span class="ayuda">Ej.: +58 412 1234567</span></div>
      </div>
      <div class="fila">
        <div class="campo"><label for="clave-nueva">Clave *</label><input id="clave-nueva" name="clave" type="password" autocomplete="new-password" required>
          <span class="ayuda">Mínimo 8 caracteres, con letras y números.</span></div>
        <div class="campo"><label for="confirmacion">Repite la clave *</label><input id="confirmacion" name="confirmacion" type="password" autocomplete="new-password" required></div>
      </div>
      <div class="mensaje mensaje-error oculto" role="alert"></div>
      <button class="btn btn-primario btn-bloque" type="submit">Crear cuenta</button>
    </form>
    <div class="separador">¿Ya tienes cuenta?</div>
    <a class="btn btn-contorno btn-bloque" href="#login">Iniciar sesión</a>`);
  const formulario = $('#form-registro');
  $('#correo').value = parametros.get('correo') || '';
  const empresa = $('#r-empresa');
  const alternarTipo = () => {
    const esGerente = formulario.querySelector('input[name=tipo]:checked').value === 'gerente';
    $('#campo-empresa').classList.toggle('oculto', !esGerente);
    empresa.required = esGerente;
    formulario.querySelector('button[type=submit]').textContent = esGerente ? 'Crear cuenta y empresa' : 'Crear cuenta';
  };
  formulario.querySelectorAll('input[name=tipo]').forEach((r) => r.addEventListener('change', alternarTipo));
  if (parametros.get('tipo') === 'gerente') formulario.querySelector('input[value=gerente]').checked = true;
  alternarTipo();
  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();

    const faltan = [...formulario.querySelectorAll('[required]')].filter((c) => !c.value.trim());
    if (faltan.length) {
      const nombres = faltan.map((c) => formulario.querySelector(`label[for="${c.id}"]`).textContent.replace(' *', '').toLowerCase());
      mostrarErrorFormulario(formulario, 'Completa: ' + nombres.join(', ') + '.');
      faltan[0].focus();
      return;
    }
    const boton = formulario.querySelector('button[type=submit]');
    boton.disabled = true;
    try {
      const sesion = await Api.registrar(datosFormulario(formulario));
      estado.cuenta = sesion.cuenta;
      estado.usuario = sesion.usuario;
      if (sesion.pendiente) {
        aviso('Cuenta creada. Te enviamos un código a tu correo para confirmarlo.');
        location.hash = 'unirse';
      } else {
        aviso(`Tu empresa «${sesion.usuario.empresa}» está lista. Ahora invita a tu equipo.`);
        location.hash = 'invitaciones';
      }
      enrutar();
    } catch (e) {
      mostrarErrorFormulario(formulario, e.message);
    } finally {
      boton.disabled = false;
    }
  });
  (parametros.get('tipo') === 'gerente' ? empresa : parametros.get('correo') ? $('#nombre') : $('#correo')).focus();
}

function pantallaVerificarCorreo() {
  marcoAcceso(`
    <h1>Confirma tu correo</h1>
    <p class="bajada">Te enviamos un código de 6 dígitos a <strong>${esc(estado.cuenta.correo)}</strong>.
      Escríbelo para activar tu cuenta.</p>
    <form id="form-verificar" novalidate>
      <div class="campo"><label for="codigo-correo">Código de 6 dígitos *</label>
        <input id="codigo-correo" name="codigo" class="codigo-grande" inputmode="numeric" autocomplete="one-time-code"
          maxlength="6" required placeholder="000000"></div>
      <div class="mensaje mensaje-error oculto" role="alert"></div>
      <button class="btn btn-primario btn-bloque" type="submit">Confirmar correo</button>
    </form>
    <p class="nota-acceso">¿No te llegó? Revisa la carpeta de spam o
      <button class="btn btn-enlace" id="reenviar-codigo" type="button">pide otro código</button>.</p>
    <button class="btn btn-enlace btn-bloque" id="salir-verificar">Cerrar sesión</button>`);
  const formulario = $('#form-verificar');
  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const codigo = $('#codigo-correo').value.replace(/\s/g, '');
    if (!/^\d{6}$/.test(codigo)) {
      mostrarErrorFormulario(formulario, 'Escribe los 6 dígitos del código.');
      return;
    }
    const boton = formulario.querySelector('button[type=submit]');
    boton.disabled = true;
    try {
      const respuesta = await Api.confirmarCorreo(codigo);
      estado.cuenta = respuesta.cuenta;
      aviso(respuesta.mensaje);
      if (respuesta.usuario) {
        estado.usuario = respuesta.usuario;
        location.hash = respuesta.usuario.rol === 'Gerente' ? 'invitaciones' : 'asistencia';
      }
      enrutar();
    } catch (e) {
      mostrarErrorFormulario(formulario, e.message);
      boton.disabled = false;
    }
  });
  $('#reenviar-codigo').addEventListener('click', async () => {
    try {
      aviso((await Api.reenviarCodigo()).mensaje);
    } catch (e) {
      aviso(e.message, 'error');
    }
  });
  $('#salir-verificar').addEventListener('click', cerrarSesion);
  $('#codigo-correo').focus();
}

function pantallaUnirse() {
  marcoAcceso(`
    <h1>¡Hola, ${esc(estado.cuenta.nombre)}! Únete a tu empresa</h1>
    <p class="bajada">Tu cuenta ya está creada. Escribe el código de invitación que te dio tu gerente.
      Al unirte tendrás el rol, el supervisor y la obra que él definió.</p>
    <form id="form-unirse" novalidate>
      <div class="campo"><label for="codigo">Código de invitación *</label>
        <input id="codigo" name="codigo" class="codigo-grande" autocomplete="off" placeholder="CT-XXXX-XXXX" required>
        <span class="ayuda">Puedes escribirlo en minúsculas; los espacios se ignoran.</span></div>
      <div class="mensaje mensaje-error oculto" role="alert"></div>
      <button class="btn btn-primario btn-bloque" type="submit">Unirme a la empresa</button>
    </form>
    <p class="pequeno suave" style="margin-top:14px">¿No tienes código? Pídeselo a tu gerente.${Api.servidor.demo ? ' Para probar: <strong>CT-RQ7K-4M2P</strong>' : ''}</p>
    <div class="separador">¿Eres gerente?</div>
    <details class="crear-empresa">
      <summary class="btn btn-contorno btn-bloque">Crear mi empresa</summary>
      <form id="form-empresa" novalidate style="margin-top:12px">
        <div class="campo"><label for="u-empresa">Nombre de la empresa *</label>
          <input id="u-empresa" name="nombre" autocomplete="organization" maxlength="60" required placeholder="Ej.: Constructora Los Andes"></div>
        <div class="mensaje mensaje-error oculto" role="alert"></div>
        <button class="btn btn-primario btn-bloque" type="submit">Crear empresa y entrar como gerente</button>
      </form>
    </details>
    <button class="btn btn-enlace btn-bloque" id="salir-unirse">Cerrar sesión</button>`);
  const formEmpresa = $('#form-empresa');
  formEmpresa.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    try {
      const respuesta = await Api.crearEmpresa($('#u-empresa').value);
      estado.usuario = respuesta.usuario;
      aviso(respuesta.mensaje);
      location.hash = 'invitaciones';
      enrutar();
    } catch (e) {
      mostrarErrorFormulario(formEmpresa, e.message);
    }
  });
  const formulario = $('#form-unirse');
  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    try {
      const respuesta = await Api.unirse($('#codigo').value);
      estado.usuario = respuesta.usuario;
      aviso(respuesta.mensaje);
      location.hash = 'asistencia';
      enrutar();
    } catch (e) {
      mostrarErrorFormulario(formulario, e.message);
      $('#codigo').select();
    }
  });
  $('#salir-unirse').addEventListener('click', cerrarSesion);
  $('#codigo').focus();
}

function pantallaDemo() {
  const activa = Boolean(Api.servidor.demoPrivada);
  marcoAcceso(`
    <h1>Demostración privada</h1>
    <p class="bajada">Entra a una empresa de prueba con datos de ejemplo. Los cambios se borran cuando el servidor se reinicia.</p>
    ${activa ? `<form id="form-demo" novalidate>
      <div class="campo"><label for="clave-demo">Clave de demostración *</label>
        <input id="clave-demo" name="clave" type="password" autocomplete="off" required></div>
      <div class="campo"><label for="rol-demo">Entrar como</label>
        <select id="rol-demo" name="rol"><option>Gerente</option><option>Supervisor</option><option>Empleado</option></select></div>
      <div class="mensaje mensaje-error oculto" role="alert"></div>
      <button class="btn btn-primario btn-bloque" type="submit">Entrar a la demo</button>
    </form>` : '<p class="mensaje mensaje-info">La demostración privada no está activada en este servidor.</p>'}
    <div class="separador">¿Tienes una cuenta real?</div>
    <a class="btn btn-contorno btn-bloque" href="#login">Iniciar sesión</a>`);
  if (!activa) return;
  const formulario = $('#form-demo');
  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    if (!$('#clave-demo').value) {
      mostrarErrorFormulario(formulario, 'Escribe la clave de demostración.');
      return;
    }
    const boton = formulario.querySelector('button[type=submit]');
    boton.disabled = true;
    try {
      const sesion = await Api.entrarDemo($('#clave-demo').value, $('#rol-demo').value);
      estado.usuario = sesion.usuario;
      estado.cuenta = sesion.cuenta;
      aviso('Entraste a la demostración como ' + sesion.usuario.rol.toLowerCase() + '.');
      location.hash = 'asistencia';
      enrutar();
    } catch (e) {
      mostrarErrorFormulario(formulario, e.codigo === 401 ? 'Clave de demostración incorrecta.' : e.message);
      $('#clave-demo').select();
    } finally {
      boton.disabled = false;
    }
  });
  $('#clave-demo').focus();
}
