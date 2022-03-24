/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package verticaldin;

import modelo.Tabla;
import modelo.BD;
import datos.AccesoDatosMy;
import datos.AccesoDatosPG;
import java.io.File;
import java.io.RandomAccessFile;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import modelo.Atributo;

/**
 *
 * @author pipe
 */
public class Vigilante {

    AccesoDatosMy accesoXamana;
    private String token;
    String miIP; //IP del nodo a donde esté el vigilante
    BD datosBD;
    long caracterLog;
    String dirLogs;
    int numeroFragmento;
    estadoHilo hilo;
    
    public Vigilante(String token, String DirLogs) {
        this.dirLogs = DirLogs;
        this.token = token;
        hilo=new estadoHilo();
        miIP = this.getIP();
        try {
            ArrayList res = null;
            ArrayList res3 = null;
            accesoXamana = new AccesoDatosMy();
            if (accesoXamana.conectar()) {
                res = accesoXamana.ejecutarConsulta("select bd.nombre, direccion, puerto, gestor, usr, psw, "
                        + "Tabla.nombre, Tabla.umbral_op, Tabla.umbral_des from bd "
                        + "join Tabla on Tabla.id_BD=bd.id_BD "
                        + "join fragmento on fragmento.id_tabla=tabla.id_tabla "
                        + "where fragmento.id_tabla=" + token);
                res3=accesoXamana.ejecutarConsulta("select id_atributo, tipo, size,nom, descriptor, multimedia from Atributo where id_tabla="+token+"");
                accesoXamana.desconectar();
            } else {
                System.out.println("Sin conexión");
            }
            if (!isNullorEmpty(res) && !this.isNullorEmpty(res3)) {
                System.out.println("Datos encontrados");
                this.datosBD.setNombd("" + ((ArrayList) res.get(0)).get(0));
                this.datosBD.setDirbd("" + ((ArrayList) res.get(0)).get(1));
                this.datosBD.setPortbd("" + ((ArrayList) res.get(0)).get(2));
                this.datosBD.setTipoBD("" + ((ArrayList) res.get(0)).get(3));
                this.datosBD.setUsubd("" + ((ArrayList) res.get(0)).get(4));
                this.datosBD.setPassbd("" + ((ArrayList) res.get(0)).get(5));
                Tabla tabla = new Tabla();
                tabla.setIdTabla(token);
                tabla.setNombre("" + ((ArrayList) res.get(0)).get(6));
                tabla.setUmbralOP(Integer.parseInt("" + ((ArrayList) res.get(0)).get(7)));
                tabla.setUmbralCO(Integer.parseInt("" + ((ArrayList) res.get(0)).get(8)));
                
                //buscar Atributos de tabla y agregarlos a la tabla
                ArrayList<Atributo> attTabla=new ArrayList<>();
                for(int k=0;k<res3.size();k++){
                    Atributo a=new Atributo();
                    a.setIdAtributo(""+((ArrayList)res3.get(k)).get(0));
                    a.setTipo(""+((ArrayList)res3.get(k)).get(1));
                    a.setTamanio(""+((ArrayList)res3.get(k)).get(2));
                    a.setNombre(""+((ArrayList)res3.get(k)).get(3));
                    boolean d=((""+((ArrayList)res3.get(k)).get(4)).compareTo("0")==0)?false:true;
                    a.setDescriptor(d);
                    d=((""+((ArrayList)res3.get(k)).get(5)).compareTo("0")==0)?false:true;
                    a.setMultimedia(d);
                    attTabla.add(a);
                }
                tabla.setAtributos(attTabla);
                tabla.llenaFragmentosxSitio(miIP);
                this.datosBD.setTabla(tabla);
                //Análisis de logs para detectar nuevos atributos (alter table add column), comparar atributos de base de xamana
                // para detectar el atributo nuevo, preguntar si es descriptor o multimedia, agregarlo a la base de xamana
                //si es descriptor o multimedia, hasta que este el duo de atributos separar en un nuevo fragmento.
                //Si no es ninguno de esos dos, entonces dejarlo en la tabla original.
                //Analizar si algun fragmento superó umbrales (incluida la tabla inicial)) mediante el analisis del log
                //MODIFICAR BASE DE DATOS XAMANA, agregando en atributo EL CAMPO cbir
                if (this.datosBD.getTipoBD().compareTo("a") == 0) {
                    //MYSQL

                    //¿Es el servidor original?
                    if (this.datosBD.getDirbd().compareTo(this.getIP()) == 0) {
                        //Abro socket
                        
                        hilo.start();
                        while (true) {
                            //La bandera de refragmentación indica cuando los fragmentos a analizar en los logs ya no son los mismos y es necesario 
                            //llenar nuevamente con los nuevos fragmentos que se analizaran.
                            if (hilo.banderaRefragmentacion) {
                                this.datosBD.getTabla().llenaFragmentosxSitio(miIP);
                                hilo.banderaRefragmentacion = false;
                            }
                            //revisar agregado de atributos solo en servidor original
                            nuevosAtributos();
                            //logMySQL();
                            Thread.sleep(300000);
                        }
                    } else {
                        //Abro socket
                        estadoHilo hilo=new estadoHilo();
                        hilo.start();
                        while (true) {
                            //La bandera de refragmentación indica cuando los fragmentos a analizar en los logs ya no son los mismos y es necesario 
                            //llenar nuevamente con los nuevos fragmentos que se analizaran.
                            if (hilo.banderaRefragmentacion) {
                                this.datosBD.getTabla().llenaFragmentosxSitio(miIP);
                                hilo.banderaRefragmentacion = false;
                            }
                            //revisar eliminado de atributos, solo para servidores secundarios
                            eliminadoAtributos();
                            //logMySQL();
                            Thread.sleep(300000);
                        }
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

    public boolean isNullorEmpty(String s) {
        return (s != null) ? ((!s.isEmpty()) ? false : true) : true;
    }

    protected String getIP() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    if (addr instanceof Inet6Address) {
                        continue;
                    }

                    ip = addr.getHostAddress();
                    System.out.println(ip);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ip;
    }

    private void logMySQL() {

        try {
            RandomAccessFile raf = new RandomAccessFile(this.dirLogs, "r");
            raf.seek(this.caracterLog);
            while (raf.getFilePointer() < raf.length()) {
                String linea = raf.readLine();
                this.caracterLog = raf.getFilePointer();

                for (int i = 0; i < this.datosBD.getTabla().getFragmentos().size(); i++) {
                    //Paso 1 identificar si hubo un alter de la tabla original para agregar columnas
                    if (linea.toLowerCase().contains("alter table") && linea.toLowerCase().contains(this.datosBD.getTabla().getNombre().toLowerCase())
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
        String IPmasOcupada = "";
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
                            ArrayList IpUso = new ArrayList<>();
                            IpUso.add(ip);  //IP
                            if (NombreIpUsos.get(nombre) != null) {
                                IpUso.add(((Integer) NombreIpUsos.get(nombre).get(1)));
                            } else {
                                IpUso.add(new Integer(0));
                            }
                            NombreIpUsos.put(nombre, IpUso);
                        }
                        if (actual.toLowerCase().contains(att1) || actual.toLowerCase().contains(att2)) {
                            String y[] = actual.split("\t");
                            nombre = y[1].substring(0, y[1].indexOf(" "));
                            NombreIpUsos.get(nombre).set(1, ((Integer) NombreIpUsos.get(nombre).get(1)) + 1);
                        }

                    }
                }
            }
            ArrayList valores = new ArrayList<>(NombreIpUsos.values());
            int aux = 0;
            for (int p = 0; p < valores.size(); p++) {
                ArrayList aux2 = (ArrayList) valores.get(p);
                if (((Integer) aux2.get(1)) > aux) {
                    aux = (Integer) aux2.get(1);
                    IPmasOcupada = "" + aux2.get(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return IPmasOcupada;
    }

    // Solo para servidor original (nodos no deben modificar sus atributos directamente)
    public void nuevosAtributos() {
        try {
            //Buscar si atributos en tabla original estan registrados en xamana
            AccesoDatosMy accExterno = new AccesoDatosMy(this.datosBD.getDirbd() + ":" + this.datosBD.getPortbd(), this.datosBD.getNombd(), this.datosBD.getUsubd(), this.datosBD.getPassbd());
            ArrayList<String> atts = null;
            if (accExterno.conectar()) {
                atts = accExterno.atributos(this.datosBD.getNombd(), this.datosBD.getTabla().getNombre());
                accExterno.desconectar();
            }
            ArrayList<String> nombresAtributosEnXamana = new ArrayList<>();
            this.datosBD.getTabla().getAtributos().forEach(x -> nombresAtributosEnXamana.add(x.getNombre()));
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
                            if (rbtn1.isSelected() || rbtn2.isSelected()) {
                                //revisar si ya hay un multimedia agregado a xamana en la tabla original
                                ArrayList res = null;
                                if (accesoXamana.conectar()) {
                                    if (rbtn1.isSelected()) {
                                        res = accesoXamana.ejecutarConsulta("select id_atributo, nom, tipo, size from Atributo where multimedia=1 and id_tabla=" + this.token);
                                    } else if (rbtn2.isSelected()) {
                                        res = accesoXamana.ejecutarConsulta("select id_atributo, nom, tipo, size from Atributo where descriptor=1 and id_tabla=" + this.token);
                                    }
                                    accesoXamana.desconectar();
                                }
                                if (!this.isNullorEmpty(res)) {
                                    //registrar atributo en xamana
                                    int t = 0;
                                    String idAtributo = "";
                                    if (this.accesoXamana.conectar()) {
                                        if (rbtn1.isSelected()) {
                                            t = this.accesoXamana.ejecutarComando("insert into Atributo (tipo,size,nom,descriptor,multimedia,id_tabla) values ('" + att.split(":")[1] + "'," + att.split(":")[2] + ",'" + att.split(":")[0] + "'," + 1 + "," + 0 + "," + this.token + ")");
                                            idAtributo = "" + ((ArrayList) this.accesoXamana.ejecutarConsulta("Select LAST_INSERT_ID()").get(0)).get(0);
                                        } else if (rbtn2.isSelected()) {
                                            t = this.accesoXamana.ejecutarComando("insert into Atributo (tipo,size,nom,descriptor,multimedia,id_tabla) values ('" + att.split(":")[1] + "'," + att.split(":")[2] + ",'" + att.split(":")[0] + "'," + 0 + "," + 1 + "," + this.token + ")");
                                            idAtributo = "" + ((ArrayList) this.accesoXamana.ejecutarConsulta("Select LAST_INSERT_ID()").get(0)).get(0);
                                        }
                                        if (t == 1 && !this.isNullorEmpty(idAtributo)) {
                                            System.out.println("Atributo insertado");
                                        } else {
                                            System.out.println("Atributo no insertado");
                                        }
                                        this.accesoXamana.desconectar();
                                    }
                                    //buscar en log que sitio ocupó más estos atributos
                                    String sitio = buscaSitioXUsoAtributosMYSQL(nombre, "" + ((ArrayList) res.get(0)).get(0));
                                    //origen
                                    AccesoDatosMy accO = new AccesoDatosMy(this.datosBD.getDirbd() + ":" + this.datosBD.getPortbd(), this.datosBD.getNombd(), this.datosBD.getUsubd(), this.datosBD.getPassbd());
                                    //Destino
                                    AccesoDatosMy accD = new AccesoDatosMy(sitio + ":" + this.datosBD.getPortbd(), this.datosBD.getNombd(), this.datosBD.getUsubd(), this.datosBD.getPassbd());
                                    //Atributos
                                    this.datosBD.getTabla().setAtributoLlave(this.llaveMySQL(accO, this.datosBD.getNombd(), this.datosBD.getTabla().getNombre()));
                                    Atributo att1 = new Atributo(idAtributo,nombre, att.split(":")[1], att.split(":")[1], (rbtn1.isSelected()) ? true : false, (rbtn2.isSelected()) ? true : false);
                                    Atributo att2 = new Atributo("" + ((ArrayList) res.get(0)).get(0),"" + ((ArrayList) res.get(0)).get(1), "" + ((ArrayList) res.get(0)).get(2), "" + ((ArrayList) res.get(0)).get(3), (rbtn1.isSelected()) ? true : false, (rbtn2.isSelected()) ? true : false);
                                    ArrayList<Atributo> atributos = new ArrayList<>(Arrays.asList(this.datosBD.getTabla().getAtributoLlave(), att1, att2));
                                    
                                    Fragmentador f = new Fragmentador(accO, accD, atributos, this.datosBD, this.numeroFragmento++, true);
                                    f.generarFragmentos();
                                    
                                }
                            }
                        } else {
                            //atributo normal, no hacer nada, se debe analizar el log
                        }
                    }
                }
            } else {
                System.out.println("Alguien borró los atributos de la tabla recientemente");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Atributo llavePostgres(AccesoDatosPG acc, String nombreBase, String nombreTabla) {

        Atributo att = null;
        ArrayList res = null;
        try {
            if (acc.conectar()) {
                String q = "select nombre, data_type "
                        + "from (SELECT column_name as nombre "
                        + "FROM information_schema.key_column_usage "
                        + "WHERE TABLE_NAME='" + nombreTabla + "' "
                        + "AND TABLE_CATALOG='" + nombreBase + "') as a "
                        + "join INFORMATION_SCHEMA.COLUMNS "
                        + "on INFORMATION_SCHEMA.COLUMNS.COLUMN_NAME=nombre "
                        + "where INFORMATION_SCHEMA.COLUMNS.TABLE_NAME='" + nombreTabla + "' "
                        + "and INFORMATION_SCHEMA.COLUMNS.TABLE_CATALOG='" + nombreBase + "';";
                res = acc.ejecutarConsulta(q);
                acc.desconectar();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (res != null) {
            att = new Atributo();
            att.setNombre("" + ((ArrayList) res.get(0)).get(0));
            att.setTipo("" + ((ArrayList) res.get(0)).get(1));
        }
        return att;
    }

    protected Atributo llaveMySQL(AccesoDatosMy acc, String nombreBase, String nombreTabla) {

        Atributo att = null;
        ArrayList res = null;
        try {
            if (acc.conectar()) {
                res = acc.ejecutarConsulta("SELECT COLUMN_NAME, DATA_TYPE, COLUMN_KEY FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = '" + nombreBase + "' AND TABLE_NAME = '" + nombreTabla + "' and COLUMN_KEY IN('PRI', 'UNI')");
                acc.desconectar();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (res != null) {
            att = new Atributo();
            att.setNombre("" + ((ArrayList) res.get(0)).get(0));
            att.setTipo("" + ((ArrayList) res.get(0)).get(1));
        }
        return att;
    }

    private void eliminadoAtributos() {
        
    }
}
