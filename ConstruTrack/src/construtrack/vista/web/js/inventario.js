const CATEGORIAS_INVENTARIO = [['HERRAMIENTA', 'Herramienta'], ['EQUIPO', 'Equipo'], ['MATERIAL', 'Material']];

async function vistaInventario() {
  const zona = contenido();
  const [articulos, personas, obras] = await Promise.all([Api.inventario(), Api.listarUsuarios(), Api.obras()]);
  const filtro = (estado.filtroInventario || '').toLowerCase();
  const categoria = estado.categoriaInventario || '';
  const visibles = articulos.filter((a) => (!categoria || a.categoria === categoria)
    && (!filtro || [a.codigo, a.nombre, a.marca, a.serie].join(' ').toLowerCase().includes(filtro)));
  const entregas = articulos.flatMap((a) => a.entregas).filter((e) => e.abierta)
    .sort((x, y) => Number(y.vencida) - Number(x.vencida) || String(y.fecha).localeCompare(String(x.fecha)));
  const unidadesFuera = entregas.reduce((total, e) => total + e.cantidad, 0);
  const vencidas = entregas.filter((e) => e.vencida).length;
  const enTaller = articulos.filter((a) => a.estado === 'EN_MANTENIMIENTO').length;
  if (!zona.isConnected) return;
  zona.innerHTML = encabezado('', 'Inventario', '<button class="btn btn-primario" id="nuevo-articulo">+ Agregar al inventario</button>') + `
    <p class="suave" style="margin-top:-8px">Carga tus herramientas, equipos y materiales, entrégalos a cada supervisor o empleado y registra
      la devolución con su estado. Cada movimiento queda en el historial del artículo.</p>
    <div class="cifras">
      <div class="cifra"><div class="descripcion">Artículos</div><div class="valor">${articulos.length}</div></div>
      <div class="cifra"><div class="descripcion">Unidades entregadas</div><div class="valor">${unidadesFuera}</div></div>
      <div class="cifra"><div class="descripcion">Devoluciones vencidas</div><div class="valor${vencidas ? ' texto-rojo' : ''}">${vencidas}</div></div>
      <div class="cifra"><div class="descripcion">En mantenimiento</div><div class="valor">${enTaller}</div></div>
    </div>
    <section class="tarjeta" style="margin-top:16px" aria-labelledby="t-entregas"><h2 id="t-entregas">Entregas activas</h2>
      ${entregas.length ? `<div class="tabla-contenedor"><table><thead><tr><th>Artículo</th><th>Tiene</th><th>Cantidad</th><th>Obra</th>
        <th>Entregado</th><th>Devolución</th><th><span class="solo-lector">Acciones</span></th></tr></thead><tbody>
        ${entregas.map((e) => `<tr><td><strong>${esc(e.nombreArticulo)}</strong><br><span class="pequeno suave">${esc(e.articulo)} · ${esc(e.categoria)}</span></td>
          <td>${esc(e.persona)}</td><td>${e.cantidad}</td><td>${esc(e.obra || '—')}</td>
          <td>${fecha(e.fecha)}<br><span class="pequeno suave">por ${esc(e.entregadoPor)}</span></td>
          <td>${e.devolucionPrevista ? chip((e.vencida ? 'Vencida · ' : '') + fecha(e.devolucionPrevista), e.vencida ? 'rojo' : 'gris') : '—'}</td>
          <td><button class="btn btn-secundario btn-chico" data-devolver="${e.id}">Registrar devolución</button></td></tr>`).join('')}
        </tbody></table></div>` : '<p class="suave">No hay nada entregado ahora mismo.</p>'}</section>
    <section class="tarjeta" style="margin-top:16px" aria-labelledby="t-articulos">
      <div class="titulo-vista" style="margin:0 0 8px"><h2 id="t-articulos" style="margin:0">Artículos</h2>
        <div class="acciones" style="margin:0">
          <select id="categoria-inventario" style="width:auto" aria-label="Categoría"><option value="">Todas las categorías</option>
            ${CATEGORIAS_INVENTARIO.map(([v, t]) => `<option value="${v}" ${v === categoria ? 'selected' : ''}>${t}</option>`).join('')}</select>
          <input id="buscar-inventario" placeholder="Buscar código, nombre, serie…" value="${esc(estado.filtroInventario || '')}" style="width:220px" aria-label="Buscar"></div></div>
      <div class="tabla-contenedor"><table><thead><tr><th>Código</th><th>Artículo</th><th>Categoría</th><th>Disponibles</th><th>Estado</th><th><span class="solo-lector">Acciones</span></th></tr></thead>
        <tbody>${visibles.map((a) => `<tr><td><strong class="codigo">${esc(a.codigo)}</strong></td>
          <td><strong>${esc(a.nombre)}</strong>${a.marca || a.serie ? `<br><span class="pequeno suave">${esc([a.marca, a.serie ? 'S/N ' + a.serie : ''].filter(Boolean).join(' · '))}</span>` : ''}
            ${a.nota ? `<br><span class="pequeno suave">${esc(a.nota)}</span>` : ''}</td>
          <td>${esc(a.categoriaNombre)}</td>
          <td><strong>${a.disponibles}</strong> / ${a.cantidad}${a.enUso ? `<br><span class="pequeno suave">${a.enUso} entregada(s)</span>` : ''}</td>
          <td>${chipEstado(a.estadoNombre)}</td>
          <td style="white-space:nowrap">${a.disponibles > 0 ? `<button class="btn btn-primario btn-chico" data-entregar="${esc(a.codigo)}">Entregar</button>` : ''}
            <button class="btn btn-enlace btn-chico" data-historial-articulo="${esc(a.codigo)}">Historial</button>
            <button class="btn btn-enlace btn-chico" data-editar-articulo="${esc(a.codigo)}">Editar</button></td></tr>`).join('')
          || '<tr><td colspan="6" class="vacio">No hay artículos que coincidan. Agrega el primero con «+ Agregar al inventario».</td></tr>'}</tbody></table></div>
    </section>`;
  const porCodigo = (codigo) => articulos.find((a) => a.codigo === codigo);
  $('#nuevo-articulo').addEventListener('click', () => formularioArticulo(null));
  const buscador = $('#buscar-inventario');
  buscador.addEventListener('input', () => {
    estado.filtroInventario = buscador.value;
    vistaInventario().then(() => { const b = $('#buscar-inventario'); b.focus(); b.setSelectionRange(b.value.length, b.value.length); });
  });
  $('#categoria-inventario').addEventListener('change', (e) => { estado.categoriaInventario = e.target.value; vistaInventario(); });
  zona.querySelectorAll('[data-editar-articulo]').forEach((b) => b.addEventListener('click', () => formularioArticulo(porCodigo(b.dataset.editarArticulo))));
  zona.querySelectorAll('[data-entregar]').forEach((b) => b.addEventListener('click', () => formularioEntrega(porCodigo(b.dataset.entregar), personas, obras)));
  zona.querySelectorAll('[data-historial-articulo]').forEach((b) => b.addEventListener('click', () => historialArticulo(porCodigo(b.dataset.historialArticulo))));
  zona.querySelectorAll('[data-devolver]').forEach((b) => b.addEventListener('click', () =>
    formularioDevolucion(entregas.find((e) => e.id === Number(b.dataset.devolver)))));
}

