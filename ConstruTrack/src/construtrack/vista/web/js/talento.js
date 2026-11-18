async function vistaTalento() {
  const zona = contenido();
  const datos = await Api.talento();
  const filtro = (estado.filtroTalento || '').toLowerCase();
  const activos = datos.activos.filter((u) => !filtro || [u.nombre, u.cedula, u.puesto, u.area].join(' ').toLowerCase().includes(filtro));
  zona.innerHTML = encabezado('', 'Talento humano') + `
    <p class="suave" style="margin-top:-8px">Despidos y renuncias, con su voucher de pago, la carta de renuncia y la devolución de equipos.
      Quien egresa queda inactivo: no puede entrar ni aparece en el personal, pero su historial se conserva.</p>
    <section class="tarjeta" aria-labelledby="t-activos">
      <div class="titulo-vista" style="margin:0 0 8px"><h2 id="t-activos" style="margin:0">Personal activo</h2>
        <input id="buscar-talento" placeholder="Buscar persona…" value="${esc(estado.filtroTalento || '')}" style="width:220px" aria-label="Buscar persona"></div>
      <div class="tabla-contenedor"><table><thead><tr><th>Persona</th><th>Rol</th><th>Puesto y área</th><th>Ingreso</th><th><span class="solo-lector">Acciones</span></th></tr></thead>
        <tbody>${activos.map((u) => `<tr><td><strong>${esc(u.nombre)}</strong><br><span class="pequeno suave">${esc(u.cedula)}</span></td>
          <td>${chipRol(u.rol)}</td><td>${esc(u.puesto || '—')}<br><span class="pequeno suave">${esc(u.area || 'Sin área')}</span></td>
          <td>${u.fechaIngreso ? fecha(u.fechaIngreso) : '—'}</td>
          <td><button class="btn btn-secundario btn-chico" data-egreso="${esc(u.cedula)}">Registrar egreso</button></td></tr>`).join('')
          || '<tr><td colspan="5" class="vacio">No hay personas activas que coincidan.</td></tr>'}</tbody></table></div>
    </section>
    <section class="tarjeta" style="margin-top:16px" aria-labelledby="t-egresos"><h2 id="t-egresos">Egresos</h2>
      <div class="tabla-contenedor"><table><thead><tr><th>Persona</th><th>Egreso</th><th>Equipos</th><th>Documentos</th><th>Ficha</th><th><span class="solo-lector">Acciones</span></th></tr></thead>
        <tbody>${datos.egresos.map((e) => `<tr>
          <td><strong>${esc(e.nombre)}</strong><br><span class="pequeno suave">${esc(e.cedula)} · ${esc(e.puesto || e.rol)}${e.area ? ' · ' + esc(e.area) : ''}</span></td>
          <td>${chip(e.tipoNombre, e.tipo === 'DESPIDO' ? 'rojo' : 'azul')} ${fecha(e.fecha)}<br><span class="pequeno">${esc(e.motivo)}</span>
            <br><span class="pequeno suave">Registró ${esc(e.registradoPor)}</span></td>
          <td>${e.devolvioEquipos ? chip('Sí los devolvió', 'verde') : chip('No los devolvió', 'rojo')}
            ${e.notaEquipos ? `<br><span class="pequeno">${esc(e.notaEquipos)}</span>` : ''}</td>
          <td>${Adjuntos.boton(e.voucher, 'Voucher de pago')}${e.carta ? '<br>' + Adjuntos.boton(e.carta, 'Carta de renuncia') : ''}</td>
          <td>${e.fichaEliminada ? chip('Eliminada', 'gris') + `<br><span class="pequeno suave">${fecha(e.fechaEliminacion)}</span>` : chip('Inactiva', 'amarillo')}</td>
          <td style="white-space:nowrap"><button class="btn btn-enlace btn-chico" data-expediente="${e.id}">Expediente</button>
            ${e.fichaEliminada ? '' : `<button class="btn btn-enlace btn-chico" style="color:var(--rojo)" data-eliminar-ficha="${e.id}" data-nombre="${esc(e.nombre)}">Eliminar ficha</button>`}</td>
        </tr>`).join('') || '<tr><td colspan="6" class="vacio">Aún no hay egresos registrados.</td></tr>'}</tbody></table></div>
    </section>`;
  const buscador = $('#buscar-talento');
  buscador.addEventListener('input', () => {
    estado.filtroTalento = buscador.value;
    vistaTalento().then(() => { const b = $('#buscar-talento'); b.focus(); b.setSelectionRange(b.value.length, b.value.length); });
  });
  zona.querySelectorAll('[data-egreso]').forEach((b) => b.addEventListener('click', () =>
    formularioEgreso(datos.activos.find((u) => u.cedula === b.dataset.egreso))));
  zona.querySelectorAll('[data-expediente]').forEach((b) => b.addEventListener('click', async () => {
    try {
      aviso('Expediente descargado: ' + await Api.descargarExpediente(b.dataset.expediente));
    } catch (e) {
      aviso(e.message, 'error');
    }
  }));
  zona.querySelectorAll('[data-eliminar-ficha]').forEach((b) => b.addEventListener('click', async () => {
    const seguro = await confirmar('Eliminar ficha', `Se eliminará la ficha de <strong>${esc(b.dataset.nombre)}</strong> de la empresa:
      su cédula y su cuenta quedarán libres. <br><br>Sus datos quedan registrados: el egreso, los documentos y un expediente con su
      asistencia, permisos y reportes se conservan aquí en Talento humano.`, 'Eliminar ficha', true);
    if (!seguro) return;
    try {
      await Api.eliminarFicha(b.dataset.eliminarFicha);
      aviso('Ficha eliminada. El expediente se conserva.');
      await vistaTalento();
    } catch (e) {
      aviso(e.message, 'error');
    }
  }));
}

