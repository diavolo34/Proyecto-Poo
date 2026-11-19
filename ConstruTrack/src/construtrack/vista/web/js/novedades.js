const CAMPOS_NOVEDAD = {
  REPOSO_MEDICO: `<fieldset class="grupo-campos"><legend>Datos del reposo médico</legend>
      <div class="fila">
        <div class="campo"><label for="n-medico">Nombre del médico *</label><input id="n-medico" name="medico" autocomplete="off"></div>
        <div class="campo"><label for="n-matricula">Matrícula médica *</label><input id="n-matricula" name="matricula" placeholder="Ej.: MPPS 45821"></div>
      </div>
      <div class="campo"><label for="n-centro">Centro asistencial *</label><input id="n-centro" name="centro" placeholder="Ej.: Hospital Domingo Luciani"></div>
      <div class="campo"><span class="etiqueta">Tipo de consulta *</span><div class="casillas">
        <label><input type="radio" name="tipoConsulta" value="INTERNA" checked> Interna</label>
        <label><input type="radio" name="tipoConsulta" value="EMERGENCIA"> Por emergencia</label></div></div>
      ${Adjuntos.campo('n-archivo', 'Imagen o documento del reposo', true)}
    </fieldset>`,
  PERMISO_LABORAL: '',
  RETARDO: `<div class="campo"><label for="n-minutos">Minutos de retraso *</label><input id="n-minutos" name="minutos" type="number" min="1" max="600" value="15"></div>`,
  INASISTENCIA: '',
  ACCIDENTE_LABORAL: `<div class="campo"><label for="n-lugar">Lugar del proyecto *</label><input id="n-lugar" name="lugar" placeholder="Ej.: Andamio 3, piso 4"></div>
    <label class="casillas"><label><input type="checkbox" name="requirioAtencion" value="true"> Requirió atención médica</label></label>`,
};

const TEXTOS_CATEGORIA = {
  LICENCIA: {
    titulo: 'Permisos y reposos',
    registrar: 'Registrar permiso o reposo',
    boton: 'Registrar',
    lista: 'Permisos y reposos',
    vacio: 'No hay permisos ni reposos registrados.',
    ayuda: 'El supervisor los registra; quedan pendientes hasta que el gerente los aprueba o rechaza con una nota. '
      + 'Un reposo médico aprobado justifica esos días como «Incapacidad» y bloquea la marca de asistencia mientras dure.',
  },
  REPORTE_OBRA: {
    titulo: 'Reportes de proyecto',
    registrar: 'Nuevo reporte de proyecto',
    boton: 'Registrar reporte',
    lista: 'Reportes del proyecto',
    vacio: 'No hay reportes de proyecto.',
    ayuda: 'Los registra el supervisor (retardos, inasistencias y accidentes) y el gerente los aprueba o rechaza con una nota obligatoria.',
  },
};

async function vistaPermisos() {
  return vistaNovedades('LICENCIA');
}

async function vistaReportesObra() {
  return vistaNovedades('REPORTE_OBRA');
}

