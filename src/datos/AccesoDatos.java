/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package datos;

import java.util.ArrayList;

/**
 *
 * @author pipe
 */
public abstract class AccesoDatos {
    public abstract boolean conectar() throws Exception;
    public abstract void desconectar() throws Exception;
    public abstract ArrayList<String> atributos(String nombreBase, String nombre);
}
