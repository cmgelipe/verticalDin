/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package verticaldin;

import modelo.Tabla;
import modelo.BD;
import datos.AccesoDatos;
import java.io.File;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
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
                res = accesoXamana.ejecutarConsulta("select bd.nombre, direccion, puerto, gestor, usr, psw, "
                        + "Tabla.nombre, Tabla.umbral_op, Tabla.umbral_des from bd "
                        + "join tabla on tabla.id_BD=bd.id_BD "
                        + "join fragmento on fragmento.id_tabla=tabla.id_tabla "
                        + "where fragmento.id_tabla=" + token);
                accesoXamana.desconectar();
            } else {
                System.out.println("Sin conexión");
            }
            if (!isNullorEmpty(res)) {
                System.out.println("Datos encontrados");
                this.datosBD.setNombd("" + ((ArrayList) res.get(0)).get(0));
                this.datosBD.setDirbd("" + ((ArrayList) res.get(0)).get(1));
                this.datosBD.setPortbd("" + ((ArrayList) res.get(0)).get(2));
                this.datosBD.setTipoBD("" + ((ArrayList) res.get(0)).get(3));
                this.datosBD.setUsubd("" + ((ArrayList) res.get(0)).get(4));
                this.datosBD.setPassbd("" + ((ArrayList) res.get(0)).get(5));
                this.tabla = new Tabla();
                this.tabla.setIdTabla(token);
                this.tabla.setNombre("" + ((ArrayList) res.get(0)).get(6));
                this.tabla.setUmbralOP(Integer.parseInt("" + ((ArrayList) res.get(0)).get(7)));
                this.tabla.setUmbralCO(Integer.parseInt("" + ((ArrayList) res.get(0)).get(8)));
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
                System.out.println("Ninguna base de datos o tabla para el token dado");
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
            //Buscar si atributos en tabla original estan registrados en xamana
            AccesoDatos accExterno = new AccesoDatos(this.datosBD.getDirbd(), this.datosBD.getNombd(), this.datosBD.getUsubd(), this.datosBD.getPassbd());
            ArrayList<String> atts = null;
            if (accExterno.conectar()) {
                atts = accExterno.atributos(this.datosBD.getNombd(), this.tabla.getNombre());
                accExterno.desconectar();
            }
            ArrayList<String> nombresAtributosEnXamana = new ArrayList<>();
            this.tabla.getAtributos().forEach(x -> nombresAtributosEnXamana.add(x.getNombre()));
            if (!this.isNullorEmpty(atts)) {
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
                        String message = "El atributo " + nombre + ", agregado recientemente, es:";
                        Object[] params = {message, rbtn1, rbtn2, rbtn3};
                        int n = JOptionPane.showConfirmDialog(null, params, "Tipo de atributo", JOptionPane.DEFAULT_OPTION);
                        //Si es descriptor o multimedia, revisar si ya se encuentra agregado el otro, para lazar una nueva fragmentación
                        //y agregar este atributo a la base de datos
                        //en caso de no ser descriptor o multimedia, agregarlo directamente a la base de datos
                        if (!rbtn3.isSelected()) {
                            if (rbtn1.isSelected()) {
                                //revisar si ya hay un multimedia agregado a xamana en la tabla original
                                ArrayList res = null;
                                if (accesoXamana.conectar()) {
                                    res = accesoXamana.ejecutarConsulta("select nom, tipo, size from Atributo where multimedia=1 and id_tabla=" + this.token);
                                    accesoXamana.desconectar();
                                }
                                if (!this.isNullorEmpty(res)) {
                                    //registrar atributo en xamana
                                    int t = 0;
                                    if (this.accesoXamana.conectar()) {
                                        t = this.accesoXamana.ejecutarComando("insert into atributo (tipo,size,nom,descriptor,multimedia,id_tabla) values ('" + att.split(":")[1] + "'," + att.split(":")[2] + ",'" + att.split(":")[0] + "'," + 1 + "," + 0 + "," + this.token + ")");
                                        this.accesoXamana.desconectar();
                                    }
                                    if (t == 1) {
                                        System.out.println("Atributo insertado");
                                    } else {
                                        System.out.println("Atributo no insertado");
                                    }
                                    //buscar en log que sitio ocupó más estos atributos
                                    String sitio = buscaSitioXUsoAtributosMYSQL(nombre, "" + ((ArrayList) res.get(0)).get(0));
                                    //AQUI ME QUEDÉ
                                    //CONECTARME PARA GENERAR TABLA Y COLOCAR ATRIBUTOS CBIR (Y PK)

                                }
                            } else if (rbtn2.isSelected()) {

                            }
                        }else{
                            //atributo normal
                        } 
                    }
                }
            } else {
                System.out.println("Alguien borró los atributos de la tabla recientemente");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

                    }

                }
                /*if (!this.tabla.getFragmentos().get(i).isCbir()) {
                    if (linea.contains(this.tabla.getFragmentos().get(i).getNombre())) {//Si la linea contiene el nombre del fragmento en turno

                    }
                }*/
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public String allocateCBIR(String att1, String att2) {
        String sitioRet = "";
        try {
            RandomAccessFile raf = new RandomAccessFile(this.dirLogs, "r");
            raf.seek(this.caracterLog);
            while (raf.getFilePointer() < raf.length()) {
                String linea = raf.readLine();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sitioRet;
    }

    public String buscaSitioXUsoAtributosMYSQL(String att1, String att2) {
        /*
         Método para buscar en log el sitio que más ocupa atributos CBIR
         */
        String IPmasOcupada="";
        try {
            File carpeta = new File(dirLogs);
            File archivos[] = carpeta.listFiles();
            HashMap<String, ArrayList> NombreIpUsos = new HashMap<>();
            
            for (File item : archivos) {
                if (item.isFile()) {
                    RandomAccessFile raf = new RandomAccessFile(item.toString(), "r");
                    String ip = "127.0.0.1";
                    String nombre = "";
                    while (raf.getFilePointer() < raf.length()) {
                        String actual = raf.readLine();
                        if (actual.contains("Connect\t") && !actual.contains("denied")) {
                            String y[] = actual.split("\t");
                            ip = y[2].substring(y[2].indexOf("@") + 1, y[2].indexOf(" ", y[2].indexOf("@") + 1));
                            nombre = y[1].substring(0, y[1].indexOf(" "));
                            if (ip.toLowerCase().compareTo("localhost") == 0 || ip.toLowerCase().compareTo("::1") == 0) {
                                ip = "127.0.0.1";
                            }
                            ArrayList IpUso=new ArrayList<>();
                            IpUso.add(ip);  //IP
                            if(NombreIpUsos.get(nombre)!=null){
                                IpUso.add(((Integer)NombreIpUsos.get(nombre).get(1)));
                            }else{
                                IpUso.add(new Integer(0));
                            }
                            NombreIpUsos.put(nombre,IpUso);
                        }
                        if (actual.toLowerCase().contains(att1) || actual.toLowerCase().contains(att2)) {
                            String y[] = actual.split("\t");
                            nombre = y[1].substring(0, y[1].indexOf(" "));
                            NombreIpUsos.get(nombre).set(1,((Integer)NombreIpUsos.get(nombre).get(1))+1);
                        }

                    }
                }
            }
            ArrayList valores=new ArrayList<>(NombreIpUsos.values());
            int aux=0;
            for(int p=0;p<valores.size();p++){
                ArrayList aux2=(ArrayList)valores.get(p);
                if(((Integer)aux2.get(1))>aux){
                    aux=(Integer)aux2.get(1);
                    IPmasOcupada=""+aux2.get(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return IPmasOcupada;
    }

}