async function vistaNovedades(categoria) {
  const zona = contenido();
  const textos = TEXTOS_CATEGORIA[categoria];
  const [opciones, lista] = await Promise.all([Api.opcionesNovedades(categoria), Api.novedades(categoria)]);
  const filtro = estado['filtro' + categoria] || 'PENDIENTE';
  const visibles = lista.filter((n) => filtro === 'TODAS' || n.estado === filtro);
  const contar = (e) => lista.filter((n) => n.estado === e).length;
  const hoy = new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10);
  const formulario = opciones.puedeRegistrar ? `<form class="tarjeta" id="form-novedad" novalidate aria-labelledby="t-registrar">
      <h2 id="t-registrar">${esc(textos.registrar)}</h2>
      <div class="campo"><label for="codigoTipo">Tipo *</label><select id="codigoTipo" name="codigoTipo">
        ${Object.entries(opciones.tipos).map(([codigo, nombre]) => `<option value="${esc(codigo)}">${esc(nombre)}</option>`).join('')}</select></div>
      <div class="campo"><label for="n-persona">Persona *</label><select id="n-persona" name="cedula">
        ${opciones.personas.map((p) => `<option value="${esc(p.cedula)}" data-obra="${esc(p.obra || '')}">${esc(p.nombre)}${p.puesto ? ' · ' + esc(p.puesto) : ''}</option>`).join('')}</select></div>
      ${categoria === 'REPORTE_OBRA' ? `<div class="campo"><label for="n-obra">Proyecto</label><select id="n-obra" name="obra">
        <option value="">— El proyecto de la persona —</option>${opciones.obras.map((o) => `<option>${esc(o.nombre)}</option>`).join('')}</select></div>` : ''}
      <div class="fila">
        <div class="campo"><label for="n-desde">Desde *</label><input id="n-desde" name="desde" type="date" value="${hoy}"></div>
        <div class="campo"><label for="n-hasta">Hasta</label><input id="n-hasta" name="hasta" type="date"><span class="ayuda">Vacío = un día</span></div>
      </div>
      <div class="campo"><label for="n-descripcion">${categoria === 'LICENCIA' ? 'Motivo *' : 'Qué pasó *'}</label>
        <textarea id="n-descripcion" name="descripcion" maxlength="300" placeholder="${categoria === 'LICENCIA' ? 'Ej.: cita en el registro civil' : 'Describe brevemente lo ocurrido'}"></textarea></div>
      <div id="campos-tipo"></div>
      <div class="mensaje mensaje-error oculto" role="alert"></div>
      <button class="btn btn-primario btn-bloque" type="submit">${esc(textos.boton)}</button>
    </form>` : '';
  if (!zona.isConnected) return;
  zona.innerHTML = encabezado('', textos.titulo) + `
    <p class="suave" style="margin-top:-8px">${esc(textos.ayuda)}</p>
    <div class="${formulario ? 'rejilla-formulario' : ''}">
      ${formulario}
      <section class="tarjeta" aria-labelledby="t-lista"><div class="titulo-vista" style="margin:0 0 8px"><h2 id="t-lista" style="margin:0">${esc(textos.lista)}</h2>
        <div class="segmentos" role="group" aria-label="Filtrar por estado">${[['PENDIENTE', 'Pendientes'], ['APROBADA', 'Aprobados'],
          ['RECHAZADA', 'Rechazados'], ['TODAS', 'Todos']].map(([valor, texto]) => `<button type="button" class="segmento${filtro === valor ? ' activo' : ''}"
          aria-pressed="${filtro === valor}" data-filtro="${valor}">${texto}${valor !== 'TODAS' ? ` <span>${contar(valor)}</span>` : ''}</button>`).join('')}</div></div>
        <div class="tabla-contenedor"><table>
          <thead><tr><th>#</th><th>Tipo</th><th>Persona</th><th>Fechas</th><th>Estado</th><th>Detalle</th><th><span class="solo-lector">Acciones</span></th></tr></thead>
          <tbody>${visibles.map((n) => `<tr><td>${n.id}</td>
            <td>${esc(n.nombreTipo)}${n.obra ? `<br><span class="pequeno suave">${esc(n.obra)}</span>` : ''}</td>
            <td>${esc(n.nombreAfectado)}</td>
            <td style="white-space:nowrap">${fechaCorta(n.desde)}${n.hasta !== n.desde ? ' – ' + fechaCorta(n.hasta) : ''}<br>
              <span class="pequeno suave">${n.dias} día(s)</span></td>
            <td>${chipEstado(n.estadoNombre)}${n.revisadaPor ? `<br><span class="pequeno">${n.estado === 'APROBADA' ? 'Autorizado por' : 'Rechazado por'}
              <strong>${esc(n.revisadaPor)}</strong></span>` : ''}</td>
            <td class="pequeno">${esc(n.descripcion)}${n.detalle ? `<br><span class="suave">${esc(n.detalle)}</span>` : ''}
              ${n.nota ? `<br>Nota: «${esc(n.nota)}»` : ''}<br><span class="suave">Registró: ${esc(n.registradaPor)}</span>
              ${Adjuntos.boton(n.archivo, 'Ver reposo')}</td>
            <td style="white-space:nowrap">${n.puedoRevisar ? `<button class="btn btn-exito btn-chico" data-revisar="${n.id}" data-estado="APROBADA">Aprobar</button>
              <button class="btn btn-peligro btn-chico" data-revisar="${n.id}" data-estado="RECHAZADA">Rechazar</button>` : ''}</td></tr>`).join('')
            || `<tr><td colspan="7" class="vacio">${esc(filtro === 'PENDIENTE' ? 'No hay pendientes. Todo al día.' : textos.vacio)}</td></tr>`}</tbody>
        </table></div></section>
    </div>`;
  zona.querySelectorAll('[data-filtro]').forEach((b) => b.addEventListener('click', () => {
    estado['filtro' + categoria] = b.dataset.filtro;
    vistaNovedades(categoria);
  }));
  zona.querySelectorAll('[data-revisar]').forEach((b) => b.addEventListener('click', () =>
    revisarNovedad(lista.find((n) => n.id === Number(b.dataset.revisar)), b.dataset.estado, () => vistaNovedades(categoria))));
  if (!formulario) return;
  const form = $('#form-novedad');
  const tipo = $('#codigoTipo');
  const mostrarCampos = () => { $('#campos-tipo').innerHTML = CAMPOS_NOVEDAD[tipo.value] || ''; };
  tipo.addEventListener('change', mostrarCampos);
  mostrarCampos();
  form.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const boton = form.querySelector('button[type=submit]');
    boton.disabled = true;
    try {
      const datos = datosFormulario(form);
      delete datos[''];
      datos.categoria = categoria;
      const archivo = form.querySelector('#n-archivo');
      if (archivo) {
        if (!archivo.files[0]) throw new Error('Adjunta la imagen o el documento del reposo.');
        boton.textContent = 'Subiendo documento…';
        datos.archivo = (await Adjuntos.subir(archivo.files[0], datos.cedula, false)).id;
      }
      const nueva = await Api.registrarNovedad(datos);
      aviso(`${nueva.nombreTipo} #${nueva.id} registrado. Queda pendiente hasta que lo apruebe el gerente.`);
      await vistaNovedades(categoria);
    } catch (e) {
      mostrarErrorFormulario(form, e.message);
      boton.disabled = false;
      boton.textContent = textos.boton;
    }
  });
}

