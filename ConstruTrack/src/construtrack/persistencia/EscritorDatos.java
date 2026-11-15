package construtrack.persistencia;

import construtrack.modelo.Empresa;

/**
 * Destino donde se guardan los cambios. Los servicios solo dependen de esta interfaz
 * (no saben si es un archivo, una base de datos o memoria): principio de inversión de dependencias.
 */
public interface EscritorDatos {

    void guardar(Empresa empresa);
}