function formularioArticulo(a) {
  const modal = Modal.abrir(a ? 'Editar ' + a.codigo : 'Agregar al inventario', `<form novalidate>
    <div class="fila">
      <div class="campo"><label for="a-categoria">Categoría *</label><select id="a-categoria" name="categoria" ${a ? 'disabled' : ''}>
        ${CATEGORIAS_INVENTARIO.map(([v, t]) => `<option value="${v}" ${a && a.categoria === v ? 'selected' : ''}>${t}</option>`).join('')}</select></div>
      <div class="campo"><label for="a-cantidad">Cantidad *</label><input id="a-cantidad" name="cantidad" type="number" min="1" value="${a ? a.cantidad : 1}"></div>
    </div>
    <div class="campo"><label for="a-nombre">Nombre *</label><input id="a-nombre" name="nombre" maxlength="80" value="${esc(a ? a.nombre : '')}" placeholder="Ej.: Taladro percutor"></div>
    <div class="fila">
      <div class="campo"><label for="a-marca">Marca y modelo</label><input id="a-marca" name="marca" maxlength="60" value="${esc(a ? a.marca : '')}" placeholder="Ej.: Bosch GSB 13 RE"></div>
      <div class="campo"><label for="a-serie">N.º de serie</label><input id="a-serie" name="serie" maxlength="40" value="${esc(a ? a.serie : '')}">
        <span class="ayuda">Con serie, la cantidad es 1.</span></div>
    </div>
    ${a ? `<div class="campo"><label for="a-estado">Estado</label><select id="a-estado" name="estado">
      ${[['OPERATIVO', 'Operativo'], ['EN_MANTENIMIENTO', 'En mantenimiento'], ['DE_BAJA', 'De baja']].map(([v, t]) =>
        `<option value="${v}" ${a.estado === v ? 'selected' : ''}>${t}</option>`).join('')}</select></div>` : ''}
    <div class="campo"><label for="a-nota">Nota</label><input id="a-nota" name="nota" maxlength="300" value="${esc(a ? a.nota : '')}" placeholder="Ej.: sacos de 42,5 kg"></div>
    <div class="mensaje mensaje-error oculto" role="alert"></div>
    <div class="acciones">${a && !a.entregas.length ? '<button type="button" class="btn btn-enlace" style="color:var(--rojo);margin-right:auto" id="a-eliminar">Eliminar</button>' : ''}
      <button type="button" class="btn btn-secundario" data-cerrar>Cancelar</button>
      <button class="btn btn-primario" type="submit">Guardar</button></div></form>`);
  const formulario = modal.querySelector('form');
  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    try {
      const datos = datosFormulario(formulario);
      const guardado = a ? await Api.editarArticulo(a.codigo, datos) : await Api.crearArticulo(datos);
      Modal.cerrar();
      aviso(`${guardado.codigo} · ${guardado.nombre} ${a ? 'actualizado' : 'agregado al inventario'}.`);
      await vistaInventario();
    } catch (e) {
      mostrarErrorFormulario(formulario, e.message);
    }
  });
  const eliminar = modal.querySelector('#a-eliminar');
  if (eliminar) {
    eliminar.addEventListener('click', async () => {
      try {
        await Api.eliminarArticulo(a.codigo);
        Modal.cerrar();
        aviso(a.codigo + ' eliminado del inventario.');
        await vistaInventario();
      } catch (e) {
        mostrarErrorFormulario(formulario, e.message);
      }
    });
  }
}

