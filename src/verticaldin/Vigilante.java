/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package verticaldin;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pipe
 */
public class Vigilante {
    AccesoDatos accesoXamana;
    private ArrayList<String> idfragmentos = null;
    private String token;
    String miIP; //IP del nodo a donde esté el vigilante
    BD datosBD;
    public Vigilante(String token, String DirLogs){
        this.token = token;
        miIP=this.getIP();
        try {
            accesoXamana=new AccesoDatos();
            ArrayList res = null;
            if(accesoXamana.conectar()){
                //¿Que fragmentos estan en el sitio a donde el vigilante esta?
                res=accesoXamana.ejecutarConsulta("select id_fragmento from fragmento "
                        + "join sitio on sitio.id_sitio=fragmento.id_sitio where id_tabla=" + token + " "
                                + "and direccion='"+this.miIP+"'");
                accesoXamana.desconectar();
            }else{
                System.out.println("Sin conexión");
            }
            if(!isNullorEmpty(res)){
                System.out.println("Fragmentos encontrados");
                idfragmentos = new ArrayList<>();
                for (Object tupla : res) {
                    idfragmentos.add("" + ((ArrayList) tupla).get(0));
                }
                res=null;
                if(accesoXamana.conectar()){
                    res=accesoXamana.ejecutarConsulta("select bd.nombre, puerto, gestor, usr, psw from bd "
                        + "join tabla on tabla.id_BD=bd.id_BD "
                        + "join fragmento on fragmento.id_tabla=tabla.id_tabla "
                        + "where fragmento.id_tabla=" + token);
                    accesoXamana.desconectar();
                }else{
                    System.out.println("Sin conexión");
                }
                if(!isNullorEmpty(res)){
                    System.out.println("Datos de BD encontrados");
                    this.datosBD.setNombd(""+((ArrayList)res.get(0)).get(0));
                    this.datosBD.setPortbd(""+((ArrayList)res.get(0)).get(1));
                    this.datosBD.setTipoBD(""+((ArrayList)res.get(0)).get(2));
                    this.datosBD.setUsubd(""+((ArrayList)res.get(0)).get(3));
                    this.datosBD.setPassbd(""+((ArrayList)res.get(0)).get(4));
                }else{
                    System.out.println("Ninguna base de datos para el token dado");
                }
                //Análisis de logs
                if(this.datosBD.getTipoBD().compareTo("a")==0){
                    //MYSQL
                }else if(this.datosBD.getTipoBD().compareTo("b")==0 || this.datosBD.getTipoBD().compareTo("c")==0){
                    //POSTGRES
                }else{
                    //MONGO
                }
            }else{
                System.out.println("Ningún fragmento con el token dado en el sitio"+this.miIP+", revise el token o su sitio");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    public boolean isNullorEmpty(ArrayList arr) {
        boolean res = false;
        try {
            if (arr == null || arr.isEmpty()) {
                res = true;
            } else {
                res = false;
            }
        } catch (Exception e) {
            res = true;
        }
        return res;
    }
    protected String getIP() {
        String ipLocal = "";
        try {
            ipLocal = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
        }
        return ipLocal;
    }
}
