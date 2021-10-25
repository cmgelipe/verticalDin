/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package verticaldin;

import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;

/**
 *
 * @author pipe
 */
public class Vigilante {

    AccesoDatos accesoXamana;
    Tabla tabla;
    private String token;
    String miIP; //IP del nodo a donde esté el vigilante
    BD datosBD;
    long caracterLog;
    String dirLogs;

    public Vigilante(String token, String DirLogs) {
        this.dirLogs = DirLogs;
        this.token = token;
        miIP = this.getIP();
        try {
            ArrayList res = null;
            if (accesoXamana.conectar()) {
                res = accesoXamana.ejecutarConsulta("select bd.nombre, direccion, puerto, gestor, usr, psw from bd "
                        + "join tabla on tabla.id_BD=bd.id_BD "
                        + "join fragmento on fragmento.id_tabla=tabla.id_tabla "
                        + "where fragmento.id_tabla=" + token);
                accesoXamana.desconectar();
            } else {
                System.out.println("Sin conexión");
            }
            if (!isNullorEmpty(res)) {
                System.out.println("Datos de BD encontrados");
                this.datosBD.setNombd("" + ((ArrayList) res.get(0)).get(0));
                this.datosBD.setDirbd("" + ((ArrayList) res.get(0)).get(1));
                this.datosBD.setPortbd("" + ((ArrayList) res.get(0)).get(2));
                this.datosBD.setTipoBD("" + ((ArrayList) res.get(0)).get(3));
                this.datosBD.setUsubd("" + ((ArrayList) res.get(0)).get(4));
                this.datosBD.setPassbd("" + ((ArrayList) res.get(0)).get(5));
                res = null;
                if (accesoXamana.conectar()) {
                    res = accesoXamana.ejecutarConsulta("select nombre, umbral_op, umbral_des from tabla where id_tabla=" + this.token);
                    accesoXamana.desconectar();
                }
                if (res != null) {
                    this.tabla = new Tabla();
                    this.tabla.setIdTabla(token);
                    this.tabla.setNombre("" + ((ArrayList) res.get(0)).get(0));
                    this.tabla.setUmbralOP(Integer.parseInt("" + ((ArrayList) res.get(0)).get(1)));
                    this.tabla.setUmbralCO(Integer.parseInt("" + ((ArrayList) res.get(0)).get(2)));
                    this.tabla.llenaFragmentosxSitio(miIP);
                    //Análisis de logs para detectar nuevos atributos (alter table add column), comparar atributos de base de xamana
                    // para detectar el atributo nuevo, preguntar si es descriptor o multimedia, agregarlo a la base de xamana
                    //si es descriptor o multimedia, hasta que este el duo de atributos separar en un nuevo fragmento.
                    //Si no es ninguno de esos dos, entonces dejarlo en la tabla original.
                    //Analizar si algun fragmento superó umbrales (incluida la tabla inicial)) mediante el analisis del log
                    //MODIFICAR BASE DE DATOS XAMANA, agregando en atributo EL CAMPO cbir
                    boolean banderaRefragmentacion = false;
                    if (this.datosBD.getTipoBD().compareTo("a") == 0) {
                        //MYSQL
                        while (true) {
                            //La bandera de refragmentación indica cuando los fragmentos a analizar en los logs ya no son los mismos y es necesario 
                            //llenar nuevamente con los nuevos fragmentos que se analizaran.
                            if (banderaRefragmentacion) {
                                this.tabla.llenaFragmentosxSitio(miIP);
                                banderaRefragmentacion = false;
                            }
                            logMySQL();
                            Thread.sleep(300000);
                        }
                    } else if (this.datosBD.getTipoBD().compareTo("b") == 0 || this.datosBD.getTipoBD().compareTo("c") == 0) {
                        //POSTGRES
                    } else {
                        //MONGO
                    }
                } else {
                    System.out.println("Ninguna tabla para el token dado");
                }
            } else {
                System.out.println("Ninguna base de datos para el token dado");
            }

        } catch (Exception e) {
            e.printStackTrace();
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

    private void logMySQL() {
        try {
            RandomAccessFile raf = new RandomAccessFile(this.dirLogs, "r");
            raf.seek(this.caracterLog);
            while (raf.getFilePointer() < raf.length()) {
                String linea = raf.readLine();
                this.caracterLog = raf.getFilePointer();
                for (int i = 0; i < this.tabla.getFragmentos().size(); i++) {
                    //Paso 1 identificar si hubo un alter de la tabla original para agregar columnas
                    if (linea.toLowerCase().contains("alter table") && linea.toLowerCase().contains(this.tabla.getNombre().toLowerCase())
                            && linea.toLowerCase().contains("add")) {
                        //conectarme a su base de datos para obtener el nuevo atributo
                        AccesoDatos accExterno = new AccesoDatos(this.datosBD.getDirbd(), this.datosBD.getNombd(), this.datosBD.getUsubd(), this.datosBD.getPassbd());
                        ArrayList<String> atts = null;
                        if (accExterno.conectar()) {
                            atts = accExterno.atributos(this.datosBD.getNombd(), this.tabla.getNombre());
                            accExterno.desconectar();
                        }
                        ArrayList<String> nombresAtributosEnXamana = new ArrayList<>();
                        this.tabla.getAtributos().forEach(x -> nombresAtributosEnXamana.add(x.getNombre()));
                        if (atts != null) {
                            for (String att : atts) {
                                String nombre = att.split(":")[0];
                                if (!nombresAtributosEnXamana.contains(nombre)) {
                                    JRadioButton rbtn1 = new JRadioButton("Descriptor", true);
                                    JRadioButton rbtn2 = new JRadioButton("Multimedia", false);
                                    JRadioButton rbtn3 = new JRadioButton("Ninguna opción anterior", false);
                                    ButtonGroup grupo1 = new ButtonGroup();
                                    grupo1.add(rbtn1);
                                    grupo1.add(rbtn2);
                                    grupo1.add(rbtn3);
                                    String message = "El atributo "+nombre+", agregado recientemente, es:";
                                    Object[] params = {message, rbtn1, rbtn2, rbtn3};
                                    int n = JOptionPane.showConfirmDialog(null, params, "Tipo de atributo", JOptionPane.DEFAULT_OPTION);
                                    //Si es descriptor o multimedia, revisar si ya se encuentra agregado el otro, para lazar una nueva fragmentación
                                    //y agregar este atributo a la base de datos
                                    //en caso de no ser descriptor o multimedia, agregarlo directamente a la base de datos
                                    //MODIFICAR BASE DE DATOS, AGRAGANDO A ATRIBUTO EL ID DE LA TABLA
                                    if(!rbtn3.isSelected()){
                                        if(rbtn1.isSelected()){
                                            //revisar si ya hay un multimedia agregado a xamana
                                            ArrayList res=null;
                                            if(accesoXamana.conectar()){
                                                res=accesoXamana.ejecutarConsulta("select nom, tipo, size where multimedia=1 and id_tabla="+this.token);
                                                accesoXamana.desconectar();
                                            }
                                            if(!this.isNullorEmpty(res)){
                                                //fragmentar
                                                //Se asignarán a donde más se ocupen
                                            }else{
                                                
                                            }
                                        }else if(rbtn2.isSelected()){
                                            
                                        }
                                    }
                                }
                            }
                        }

                    }
                    if (!this.tabla.getFragmentos().get(i).isCbir()) {
                        if (linea.contains(this.tabla.getFragmentos().get(i).getNombre())) {//Si la linea contiene el nombre del fragmento en turno

                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
