package modelo;

/**
 * Línea de conexión de un sitio remoto (puerto, base de datos, gestor,
 * usuario, contraseña), tal como se consulta desde el registro de sitios en
 * la base de datos de XAMANA. El observer-fragmenter nunca trae estas
 * credenciales empaquetadas en el ejecutable: las consulta en tiempo de
 * ejecución con la conexión que ya tiene a XAMANA.
 *
 * @author pipe
 */
public class SitioConexion {

    private String direccion;
    private String puerto;
    private String basedatos;
    private String gestor;
    private String usuario;
    private String password;

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getPuerto() {
        return puerto;
    }

    public void setPuerto(String puerto) {
        this.puerto = puerto;
    }

    public String getBasedatos() {
        return basedatos;
    }

    public void setBasedatos(String basedatos) {
        this.basedatos = basedatos;
    }

    public String getGestor() {
        return gestor;
    }

    public void setGestor(String gestor) {
        this.gestor = gestor;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
