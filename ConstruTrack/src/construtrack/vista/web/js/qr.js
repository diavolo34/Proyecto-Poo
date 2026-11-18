const QR = (() => {
  const BLOQUES_ECC = [-1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26, 30, 22, 22, 24, 24, 28, 28, 26, 26, 26];
  const NUM_BLOQUES = [-1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5, 5, 8, 9, 9, 10, 10, 11, 13, 14, 16];
  const NIVEL_M = 0;

  const bit = (valor, i) => ((valor >>> i) & 1) !== 0;

  function modulosCrudos(version) {
    let total = (16 * version + 128) * version + 64;
    if (version >= 2) {
      const alineaciones = Math.floor(version / 7) + 2;
      total -= (25 * alineaciones - 10) * alineaciones - 55;
      if (version >= 7) total -= 36;
    }
    return total;
  }

  const palabrasDatos = (version) => Math.floor(modulosCrudos(version) / 8) - BLOQUES_ECC[version] * NUM_BLOQUES[version];

  function multiplicar(x, y) {
    let z = 0;
    for (let i = 7; i >= 0; i--) {
      z = (z << 1) ^ ((z >>> 7) * 0x11d);
      z ^= ((y >>> i) & 1) * x;
    }
    return z;
  }

  function divisor(grado) {
    const resultado = new Array(grado).fill(0);
    resultado[grado - 1] = 1;
    let raiz = 1;
    for (let i = 0; i < grado; i++) {
      for (let j = 0; j < resultado.length; j++) {
        resultado[j] = multiplicar(resultado[j], raiz);
        if (j + 1 < resultado.length) resultado[j] ^= resultado[j + 1];
      }
      raiz = multiplicar(raiz, 0x02);
    }
    return resultado;
  }

  function resto(datos, div) {
    const resultado = div.map(() => 0);
    for (const b of datos) {
      const factor = b ^ resultado.shift();
      resultado.push(0);
      div.forEach((coef, i) => { resultado[i] ^= multiplicar(coef, factor); });
    }
    return resultado;
  }

  function codificarDatos(bytes, version) {
    const bits = [];
    const agregar = (valor, largo) => { for (let i = largo - 1; i >= 0; i--) bits.push((valor >>> i) & 1); };
    agregar(4, 4);
    agregar(bytes.length, version <= 9 ? 8 : 16);
    bytes.forEach((b) => agregar(b, 8));
    const capacidad = palabrasDatos(version) * 8;
    agregar(0, Math.min(4, capacidad - bits.length));
    agregar(0, (8 - (bits.length % 8)) % 8);
    for (let relleno = 0xec; bits.length < capacidad; relleno ^= 0xec ^ 0x11) agregar(relleno, 8);
    const palabras = [];
    for (let i = 0; i < bits.length; i += 8) palabras.push(parseInt(bits.slice(i, i + 8).join(''), 2));
    return palabras;
  }

  function intercalar(datos, version) {
    const numBloques = NUM_BLOQUES[version];
    const largoEcc = BLOQUES_ECC[version];
    const crudas = Math.floor(modulosCrudos(version) / 8);
    const cortos = numBloques - (crudas % numBloques);
    const largoCorto = Math.floor(crudas / numBloques);
    const div = divisor(largoEcc);
    const bloques = [];
    for (let i = 0, k = 0; i < numBloques; i++) {
      const dat = datos.slice(k, k + largoCorto - largoEcc + (i < cortos ? 0 : 1));
      k += dat.length;
      const ecc = resto(dat, div);
      if (i < cortos) dat.push(0);
      bloques.push(dat.concat(ecc));
    }
    const resultado = [];
    for (let i = 0; i < bloques[0].length; i++) {
      bloques.forEach((bloque, j) => {
        if (i !== largoCorto - largoEcc || j >= cortos) resultado.push(bloque[i]);
      });
    }
    return resultado;
  }

  function matriz(version, palabras, mascara) {
    const tam = version * 4 + 17;
    const modulos = Array.from({ length: tam }, () => new Array(tam).fill(false));
    const funcion = Array.from({ length: tam }, () => new Array(tam).fill(false));
    const fijar = (x, y, oscuro) => { modulos[y][x] = oscuro; funcion[y][x] = true; };

    for (let i = 0; i < tam; i++) {
      fijar(6, i, i % 2 === 0);
      fijar(i, 6, i % 2 === 0);
    }
    const buscador = (x, y) => {
      for (let dy = -4; dy <= 4; dy++) {
        for (let dx = -4; dx <= 4; dx++) {
          const d = Math.max(Math.abs(dx), Math.abs(dy));
          if (x + dx >= 0 && x + dx < tam && y + dy >= 0 && y + dy < tam) fijar(x + dx, y + dy, d !== 2 && d !== 4);
        }
      }
    };
    buscador(3, 3);
    buscador(tam - 4, 3);
    buscador(3, tam - 4);
    if (version >= 2) {
      const n = Math.floor(version / 7) + 2;
      const paso = Math.ceil((version * 4 + 4) / (n * 2 - 2)) * 2;
      const posiciones = [6];
      for (let p = tam - 7; posiciones.length < n; p -= paso) posiciones.splice(1, 0, p);
      posiciones.forEach((x, i) => posiciones.forEach((y, j) => {
        if ((i === 0 && j === 0) || (i === 0 && j === n - 1) || (i === n - 1 && j === 0)) return;
        for (let dy = -2; dy <= 2; dy++) {
          for (let dx = -2; dx <= 2; dx++) fijar(x + dx, y + dy, Math.max(Math.abs(dx), Math.abs(dy)) !== 1);
        }
      }));
    }
    const formato = (m) => {
      const datos = (NIVEL_M << 3) | m;
      let r = datos;
      for (let i = 0; i < 10; i++) r = (r << 1) ^ ((r >>> 9) * 0x537);
      const bits = ((datos << 10) | r) ^ 0x5412;
      for (let i = 0; i <= 5; i++) fijar(8, i, bit(bits, i));
      fijar(8, 7, bit(bits, 6));
      fijar(8, 8, bit(bits, 7));
      fijar(7, 8, bit(bits, 8));
      for (let i = 9; i < 15; i++) fijar(14 - i, 8, bit(bits, i));
      for (let i = 0; i < 8; i++) fijar(tam - 1 - i, 8, bit(bits, i));
      for (let i = 8; i < 15; i++) fijar(8, tam - 15 + i, bit(bits, i));
      fijar(8, tam - 8, true);
    };
    formato(0);
    if (version >= 7) {
      let r = version;
      for (let i = 0; i < 12; i++) r = (r << 1) ^ ((r >>> 11) * 0x1f25);
      const bits = (version << 12) | r;
      for (let i = 0; i < 18; i++) {
        const a = tam - 11 + (i % 3);
        const b = Math.floor(i / 3);
        fijar(a, b, bit(bits, i));
        fijar(b, a, bit(bits, i));
      }
    }
    let i = 0;
    for (let derecha = tam - 1; derecha >= 1; derecha -= 2) {
      if (derecha === 6) derecha = 5;
      for (let v = 0; v < tam; v++) {
        for (let j = 0; j < 2; j++) {
          const x = derecha - j;
          const y = ((derecha + 1) & 2) === 0 ? tam - 1 - v : v;
          if (!funcion[y][x] && i < palabras.length * 8) {
            modulos[y][x] = bit(palabras[i >>> 3], 7 - (i & 7));
            i++;
          }
        }
      }
    }
    const regla = [
      (x, y) => (x + y) % 2 === 0, (x, y) => y % 2 === 0, (x) => x % 3 === 0, (x, y) => (x + y) % 3 === 0,
      (x, y) => (Math.floor(x / 3) + Math.floor(y / 2)) % 2 === 0, (x, y) => ((x * y) % 2) + ((x * y) % 3) === 0,
      (x, y) => (((x * y) % 2) + ((x * y) % 3)) % 2 === 0, (x, y) => (((x + y) % 2) + ((x * y) % 3)) % 2 === 0,
    ][mascara];
    for (let y = 0; y < tam; y++) {
      for (let x = 0; x < tam; x++) if (!funcion[y][x] && regla(x, y)) modulos[y][x] = !modulos[y][x];
    }
    formato(mascara);
    return modulos;
  }

  function penalizacion(m) {
    const tam = m.length;
    let puntos = 0;
    for (let eje = 0; eje < 2; eje++) {
      for (let a = 0; a < tam; a++) {
        let racha = 1;
        for (let b = 1; b <= tam; b++) {
          const igual = b < tam && (eje ? m[b][a] === m[b - 1][a] : m[a][b] === m[a][b - 1]);
          if (igual) racha++;
          else {
            if (racha >= 5) puntos += racha - 2;
            racha = 1;
          }
        }
      }
    }
    for (let y = 0; y < tam - 1; y++) {
      for (let x = 0; x < tam - 1; x++) {
        if (m[y][x] === m[y][x + 1] && m[y][x] === m[y + 1][x] && m[y][x] === m[y + 1][x + 1]) puntos += 3;
      }
    }
    const oscuros = m.flat().filter(Boolean).length;
    puntos += Math.floor(Math.abs(oscuros * 20 - tam * tam * 10) / (tam * tam)) * 10;
    return puntos;
  }

  function modulos(texto) {
    const bytes = Array.from(new TextEncoder().encode(texto));
    let version = 1;
    while (version < BLOQUES_ECC.length && palabrasDatos(version) * 8 < 4 + (version <= 9 ? 8 : 16) + bytes.length * 8) version++;
    if (version >= BLOQUES_ECC.length) throw new Error('El texto es demasiado largo para el código QR.');
    const palabras = intercalar(codificarDatos(bytes, version), version);
    let mejor = null;
    let menor = Infinity;
    for (let mascara = 0; mascara < 8; mascara++) {
      const candidata = matriz(version, palabras, mascara);
      const puntos = penalizacion(candidata);
      if (puntos < menor) {
        menor = puntos;
        mejor = candidata;
      }
    }
    return mejor;
  }

  function svg(texto, etiqueta) {
    const m = modulos(texto);
    const borde = 4;
    const lado = m.length + borde * 2;
    let trazo = '';
    m.forEach((fila, y) => fila.forEach((oscuro, x) => { if (oscuro) trazo += `M${x + borde} ${y + borde}h1v1h-1z`; }));
    return `<svg class="codigo-qr" viewBox="0 0 ${lado} ${lado}" role="img" aria-label="${etiqueta || 'Código QR'}" shape-rendering="crispEdges">`
      + `<rect width="${lado}" height="${lado}" fill="#fff"/><path d="${trazo}" fill="#000"/></svg>`;
  }

  return { modulos, svg };
})();

if (typeof module !== 'undefined') module.exports = QR;