function revisarNovedad(n, nuevoEstado, recargar) {
  const aprobar = nuevoEstado === 'APROBADA';
  const modal = Modal.abrir(`${aprobar ? 'Aprobar' : 'Rechazar'} · ${n.nombreTipo} #${n.id}`, `<form novalidate>
    <p><strong>${esc(n.nombreAfectado)}</strong> · ${fecha(n.desde)}${n.hasta !== n.desde ? ' al ' + fecha(n.hasta) : ''}<br>
      <span class="suave pequeno">${esc(n.descripcion)}</span></p>
    <div class="campo"><label for="r-nota">Nota ${aprobar ? 'de aprobación' : 'del rechazo'} *</label>
      <textarea id="r-nota" name="nota" maxlength="300" placeholder="${aprobar ? 'Ej.: informe verificado con la clínica' : 'Ej.: falta la firma del médico'}"></textarea>
      <span class="ayuda">Obligatoria. La verá el supervisor que lo registró; quedará tu nombre como ${aprobar ? 'autorizador' : 'responsable del rechazo'}.</span></div>
    <div class="mensaje mensaje-error oculto" role="alert"></div>
    <div class="acciones"><button type="button" class="btn btn-secundario" data-cerrar>Cancelar</button>
      <button class="btn ${aprobar ? 'btn-exito' : 'btn-peligro'}" type="submit">${aprobar ? 'Aprobar' : 'Rechazar'}</button></div></form>`);
  const formulario = modal.querySelector('form');
  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    try {
      await Api.revisarNovedad(n.id, nuevoEstado, $('#r-nota').value);
      Modal.cerrar();
      aviso(`${n.nombreTipo} #${n.id} ${aprobar ? 'aprobado' : 'rechazado'}.`);
      await recargar();
    } catch (e) {
      mostrarErrorFormulario(formulario, e.message);
    }
  });
}

