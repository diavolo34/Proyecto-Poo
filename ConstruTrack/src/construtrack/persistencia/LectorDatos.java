package construtrack.persistencia;

import construtrack.modelo.Empresa;

/** Fuente desde la que se cargan los datos de la empresa al iniciar la aplicación. */
public interface LectorDatos {

    Empresa cargar();
}
