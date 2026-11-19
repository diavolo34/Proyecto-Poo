async function vistaAreas() {
  const zona = contenido();
  const datos = await Api.areas();
  const gerente = esGerente();
  const opcionesArea = datos.areas.map((a) => `<option value="${esc(a.nombre)}">${esc(a.nombre)}${a.supervisor ? ' · ' + esc(a.supervisor.nombre) : ' · sin supervisor'}</option>`).join('');
  const opcionesSupervisor = (elegido) => datos.supervisores.map((s) =>
    `<option value="${esc(s.cedula)}" ${s.cedula === elegido ? 'selected' : ''}>${esc(s.nombre)}</option>`).join('');
  const integrante = (u, a) => {
    const responsable = a.supervisor && a.supervisor.cedula === u.cedula;
    return `<li><span><strong>${esc(u.nombre)}</strong> ${chipRol(u.rol)}
      <span class="pequeno suave">${esc(u.puesto || 'Sin puesto')}</span></span>
      ${responsable ? '<span class="pequeno suave">Responsable del área</span>' : gerente ? `<span class="acciones-integrante">
        <label class="solo-lector" for="mover-${esc(u.cedula)}">Mover a ${esc(u.nombre)}</label>
        <select id="mover-${esc(u.cedula)}" data-mover="${esc(u.cedula)}" style="width:auto"><option value="">Mover a…</option>
          ${datos.areas.filter((otra) => otra.nombre !== a.nombre).map((otra) => `<option value="${esc(otra.nombre)}">${esc(otra.nombre)}</option>`).join('')}</select>
        <button class="btn btn-enlace btn-chico" style="color:var(--rojo)" data-sacar="${esc(u.cedula)}" data-nombre="${esc(u.nombre)}">Sacar</button></span>` : ''}</li>`;
  };
  if (!zona.isConnected) return;
  zona.innerHTML = encabezado('', 'Áreas', gerente ? '<button class="btn btn-primario" id="nueva-area">+ Nueva área</button>' : '') + `
    <p class="suave" style="margin-top:-8px">${gerente
      ? 'Cada área tiene un supervisor responsable. Al incluir a un empleado en un área, queda a cargo de su supervisor; si cambias el supervisor del área, cambia para todos sus integrantes.'
      : 'Estas son tus áreas. Puedes sumar a ellas a las personas que todavía no tienen área; para moverlas o sacarlas, habla con el gerente.'}</p>
    <div class="rejilla-obras">${datos.areas.map((a) => `<section class="tarjeta tarjeta-area" aria-labelledby="area-${esc(a.nombre)}">
        <div class="titulo-vista" style="margin:0 0 6px"><h2 id="area-${esc(a.nombre)}" style="margin:0">${esc(a.nombre)}</h2>
          ${chip(a.integrantes.length + ' integrante(s)', 'gris')}</div>
        <p class="area-supervisor">${a.supervisor ? `Supervisor: <strong>${esc(a.supervisor.nombre)}</strong>`
          : '<span class="texto-rojo">Sin supervisor: asígnale uno</span>'}</p>
        ${gerente ? `<div class="fila-agregar"><label class="solo-lector" for="sup-${esc(a.nombre)}">Supervisor de ${esc(a.nombre)}</label>
          <select id="sup-${esc(a.nombre)}" data-supervisor-area="${esc(a.nombre)}">
            ${a.supervisor ? '' : '<option value="">Elegir supervisor…</option>'}${opcionesSupervisor(a.supervisor && a.supervisor.cedula)}</select></div>` : ''}
        <ul class="lista-simple">${a.integrantes.map((u) => integrante(u, a)).join('') || '<li class="suave">Nadie en esta área todavía.</li>'}</ul>
        ${gerente ? `<div class="acciones" style="justify-content:flex-start">
          <button class="btn btn-enlace btn-chico" style="color:var(--rojo)" data-eliminar-area="${esc(a.nombre)}" data-cuantos="${a.integrantes.length}">Eliminar área</button></div>` : ''}
      </section>`).join('') || `<p class="suave">${gerente ? 'Aún no hay áreas. Crea la primera, por ejemplo «Plomería», «Electricidad» o «Transporte», y elige su supervisor.' : 'Todavía no tienes áreas a tu cargo.'}</p>`}</div>
    <section class="tarjeta" style="margin-top:16px" aria-labelledby="t-sin-area"><h2 id="t-sin-area">Personas sin área (${datos.sinArea.length})</h2>
      ${datos.sinArea.length && datos.areas.length ? `<ul class="lista-simple">${datos.sinArea.map((u) => `<li><span><strong>${esc(u.nombre)}</strong> ${chipRol(u.rol)}
          <span class="pequeno suave">${esc(u.puesto || 'Sin puesto')}</span></span>
          <span><label class="solo-lector" for="incluir-${esc(u.cedula)}">Incluir a ${esc(u.nombre)}</label>
          <select id="incluir-${esc(u.cedula)}" data-incluir="${esc(u.cedula)}" style="width:auto"><option value="">Incluir en…</option>${opcionesArea}</select></span></li>`).join('')}</ul>`
        : `<p class="suave">${datos.sinArea.length ? 'Primero hace falta un área para incluirlas.' : 'Todas las personas tienen área.'}</p>`}</section>`;
  const intentar = (accion) => async (evento) => {
    try {
      await accion(evento);
    } catch (e) {
      aviso(e.message, 'error');
      await vistaAreas();
    }
  };
  const nueva = zona.querySelector('#nueva-area');
  if (nueva) nueva.addEventListener('click', () => formularioArea(datos));
  zona.querySelectorAll('[data-incluir]').forEach((s) => s.addEventListener('change', intentar(async () => {
    if (!s.value) return;
    const persona = await Api.incluirEnArea(s.dataset.incluir, s.value);
    aviso(`${persona.nombre} ahora está en «${persona.area}».`);
    await vistaAreas();
  })));
  zona.querySelectorAll('[data-mover]').forEach((s) => s.addEventListener('change', intentar(async () => {
    if (!s.value) return;
    const persona = await Api.incluirEnArea(s.dataset.mover, s.value);
    aviso(`${persona.nombre} pasó a «${persona.area}».`);
    await vistaAreas();
  })));
  zona.querySelectorAll('[data-sacar]').forEach((b) => b.addEventListener('click', intentar(async () => {
    await Api.sacarDeArea(b.dataset.sacar);
    aviso(`${b.dataset.nombre} quedó sin área.`);
    await vistaAreas();
  })));
  zona.querySelectorAll('[data-supervisor-area]').forEach((s) => s.addEventListener('change', intentar(async () => {
    if (!s.value) return;
    await Api.cambiarSupervisorArea(s.dataset.supervisorArea, s.value);
    aviso(`Nuevo supervisor de «${s.dataset.supervisorArea}». Sus empleados pasan a su cargo.`);
    await vistaAreas();
  })));
  zona.querySelectorAll('[data-eliminar-area]').forEach((b) => b.addEventListener('click', intentar(async () => {
    const cuantos = Number(b.dataset.cuantos);
    if (!await confirmar('Eliminar área', `¿Eliminar el área «${esc(b.dataset.eliminarArea)}»?${cuantos
      ? ` Sus ${cuantos} integrante(s) quedarán sin área y sin supervisor hasta que los incluyas en otra.` : ''}`, 'Eliminar área', true)) return;
    await Api.eliminarArea(b.dataset.eliminarArea);
    aviso(`Área «${b.dataset.eliminarArea}» eliminada.`);
    await vistaAreas();
  })));
}

