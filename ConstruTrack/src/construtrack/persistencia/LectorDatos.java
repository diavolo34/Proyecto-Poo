package construtrack.persistencia;

import construtrack.modelo.Plataforma;

/** Fuente desde la que se cargan los datos de la empresa al iniciar la aplicación. */
public interface LectorDatos {

    Plataforma cargar();
}