async function formularioEgreso(persona) {
  let pendientes;
  try {
    pendientes = (await Api.pendientesEgreso(persona.cedula)).herramientas;
  } catch (e) {
    aviso(e.message, 'error');
    return;
  }
  const hoy = new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10);
  const modal = Modal.abrir('Egreso de ' + persona.nombre, `<form novalidate>
    <div class="campo"><span class="etiqueta">Tipo de egreso *</span><div class="casillas">
      <label><input type="radio" name="tipo" value="DESPIDO" checked> Despido</label>
      <label><input type="radio" name="tipo" value="RENUNCIA"> Renuncia</label></div></div>
    <div class="campo"><label for="e-fecha">Fecha de egreso *</label><input id="e-fecha" name="fecha" type="date" value="${hoy}"></div>
    <div class="campo"><label for="e-motivo">Motivo *</label><textarea id="e-motivo" name="motivo" maxlength="500"></textarea></div>
    ${Adjuntos.campo('e-voucher', 'Voucher de pago de la liquidación', true)}
    <div id="campo-carta" class="oculto">${Adjuntos.campo('e-carta', 'Carta de renuncia firmada', true)}</div>
    <fieldset class="grupo-campos"><legend>Devolución de equipos y herramientas</legend>
      ${pendientes.length ? `<p class="pequeno">Tiene a su cargo según el inventario:</p><ul class="lista-simple">${pendientes.map((h) =>
        `<li><span><strong>${esc(h.nombreArticulo)}</strong>${h.cantidad > 1 ? ' × ' + h.cantidad : ''} <span class="pequeno suave">${esc(h.articulo)}</span></span></li>`).join('')}</ul>`
        : '<p class="pequeno suave">No tiene herramientas registradas a su cargo en el inventario.</p>'}
      <div class="casillas">
        <label><input type="radio" name="devolvioEquipos" value="true" ${pendientes.length ? '' : 'checked'}> Sí los devolvió</label>
        <label><input type="radio" name="devolvioEquipos" value="false"> No los devolvió</label></div>
      <div class="campo" style="margin-top:8px"><label for="e-nota">Nota de lo que falta</label>
        <textarea id="e-nota" name="notaEquipos" maxlength="300" placeholder="Obligatoria si no los devolvió"></textarea>
        <span class="ayuda">«Sí» registra la devolución en buen estado de todo lo pendiente. «No» los deja pendientes a su nombre.</span></div>
    </fieldset>
    <div class="mensaje mensaje-error oculto" role="alert"></div>
    <div class="acciones"><button type="button" class="btn btn-secundario" data-cerrar>Cancelar</button>
      <button class="btn btn-peligro" type="submit">Registrar egreso</button></div></form>`);
  const formulario = modal.querySelector('form');
  const alternarCarta = () => {
    const renuncia = formulario.querySelector('input[name=tipo]:checked').value === 'RENUNCIA';
    $('#campo-carta').classList.toggle('oculto', !renuncia);
  };
  formulario.querySelectorAll('input[name=tipo]').forEach((r) => r.addEventListener('change', alternarCarta));
  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const boton = formulario.querySelector('button[type=submit]');
    boton.disabled = true;
    try {
      const datos = datosFormulario(formulario);
      if (!datos.devolvioEquipos) throw new Error('Indica si devolvió los equipos y herramientas.');
      const voucher = $('#e-voucher').files[0];
      if (!voucher) throw new Error('Adjunta el voucher de pago.');
      const renuncia = datos.tipo === 'RENUNCIA';
      const carta = $('#e-carta').files[0];
      if (renuncia && !carta) throw new Error('Adjunta la carta de renuncia.');
      boton.textContent = 'Subiendo documentos…';
      datos.voucher = (await Adjuntos.subir(voucher, persona.cedula, true)).id;
      if (renuncia) datos.carta = (await Adjuntos.subir(carta, persona.cedula, true)).id;
      await Api.registrarEgreso(persona.cedula, datos);
      Modal.cerrar();
      aviso(`Egreso de ${persona.nombre} registrado. Su acceso quedó desactivado.`);
      await vistaTalento();
    } catch (e) {
      mostrarErrorFormulario(formulario, e.message);
      boton.disabled = false;
      boton.textContent = 'Registrar egreso';
    }
  });
}