function formularioEntrega(a, personas, obras) {
  const modal = Modal.abrir('Entregar ' + a.nombre, `<form novalidate>
    <p class="suave">${esc(a.codigo)} · disponibles: <strong>${a.disponibles}</strong> de ${a.cantidad}</p>
    <div class="campo"><label for="g-persona">Entregar a *</label><select id="g-persona" name="cedula">
      ${personas.map((p) => `<option value="${esc(p.cedula)}">${esc(p.nombre)} · ${esc(p.puesto || p.rol)}</option>`).join('')}</select></div>
    <div class="fila">
      <div class="campo"><label for="g-cantidad">Cantidad *</label><input id="g-cantidad" name="cantidad" type="number" min="1" max="${a.disponibles}" value="1" ${a.disponibles === 1 ? 'readonly' : ''}></div>
      <div class="campo"><label for="g-prevista">Devolver el</label><input id="g-prevista" name="devolucionPrevista" type="date">
        <span class="ayuda">Opcional. Si pasa la fecha, el gerente recibe una alerta.</span></div>
    </div>
    <div class="campo"><label for="g-obra">Obra</label><select id="g-obra" name="obra"><option value="">— La obra de la persona —</option>
      ${obras.filter((o) => o.estado !== 'CONCLUIDA').map((o) => `<option value="${esc(o.codigo)}">${esc(o.nombre)}</option>`).join('')}</select></div>
    <div class="campo"><label for="g-nota">Nota</label><input id="g-nota" name="nota" maxlength="300" placeholder="Ej.: con maletín y 2 baterías"></div>
    <div class="mensaje mensaje-error oculto" role="alert"></div>
    <div class="acciones"><button type="button" class="btn btn-secundario" data-cerrar>Cancelar</button>
      <button class="btn btn-primario" type="submit">Registrar entrega</button></div></form>`);
  const formulario = modal.querySelector('form');
  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    try {
      const entrega = await Api.entregarArticulo(a.codigo, datosFormulario(formulario));
      Modal.cerrar();
      aviso(`${entrega.nombreArticulo} entregado a ${entrega.persona}.`);
      await vistaInventario();
    } catch (e) {
      mostrarErrorFormulario(formulario, e.message);
    }
  });
}

