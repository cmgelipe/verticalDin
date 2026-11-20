package datos;

import java.util.ArrayList;
import modelo.SitioConexion;

/**
 * Consulta el registro de sitios (tabla {@code sitio_conexion}) en la base
 * de datos de XAMANA, usando la misma conexión que el observer-fragmenter ya
 * tiene hacia esa base. Ninguna credencial de un sitio remoto se guarda ni se
 * empaqueta en este ejecutable: se consulta en tiempo de ejecución.
 *
 * @author pipe
 */
public class SitioConexionDAO {

    /**
     * Devuelve la línea de conexión del sitio con esa dirección, o
     * {@code null} si no está registrado.
     */
    public SitioConexion obtenerConexion(String direccion) throws Exception {
        SitioConexion sitio = null;
        AccesoDatosMy acc = new AccesoDatosMy();
        if (acc.conectar()) {
            try {
                ArrayList res = acc.ejecutarConsultaParam(
                        "select direccion, puerto, basedatos, gestor, usuario, password from sitio_conexion where direccion=?",
                        direccion);
                if (res != null && !res.isEmpty()) {
                    ArrayList fila = (ArrayList) res.get(0);
                    sitio = new SitioConexion();
                    sitio.setDireccion(valor(fila, 0));
                    sitio.setPuerto(valor(fila, 1));
                    sitio.setBasedatos(valor(fila, 2));
                    sitio.setGestor(valor(fila, 3));
                    sitio.setUsuario(valor(fila, 4));
                    sitio.setPassword(valor(fila, 5));
                }
            } finally {
                acc.desconectar();
            }
        }
        return sitio;
    }

    private String valor(ArrayList fila, int indice) {
        Object v = fila.get(indice);
        return v == null ? null : ("" + v);
    }
}
