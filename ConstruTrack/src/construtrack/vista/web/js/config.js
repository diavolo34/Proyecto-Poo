// Configuración de la conexión con el servidor Java (la API).
// Es el único archivo que hay que tocar al publicar la página en otro sitio.
const CONFIG = {
  // Servidor Java publicado en internet (Render). Si tu servicio tiene otra dirección, cámbiala aquí.
  apiRemota: 'https://construtrack-api.onrender.com',
  // Sitios que solo entregan archivos (no ejecutan Java): desde ellos se usa la API remota.
  sitiosEstaticos: ['netlify.app', 'github.io'],
};

// Netlify pone su insignia «Powered by Netlify» abajo a la derecha: los avisos suben para que no la tape.
if (location.hostname.endsWith('netlify.app')) document.documentElement.classList.add('con-insignia-netlify');

CONFIG.api = (() => {
  // Para pruebas se puede forzar otra API con ?api=http://servidor:puerto (queda guardada en este navegador)
  const parametros = new URLSearchParams(location.search);
  if (parametros.has('api')) localStorage.setItem('construtrack-api', parametros.get('api'));
  const forzada = localStorage.getItem('construtrack-api');
  if (forzada) return forzada;
  if (location.protocol === 'file:') return 'http://localhost:8080'; // abierto con doble clic
  const estatico = CONFIG.sitiosEstaticos.some((dominio) => location.hostname.endsWith(dominio));
  return estatico ? CONFIG.apiRemota : ''; // '' = el mismo servidor que entrega la página (NetBeans o Render)
})();