function formularioDevolucion(e) {
  const modal = Modal.abrir('Devolución de ' + e.nombreArticulo, `<form novalidate>
    <p class="suave">${esc(e.persona)} · ${e.cantidad} unidad(es) · entregado el ${fecha(e.fecha)}</p>
    <div class="campo"><span class="etiqueta">¿Cómo se devolvió? *</span><div class="casillas casillas-columna">
      <label><input type="radio" name="condicion" value="BUENA" checked> En buen estado</label>
      <label><input type="radio" name="condicion" value="DANADA"> Con daños (pasa a mantenimiento)</label>
      <label><input type="radio" name="condicion" value="EXTRAVIADO"> No se devolvió: extraviado</label></div></div>
    <div class="campo"><label for="d-nota">Nota</label><textarea id="d-nota" name="nota" maxlength="300" placeholder="Obligatoria si tiene daños o se extravió"></textarea></div>
    <div class="mensaje mensaje-error oculto" role="alert"></div>
    <div class="acciones"><button type="button" class="btn btn-secundario" data-cerrar>Cancelar</button>
      <button class="btn btn-primario" type="submit">Registrar devolución</button></div></form>`);
  const formulario = modal.querySelector('form');
  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    try {
      const datos = datosFormulario(formulario);
      await Api.devolverArticulo(e.id, datos.condicion === 'EXTRAVIADO' ? 'EXTRAVIADA' : datos.condicion, datos.nota);
      Modal.cerrar();
      aviso('Devolución registrada.');
      await vistaInventario();
    } catch (err) {
      mostrarErrorFormulario(formulario, err.message);
    }
  });
}

function historialArticulo(a) {
  Modal.abrir(`Historial de ${a.codigo} · ${a.nombre}`, `<div class="tabla-contenedor"><table><thead><tr><th>Entregado a</th><th>Cantidad</th>
    <th>Entrega</th><th>Devolución</th></tr></thead><tbody>
    ${[...a.entregas].reverse().map((e) => `<tr><td>${esc(e.persona)}${e.obra ? `<br><span class="pequeno suave">${esc(e.obra)}</span>` : ''}</td>
      <td>${e.cantidad}</td><td>${fecha(e.fecha)}<br><span class="pequeno suave">por ${esc(e.entregadoPor)}</span>${e.nota ? `<br><span class="pequeno">${esc(e.nota)}</span>` : ''}</td>
      <td>${e.abierta ? chip(e.vencida ? 'Vencida' : 'En uso', e.vencida ? 'rojo' : 'azul')
        : `${fecha(e.fechaDevolucion)} · ${chip(e.condicion, e.condicion === 'En buen estado' ? 'verde' : 'amarillo')}
          <br><span class="pequeno suave">recibió ${esc(e.recibidoPor)}</span>${e.notaDevolucion ? `<br><span class="pequeno">${esc(e.notaDevolucion)}</span>` : ''}`}</td></tr>`).join('')
      || '<tr><td colspan="4" class="vacio">Todavía no se ha entregado.</td></tr>'}</tbody></table></div>
    <div class="acciones"><button type="button" class="btn btn-primario" data-cerrar>Cerrar</button></div>`);
}