async function vistaTablero() {
  const zona = contenido();
  const lista = await Api.tablero();
  const columnas = [['PENDIENTE', 'Pendientes'], ['APROBADA', 'Aprobados'], ['RECHAZADA', 'Rechazados']];
  const filtro = estado.filtroTablero || '';
  const categoria = estado.categoriaTablero || '';
  const visibles = lista.filter((n) => (!categoria || n.categoria === categoria)
    && (!filtro || (n.nombreAfectado + ' ' + n.nombreTipo).toLowerCase().includes(filtro.toLowerCase())));
  const tarjeta = (n) => `<article class="tarjeta-kanban categoria-${n.categoria.toLowerCase()}" aria-label="${esc(n.nombreTipo)} ${n.id}">
      <p class="pequeno suave" style="margin:0">${esc(n.categoriaNombre)}</p>
      <h4>#${n.id} · ${esc(n.nombreTipo)}</h4><p>${esc(n.nombreAfectado)}</p>
      <p class="suave pequeno">${fecha(n.desde)}${n.hasta !== n.desde ? ' → ' + fecha(n.hasta) : ''}${n.obra ? ' · ' + esc(n.obra) : ''}</p>
      <p class="pequeno">${esc(n.descripcion)}</p>
      <p class="pequeno suave">Registró: ${esc(n.registradaPor)}${n.revisadaPor ? `<br>${n.estado === 'APROBADA' ? 'Autorizó' : 'Rechazó'}: ${esc(n.revisadaPor)}` : ''}</p>
      ${n.incapacidad ? chip('Incapacidad', 'morado') : ''} ${Adjuntos.boton(n.archivo, 'Ver reposo')}
      ${n.puedoRevisar ? `<div class="acciones">
        <button class="btn btn-exito btn-chico" data-revisar="${n.id}" data-estado="APROBADA">Aprobar</button>
        <button class="btn btn-peligro btn-chico" data-revisar="${n.id}" data-estado="RECHAZADA">Rechazar</button></div>` : ''}
    </article>`;
  if (!zona.isConnected) return;
  zona.innerHTML = encabezado('', 'Panel de control',
    `<label class="solo-lector" for="categoria-tablero">Mostrar</label>
     <select id="categoria-tablero" style="width:auto"><option value="">Todo</option>
       <option value="LICENCIA" ${categoria === 'LICENCIA' ? 'selected' : ''}>Permisos y reposos</option>
       <option value="REPORTE_OBRA" ${categoria === 'REPORTE_OBRA' ? 'selected' : ''}>Reportes de proyecto</option></select>
     <label class="solo-lector" for="filtro-tablero">Buscar en el panel de control</label>
     <input id="filtro-tablero" placeholder="Buscar persona o tipo…" value="${esc(filtro)}" style="width:220px">`) + `
    <p class="suave pequeno" style="margin-top:-8px">Los aprobados y rechazados salen del panel de control 3 horas después de revisarse.
      Siguen en el historial de cada persona y en <a href="#supervision">Supervisión → Reportes</a>.</p>
    <div class="kanban kanban-3">${columnas.map(([valor, titulo]) => {
      const tarjetas = visibles.filter((n) => n.estado === valor);
      return `<section class="kanban-columna" aria-label="${titulo}"><h3>${titulo} <span>${tarjetas.length}</span></h3>
        ${tarjetas.map(tarjeta).join('') || '<p class="suave pequeno" style="margin:4px">Nada por aquí</p>'}</section>`;
    }).join('')}</div>`;
  const buscador = $('#filtro-tablero');
  buscador.addEventListener('input', () => {
    estado.filtroTablero = buscador.value;
    vistaTablero().then(() => { const b = $('#filtro-tablero'); b.focus(); b.setSelectionRange(b.value.length, b.value.length); });
  });
  $('#categoria-tablero').addEventListener('change', (e) => { estado.categoriaTablero = e.target.value; vistaTablero(); });
  zona.querySelectorAll('[data-revisar]').forEach((b) => b.addEventListener('click', () =>
    revisarNovedad(lista.find((n) => n.id === Number(b.dataset.revisar)), b.dataset.estado, vistaTablero)));
}
