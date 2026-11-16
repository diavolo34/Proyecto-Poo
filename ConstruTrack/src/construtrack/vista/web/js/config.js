const CONFIG = {
  apiRemota: 'https://construtrack-api.onrender.com',

  sitiosEstaticos: ['netlify.app', 'github.io'],
};

if (location.hostname.endsWith('netlify.app')) document.documentElement.classList.add('con-insignia-netlify');

CONFIG.api = (() => {
  const parametros = new URLSearchParams(location.search);
  if (parametros.has('api')) localStorage.setItem('construtrack-api', parametros.get('api'));
  const forzada = localStorage.getItem('construtrack-api');
  if (forzada) return forzada;
  if (location.protocol === 'file:') return 'http://localhost:8080';
  const estatico = CONFIG.sitiosEstaticos.some((dominio) => location.hostname.endsWith(dominio));
  return estatico ? CONFIG.apiRemota : '';
})();