async function vistaCatalogo() {
  const zona = contenido();
  const catalogo = await Api.catalogo();
  const bloque = (tipo, titulo, lista, ejemplo) => `<section class="tarjeta" aria-labelledby="t-${tipo}"><h2 id="t-${tipo}">${titulo}</h2>
      <ul class="lista-simple">${lista.map((x) => `<li><span>${esc(x)}</span>
        <button class="btn btn-enlace btn-chico" style="color:var(--rojo)" data-quitar="${esc(x)}" data-tipo="${tipo}" aria-label="Quitar ${esc(x)}">Quitar</button></li>`).join('')
        || '<li class="suave">Vacío.</li>'}</ul>
      <form class="fila-agregar" data-agregar="${tipo}" novalidate>
        <label class="solo-lector" for="nuevo-${tipo}">Nuevo</label>
        <input id="nuevo-${tipo}" name="nombre" placeholder="Ej.: ${ejemplo}" maxlength="60">
        <button class="btn btn-primario" type="submit">Agregar</button></form></section>`;
  zona.innerHTML = encabezado('', 'Puestos y áreas') + `
    <p class="suave" style="margin-top:-8px">El rol (empleado, supervisor o gerente) define los permisos en el sistema; el puesto y el área
      describen el cargo y el departamento de cada persona. Se eligen al registrar o invitar a alguien.</p>
    <div class="rejilla-2">${bloque('puestos', 'Puestos', catalogo.puestos, 'Soldador')}${bloque('areas', 'Áreas', catalogo.areas, 'Seguridad industrial')}</div>`;
  zona.querySelectorAll('[data-agregar]').forEach((f) => f.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    try {
      await Api.agregarCatalogo(f.dataset.agregar, f.querySelector('input').value);
      aviso('Agregado al catálogo.');
      await vistaCatalogo();
    } catch (e) {
      aviso(e.message, 'error');
    }
  }));
  zona.querySelectorAll('[data-quitar]').forEach((b) => b.addEventListener('click', async () => {
    try {
      await Api.quitarCatalogo(b.dataset.tipo, b.dataset.quitar);
      aviso(`«${b.dataset.quitar}» se quitó del catálogo.`);
      await vistaCatalogo();
    } catch (e) {
      aviso(e.message, 'error');
    }
  }));
}