function formularioArea(datos) {
  if (!datos.supervisores.length) {
    aviso('Primero registra a un supervisor: cada área necesita uno.', 'error');
    return;
  }
  const modal = Modal.abrir('Nueva área', `<form novalidate>
    <div class="campo"><label for="ar-nombre">Nombre del área *</label>
      <input id="ar-nombre" name="nombre" maxlength="60" placeholder="Ej.: Plomería, Electricidad, Transporte"></div>
    <div class="campo"><label for="ar-supervisor">Supervisor responsable *</label><select id="ar-supervisor" name="supervisor">
      ${datos.supervisores.map((s) => `<option value="${esc(s.cedula)}">${esc(s.nombre)}</option>`).join('')}</select>
      <span class="ayuda">Los empleados que incluyas en esta área quedarán a su cargo.</span></div>
    <div class="mensaje mensaje-error oculto" role="alert"></div>
    <div class="acciones"><button type="button" class="btn btn-secundario" data-cerrar>Cancelar</button>
      <button class="btn btn-primario" type="submit">Crear área</button></div></form>`);
  const formulario = modal.querySelector('form');
  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    try {
      const area = await Api.crearArea(modal.querySelector('#ar-nombre').value, modal.querySelector('#ar-supervisor').value);
      Modal.cerrar();
      aviso(`Área «${area.nombre}» creada.`);
      await vistaAreas();
    } catch (e) {
      mostrarErrorFormulario(formulario, e.message);
    }
  });
}
